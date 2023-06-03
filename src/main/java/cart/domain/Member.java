package cart.domain;

import cart.exception.payment.PaymentException;

public class Member {
    private Long id;
    private String email;
    private String password;
    private Point point;

    public Member(Long id, String email, String password, Point point) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.point = point;
    }

    public Member(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public Member(String email, String password, Point point) {
        this.email = email;
        this.password = password;
        this.point = point;
    }

    public void usePoint(Point usePoint) {
        if (point.isSmallerThan(usePoint)) {
            throw new PaymentException("보유한 포인트가 사용하고자 하는 포인트보다 적습니다.");
        }
        this.point = this.point.subtract(usePoint);
    }

    public void earnPoint(Point earningPoint) {
        this.point = this.point.add(earningPoint);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Point getPoint() {
        return point;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", point=" + point +
                '}';
    }
}
