package com.rexxo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_slug", columnList = "slug", unique = true),
    @Index(name = "idx_products_category", columnList = "category_id"),
    @Index(name = "idx_products_active", columnList = "is_active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(nullable = false)
    @Builder.Default
    private String sku = "";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, DRAFT, OUT_OF_STOCK, ARCHIVED

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "products"})
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications; // JSON string of key-value pairs

    @Column(length = 100)
    private String material;

    @Column(length = 50)
    private String color;

    @Column(length = 100)
    private String dimensions;

    /** Weight in grams for shipping calculations */
    @Column(name = "weight_grams")
    private Integer weightGrams;

    /** Shipping dimensions in cm (used for volumetric weight calculation) */
    @Column(name = "length_cm", precision = 6, scale = 2)
    private java.math.BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 6, scale = 2)
    private java.math.BigDecimal widthCm;

    @Column(name = "height_cm", precision = 6, scale = 2)
    private java.math.BigDecimal heightCm;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getStatus() {
        if (status != null && !status.isBlank()) {
            return status;
        }
        if (Boolean.FALSE.equals(isActive)) {
            return "DRAFT";
        }
        if (stock != null && stock <= 0) {
            return "OUT_OF_STOCK";
        }
        return "ACTIVE";
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold != null ? lowStockThreshold : 10;
    }
}

