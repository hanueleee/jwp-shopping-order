package cart.domain;

import cart.exception.AuthorizationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Order {
    private Long id;
    private Member member;
    private List<OrderItem> orderItems;
    private LocalDateTime orderDateTime;

    public Order(Member member, List<OrderItem> orderItems) {
        this.member = member;
        this.orderItems = orderItems;
    }

    public Order(Long id, Member member, LocalDateTime orderDateTime) {
        this.id = id;
        this.member = member;
        this.orderDateTime = orderDateTime;
    }

    public Order(Long id, Member member, List<OrderItem> orderItems, LocalDateTime orderDateTime) {
        this.id = id;
        this.member = member;
        this.orderItems = orderItems;
        this.orderDateTime = orderDateTime;
    }

    public Payment calculatePayment(Point usePoint) {
        int totalProductPrice = 0;
        for (OrderItem orderItem : orderItems) {
            totalProductPrice += orderItem.calculatePrice();
        }
        int deliveryFee = DeliveryPolicy.calculateDeliveryFee(totalProductPrice);
        PointPolicy.usePoint(member, usePoint);
        PointPolicy.earnPoint(member, totalProductPrice);
        int totalPrice = totalProductPrice + deliveryFee - usePoint.getValue();

        return new Payment(totalProductPrice, deliveryFee, usePoint.getValue(), totalPrice);
    }

    public void checkOwner(Member member) {
        if (!Objects.equals(this.member.getId(), member.getId())) {
            throw new AuthorizationException("해당 사용자의 주문이 아닙니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public LocalDateTime getOrderDateTime() {
        return orderDateTime;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", member=" + member +
                ", orderItems=" + orderItems +
                '}';
    }
}
