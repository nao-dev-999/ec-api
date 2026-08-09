package com.example.ecapi.service.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.Customer;
import com.example.ecapi.entity.Product;
import com.example.ecapi.entity.Review;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.exception.ReviewAlreadyExistsException;
import com.example.ecapi.exception.ReviewNotAllowedException;
import com.example.ecapi.exception.ReviewNotFoundException;
import com.example.ecapi.repository.CustomerOrderDetailRepository;
import com.example.ecapi.repository.CustomerRepository;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.repository.ReviewRepository;
import com.example.ecapi.service.review.dto.CreateReview;
import com.example.ecapi.service.review.dto.ReviewResult;
import com.example.ecapi.service.review.dto.ReviewSummaryResult;
import com.example.ecapi.service.review.dto.UpdateReview;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerOrderDetailRepository customerOrderDetailRepository;

    @InjectMocks private ReviewService reviewService;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    private Product product;
    private Customer customer;
    private Review review;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(500));
        product.setStock(20);

        customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setLastName("山田");
        customer.setFirstName("太郎");

        review = new Review();
        review.setId(100L);
        review.setCustomerId(CUSTOMER_ID);
        review.setProductId(PRODUCT_ID);
        review.setRating(5);
        review.setComment("とても良かったです");
        ReflectionTestUtils.setField(review, "createdAt", Instant.now());
        ReflectionTestUtils.setField(review, "updatedAt", Instant.now());
        ReflectionTestUtils.setField(review, "version", 0);
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("配送完了済みの購入実績があれば投稿できること")
        void shouldCreateReviewWhenPurchased() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(customerOrderDetailRepository.existsByCustomerIdAndProductIdAndOrderStatus(
                            CUSTOMER_ID, PRODUCT_ID, OrderStatus.DELIVERED))
                    .thenReturn(true);
            when(reviewRepository.existsByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                    .thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenReturn(review);

            ReviewResult result =
                    reviewService.create(new CreateReview(CUSTOMER_ID, PRODUCT_ID, 5, "最高"));

            assertThat(result.productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.customerName()).isEqualTo("山田");
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("配送完了済みの購入実績がない場合、ReviewNotAllowedExceptionをスローすること")
        void shouldThrowExceptionWhenNotPurchased() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(customerOrderDetailRepository.existsByCustomerIdAndProductIdAndOrderStatus(
                            CUSTOMER_ID, PRODUCT_ID, OrderStatus.DELIVERED))
                    .thenReturn(false);

            assertThatThrownBy(
                            () ->
                                    reviewService.create(
                                            new CreateReview(CUSTOMER_ID, PRODUCT_ID, 5, "最高")))
                    .isInstanceOf(ReviewNotAllowedException.class);
        }

        @Test
        @DisplayName("すでにレビュー済みの商品の場合、ReviewAlreadyExistsExceptionをスローすること")
        void shouldThrowExceptionWhenAlreadyReviewed() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(customerOrderDetailRepository.existsByCustomerIdAndProductIdAndOrderStatus(
                            CUSTOMER_ID, PRODUCT_ID, OrderStatus.DELIVERED))
                    .thenReturn(true);
            when(reviewRepository.existsByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                    .thenReturn(true);

            assertThatThrownBy(
                            () ->
                                    reviewService.create(
                                            new CreateReview(CUSTOMER_ID, PRODUCT_ID, 5, "最高")))
                    .isInstanceOf(ReviewAlreadyExistsException.class);
        }

        @Test
        @DisplayName("存在しない商品の場合、ProductNotFoundExceptionをスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                            () -> reviewService.create(new CreateReview(CUSTOMER_ID, 99L, 5, "最高")))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("自分のレビューを更新できること")
        void shouldUpdateOwnReview() {
            when(reviewRepository.findByIdAndCustomerId(100L, CUSTOMER_ID))
                    .thenReturn(Optional.of(review));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(reviewRepository.save(review)).thenReturn(review);

            reviewService.update(new UpdateReview(100L, CUSTOMER_ID, 3, "普通でした", 0));

            assertThat(review.getRating()).isEqualTo(3);
            assertThat(review.getComment()).isEqualTo("普通でした");
            verify(reviewRepository).save(review);
        }

        @Test
        @DisplayName("他人のレビューを指定した場合、ReviewNotFoundExceptionをスローすること")
        void shouldThrowExceptionWhenNotOwnReview() {
            when(reviewRepository.findByIdAndCustomerId(100L, 999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.update(new UpdateReview(100L, 999L, 3, "普通", 0)))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTest {

        @Test
        @DisplayName("自分のレビューを削除できること")
        void shouldDeleteOwnReview() {
            when(reviewRepository.findByIdAndCustomerId(100L, CUSTOMER_ID))
                    .thenReturn(Optional.of(review));

            reviewService.delete(100L, CUSTOMER_ID);

            verify(reviewRepository).delete(review);
        }

        @Test
        @DisplayName("他人のレビューを指定した場合、ReviewNotFoundExceptionをスローすること")
        void shouldThrowExceptionWhenNotOwnReview() {
            when(reviewRepository.findByIdAndCustomerId(100L, 999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.delete(100L, 999L))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteByAdmin")
    class DeleteByAdminTest {

        @Test
        @DisplayName("管理者は所有者チェックなしで削除できること")
        void shouldDeleteRegardlessOfOwner() {
            when(reviewRepository.existsById(100L)).thenReturn(true);

            reviewService.deleteByAdmin(100L);

            verify(reviewRepository).deleteById(100L);
        }

        @Test
        @DisplayName("存在しないレビューの場合、ReviewNotFoundExceptionをスローすること")
        void shouldThrowExceptionWhenNotFound() {
            when(reviewRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.deleteByAdmin(999L))
                    .isInstanceOf(ReviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummaryTest {

        @Test
        @DisplayName("平均評価・件数を取得できること")
        void shouldReturnSummary() {
            when(reviewRepository.findAverageRatingByProductId(PRODUCT_ID))
                    .thenReturn(Optional.of(4.5));
            when(reviewRepository.countByProductId(PRODUCT_ID)).thenReturn(2L);

            ReviewSummaryResult result = reviewService.getSummary(PRODUCT_ID);

            assertThat(result.averageRating()).isEqualTo(4.5);
            assertThat(result.reviewCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("レビューが0件の場合、平均評価0を返すこと")
        void shouldReturnZeroAverageWhenNoReviews() {
            when(reviewRepository.findAverageRatingByProductId(PRODUCT_ID))
                    .thenReturn(Optional.empty());
            when(reviewRepository.countByProductId(PRODUCT_ID)).thenReturn(0L);

            ReviewSummaryResult result = reviewService.getSummary(PRODUCT_ID);

            assertThat(result.averageRating()).isEqualTo(0.0);
            assertThat(result.reviewCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("listAllForAdmin")
    class ListAllForAdminTest {

        @Test
        @DisplayName("全レビューをページングで取得できること")
        void shouldReturnAllReviews() {
            PageRequest pageable = PageRequest.of(0, 20);
            when(reviewRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(review), pageable, 1));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
            when(customerRepository.findAllById(List.of(CUSTOMER_ID)))
                    .thenReturn(List.of(customer));

            var result = reviewService.listAllForAdmin(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).productName()).isEqualTo("Test Product");
            assertThat(result.getContent().get(0).customerName()).isEqualTo("山田");
        }
    }

    @Nested
    @DisplayName("listByProduct")
    class ListByProductTest {

        @Test
        @DisplayName("商品のレビュー一覧を取得できること")
        void shouldReturnReviews() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(reviewRepository.findAllByProductIdOrderByCreatedAtDesc(PRODUCT_ID))
                    .thenReturn(List.of(review));
            when(customerRepository.findAllById(List.of(CUSTOMER_ID)))
                    .thenReturn(List.of(customer));

            List<ReviewResult> result = reviewService.listByProduct(PRODUCT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).customerName()).isEqualTo("山田");
        }

        @Test
        @DisplayName("存在しない商品の場合、ProductNotFoundExceptionをスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.listByProduct(99L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }
}
