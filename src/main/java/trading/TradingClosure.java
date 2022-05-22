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

import com.alipay.sofa.jraft.Closure;
import trading.rpc.DLTOutter.TxnResponse;
import trading.rpc.TradingOutter.ValueResponse;

public abstract class TradingClosure implements Closure {
    private ValueResponse valueResponse;
    private TradingOperation tradingOperation;

    private TxnResponse txnResponse;

    public void setTradingOperation(TradingOperation tradingOperation) {
        this.tradingOperation = tradingOperation;
    }

    public TradingOperation getTradingOperation() {
        return tradingOperation;
    }

    public ValueResponse getValueResponse() {
        return valueResponse;
    }

    public TxnResponse getTxnResponse() {
        return txnResponse;
    }

    public void setValueResponse(ValueResponse valueResponse) {
        this.valueResponse = valueResponse;
    }

    public void setTxnResponse(TxnResponse txnResponse) {
        this.txnResponse = txnResponse;
    }

    protected void failure(final String errorMsg, final String redirect) {
        final ValueResponse response = ValueResponse.newBuilder()
                .setBalance(-1)
                .setSuccess(false)
                .setErrorMsg(errorMsg)
                .setRedirect(redirect)
                .build();

        setValueResponse(response);
    }

    protected void txnFailure(final String txnHash, final String errorMsg, final String redirect) {
        final TxnResponse response = TxnResponse.newBuilder()
                .setTxnHash(txnHash)
                .setTxnOrder(-1)
                .setSuccess(false)
                .setErrorMsg(errorMsg)
                .setRedirect(redirect)
                .build();

        setTxnResponse(response);
    }

    protected void success(final int balance) {
        final ValueResponse response = ValueResponse.newBuilder()
                .setBalance(balance)
                .setSuccess(true)
                .build();

        setValueResponse(response);
    }

    protected void txnSuccess(final String txnHash, final int order) {
        final TxnResponse response = TxnResponse.newBuilder()
                .setTxnHash(txnHash)
                .setTxnOrder(order)
                .setSuccess(true)
                .build();

        setTxnResponse(response);
    }
}
