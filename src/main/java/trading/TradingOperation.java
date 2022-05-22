/*
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
package trading;

import java.io.Serializable;

public class TradingOperation implements Serializable {
    private static final long serialVersionUID = -6597003954824547294L;

    public static final byte CREATE_ACCOUNT = 0x03;

    public static final byte SEND_PAYMENT = 0x04;

    public static final byte QUERY_ACCOUNT = 0x05;

    public static final byte Send_Txn = 0x06;

    private byte op;

    private String fromAccountID;

    private String toAccountID;

    private int amount;

    private String txnHash;

    public static TradingOperation createCreate_Account(final String fromAccountID, final int amount) {
        return new TradingOperation(CREATE_ACCOUNT, fromAccountID, amount);
    }

    public static TradingOperation createSend_Payment(final String fromAccountID, final String toAccountID, final int amount) {
        return new TradingOperation(SEND_PAYMENT, fromAccountID, toAccountID, amount);
    }

    public static TradingOperation createQuery_Account(final String fromAccountID) {
        return new TradingOperation(QUERY_ACCOUNT, fromAccountID);
    }

    public static TradingOperation createSend_Transaction(final String txnHash) {
        return new TradingOperation(Send_Txn, txnHash, true);
    }

    public TradingOperation(byte op, String fromAccountID, int amount) {
        this.op = op;
        this.fromAccountID = fromAccountID;
        this.amount = amount;
    }

    public TradingOperation(byte op, String fromAccountID, String toAccountID, int amount) {
        this.op = op;
        this.fromAccountID = fromAccountID;
        this.toAccountID = toAccountID;
        this.amount = amount;
    }

    public TradingOperation(byte op, String fromAccountID) {
        this.op = op;
        this.fromAccountID = fromAccountID;
    }

    public TradingOperation(byte op, String txnHash, boolean isTxn) {
        this.op = op;
        this.txnHash = txnHash;
    }

    public byte getOp() {
        return op;
    }

    public String getFromAccountID() {
        return fromAccountID;
    }

    public String getToAccountID() {
        return toAccountID;
    }

    public int getAmount() {
        return amount;
    }

    public String getTxnHash() { return txnHash; }
}
