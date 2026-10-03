package com.rexxo.controller;

import com.rexxo.entity.Product;
import com.rexxo.entity.ProductImage;
import com.rexxo.entity.Category;
import com.rexxo.exception.RexxoException;
import com.rexxo.repository.CategoryRepository;
import com.rexxo.repository.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<Page<ProductSummaryDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search) {

        if (categoryId == null && category != null && !category.isBlank()) {
            String trimmedCat = category.trim();
            if (!"all".equalsIgnoreCase(trimmedCat)) {
                categoryId = categoryRepository.findBySlug(trimmedCat.toLowerCase())
                    .map(Category::getId)
                    .orElse(null);
            }
        }

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<Product> products;
        if (categoryId == null && minPrice == null && maxPrice == null && trimmedSearch == null) {
            products = productRepository.findByIsActiveTrue(pageable);
        } else {
            products = productRepository.findWithFilters(categoryId, minPrice, maxPrice, trimmedSearch, pageable);
        }
        return ResponseEntity.ok(products.map(this::toSummaryDto));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ProductSummaryDto>> featured() {
        return ResponseEntity.ok(
            productRepository.findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc()
                .stream().map(this::toSummaryDto).toList()
        );
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductDetailDto> getBySlug(@PathVariable String slug) {
        Product p = productRepository.findBySlug(slug)
            .filter(Product::getIsActive)
            .orElseGet(() -> {
                try {
                    Long id = Long.parseLong(slug);
                    return productRepository.findById(id).filter(Product::getIsActive).orElse(null);
                } catch (NumberFormatException e) {
                    return null;
                }
            });
        if (p == null) {
            throw RexxoException.notFound("Product not found: " + slug);
        }
        return ResponseEntity.ok(toDetailDto(p));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ProductDetailDto> getById(@PathVariable Long id) {
        Product p = productRepository.findById(id)
            .filter(Product::getIsActive)
            .orElseThrow(() -> RexxoException.notFound("Product not found"));
        return ResponseEntity.ok(toDetailDto(p));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ProductDetailDto> create(@Valid @RequestBody ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> RexxoException.notFound("Category not found"));

        if (productRepository.existsBySlug(request.slug())) {
            throw RexxoException.conflict("Slug already exists: " + request.slug());
        }

        Product p = Product.builder()
            .name(request.name())
            .slug(request.slug())
            .description(request.description())
            .shortDescription(request.shortDescription())
            .price(request.price())
            .originalPrice(request.originalPrice())
            .discountPercent(request.discountPercent())
            .stock(request.stock())
            .sku(request.sku() != null ? request.sku() : "")
            .category(category)
            .isFeatured(request.isFeatured() != null && request.isFeatured())
            .isActive(true)
            .material(request.material())
            .color(request.color())
            .dimensions(request.dimensions())
            .weightGrams(request.weightGrams())
            .specifications(request.specifications())
            .build();

        if (request.imageUrls() != null) {
            for (int i = 0; i < request.imageUrls().size(); i++) {
                String url = request.imageUrls().get(i);
                ProductImage img = ProductImage.builder()
                    .product(p)
                    .imageUrl(url)
                    .displayOrder(i)
                    .isPrimary(i == 0)
                    .altText(request.name())
                    .build();
                p.getImages().add(img);
            }
        }

        return ResponseEntity.ok(toDetailDto(productRepository.save(p)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ProductDetailDto> update(@PathVariable Long id,
                                                    @Valid @RequestBody ProductRequest request) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Product not found"));

        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> RexxoException.notFound("Category not found"));

        p.setName(request.name());
        p.setDescription(request.description());
        p.setShortDescription(request.shortDescription());
        p.setPrice(request.price());
        p.setOriginalPrice(request.originalPrice());
        p.setDiscountPercent(request.discountPercent());
        p.setStock(request.stock());
        p.setCategory(category);
        if (request.isFeatured() != null) p.setIsFeatured(request.isFeatured());
        if (request.material() != null) p.setMaterial(request.material());
        if (request.color() != null) p.setColor(request.color());
        if (request.specifications() != null) p.setSpecifications(request.specifications());

        return ResponseEntity.ok(toDetailDto(productRepository.save(p)));
    }

    @PatchMapping("/{id}/category")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ProductDetailDto> updateCategory(@PathVariable Long id,
                                                           @RequestBody Map<String, Object> body) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Product not found"));

        Long targetCategoryId = null;
        if (body.get("categoryId") != null) {
            targetCategoryId = Long.valueOf(body.get("categoryId").toString());
        } else if (body.get("slug") != null) {
            targetCategoryId = categoryRepository.findBySlug(body.get("slug").toString().trim().toLowerCase())
                .map(Category::getId).orElse(null);
        } else if (body.get("categoryName") != null) {
            String nameQuery = body.get("categoryName").toString().trim();
            targetCategoryId = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(nameQuery))
                .map(Category::getId).findFirst().orElse(null);
        }

        if (targetCategoryId == null) {
            throw RexxoException.badRequest("Valid categoryId, slug, or categoryName is required.");
        }

        final Long finalCategoryId = targetCategoryId;
        Category cat = categoryRepository.findById(finalCategoryId)
            .orElseThrow(() -> RexxoException.notFound("Category not found with ID: " + finalCategoryId));
        p.setCategory(cat);
        return ResponseEntity.ok(toDetailDto(productRepository.save(p)));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> updateStock(@PathVariable Long id,
                                                            @RequestBody Map<String, Integer> body) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Product not found"));
        p.setStock(body.get("stock"));
        productRepository.save(p);
        return ResponseEntity.ok(Map.of("id", id, "stock", p.getStock()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> RexxoException.notFound("Product not found"));
        p.setIsActive(false);
        productRepository.save(p);
        return ResponseEntity.ok(Map.of("message", "Product deactivated"));
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    private ProductSummaryDto toSummaryDto(Product p) {
        String primaryImage = p.getImages().stream()
            .filter(ProductImage::getIsPrimary)
            .map(ProductImage::getImageUrl)
            .findFirst()
            .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

        return new ProductSummaryDto(
            p.getId(), p.getName(), p.getSlug(), p.getDescription(), p.getShortDescription(),
            p.getPrice(), p.getOriginalPrice(), p.getDiscountPercent(),
            primaryImage, p.getAvgRating(), p.getReviewCount(),
            p.getStock() != null ? p.getStock() : 0,
            p.getStock() != null && p.getStock() > 0,
            p.getIsFeatured() != null && p.getIsFeatured(),
            p.getSku(),
            p.getStatus() != null ? p.getStatus() : "ACTIVE",
            p.getLowStockThreshold() != null ? p.getLowStockThreshold() : 10,
            p.getCategory() != null ? p.getCategory().getName() : null,
            p.getCategory() != null ? p.getCategory().getSlug() : null
        );
    }

    private ProductDetailDto toDetailDto(Product p) {
        List<String> imageUrls = p.getImages().stream().map(ProductImage::getImageUrl).toList();
        return new ProductDetailDto(
            p.getId(), p.getName(), p.getSlug(), p.getDescription(), p.getShortDescription(),
            p.getPrice(), p.getOriginalPrice(), p.getDiscountPercent(),
            imageUrls, p.getAvgRating(), p.getReviewCount(),
            p.getStock(), p.getIsFeatured(), p.getMaterial(), p.getColor(),
            p.getDimensions(), p.getWeightGrams(), p.getSpecifications(),
            p.getCategory() != null ? p.getCategory().getName() : null,
            p.getCategory() != null ? p.getCategory().getSlug() : null,
            p.getCategory() != null ? p.getCategory().getId() : null,
            p.getCreatedAt()
        );
    }

    record ProductSummaryDto(
        Long id, String name, String slug, String description, String shortDescription,
        BigDecimal price, BigDecimal originalPrice, BigDecimal discountPercent,
        String primaryImage, BigDecimal avgRating, Integer reviewCount,
        Integer stock, boolean inStock, boolean isFeatured,
        String sku, String status, Integer lowStockThreshold,
        String categoryName, String categorySlug
    ) {}

    record ProductDetailDto(
        Long id, String name, String slug, String description, String shortDescription,
        BigDecimal price, BigDecimal originalPrice, BigDecimal discountPercent,
        List<String> images, BigDecimal avgRating, Integer reviewCount,
        Integer stock, boolean isFeatured, String material, String color,
        String dimensions, Integer weightGrams, String specifications,
        String categoryName, String categorySlug, Long categoryId,
        java.time.LocalDateTime createdAt
    ) {}

    record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 220) String slug,
        String description,
        @Size(max = 500) String shortDescription,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        BigDecimal originalPrice,
        BigDecimal discountPercent,
        @NotNull @Min(0) Integer stock,
        String sku,
        @NotNull Long categoryId,
        Boolean isFeatured,
        List<String> imageUrls,
        String material, String color, String dimensions,
        Integer weightGrams, String specifications
    ) {}
}
