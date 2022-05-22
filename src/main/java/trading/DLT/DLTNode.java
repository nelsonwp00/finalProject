package trading.DLT;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import trading.rpc.GrpcHelper;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class DLTNode {
    private static int operationTimeout = 5000;

    private static LinkedList<Block> blockchain = new LinkedList<Block>();

    private static final int blockSize = Block.blockSize;

    private static byte[] prevHash;

    private static MessageDigest digest; // for SHA-256 Hash

    private static int minOrderRecv = 0;

    // ConcurrentHashMap<Order, Transaction Hash>
    private static ConcurrentHashMap<Integer, String> orderTxnMap = new ConcurrentHashMap<>();

    public static void main(final String[] args) throws Exception {
        System.out.println("CreateAccount Usage : provide args {GroupId} {Conf} {Action} {AccountID} {Balance}");

        final String groupId = args[0];
        final String confStr = args[1];
        GrpcHelper.initGRpc();
        final Configuration conf = new Configuration();
        if (!conf.parse(confStr)) {
            throw new IllegalArgumentException("Fail to parse conf:" + confStr);
        }
        RouteTable.getInstance().updateConfiguration(groupId, conf);
        final CliClientServiceImpl cliClientService = new CliClientServiceImpl();
        cliClientService.init(new CliOptions());
        if (!RouteTable.getInstance().refreshLeader(cliClientService, groupId, 1000).isOk()) {
            throw new IllegalStateException("Refresh leader failed");
        }
        final PeerId leader = RouteTable.getInstance().selectLeader(groupId);

        digest = MessageDigest.getInstance("SHA-256");

        // Create the Gensis Block
        createBlock(new ArrayList<String>(), true);

        final String operation = args[2];
        System.out.println("Operation : " + operation);

        final int n = 1;
        final CountDownLatch latch = new CountDownLatch(n);
        final long start = System.currentTimeMillis();

        switch (operation) {
            case "SendTransaction":
                break;
        }
    }

    private synchronized static String createBlock(ArrayList<String> txns, boolean isGensis) {
        Block block = new Block();

        String txnHashes = String.join("", txns);
        if (isGensis)
            txnHashes = UUID.randomUUID().toString();

        block.hash = digest.digest(txnHashes.getBytes(StandardCharsets.UTF_8));

        if (!isGensis)
            block.prevHash = prevHash;

        block.transactions = txns;

        blockchain.add(block);

        prevHash = block.hash;

        return convertSHA256toString(block.hash);
    }

    private static String convertSHA256toString(byte[] sha256Hash) {
        BigInteger number = new BigInteger(1, sha256Hash);

        StringBuilder hexString = new StringBuilder(number.toString(16));

        while (hexString.length() < 64)
            hexString.insert(0, '0');

        return hexString.toString();
    }
}
