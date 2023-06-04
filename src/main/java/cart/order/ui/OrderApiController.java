package cart.order.ui;

import cart.common.dto.OrderDto;
import cart.common.dto.OrderRequest;
import cart.member.application.Member;
import cart.order.application.OrderService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderApiController {
    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Void> order(Member member, @RequestBody OrderRequest orderRequest) {
        System.out.println("OrderApiController.order");
        Long orderId = orderService.order(member, orderRequest);
        System.out.println("OrderApiController.order");
        return ResponseEntity.created(URI.create("/orders/" + orderId)).build();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderDetail(Member member, @PathVariable Long orderId) {
        OrderDto orderDto = orderService.getOrderDetail(member, orderId);
        return ResponseEntity.ok().body(orderDto);
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrderList(Member member) {
        List<OrderDto> orderDtos = orderService.getOrderList(member);
        return ResponseEntity.ok().body(orderDtos);
    }
}
