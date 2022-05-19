package trading.rpc;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import trading.TradingClosure;
import trading.TradingService;
import trading.rpc.TradingOutter.QueryRequest;

public class QueryRequestProcessor implements RpcProcessor<QueryRequest> {
    private final TradingService tradingService;

    public QueryRequestProcessor(TradingService tradingService) {
        super();
        this.tradingService = tradingService;
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final QueryRequest request) {
        final TradingClosure closure = new TradingClosure() {
            @Override
            public void run(Status status) {
                rpcCtx.sendResponse(getValueResponse());
            }
        };

        this.tradingService.queryAccount(
                request.getAccountID(),
                closure
        );
    }

    @Override
    public String interest() {
        return QueryRequest.class.getName();
    }
}
