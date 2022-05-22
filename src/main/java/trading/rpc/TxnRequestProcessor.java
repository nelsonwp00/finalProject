package trading.rpc;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import trading.TradingClosure;
import trading.TradingService;
import trading.rpc.DLTOutter.TxnRequest;

public class TxnRequestProcessor implements RpcProcessor<TxnRequest> {
    private final TradingService tradingService;

    public TxnRequestProcessor(TradingService tradingService) {
        super();
        this.tradingService = tradingService;
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final TxnRequest request) {
        final TradingClosure closure = new TradingClosure() {
            @Override
            public void run(Status status) {
                rpcCtx.sendResponse(getTxnResponse());
            }
        };

        this.tradingService.sendTransaction(
                request.getTxnHash(),
                closure
        );
    }

    @Override
    public String interest() {
        return TxnRequest.class.getName();
    }
}
