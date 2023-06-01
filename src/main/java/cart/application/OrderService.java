package cart.application;

import cart.domain.Member;
import cart.domain.Order;
import cart.domain.OrderItem;
import cart.domain.Payment;
import cart.domain.Point;
import cart.domain.Product;
import cart.dto.CartItemDto;
import cart.dto.OrderRequest;
import cart.exception.OrderException;
import cart.exception.PaymentException;
import cart.repository.OrderRepository;
import cart.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public Long order(Member member, OrderRequest orderRequest) {
        /**
         * 검증해야 할 것
         * 1. request 속 product와 db 속 product가 같은지
         * 2. product의 재고
         * 3. request 속 payment와 비즈니스 로직을 통해 계산한 payment가 같은지
         */
        List<CartItemDto> cartItems = orderRequest.getCartItems();

        List<Product> productsInRequest = cartItems.stream()
                .map(it -> it.getProduct())
                .map(it -> new Product(it.getProductId(), it.getName(), it.getPrice(), it.getImageUrl(), it.getStock()))
                .collect(Collectors.toList());
        List<Long> productIds = cartItems.stream()
                .map(it -> it.getProduct().getProductId())
                .collect(Collectors.toList());
        List<Product> productsInDb = productRepository.getProductsByIds(productIds);

        // 1. request와 db가 같은지 (stock제외)
        if (!productsInRequest.equals(productsInDb)) {
            throw new OrderException("product 정보가 업데이트 되었습니다. 다시 확인해주세요.");
        }

        // 2. 재고가 충분한지 + product stock 업데이트
        int size = cartItems.size();
        List<OrderItem> orderItems = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            orderItems.add(OrderItem.of(productsInRequest.get(i), cartItems.get(i).getQuantity()));
        }

        // 3. point 쓸 수 있는지, payment가 제대로 됐는지 + member(point) 업데이트
        Order order = new Order(member, orderItems);
        Payment payment = order.calculatePayment(new Point(orderRequest.getUsePoint()));
        validatePayment(orderRequest, payment);

        // 주문 성공시 -> cart_item 싹 지우기, member(point) 업데이트, product 싹 업데이트, order 저장, order_item 싹 저장, payment저장
        List<Long> cartItemIds = cartItems.stream()
                .map(it -> it.getCartItemId())
                .collect(Collectors.toList());
        return orderRepository.order(cartItemIds, member, order, payment);
    }

    private static void validatePayment(OrderRequest orderRequest, Payment payment) {
        if (payment.getTotalProductPrice() != orderRequest.getTotalProductPrice()) {
            throw new PaymentException("상품의 총 금액이 다릅니다."
                    + " 입력된 금액 : "+Integer.toString(orderRequest.getTotalProductPrice())
                    + " 실제 금액 : "+ Integer.toString(payment.getTotalProductPrice()));
        }
        if (payment.getTotalDeliveryFee() != orderRequest.getTotalDeliveryFee()) {
            throw new PaymentException("총 배달비가 다릅니다.");
        }
        if (payment.getTotalPrice() != orderRequest.getTotalPrice()) {
            throw new PaymentException("주문의 총 금액이 다릅니다.");
        }
    }
}
