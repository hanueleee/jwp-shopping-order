package cart.domain;

import cart.exception.AuthorizationException;
import java.util.Objects;

public class CartItem {
    private Long id;
    private int quantity;
    private final Product product;
    private final Member member;

    public CartItem(Member member, Product product) {
        this.quantity = 1;
        this.member = member;
        this.product = product;
    }

    public CartItem(Long id, int quantity, Product product, Member member) {
        this.id = id;
        this.quantity = quantity;
        this.product = product;
        this.member = member;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void checkOwner(Member member) {
        if (!Objects.equals(this.member.getId(), member.getId())) {
            throw new AuthorizationException(
                    "Illegal member attempts to cart; cartItemId=" + this.id + ", memberId=" + member.getId());
        }
    }

    public void changeQuantity(int quantity) {
        this.quantity = quantity;
    }
}
