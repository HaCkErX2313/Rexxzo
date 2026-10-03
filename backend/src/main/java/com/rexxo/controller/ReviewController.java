package com.rexxo.controller;

import com.rexxo.entity.*;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewDto>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
            reviewRepository.findByProductIdAndIsApprovedTrue(productId, pageable).map(this::toDto)
        );
    }

    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ReviewSummaryDto> getProductReviewSummary(
            Authentication auth,
            @PathVariable Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> RexxoException.notFound("Product not found"));

        List<Review> approvedReviews = reviewRepository.findByProductIdAndIsApprovedTrue(
            productId, PageRequest.of(0, 1000)).getContent();

        int totalCount = approvedReviews.size();
        double avg = approvedReviews.stream()
            .mapToInt(Review::getRating)
            .average()
            .orElse(0.0);

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0L);
        for (Review r : approvedReviews) {
            distribution.merge(r.getRating(), 1L, Long::sum);
        }

        boolean canReview = false;
        boolean alreadyReviewed = false;
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user != null) {
                alreadyReviewed = reviewRepository.existsByProductIdAndUserId(productId, user.getId());
                boolean hasPurchased = orderRepository.findByUserIdOrderByCreatedAtDesc(
                    user.getId(), PageRequest.of(0, 100))
                    .getContent().stream()
                    .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
                    .flatMap(o -> o.getItems().stream())
                    .anyMatch(i -> i.getProduct().getId().equals(productId));
                canReview = hasPurchased && !alreadyReviewed;
            }
        }

        List<ReviewDto> reviewList = approvedReviews.stream().map(this::toDto).toList();

        return ResponseEntity.ok(new ReviewSummaryDto(
            BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP),
            totalCount,
            distribution,
            reviewList,
            canReview,
            alreadyReviewed
        ));
    }

    @PostMapping("/product/{productId}")
    public ResponseEntity<ReviewDto> addReview(Authentication auth,
                                                @PathVariable Long productId,
                                                @Valid @RequestBody ReviewRequest request) {
        User user = getUser(auth);
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> RexxoException.notFound("Product not found"));

        if (reviewRepository.existsByProductIdAndUserId(productId, user.getId())) {
            throw RexxoException.conflict("You have already reviewed this product");
        }

        // Enforce verified purchase requirement: Must have purchased in non-cancelled order
        boolean hasPurchased = orderRepository.findByUserIdOrderByCreatedAtDesc(
            user.getId(), PageRequest.of(0, 100))
            .getContent().stream()
            .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
            .flatMap(o -> o.getItems().stream())
            .anyMatch(i -> i.getProduct().getId().equals(productId));

        if (!hasPurchased) {
            throw RexxoException.forbidden("Only verified purchasers can review this product.");
        }

        Review review = Review.builder()
            .product(product)
            .user(user)
            .rating(request.rating())
            .title(request.title())
            .body(request.body())
            .isVerifiedPurchase(true)
            .isApproved(true)
            .status(Review.ReviewStatus.APPROVED)
            .build();

        review = reviewRepository.save(review);
        updateProductStats(product);

        return ResponseEntity.ok(toDto(review));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReviewDto> updateReview(Authentication auth,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody ReviewRequest request) {
        User user = getUser(auth);
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Review not found"));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        if (!isAdmin && !review.getUser().getId().equals(user.getId())) {
            throw RexxoException.forbidden("You do not own this review");
        }

        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setBody(request.body());
        Review saved = reviewRepository.save(review);
        updateProductStats(review.getProduct());

        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteReview(Authentication auth, @PathVariable Long id) {
        User user = getUser(auth);
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Review not found"));

        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        if (!isAdmin && !review.getUser().getId().equals(user.getId())) {
            throw RexxoException.forbidden("You do not own this review");
        }

        Product p = review.getProduct();
        reviewRepository.delete(review);
        updateProductStats(p);

        return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
    }

    private void updateProductStats(Product product) {
        List<Review> approved = reviewRepository.findByProductIdAndIsApprovedTrue(
            product.getId(), PageRequest.of(0, 1000)).getContent();

        double avg = approved.stream().mapToInt(Review::getRating).average().orElse(0.0);
        product.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        product.setReviewCount(approved.size());
        productRepository.save(product);
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> RexxoException.notFound("User not found"));
    }

    private ReviewDto toDto(Review r) {
        return new ReviewDto(r.getId(), r.getUser().getName(), r.getRating(),
            r.getTitle(), r.getBody(), r.getIsVerifiedPurchase(), r.getStatus().name(),
            r.getHelpfulCount(), r.getCreatedAt());
    }

    public record ReviewDto(Long id, String userName, Integer rating, String title,
                     String body, Boolean verifiedPurchase, String status,
                     Integer helpfulCount, LocalDateTime createdAt) {}

    public record ReviewSummaryDto(
        BigDecimal averageRating,
        int totalReviews,
        Map<Integer, Long> ratingDistribution,
        List<ReviewDto> reviews,
        boolean canReview,
        boolean alreadyReviewed
    ) {}

    public record ReviewRequest(
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 200) String title,
        @Size(max = 2000) String body
    ) {}
}
