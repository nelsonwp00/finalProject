package trading.rpc;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.rpc.RpcContext;
import com.alipay.sofa.jraft.rpc.RpcProcessor;
import trading.TradingClosure;
import trading.TradingService;
import trading.rpc.TradingOutter.SendPaymentRequest;

public class SendPaymentRequestProcessor implements RpcProcessor<SendPaymentRequest> {
    private final TradingService tradingService;

    public SendPaymentRequestProcessor(TradingService tradingService) {
        super();
        this.tradingService = tradingService;
    }

    @Override
    public void handleRequest(final RpcContext rpcCtx, final SendPaymentRequest request) {
        final TradingClosure closure = new TradingClosure() {
            @Override
            public void run(Status status) {
                rpcCtx.sendResponse(getValueResponse());
            }
        };

        this.tradingService.sendPayment(
                request.getFromAccountID(),
                request.getToAccountID(),
                request.getAmount(),
                closure
        );
    }

    @Override
    public String interest() {
        return SendPaymentRequest.class.getName();
    }
}
