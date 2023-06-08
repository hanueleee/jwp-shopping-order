package cart.acceptance;

import static cart.acceptance.CartItemSteps.장바구니_상품_수량_수정_요청;
import static cart.acceptance.CartItemSteps.장바구니에_상품_추가하고_아이디_반환;
import static cart.acceptance.CommonSteps.LOCATION_헤더를_검증한다;
import static cart.acceptance.CommonSteps.LOCATION_헤더에서_ID_추출;
import static cart.acceptance.CommonSteps.STATUS_CODE를_검증한다;
import static cart.acceptance.CommonSteps.정상_생성;
import static cart.acceptance.CommonSteps.정상_처리;
import static cart.acceptance.MemberSteps.유저_생성_요청하고_유저_반환;
import static cart.acceptance.OrderSteps.주문_목록_조회_요청;
import static cart.acceptance.OrderSteps.주문_상세_조회_요청;
import static cart.acceptance.OrderSteps.주문_요청;
import static cart.acceptance.OrderSteps.주문_요청하고_아이디_반환;
import static cart.acceptance.ProductSteps.상품_생성하고_아이디_반환;
import static org.assertj.core.api.Assertions.assertThat;

import cart.cartItem.persistence.CartItemDao;
import cart.cartItem.presentation.request.CartItemRequest;
import cart.order.application.dto.CartItemDto;
import cart.order.application.dto.OrderDto;
import cart.order.application.dto.OrderItemDto;
import cart.order.application.dto.ProductInCartItemDto;
import cart.order.presentation.request.OrderAddRequest;
import cart.order.application.dto.ProductInOrderItemDto;
import cart.product.presentation.request.ProductAddRequest;
import cart.member.domain.Member;
import cart.member.persistence.MemberDao;
import cart.order.persistence.OrderItemDao;
import cart.order.persistence.PaymentDao;
import cart.product.persistence.ProductDao;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(ReplaceUnderscores.class)
@DisplayName("Orderontroller 인수테스트")
@Sql(scripts = {"/delete.sql", "/schema.sql"})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class OrderAcceptanceTest {
    @LocalServerPort
    private int port;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private ProductDao productDao;
    @Autowired
    private CartItemDao cartItemDao;
    @Autowired
    private OrderItemDao orderItemDao;
    @Autowired
    private PaymentDao paymentDao;

    private ProductAddRequest productAddRequest1;
    private ProductAddRequest productAddRequest2;
    private ProductAddRequest productAddRequest3;

    private Long productId1;
    private Long productId2;
    private Long productId3;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        productAddRequest1 = new ProductAddRequest("떡볶이", 5000, "http://teokbboki.jpg", 30);
        productAddRequest2 = new ProductAddRequest("치킨", 10000, "http://chicken.jpg", 30);
        productAddRequest3 = new ProductAddRequest("피자", 15000, "http://pizza.jpg", 30);

        productId1 = 상품_생성하고_아이디_반환(productAddRequest1);
        productId2 = 상품_생성하고_아이디_반환(productAddRequest2);
        productId3 = 상품_생성하고_아이디_반환(productAddRequest3);
    }

    @Test
    void 주문한다() {
        // given
        Member member = 유저_생성_요청하고_유저_반환("abc", "1234", 5000);
        Long cartItemId1 = 장바구니에_상품_추가하고_아이디_반환(member, new CartItemRequest(productId1));
        Long cartItemId2 = 장바구니에_상품_추가하고_아이디_반환(member, new CartItemRequest(productId2));
        장바구니_상품_수량_수정_요청(member, cartItemId1, 5);
        장바구니_상품_수량_수정_요청(member, cartItemId2, 2);

        // when
        CartItemDto cartItemDto1 = toCartItemDto(cartItemId1, 5, productId1, productAddRequest1);
        CartItemDto cartItemDto2 = toCartItemDto(cartItemId2, 2, productId2, productAddRequest2);
        OrderAddRequest orderAddRequest = new OrderAddRequest(List.of(cartItemDto1, cartItemDto2), 45000, 3000, 3000, 45000);

        var response = 주문_요청(member, orderAddRequest);

        // then
        STATUS_CODE를_검증한다(response, 정상_생성);
        LOCATION_헤더를_검증한다(response);

        long orderId = LOCATION_헤더에서_ID_추출(response);

        Member findMember = memberDao.getMemberByEmail(member.getEmail());
        유저의_포인트가_갱신되었는지_검증한다(findMember, 4250);
        유저의_장바구니속_아이템이_삭제되었는지_확인한다(findMember, List.of(cartItemId1, cartItemId2));
        상품의_재고가_갱신되었는지_확인한다(List.of(productId1, productId2), List.of(25, 28));
        주문_상품이_저장되었는지_확인한다(orderId, 2);
        결제_금액이_올바른지_확인한다(orderId, 45000);
    }

    private static void 유저의_포인트가_갱신되었는지_검증한다(Member findMember, int expectedPoint) {
        assertThat(findMember.getPoint().getValue()).isEqualTo(expectedPoint);
    }

    private void 유저의_장바구니속_아이템이_삭제되었는지_확인한다(Member findMember, List<Long> cartItemIds) {
        List<Long> remainedCartItemIds = cartItemDao.findByMemberId(findMember.getId())
                .stream()
                .map(it -> it.getId())
                .collect(Collectors.toList());
        assertThat(remainedCartItemIds).doesNotContainAnyElementsOf(cartItemIds);
    }

    private void 상품의_재고가_갱신되었는지_확인한다(List<Long> productIds, List<Integer> stocks) {
        for (int i = 0; i < productIds.size(); i++) {
            assertThat(productDao.findById(productIds.get(i)).get().getStock()).isEqualTo(stocks.get(i));
        }
    }

    private void 주문_상품이_저장되었는지_확인한다(long orderId, int orderItemCount) {
        assertThat(orderItemDao.findByOrderId(orderId)).hasSize(orderItemCount);
    }

    private void 결제_금액이_올바른지_확인한다(long orderId, int expectedTotalPrice) {
        assertThat(paymentDao.findByOrderId(orderId).get().getTotalPrice()).isEqualTo(expectedTotalPrice);
    }

    @Test
    void 주문_상세를_조회한다() {
        // given
        Member member = 유저_생성_요청하고_유저_반환("abc", "1234", 5000);
        Long cartItemId1 = 장바구니에_상품_추가하고_아이디_반환(member, new CartItemRequest(productId1));
        Long cartItemId2 = 장바구니에_상품_추가하고_아이디_반환(member, new CartItemRequest(productId2));
        장바구니_상품_수량_수정_요청(member, cartItemId1, 5);
        장바구니_상품_수량_수정_요청(member, cartItemId2, 2);

        OrderAddRequest orderAddRequest = new OrderAddRequest(List.of(toCartItemDto(cartItemId1, 5, productId1,
                        productAddRequest1),
                toCartItemDto(cartItemId2, 2, productId2, productAddRequest2)), 45000, 3000, 3000, 45000);
        Long orderId = 주문_요청하고_아이디_반환(member, orderAddRequest);

        // when
        var response = 주문_상세_조회_요청(member, orderId);

        // then
        STATUS_CODE를_검증한다(response, 정상_처리);

        OrderDto orderDto = new OrderDto(orderId, LocalDateTime.now().toString(),
                List.of(toOrderItemDto(5, productId1, productAddRequest1, 25),
                        toOrderItemDto(2, productId2, productAddRequest2, 28)), 45000);
        주문_상세_응답을_검증한다(response, orderDto);
    }

    private void 주문_상세_응답을_검증한다(ExtractableResponse<Response> response, OrderDto orderDto) {
        OrderDto orderResponse = response.as(OrderDto.class);

        assertThat(orderResponse.getOrderId()).isEqualTo(orderDto.getOrderId());
        assertThat(orderResponse.getTotalPrice()).isEqualTo(orderDto.getTotalPrice());

        assertThat(orderResponse.getOrderItems()).usingRecursiveComparison()
                .isEqualTo(orderDto.getOrderItems());
    }

    @Test
    void 주문_목록을_조회한다() {
        // given
        Member member = 유저_생성_요청하고_유저_반환("abc", "1234", 5000);
        Long cartItemId1 = 장바구니에_상품_추가하고_아이디_반환(member, new CartItemRequest(productId1));
        Long cartItemId2 = 장바구니에_상품_추가하고_아이디_반환(member, new CartItemRequest(productId2));
        장바구니_상품_수량_수정_요청(member, cartItemId1, 5);
        장바구니_상품_수량_수정_요청(member, cartItemId2, 2);

        OrderAddRequest orderAddRequest1 = new OrderAddRequest(
                List.of(toCartItemDto(cartItemId1, 5, productId1, productAddRequest1),
                        toCartItemDto(cartItemId2, 2, productId2, productAddRequest2)), 45000, 3000, 3000, 45000);
        Long orderId1 = 주문_요청하고_아이디_반환(member, orderAddRequest1);

        Long cartItemId3 = 장바구니에_상품_추가하고_아이디_반환(member, new CartItemRequest(productId2));
        Long cartItemId4 = 장바구니에_상품_추가하고_아이디_반환(member, new CartItemRequest(productId3));
        장바구니_상품_수량_수정_요청(member, cartItemId3, 3);
        장바구니_상품_수량_수정_요청(member, cartItemId4, 4);

        OrderAddRequest orderAddRequest2 = new OrderAddRequest(
                List.of(toCartItemDto(cartItemId3, 3, productId2, productAddRequest2),
                        toCartItemDto(cartItemId4, 4, productId3, productAddRequest3)), 90000, 0, 4000, 86000);
        Long orderId2 = 주문_요청하고_아이디_반환(member, orderAddRequest2);

        // when
        var response = 주문_목록_조회_요청(member);

        // then
        STATUS_CODE를_검증한다(response, 정상_처리);

        OrderDto orderDto1 = new OrderDto(orderId1, LocalDateTime.now().toString(),
                List.of(toOrderItemDto(5, productId1, productAddRequest1, 25),
                        toOrderItemDto(2, productId2, productAddRequest2, 28)), 45000);
        OrderDto orderDto2 = new OrderDto(orderId2, LocalDateTime.now().toString(),
                List.of(toOrderItemDto(3, productId2, productAddRequest2, 25),
                        toOrderItemDto(4, productId3, productAddRequest3, 26)), 86000);
        주문_목록_조회_결과를_검증한다(response, List.of(orderDto1, orderDto2));
    }

    private static void 주문_목록_조회_결과를_검증한다(ExtractableResponse<Response> response, List<OrderDto> expectedDtos) {
        List<OrderDto> actualDtos = response.as(new TypeRef<>() {
        });

        for (int i = 0; i < actualDtos.size(); i++) {
            assertThat(actualDtos.get(i).getOrderId()).isEqualTo(expectedDtos.get(i).getOrderId());
            assertThat(actualDtos.get(i).getTotalPrice()).isEqualTo(expectedDtos.get(i).getTotalPrice());

            assertThat(actualDtos.get(i).getOrderItems())
                    .usingRecursiveComparison()
                    .isEqualTo(expectedDtos.get(i).getOrderItems());
        }
    }

    private CartItemDto toCartItemDto(Long cartItemId, int quantity, Long productId, ProductAddRequest productAddRequest) {
        return new CartItemDto(cartItemId, quantity, toProductDto(productId, productAddRequest));
    }

    private OrderItemDto toOrderItemDto(int quantity, Long productId, ProductAddRequest productAddRequest, int stock) {
        return new OrderItemDto(quantity, new ProductInOrderItemDto(productId, productAddRequest.getPrice(), productAddRequest.getName(),
                productAddRequest.getImageUrl(), stock));
    }

    private cart.order.application.dto.ProductInCartItemDto toProductDto(Long productId, ProductAddRequest productAddRequest) {
        return new ProductInCartItemDto(productId, productAddRequest.getPrice(), productAddRequest.getName(),
                productAddRequest.getImageUrl(), productAddRequest.getStock());
    }
}
