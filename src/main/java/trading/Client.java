package trading;/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RemotingException;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.InvokeCallback;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import trading.rpc.GrpcHelper;
import trading.rpc.TradingOutter.CreateAccountRequest;
import trading.rpc.TradingOutter.QueryRequest;
import trading.rpc.TradingOutter.SendPaymentRequest;
import trading.rpc.TradingOutter.ValueResponse;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public class Client {
    private static int operationTimeout = 5000;
    public static void main(final String[] args) throws Exception {
        System.out.println("CreateAccount Usage : provide args {GroupId} {Conf} {Action} {AccountID} {Balance}");
        System.out.println("SendPayment Usage : provide args {GroupId} {Conf} {Action} {From AccountID} {To AccountID} {Balance}");
        System.out.println("QueryAccount Usage : provide args {GroupId} {Conf} {Action} {AccountID}");
        System.out.println("NonStopCreateAccount Usage : provide args {GroupId} {Conf} {Action} {Interval}");

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


        final String action = args[2];
        String fromAccount = null;
        String toAccount = null;
        int amount = 0;
        System.out.println("Action : " + action);

        if (action.equals("CreateAccount") && args.length == 5) {
            fromAccount = args[3];
            amount = Integer.parseInt(args[4]);
            assert (amount >= 0);
            handleOperation(cliClientService, leader, action, fromAccount, null, amount);
        }
        else if (action.equals("SendPayment") && args.length == 6) {
            fromAccount = args[3];
            toAccount = args[4];
            amount = Integer.parseInt(args[5]);
            assert (amount > 0);
            handleOperation(cliClientService, leader, action, fromAccount, toAccount, amount);
        }
        else if (action.equals("QueryAccount") && args.length == 4) {
            fromAccount = args[3];
            handleOperation(cliClientService, leader, action, fromAccount, null, 0);
        }
        else if (action.equals("NonStopCreateAccount") && args.length == 4) {
            long interval = Long.parseLong(args[3]);
            new Timer().scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    try {
                        nonStopCreateAccount(cliClientService, leader);
                    } catch (RemotingException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            },0,interval);
        }
        else if (action.equals("NonStopSendPayment") && args.length == 4) {
            long interval = Long.parseLong(args[3]);
            new Timer().scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    try {
                        nonStopSendPayment(cliClientService, leader);
                    } catch (RemotingException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            },0,interval);
        }
        else if (action.equals("NonStopQueryAccount") && args.length == 4) {
            long interval = Long.parseLong(args[3]);
            new Timer().scheduleAtFixedRate(new TimerTask(){
                @Override
                public void run(){
                    try {
                        nonStopQueryAccount(cliClientService, leader);
                    } catch (RemotingException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            },0,interval);
        }
        else {
            System.out.println("Action does not have enough args.");
            System.exit(0);
        }
    }

    private static void handleOperation(
            final CliClientServiceImpl cliClientService,
            final PeerId leader,
            final String operation,
            final String fromAccount,
            final String toAccount,
            final int amount) throws InterruptedException, RemotingException
    {
        final int n = 1;
        final CountDownLatch latch = new CountDownLatch(n);

        //System.out.println("Leader is " + leader + ". Number of Operation = " + n);

        final long start = System.currentTimeMillis();

        switch (operation) {
            case "CreateAccount":
                createAccount(cliClientService, leader, latch, fromAccount, amount);
                break;
            case "SendPayment":
                sendPayment(cliClientService, leader, latch, fromAccount, toAccount, amount);
                break;
            case "QueryAccount":
                queryAccount(cliClientService, leader, latch, fromAccount);
                break;
        }

        latch.await();
        System.out.println("Completed Operation : " + operation + ". Latency = " + (System.currentTimeMillis() - start) + "ms");
    }

    private static void createAccount(
            final CliClientServiceImpl cliClientService,
            final PeerId leader,
            CountDownLatch latch,
            final String accountID,
            final int balance) throws RemotingException, InterruptedException
    {
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setAccountID(accountID)
                .setBalance(balance)
                .build();

        InvokeCallback callback = new InvokeCallback() {
            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    latch.countDown();
                    ValueResponse response = (ValueResponse) result;
                    if (response.getSuccess())
                        System.out.println("Operation : Create Account : Success" + ". Account Balance = $" + response.getBalance());
                    else
                        System.out.println("Operation : Create Account : Fail. " + response.getErrorMsg());
                } else {
                    latch.countDown();
                    System.out.println("Operation : Create Account : Timeout ");
                }
            }

            @Override
            public Executor executor() {
                return null;
            }
        };

        cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, callback, operationTimeout);
    }

    private static void nonStopCreateAccount(final CliClientServiceImpl cliClientService, final PeerId leader) throws RemotingException, InterruptedException
    {
        String accountID = UUID.randomUUID().toString();
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setAccountID(accountID)
                .setBalance(0)
                .build();

        long startTime = System.currentTimeMillis();
        InvokeCallback callback = new InvokeCallback() {
            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    long latency = System.currentTimeMillis() - startTime;
                    ValueResponse response = (ValueResponse) result;
                    if (response.getSuccess())
                        System.out.println("Operation : Create Account : Success. Latency = " + latency + "ms");
                    else
                        System.out.println("Operation : Create Account : Fail. Latency = " + latency + "ms");
                }
                else {
                    System.out.println("Operation : Create Account : Timeout");
                }
            }

            @Override
            public Executor executor() {
                return null;
            }
        };

        cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, callback, operationTimeout);
    }

    private static void sendPayment(
            final CliClientServiceImpl cliClientService,
            final PeerId leader,
            CountDownLatch latch,
            final String fromAccountID,
            final String toAccountID,
            final int amount) throws RemotingException, InterruptedException
    {
        SendPaymentRequest request = SendPaymentRequest.newBuilder()
                .setFromAccountID(fromAccountID)
                .setToAccountID(toAccountID)
                .setAmount(amount)
                .build();

        InvokeCallback callback = new InvokeCallback() {
            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    latch.countDown();
                    ValueResponse response = (ValueResponse) result;
                    if (response.getSuccess())
                        System.out.println("Operation : Send Payment : Success. Account Balance = $ " + response.getBalance());
                    else
                        System.out.println("Operation : Send Payment : Fail. " + response.getErrorMsg());
                } else {
                    err.printStackTrace();
                    latch.countDown();
                }
            }

            @Override
            public Executor executor() {
                return null;
            }
        };

        cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, callback, operationTimeout);
    }

    private static void nonStopSendPayment(final CliClientServiceImpl cliClientService, final PeerId leader) throws RemotingException, InterruptedException
    {
        int max = 16;
        int min = 1;
        String fromAccountID = "acc" + (min + (int) (Math.random() * ((max - min) + 1)));
        String toAccountID = "acc" + (min + (int) (Math.random() * ((max - min) + 1)));
        //System.out.println(fromAccountID + " " + toAccountID);

        SendPaymentRequest request = SendPaymentRequest.newBuilder()
                .setFromAccountID(fromAccountID)
                .setToAccountID(toAccountID)
                .setAmount(0)
                .build();

        long startTime = System.currentTimeMillis();
        InvokeCallback callback = new InvokeCallback() {
            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    long latency = System.currentTimeMillis() - startTime;
                    ValueResponse response = (ValueResponse) result;
                    if (response.getSuccess())
                        System.out.println("Operation : Send Payment : Success. Latency = " + latency + "ms");
                    else
                        System.out.println("Operation : Send Payment : Fail. Latency = " + latency + "ms");
                }
                else {
                    System.out.println("Operation : Send Payment : Timeout");
                }
            }

            @Override
            public Executor executor() {
                return null;
            }
        };

        cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, callback, operationTimeout);
    }

    private static void queryAccount(
            final CliClientServiceImpl cliClientService,
            final PeerId leader,
            CountDownLatch latch,
            final String accountID) throws RemotingException, InterruptedException
    {
        QueryRequest request = QueryRequest.newBuilder()
                .setAccountID(accountID)
                .build();

        InvokeCallback callback = new InvokeCallback() {
            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    latch.countDown();
                    ValueResponse response = (ValueResponse) result;
                    if (response.getSuccess())
                        System.out.println("Operation : Query Account : Success" + ". Account Balance = $" + response.getBalance());
                    else
                        System.out.println("Operation : Query Account : Fail. " + response.getErrorMsg());
                } else {
                    err.printStackTrace();
                    latch.countDown();
                }
            }

            @Override
            public Executor executor() {
                return null;
            }
        };

        cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, callback, operationTimeout);
    }

    private static void nonStopQueryAccount(final CliClientServiceImpl cliClientService, final PeerId leader) throws RemotingException, InterruptedException
    {
        int max = 16;
        int min = 1;
        String accountID = "acc" + (min + (int) (Math.random() * ((max - min) + 1)));
        //System.out.println(accountID);

        QueryRequest request = QueryRequest.newBuilder()
                .setAccountID(accountID)
                .build();

        long startTime = System.currentTimeMillis();
        InvokeCallback callback = new InvokeCallback() {
            @Override
            public void complete(Object result, Throwable err) {
                if (err == null) {
                    long latency = System.currentTimeMillis() - startTime;
                    ValueResponse response = (ValueResponse) result;
                    if (response.getSuccess())
                        System.out.println("Operation : Query Account : Success. Latency = " + latency + "ms");
                    else
                        System.out.println("Operation : Query Account : Fail. Latency = " + latency + "ms");
                }
                else {
                    System.out.println("Operation : Query Account : Timeout");
                }
            }

            @Override
            public Executor executor() {
                return null;
            }
        };

        cliClientService.getRpcClient().invokeAsync(leader.getEndpoint(), request, callback, operationTimeout);
    }
}
