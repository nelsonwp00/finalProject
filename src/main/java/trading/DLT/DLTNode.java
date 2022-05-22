package trading.DLT;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RemotingException;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.InvokeCallback;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import trading.rpc.DLTOutter.TxnRequest;
import trading.rpc.DLTOutter.TxnResponse;
import trading.rpc.GrpcHelper;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class DLTNode {
    private static CliClientServiceImpl cliClientService;
    private static PeerId leader;

    private static int operationTimeout = 5000;

    private static LinkedList<Block> blockchain = new LinkedList<Block>(); // thread-safe
    private static byte[] prevHash; // thread-safe

    private static final int blockSize = Block.blockSize;

    private static MessageDigest digest; // for SHA-256 Hash

    private static HashMap<Integer, String> orderTxnMap; // Map<Transaction, Transaction Order>

    private static AtomicInteger txnIndex = new AtomicInteger(1); // the smallest correct order of txn

    private static ConcurrentHashMap<String, String> txnClientMap; // Map<Transaction, Client Address>

    private static ConcurrentHashMap<String, PrintWriter> clientOutMap; // Map<Client Address, Client Output Stream>


    public static void main(final String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Not enough arguments. Usage : provide args {GroupId} {Conf} {Port}\n");
            System.exit(0);
        }

        init(args[0], args[1]);

        start(Integer.parseInt(args[2])); // Start waiting for client
    }

    private static void init(final String groupId, final String confStr) throws Exception {
        GrpcHelper.initGRpc();

        final Configuration conf = new Configuration();

        if (!conf.parse(confStr)) {
            throw new IllegalArgumentException("Fail to parse conf:" + confStr);
        }

        RouteTable.getInstance().updateConfiguration(groupId, conf);

        cliClientService = new CliClientServiceImpl();
        cliClientService.init(new CliOptions());

        if (!RouteTable.getInstance().refreshLeader(cliClientService, groupId, 1000).isOk()) {
            throw new IllegalStateException("Refresh leader failed");
        }

        leader = RouteTable.getInstance().selectLeader(groupId);

        digest = MessageDigest.getInstance("SHA-256");

        orderTxnMap = new HashMap<>();

        // Create the Gensis Block
        createBlock(new ArrayList<String>(), true);

        txnClientMap = new ConcurrentHashMap<>();

        clientOutMap = new ConcurrentHashMap<>();
    }

    private static void start(final int PORT) throws IOException {
        ServerSocket nodeSocket = new ServerSocket(PORT);

        System.out.println("Node starts. Waiting for clients ...\n");

        while (true) {
            Socket clientSocket = nodeSocket.accept();

            Thread thread = new Thread(() -> {
                try {
                    String clientAddress = clientSocket.getInetAddress() + ":"+ clientSocket.getPort();
                    System.out.println("Connected to Client " + clientAddress);

                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    Scanner in = new Scanner(clientSocket.getInputStream());

                    clientOutMap.put(clientAddress, out);

                    while (in.hasNextLine()) {
                        String txn = in.nextLine();

                        if (txn.equals("exit")) {
                            System.out.println("Client : " + clientAddress + " exits.");
                            out.close();
                            in.close();
                            break;
                        }

                        System.out.println("Client submits txn : " + txn);
                        txnClientMap.put(txn, clientAddress);

                        sendTransaction(cliClientService, leader, txn);
                    }
                }
                catch (IOException | RemotingException | InterruptedException e) {
                    System.out.println("Thread Error : " + e.getMessage());
                }
            });

            thread.start();
        }
    }

    private synchronized static void createBlock(ArrayList<String> txns, boolean isGensis) {
        Block block = new Block();

        String txnHashes = String.join("", txns);
        if (isGensis)
            txnHashes = UUID.randomUUID().toString();

        block.hash = digest.digest(txnHashes.getBytes(StandardCharsets.UTF_8));

        if (!isGensis)
            block.prevHash = prevHash;

        block.timeStamp = LocalDateTime.now();

        block.transactions = txns;

        String blockHashString = convertSHA256toString(block.hash);
        int blockIndex = blockchain.size();

        System.out.println("Form Block " + blockIndex + ". Block Hash : " + blockHashString);
        System.out.println("Block Timestamp : " + block.timeStamp);

        int order = 1;
        if (!isGensis) {
            System.out.println("Previous Block Hash : " + convertSHA256toString(block.prevHash));
            for (String txn : txns) {
                System.out.println("Txn " + order++ + " : " + txn);
                notifyClient(txn, blockHashString, blockIndex);
            }
        }

        blockchain.add(block);

        prevHash = block.hash;
    }

    private static String convertSHA256toString(byte[] sha256Hash) {
        BigInteger number = new BigInteger(1, sha256Hash);

        StringBuilder hexString = new StringBuilder(number.toString(16));

        while (hexString.length() < 64)
            hexString.insert(0, '0');

        return hexString.toString();
    }

    private static void notifyClient(final String txn, final String blockHash, final int blockIndex) {
        String clientID = txnClientMap.get(txn);
        PrintWriter out = clientOutMap.get(clientID);

        if (out != null) {
            out.println("Txn " + txn + " is included in the ledger at Block " + blockIndex + ". Block Hash : " + blockHash);
        }
        else {
            System.out.println("notifyClient: " + clientID + " output stream not found.");
        }
    }

    private static void sendTransaction(
            final CliClientServiceImpl cliClientService,
            final PeerId leader,
            final String txn) throws RemotingException, InterruptedException
    {
        TxnRequest request = TxnRequest.newBuilder().setTxnHash(txn).build();

        InvokeCallback callback = new InvokeCallback() {
            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    TxnResponse response = (TxnResponse) result;
                    if (response.getSuccess())
                        handleResponse(response.getTxnHash(), response.getTxnOrder());
                    else
                        System.out.println("Send Transaction : Fail. " + " Txn Hash : " + response.getTxnHash() + ". " + response.getErrorMsg());
                } else {
                    err.printStackTrace();
                }
            }

            @Override
            public Executor executor() {
                return null;
            }
        };

        cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, callback, operationTimeout);
    }

    private synchronized static void handleResponse(final String _txn, final int txnOrder) {
        System.out.println("Send Transaction : Success. Txn : " + _txn + ". Order : " + txnOrder);

        orderTxnMap.put(txnOrder, _txn);

        int currentIndex = txnIndex.get();

        ArrayList<String> txns = new ArrayList<>(blockSize);

        for (int order = currentIndex; order < currentIndex + blockSize; order++) {
            String txn = orderTxnMap.getOrDefault(order, null);

            // not enough transactions (in correct order) to form a block
            if (txn == null) return;

            txns.add(txn);
        }

        createBlock(txns, false);

        for (int order = currentIndex; order < currentIndex + blockSize; order++) {
            orderTxnMap.remove(order);
        }

        // Update txnIndex
        txnIndex.getAndAdd(blockSize);
    }

}
