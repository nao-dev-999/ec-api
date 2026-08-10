package com.example.ecapi.service.coupon;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.Coupon;
import com.example.ecapi.exception.CouponCodeDuplicateException;
import com.example.ecapi.exception.CouponNotAllowedException;
import com.example.ecapi.exception.CouponNotFoundException;
import com.example.ecapi.repository.CouponRepository;
import com.example.ecapi.repository.CustomerOrderRepository;
import com.example.ecapi.service.coupon.dto.CouponResult;
import com.example.ecapi.service.coupon.dto.CreateCoupon;
import com.example.ecapi.service.coupon.dto.UpdateCoupon;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public Page<CouponResult> findAll(Pageable pageable) {
        return couponRepository.findAll(pageable).map(this::toResult);
    }

    public CouponResult findById(Long id) {
        return couponRepository
                .findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new CouponNotFoundException(id));
    }

    /**
     * @throws CouponCodeDuplicateException すでに同じコードのクーポンが存在する場合
     */
    @Transactional
    public CouponResult create(CreateCoupon dto) {
        if (couponRepository.existsByCode(dto.code())) {
            throw new CouponCodeDuplicateException(dto.code());
        }
        Coupon coupon = new Coupon();
        coupon.setCode(dto.code());
        coupon.setDiscountAmount(dto.discountAmount());
        coupon.setValidFrom(toInstant(dto.validFrom()));
        coupon.setValidTo(toInstant(dto.validTo()));
        coupon.setUsageLimit(dto.usageLimit());
        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon created couponId={} code={}", saved.getId(), saved.getCode());
        return toResult(saved);
    }

    /**
     * @throws CouponNotFoundException 指定されたIDのクーポンが見つからない場合
     * @throws OptimisticLockException 楽観ロックの競合が発生した場合
     */
    @Transactional
    public CouponResult update(UpdateCoupon dto) {
        Coupon coupon =
                couponRepository
                        .findById(dto.id())
                        .orElseThrow(() -> new CouponNotFoundException(dto.id()));
        if (dto.discountAmount() != null) coupon.setDiscountAmount(dto.discountAmount());
        coupon.setValidFrom(toInstant(dto.validFrom()));
        coupon.setValidTo(toInstant(dto.validTo()));
        coupon.setUsageLimit(dto.usageLimit());
        if (dto.active() != null) coupon.setActive(dto.active());
        coupon.setVersion(dto.version());
        log.info("Coupon updated couponId={}", dto.id());
        return toResult(couponRepository.save(coupon));
    }

    /**
     * @throws CouponNotFoundException 指定されたIDのクーポンが見つからない場合
     */
    @Transactional
    public void delete(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new CouponNotFoundException(id);
        }
        try {
            couponRepository.deleteById(id);
            couponRepository.flush();
        } catch (DataIntegrityViolationException _) {
            // coupon_code は customer_order への外部キーではないため通常到達しないが、念のため保護。
            throw new CouponNotAllowedException(id);
        }
        log.info("Coupon deleted couponId={}", id);
    }

    /**
     * クーポンコードを検証し、実際に適用した場合の割引額を返します（注文への適用や利用回数の加算は行わない）。 カート画面等での事前確認向け。
     *
     * @param subtotal 割引適用前の注文小計
     * @return 適用した場合の割引額（{@code subtotal} を上限とする）
     * @throws CouponNotFoundException 指定されたコードのクーポンが存在しない場合
     * @throws CouponNotAllowedException 無効化済み・有効期限外・利用上限到達・当該顧客が使用済みの場合
     */
    public BigDecimal preview(String code, Long customerId, BigDecimal subtotal) {
        Coupon coupon = findApplicableCoupon(code, customerId);
        return coupon.getDiscountAmount().min(subtotal);
    }

    /**
     * クーポンコードを検証し、割引額を確定・適用します（利用回数のインクリメントを含む）。 呼び出し元（注文作成）のトランザクションに参加します。
     *
     * @param subtotal 割引適用前の注文小計
     * @return 実際に適用する割引額（{@code subtotal} を上限とする）
     * @throws CouponNotFoundException 指定されたコードのクーポンが存在しない場合
     * @throws CouponNotAllowedException 無効化済み・有効期限外・利用上限到達・当該顧客が使用済みの場合
     */
    @Transactional
    public BigDecimal validateAndApply(String code, Long customerId, BigDecimal subtotal) {
        Coupon coupon = findApplicableCoupon(code, customerId);

        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);

        BigDecimal discount = coupon.getDiscountAmount().min(subtotal);
        log.info(
                "Coupon applied code={} customerId={} discountAmount={}",
                code,
                customerId,
                discount);
        return discount;
    }

    /**
     * 注文キャンセルに伴い、クーポンの利用回数・当該顧客の使用済み判定を解放します。 クーポンコードが指定されていない注文（{@code couponCode ==
     * null}）の場合は何もしません。
     */
    @Transactional
    public void releaseUsage(String couponCode) {
        if (couponCode == null) {
            return;
        }
        couponRepository
                .findByCode(couponCode)
                .ifPresent(
                        coupon -> {
                            coupon.setUsageCount(Math.max(0, coupon.getUsageCount() - 1));
                            couponRepository.save(coupon);
                            log.info("Coupon usage released code={}", couponCode);
                        });
    }

    private Coupon findApplicableCoupon(String code, Long customerId) {
        Coupon coupon =
                couponRepository
                        .findByCode(code)
                        .orElseThrow(() -> new CouponNotFoundException(code));

        Instant now = Instant.now();
        boolean withinPeriod =
                (coupon.getValidFrom() == null || !now.isBefore(coupon.getValidFrom()))
                        && (coupon.getValidTo() == null || !now.isAfter(coupon.getValidTo()));
        boolean withinUsageLimit =
                coupon.getUsageLimit() == null || coupon.getUsageCount() < coupon.getUsageLimit();
        // キャンセル済みの注文は「使用済み」判定から除外し、同じクーポンを再利用可能にする。
        boolean alreadyUsedByCustomer =
                customerOrderRepository.existsByCustomerIdAndCouponCodeAndDeletedFalseAndStatusNot(
                        customerId, code, OrderStatus.CANCELLED);

        if (!coupon.isActive() || !withinPeriod || !withinUsageLimit || alreadyUsedByCustomer) {
            throw new CouponNotAllowedException(code);
        }
        return coupon;
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private CouponResult toResult(Coupon coupon) {
        return new CouponResult(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountAmount(),
                toLocalDateTime(coupon.getValidFrom()),
                toLocalDateTime(coupon.getValidTo()),
                coupon.getUsageLimit(),
                coupon.getUsageCount(),
                coupon.isActive(),
                toLocalDateTime(coupon.getCreatedAt()),
                toLocalDateTime(coupon.getUpdatedAt()),
                coupon.getVersion());
    }
}
