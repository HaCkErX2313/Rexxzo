package com.rexxo.controller;

import com.rexxo.entity.*;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<WishlistDto> getWishlist(Authentication auth) {
        Long userId = getUser(auth).getId();
        Wishlist wishlist = wishlistRepository.findByUserIdWithItems(userId)
            .orElseThrow(() -> RexxoException.notFound("Wishlist not found"));
        return ResponseEntity.ok(toDto(wishlist));
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistDto> addItem(Authentication auth,
                                                @RequestBody Map<String, Long> body) {
        User user = getUser(auth);
        Long productId = body.get("productId");
        if (productId == null) throw RexxoException.badRequest("productId required");

        Product product = productRepository.findById(productId)
            .filter(Product::getIsActive)
            .orElseThrow(() -> RexxoException.notFound("Product not found"));

        Wishlist wishlist = wishlistRepository.findByUserId(user.getId())
            .orElseThrow(() -> RexxoException.notFound("Wishlist not found"));

        if (!wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            WishlistItem item = WishlistItem.builder().wishlist(wishlist).product(product).build();
            wishlistItemRepository.save(item);
        }

        return ResponseEntity.ok(toDto(wishlistRepository.findByUserIdWithItems(user.getId()).orElseThrow()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<WishlistDto> removeItem(Authentication auth,
                                                   @PathVariable Long productId) {
        User user = getUser(auth);
        Wishlist wishlist = wishlistRepository.findByUserId(user.getId())
            .orElseThrow(() -> RexxoException.notFound("Wishlist not found"));

        wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.getId(), productId);
        return ResponseEntity.ok(toDto(wishlistRepository.findByUserIdWithItems(user.getId()).orElseThrow()));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> check(Authentication auth,
                                                       @PathVariable Long productId) {
        User user = getUser(auth);
        Wishlist wishlist = wishlistRepository.findByUserId(user.getId())
            .orElseThrow(() -> RexxoException.notFound("Wishlist not found"));
        boolean inWishlist = wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> RexxoException.notFound("User not found"));
    }

    private WishlistDto toDto(Wishlist w) {
        List<WishlistItemDto> items = w.getItems().stream().map(i -> {
            Product p = i.getProduct();
            String img = p.getImages().stream().filter(ProductImage::getIsPrimary)
                .map(ProductImage::getImageUrl).findFirst()
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());
            return new WishlistItemDto(p.getId(), p.getName(), p.getSlug(),
                img, p.getPrice(), p.getOriginalPrice(), p.getAvgRating(), p.getReviewCount());
        }).toList();
        return new WishlistDto(w.getId(), items, items.size());
    }

    record WishlistDto(Long id, List<WishlistItemDto> items, int count) {}
    record WishlistItemDto(Long productId, String name, String slug, String imageUrl,
                           java.math.BigDecimal price, java.math.BigDecimal originalPrice,
                           java.math.BigDecimal avgRating, Integer reviewCount) {}
}
