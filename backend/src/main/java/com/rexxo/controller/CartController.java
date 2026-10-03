package com.rexxo.controller;

import com.rexxo.entity.*;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Transactional
public class CartController {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @GetMapping
    public ResponseEntity<CartDto> getCart(Authentication auth) {
        Long userId = getUser(auth).getId();
        Cart cart = cartRepository.findByUserIdWithItems(userId)
            .orElseThrow(() -> RexxoException.notFound("Cart not found"));
        return ResponseEntity.ok(toDto(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(Authentication auth,
                                            @Valid @RequestBody AddItemRequest request) {
        User user = getUser(auth);
        Cart cart = cartRepository.findByUserId(user.getId())
            .orElseThrow(() -> RexxoException.notFound("Cart not found"));

        Product product = productRepository.findById(request.productId())
            .filter(Product::getIsActive)
            .orElseThrow(() -> RexxoException.notFound("Product not found or is currently unavailable"));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
            .orElseGet(() -> CartItem.builder()
                .cart(cart)
                .product(product)
                .unitPrice(product.getPrice())
                .quantity(0)
                .build());

        int currentQty = item.getQuantity() != null ? item.getQuantity() : 0;
        int targetQty = currentQty + request.quantity();

        if (product.getStock() < targetQty) {
            throw RexxoException.badRequest("Insufficient stock. Only " + product.getStock() + " available.");
        }

        item.setQuantity(targetQty);
        item.setUnitPrice(product.getPrice());
        cartItemRepository.save(item);
        entityManager.flush();
        entityManager.clear();

        return ResponseEntity.ok(toDto(cartRepository.findByUserIdWithItems(user.getId()).orElseThrow()));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItem(Authentication auth,
                                               @PathVariable Long itemId,
                                               @RequestBody Map<String, Integer> body) {
        User user = getUser(auth);
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> RexxoException.notFound("Cart item not found"));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw RexxoException.forbidden("Not your cart item");
        }

        int qty = body.getOrDefault("quantity", 1);
        if (qty <= 0) {
            cartItemRepository.delete(item);
        } else {
            Product product = item.getProduct();
            if (!Boolean.TRUE.equals(product.getIsActive())) {
                throw RexxoException.badRequest("Product is no longer available.");
            }
            if (product.getStock() < qty) {
                throw RexxoException.badRequest("Insufficient stock. Only " + product.getStock() + " available.");
            }
            item.setQuantity(qty);
            item.setUnitPrice(product.getPrice());
            cartItemRepository.save(item);
        }

        entityManager.flush();
        entityManager.clear();
        return ResponseEntity.ok(toDto(cartRepository.findByUserIdWithItems(user.getId()).orElseThrow()));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(Authentication auth, @PathVariable Long itemId) {
        User user = getUser(auth);
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> RexxoException.notFound("Cart item not found"));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw RexxoException.forbidden("Not your cart item");
        }

        cartItemRepository.delete(item);
        entityManager.flush();
        entityManager.clear();
        return ResponseEntity.ok(toDto(cartRepository.findByUserIdWithItems(user.getId()).orElseThrow()));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart(Authentication auth) {
        User user = getUser(auth);
        Cart cart = cartRepository.findByUserId(user.getId())
            .orElseThrow(() -> RexxoException.notFound("Cart not found"));
        cartItemRepository.deleteByCartId(cart.getId());
        return ResponseEntity.ok(Map.of("message", "Cart cleared"));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> RexxoException.notFound("User not found"));
    }

    private CartDto toDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream().map(i -> {
            Product p = i.getProduct();
            String img = p.getImages().stream().filter(ProductImage::getIsPrimary)
                .map(ProductImage::getImageUrl).findFirst()
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());
            BigDecimal unitPrice = p.getPrice();
            int qty = i.getQuantity();
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            return new CartItemDto(
                i.getId(),
                p.getId(),
                p.getName(),
                p.getName(),
                p.getSlug(),
                p.getSlug(),
                img,
                unitPrice,
                unitPrice,
                qty,
                p.getStock(),
                itemSubtotal
            );
        }).toList();

        BigDecimal subtotal = items.stream()
            .map(CartItemDto::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shipping = (subtotal.compareTo(new BigDecimal("2000")) > 0 || subtotal.compareTo(BigDecimal.ZERO) == 0)
            ? BigDecimal.ZERO : new BigDecimal("150");
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal.add(shipping).subtract(discount);
        int totalUnits = items.stream().mapToInt(CartItemDto::quantity).sum();

        return new CartDto(cart.getId(), items, subtotal, shipping, discount, total, totalUnits);
    }

    public record CartDto(
        Long id,
        List<CartItemDto> items,
        BigDecimal subtotal,
        BigDecimal shipping,
        BigDecimal discount,
        BigDecimal total,
        int itemCount
    ) {}

    public record CartItemDto(
        Long id,
        Long productId,
        String productName,
        String name,
        String productSlug,
        String slug,
        String imageUrl,
        BigDecimal price,
        BigDecimal unitPrice,
        Integer quantity,
        Integer stockQuantity,
        BigDecimal subtotal
    ) {}

    public record AddItemRequest(
        @NotNull Long productId,
        @Min(1) @Max(99) int quantity
    ) {}
}
