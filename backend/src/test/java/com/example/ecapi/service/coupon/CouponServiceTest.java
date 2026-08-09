package com.example.ecapi.service.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.entity.Coupon;
import com.example.ecapi.exception.CouponCodeDuplicateException;
import com.example.ecapi.exception.CouponNotAllowedException;
import com.example.ecapi.exception.CouponNotFoundException;
import com.example.ecapi.repository.CouponRepository;
import com.example.ecapi.repository.CustomerOrderRepository;
import com.example.ecapi.service.coupon.dto.CouponResult;
import com.example.ecapi.service.coupon.dto.CreateCoupon;
import com.example.ecapi.service.coupon.dto.UpdateCoupon;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CustomerOrderRepository customerOrderRepository;

    @InjectMocks private CouponService couponService;

    private static final Long CUSTOMER_ID = 1L;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        coupon = new Coupon();
        coupon.setId(1L);
        coupon.setCode("SAVE500");
        coupon.setDiscountAmount(BigDecimal.valueOf(500));
        coupon.setActive(true);
        coupon.setUsageCount(0);
        ReflectionTestUtils.setField(coupon, "createdAt", Instant.now());
        ReflectionTestUtils.setField(coupon, "updatedAt", Instant.now());
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("指定したIDのクーポンを取得できること")
        void shouldFindCouponById() {
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

            CouponResult result = couponService.findById(1L);

            assertThat(result.code()).isEqualTo("SAVE500");
        }

        @Test
        @DisplayName("指定したIDのクーポンが見つからない場合、CouponNotFoundException をスローすること")
        void shouldThrowExceptionWhenNotFound() {
            when(couponRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> couponService.findById(99L))
                    .isInstanceOf(CouponNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("クーポンを作成できること")
        void shouldCreateCoupon() {
            when(couponRepository.existsByCode("SAVE500")).thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

            CouponResult result =
                    couponService.create(
                            new CreateCoupon("SAVE500", BigDecimal.valueOf(500), null, null, null));

            assertThat(result.code()).isEqualTo("SAVE500");
            verify(couponRepository).save(any(Coupon.class));
        }

        @Test
        @DisplayName("コードが重複する場合、CouponCodeDuplicateException をスローすること")
        void shouldThrowExceptionWhenCodeDuplicate() {
            when(couponRepository.existsByCode("SAVE500")).thenReturn(true);
            CreateCoupon request =
                    new CreateCoupon("SAVE500", BigDecimal.valueOf(500), null, null, null);

            assertThatThrownBy(() -> couponService.create(request))
                    .isInstanceOf(CouponCodeDuplicateException.class);
            verify(couponRepository, never()).save(any(Coupon.class));
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTest {

        @Test
        @DisplayName("クーポン一覧をページングで取得できること")
        void shouldFindAllCoupons() {
            PageRequest pageable = PageRequest.of(0, 20);
            when(couponRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(coupon), pageable, 1));

            var result = couponService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).code()).isEqualTo("SAVE500");
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTest {

        @Test
        @DisplayName("クーポンを削除できること")
        void shouldDeleteCoupon() {
            when(couponRepository.existsById(1L)).thenReturn(true);

            couponService.delete(1L);

            verify(couponRepository).deleteById(1L);
        }

        @Test
        @DisplayName("指定したIDのクーポンが見つからない場合、CouponNotFoundException をスローすること")
        void shouldThrowExceptionWhenNotFound() {
            when(couponRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> couponService.delete(99L))
                    .isInstanceOf(CouponNotFoundException.class);
            verify(couponRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("削除時に整合性制約違反が発生した場合、CouponNotAllowedException をスローすること")
        void shouldThrowExceptionWhenDataIntegrityViolation() {
            when(couponRepository.existsById(1L)).thenReturn(true);
            doThrow(new DataIntegrityViolationException("constraint"))
                    .when(couponRepository)
                    .deleteById(1L);

            assertThatThrownBy(() -> couponService.delete(1L))
                    .isInstanceOf(CouponNotAllowedException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("有効フラグを更新できること")
        void shouldDeactivateCoupon() {
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
            when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

            couponService.update(new UpdateCoupon(1L, null, null, null, null, false, 0));

            assertThat(coupon.isActive()).isFalse();
        }

        @Test
        @DisplayName("指定したIDのクーポンが見つからない場合、CouponNotFoundException をスローすること")
        void shouldThrowExceptionWhenNotFound() {
            when(couponRepository.findById(99L)).thenReturn(Optional.empty());
            UpdateCoupon request = new UpdateCoupon(99L, null, null, null, null, null, 0);

            assertThatThrownBy(() -> couponService.update(request))
                    .isInstanceOf(CouponNotFoundException.class);
        }

        @Test
        @DisplayName("割引額・有効期限・利用上限を更新できること")
        void shouldUpdateDiscountAmountAndPeriodAndUsageLimit() {
            when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
            when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);
            LocalDateTime validFrom = LocalDateTime.now();
            LocalDateTime validTo = LocalDateTime.now().plusDays(30);

            couponService.update(
                    new UpdateCoupon(
                            1L, BigDecimal.valueOf(1000), validFrom, validTo, 50, true, 0));

            assertThat(coupon.getDiscountAmount()).isEqualByComparingTo("1000");
            assertThat(coupon.getUsageLimit()).isEqualTo(50);
            assertThat(coupon.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("validateAndApply")
    class ValidateAndApplyTest {

        @Test
        @DisplayName("有効なクーポンの場合、割引額を返し利用回数が加算されること")
        void shouldApplyValidCoupon() {
            when(couponRepository.findByCode("SAVE500")).thenReturn(Optional.of(coupon));
            when(customerOrderRepository.existsByCustomerIdAndCouponCodeAndDeletedFalse(
                            CUSTOMER_ID, "SAVE500"))
                    .thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

            BigDecimal discount =
                    couponService.validateAndApply(
                            "SAVE500", CUSTOMER_ID, BigDecimal.valueOf(1000));

            assertThat(discount).isEqualByComparingTo("500");
            assertThat(coupon.getUsageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("割引額が小計を上回る場合、小計を上限として適用されること")
        void shouldCapDiscountAtSubtotal() {
            when(couponRepository.findByCode("SAVE500")).thenReturn(Optional.of(coupon));
            when(customerOrderRepository.existsByCustomerIdAndCouponCodeAndDeletedFalse(
                            CUSTOMER_ID, "SAVE500"))
                    .thenReturn(false);
            when(couponRepository.save(any(Coupon.class))).thenReturn(coupon);

            BigDecimal discount =
                    couponService.validateAndApply("SAVE500", CUSTOMER_ID, BigDecimal.valueOf(300));

            assertThat(discount).isEqualByComparingTo("300");
        }

        @Test
        @DisplayName("存在しないコードの場合、CouponNotFoundException をスローすること")
        void shouldThrowExceptionWhenCodeNotFound() {
            when(couponRepository.findByCode("INVALID")).thenReturn(Optional.empty());
            BigDecimal subtotal = BigDecimal.valueOf(1000);

            assertThatThrownBy(
                            () -> couponService.validateAndApply("INVALID", CUSTOMER_ID, subtotal))
                    .isInstanceOf(CouponNotFoundException.class);
        }

        @Test
        @DisplayName("無効化済みの場合、CouponNotAllowedException をスローすること")
        void shouldThrowExceptionWhenInactive() {
            coupon.setActive(false);
            when(couponRepository.findByCode("SAVE500")).thenReturn(Optional.of(coupon));
            BigDecimal subtotal = BigDecimal.valueOf(1000);

            assertThatThrownBy(
                            () -> couponService.validateAndApply("SAVE500", CUSTOMER_ID, subtotal))
                    .isInstanceOf(CouponNotAllowedException.class);
        }

        @Test
        @DisplayName("有効期限が過ぎている場合、CouponNotAllowedException をスローすること")
        void shouldThrowExceptionWhenExpired() {
            coupon.setValidTo(
                    LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant());
            when(couponRepository.findByCode("SAVE500")).thenReturn(Optional.of(coupon));
            BigDecimal subtotal = BigDecimal.valueOf(1000);

            assertThatThrownBy(
                            () -> couponService.validateAndApply("SAVE500", CUSTOMER_ID, subtotal))
                    .isInstanceOf(CouponNotAllowedException.class);
        }

        @Test
        @DisplayName("利用上限に達している場合、CouponNotAllowedException をスローすること")
        void shouldThrowExceptionWhenUsageLimitReached() {
            coupon.setUsageLimit(1);
            coupon.setUsageCount(1);
            when(couponRepository.findByCode("SAVE500")).thenReturn(Optional.of(coupon));
            BigDecimal subtotal = BigDecimal.valueOf(1000);

            assertThatThrownBy(
                            () -> couponService.validateAndApply("SAVE500", CUSTOMER_ID, subtotal))
                    .isInstanceOf(CouponNotAllowedException.class);
        }

        @Test
        @DisplayName("当該顧客が使用済みの場合、CouponNotAllowedException をスローすること")
        void shouldThrowExceptionWhenAlreadyUsedByCustomer() {
            when(couponRepository.findByCode("SAVE500")).thenReturn(Optional.of(coupon));
            when(customerOrderRepository.existsByCustomerIdAndCouponCodeAndDeletedFalse(
                            CUSTOMER_ID, "SAVE500"))
                    .thenReturn(true);
            BigDecimal subtotal = BigDecimal.valueOf(1000);

            assertThatThrownBy(
                            () -> couponService.validateAndApply("SAVE500", CUSTOMER_ID, subtotal))
                    .isInstanceOf(CouponNotAllowedException.class);
        }
    }
}
