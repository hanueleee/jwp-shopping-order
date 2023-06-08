package cart.order.exception.order;

import cart.order.exception.order.OrderException;

public class ProductInfoDoesNotMatchException extends OrderException {
    private static final String MESSAGE = "product 정보가 일치하지 않습니다. 다시 확인해주세요.";

    public ProductInfoDoesNotMatchException() {
        super(MESSAGE);
    }
}
