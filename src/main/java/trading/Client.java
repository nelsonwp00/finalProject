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
        System.out.println("Action : " + action);

        final int n = 1;
        final CountDownLatch latch = new CountDownLatch(n);
        final long start = System.currentTimeMillis();

        switch (action) {
            case "CreateAccount":
                assert (args.length == 5);
                int balance = Integer.parseInt(args[4]);
                assert (balance >= 0);
                createAccount(cliClientService, leader, latch, args[3], balance);
                latch.await();
                System.out.println("Completed Operation : " + action + ". Latency = " + (System.currentTimeMillis() - start) + "ms");
                break;
            case "SendPayment":
                assert (args.length == 6);
                int payment = Integer.parseInt(args[5]);
                assert (payment > 0);
                sendPayment(cliClientService, leader, latch, args[3], args[4], payment);
                latch.await();
                System.out.println("Completed Operation : " + action + ". Latency = " + (System.currentTimeMillis() - start) + "ms");
                break;
            case "QueryAccount":
                assert (args.length == 4);
                queryAccount(cliClientService, leader, latch, args[3]);
                latch.await();
                System.out.println("Completed Operation : " + action + ". Latency = " + (System.currentTimeMillis() - start) + "ms");
                break;
            case "NonStopCreateAccount":
                assert (args.length == 3);
                nonStopCreateAccount(cliClientService, leader);
                break;
            case "NonStopSendPayment":
                assert (args.length == 3);
                nonStopSendPayment(cliClientService, leader);
                break;
            case "NonStopQueryAccount":
                assert (args.length == 3);
                nonStopQueryAccount(cliClientService, leader);
                break;
        }
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

                try {
                    nonStopCreateAccount(cliClientService, leader);
                } catch (RemotingException | InterruptedException e) {
                    throw new RuntimeException(e);
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

                try {
                    nonStopSendPayment(cliClientService, leader);
                } catch (RemotingException | InterruptedException e) {
                    throw new RuntimeException(e);
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

                try {
                    nonStopQueryAccount(cliClientService, leader);
                } catch (RemotingException | InterruptedException e) {
                    throw new RuntimeException(e);
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
