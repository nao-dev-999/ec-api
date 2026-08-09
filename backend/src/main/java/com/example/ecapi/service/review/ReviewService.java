package com.example.ecapi.service.review;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.Customer;
import com.example.ecapi.entity.Product;
import com.example.ecapi.entity.Review;
import com.example.ecapi.exception.CustomerNotFoundException;
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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CustomerOrderDetailRepository customerOrderDetailRepository;

    /** 商品詳細画面向け: 指定商品のレビュー一覧（新しい順） */
    public List<ReviewResult> listByProduct(Long productId) {
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException(productId));
        List<Review> reviews = reviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId);

        Map<Long, Customer> customersById =
                customerRepository
                        .findAllById(reviews.stream().map(Review::getCustomerId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(Customer::getId, Function.identity()));

        return reviews.stream()
                .map(review -> toResult(review, product, customersById.get(review.getCustomerId())))
                .toList();
    }

    /** 商品詳細画面向け: 平均評価・件数 */
    public ReviewSummaryResult getSummary(Long productId) {
        double average = reviewRepository.findAverageRatingByProductId(productId).orElse(0.0);
        long count = reviewRepository.countByProductId(productId);
        return new ReviewSummaryResult(average, count);
    }

    /** 管理画面向け: 全レビュー一覧（新しい順、商品名込み） */
    public Page<ReviewResult> listAllForAdmin(Pageable pageable) {
        Page<Review> page = reviewRepository.findAll(pageable);
        Map<Long, Product> productsById =
                productRepository
                        .findAllById(page.stream().map(Review::getProductId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, Customer> customersById =
                customerRepository
                        .findAllById(page.stream().map(Review::getCustomerId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(Customer::getId, Function.identity()));
        return page.map(
                review ->
                        toResult(
                                review,
                                productsById.get(review.getProductId()),
                                customersById.get(review.getCustomerId())));
    }

    /**
     * @throws ProductNotFoundException 商品が存在しない場合
     * @throws ReviewNotAllowedException 配送完了済みの購入実績がない場合
     * @throws ReviewAlreadyExistsException すでにその商品にレビュー済みの場合
     */
    @Transactional
    public ReviewResult create(CreateReview dto) {
        Product product =
                productRepository
                        .findById(dto.productId())
                        .orElseThrow(() -> new ProductNotFoundException(dto.productId()));
        Customer customer =
                customerRepository
                        .findById(dto.customerId())
                        .orElseThrow(() -> new CustomerNotFoundException(dto.customerId()));

        boolean purchased =
                customerOrderDetailRepository.existsByCustomerIdAndProductIdAndOrderStatus(
                        dto.customerId(), dto.productId(), OrderStatus.DELIVERED);
        if (!purchased) {
            throw new ReviewNotAllowedException(dto.productId());
        }
        if (reviewRepository.existsByCustomerIdAndProductId(dto.customerId(), dto.productId())) {
            throw new ReviewAlreadyExistsException(dto.productId());
        }

        Review review = new Review();
        review.setCustomerId(dto.customerId());
        review.setProductId(dto.productId());
        review.setRating(dto.rating());
        review.setComment(dto.comment());
        Review saved = reviewRepository.save(review);
        log.info(
                "Review created reviewId={} customerId={} productId={}",
                saved.getId(),
                dto.customerId(),
                dto.productId());
        return toResult(saved, product, customer);
    }

    /**
     * 自分自身のレビューのみ更新可能。他人のレビューIDを指定した場合は{@link ReviewNotFoundException}を返し、
     * 存在有無を推測されないようにする。
     */
    @Transactional
    public ReviewResult update(UpdateReview dto) {
        Review review =
                reviewRepository
                        .findByIdAndCustomerId(dto.reviewId(), dto.customerId())
                        .orElseThrow(() -> new ReviewNotFoundException(dto.reviewId()));
        Product product =
                productRepository
                        .findById(review.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException(review.getProductId()));
        Customer customer =
                customerRepository
                        .findById(dto.customerId())
                        .orElseThrow(() -> new CustomerNotFoundException(dto.customerId()));

        review.setRating(dto.rating());
        review.setComment(dto.comment());
        review.setVersion(dto.version());
        Review saved = reviewRepository.save(review);
        log.info("Review updated reviewId={} customerId={}", dto.reviewId(), dto.customerId());
        return toResult(saved, product, customer);
    }

    /** 自分自身のレビューのみ削除可能 */
    @Transactional
    public void delete(Long reviewId, Long customerId) {
        Review review =
                reviewRepository
                        .findByIdAndCustomerId(reviewId, customerId)
                        .orElseThrow(() -> new ReviewNotFoundException(reviewId));
        reviewRepository.delete(review);
        log.info("Review deleted reviewId={} customerId={}", reviewId, customerId);
    }

    /** 管理者によるレビュー削除（不適切投稿のモデレーション。所有者チェックなし） */
    @Transactional
    public void deleteByAdmin(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ReviewNotFoundException(reviewId);
        }
        reviewRepository.deleteById(reviewId);
        log.info("Review deleted by admin reviewId={}", reviewId);
    }

    private ReviewResult toResult(Review review, Product product, Customer customer) {
        return new ReviewResult(
                review.getId(),
                review.getProductId(),
                product == null ? null : product.getName(),
                review.getCustomerId(),
                customer == null ? null : customer.getLastName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getVersion());
    }
}
