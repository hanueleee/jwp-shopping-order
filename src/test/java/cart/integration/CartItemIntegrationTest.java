package cart.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import cart.dao.MemberDao;
import cart.domain.Member;
import cart.domain.Point;
import cart.domain.Product;
import cart.dto.CartItemDto;
import cart.dto.CartItemQuantityUpdateRequest;
import cart.dto.CartItemRequest;
import cart.dto.ProductRequest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class CartItemIntegrationTest extends IntegrationTest {


    @Autowired
    private MemberDao memberDao;

    private Long productId;
    private Long productId2;
    private Member member;
    private Member member2;

    @BeforeEach
    void setUp() {
        super.setUp();

        productId = createProduct(new ProductRequest("치킨", 10_000, "http://example.com/chicken.jpg", 30));
        productId2 = createProduct(new ProductRequest("피자", 15_000, "http://example.com/pizza.jpg", 30));

        member = memberDao.getMemberById(1L);
        member2 = memberDao.getMemberById(2L);

    }

    @Test
    void testtest() {
        List<Product> list1 = List.of(
                new Product(1L, "p1", 1000, "url1", 10),
                new Product(2L, "p2", 2000, "url2", 20),
                new Product(3L, "p3", 3000, "url3", 30)
        );

        List<Product> list2 = List.of(
                new Product(1L, "p1", 1000, "url1", 100),
                new Product(2L, "p2", 2000, "url2", 200),
                new Product(3L, "p3", 3000, "url5", 300)
        );

        System.out.println(list1.equals(list2));
    }

    @DisplayName("[장바구니 추가] 장바구니에 아이템을 추가한다.")
    @Test
    void addCartItem() {
        CartItemRequest cartItemRequest = new CartItemRequest(productId);
        ExtractableResponse<Response> response = requestAddCartItem(member, cartItemRequest);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    @DisplayName("[장바구니 추가] 잘못된 사용자 정보로 장바구니에 아이템을 추가 요청시 실패한다.")
    @Test
    void addCartItemByIllegalMember() {
        Member illegalMember = new Member(member.getId(), member.getEmail(), member.getPassword() + "asdf",
                new Point(0));
        CartItemRequest cartItemRequest = new CartItemRequest(productId);
        ExtractableResponse<Response> response = requestAddCartItem(illegalMember, cartItemRequest);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("[장바구니 목록 조회] 사용자가 담은 장바구니 아이템을 조회한다.")
    @Test
    void getCartItems() {
        Long cartItemId1 = requestAddCartItemAndGetId(member, productId);
        Long cartItemId2 = requestAddCartItemAndGetId(member, productId2);

        ExtractableResponse<Response> response = requestGetCartItems(member);
        List<CartItemDto> cartItems = response.as(new TypeRef<>() {
        });

        assertThat(cartItems).isNotEmpty();
    }

    @DisplayName("[장바구니 수량 수정] 장바구니에 담긴 아이템의 수량을 변경한다.")
    @Test
    void increaseCartItemQuantity() {
        Long cartItemId = requestAddCartItemAndGetId(member, productId);

        ExtractableResponse<Response> response = requestUpdateCartItemQuantity(member, cartItemId, 10);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        ExtractableResponse<Response> cartItemsResponse = requestGetCartItems(member);

//        Optional<CartItemResponse> selectedCartItemResponse = cartItemsResponse.jsonPath()
//                .getList(".", CartItemResponse.class)
//                .stream()
//                .filter(cartItemResponse -> cartItemResponse.getCartItemId().equals(cartItemId))
//                .findFirst();
//
//        assertThat(selectedCartItemResponse.isPresent()).isTrue();
//        assertThat(selectedCartItemResponse.get().getQuantity()).isEqualTo(10);
    }

    @DisplayName("[장바구니 수량 수정] 장바구니에 담긴 아이템의 수량을 0으로 변경하면, 장바구니에서 아이템이 삭제된다.")
    @Test
    void decreaseCartItemQuantityToZero() {
        Long cartItemId = requestAddCartItemAndGetId(member, productId);

        ExtractableResponse<Response> response = requestUpdateCartItemQuantity(member, cartItemId, 0);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

        ExtractableResponse<Response> cartItemsResponse = requestGetCartItems(member);

//        Optional<CartItemResponse> selectedCartItemResponse = cartItemsResponse.jsonPath()
//                .getList(".", CartItemResponse.class)
//                .stream()
//                .filter(cartItemResponse -> cartItemResponse.getCartItemId().equals(cartItemId))
//                .findFirst();
//
//        assertThat(selectedCartItemResponse.isPresent()).isFalse();
    }

    @DisplayName("[장바구니 수량 수정] 다른 사용자가 담은 장바구니 아이템의 수량을 변경하려 하면 실패한다.")
    @Test
    void updateOtherMembersCartItem() {
        Long cartItemId = requestAddCartItemAndGetId(member, productId);

        ExtractableResponse<Response> response = requestUpdateCartItemQuantity(member2, cartItemId, 10);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @DisplayName("[장바구니 삭제] 장바구니에 담긴 아이템을 삭제한다.")
    @Test
    void removeCartItem() {
        Long cartItemId = requestAddCartItemAndGetId(member, productId);

        ExtractableResponse<Response> response = requestDeleteCartItem(cartItemId);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

        ExtractableResponse<Response> cartItemsResponse = requestGetCartItems(member);
//
//        Optional<CartItemResponse> selectedCartItemResponse = cartItemsResponse.jsonPath()
//                .getList(".", CartItemResponse.class)
//                .stream()
//                .filter(cartItemResponse -> cartItemResponse.getCartItemId().equals(cartItemId))
//                .findFirst();

//        assertThat(selectedCartItemResponse.isPresent()).isFalse();
    }

    private Long createProduct(ProductRequest productRequest) {
        ExtractableResponse<Response> response = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(productRequest)
                .when()
                .post("/products")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract();

        return getIdFromCreatedResponse(response);
    }

    private long getIdFromCreatedResponse(ExtractableResponse<Response> response) {
        return Long.parseLong(response.header("Location").split("/")[2]);
    }

    private ExtractableResponse<Response> requestAddCartItem(Member member, CartItemRequest cartItemRequest) {
        return given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .auth().preemptive().basic(member.getEmail(), member.getPassword())
                .body(cartItemRequest)
                .when()
                .post("/cart-items")
                .then()
                .log().all()
                .extract();
    }

    private Long requestAddCartItemAndGetId(Member member, Long productId) {
        ExtractableResponse<Response> response = requestAddCartItem(member, new CartItemRequest(productId));
        return getIdFromCreatedResponse(response);
    }

    private ExtractableResponse<Response> requestGetCartItems(Member member) {
        return given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .auth().preemptive().basic(member.getEmail(), member.getPassword())
                .when()
                .get("/cart-items")
                .then()
                .log().all()
                .extract();
    }

    private ExtractableResponse<Response> requestUpdateCartItemQuantity(Member member, Long cartItemId, int quantity) {
        CartItemQuantityUpdateRequest quantityUpdateRequest = new CartItemQuantityUpdateRequest(quantity);
        return given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .auth().preemptive().basic(member.getEmail(), member.getPassword())
                .when()
                .body(quantityUpdateRequest)
                .patch("/cart-items/{cartItemId}", cartItemId)
                .then()
                .log().all()
                .extract();
    }

    private ExtractableResponse<Response> requestDeleteCartItem(Long cartItemId) {
        return given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .auth().preemptive().basic(member.getEmail(), member.getPassword())
                .when()
                .delete("/cart-items/{cartItemId}", cartItemId)
                .then()
                .log().all()
                .extract();
    }
}
