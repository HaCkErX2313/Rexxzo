package com.rexxo.repository;

import com.rexxo.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    List<Product> findByIsFeaturedTrueAndIsActiveTrueOrderByCreatedAtDesc();

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.isActive = true
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (CAST(:search AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """)
    Page<Product> findWithFilters(
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("search") String search,
        Pageable pageable
    );

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    Optional<Product> findBySku(String sku);

    @Query("""
        SELECT p FROM Product p
        WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:status IS NULL OR :status = 'ALL' 
             OR LOWER(p.status) = LOWER(:status)
             OR (:status = 'ACTIVE' AND p.isActive = true AND (p.status IS NULL OR p.status != 'DRAFT'))
             OR (:status = 'DRAFT' AND (p.status = 'DRAFT' OR p.isActive = false)))
        AND (:stockFilter IS NULL OR :stockFilter = 'ALL'
             OR (:stockFilter = 'OUT_OF_STOCK' AND p.stock <= 0)
             OR (:stockFilter = 'LOW_STOCK' AND p.stock > 0 AND p.stock <= COALESCE(p.lowStockThreshold, 10))
             OR (:stockFilter = 'IN_STOCK' AND p.stock > COALESCE(p.lowStockThreshold, 10)))
        AND (CAST(:search AS string) IS NULL 
             OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(p.sku) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """)
    Page<Product> findAdminProductsWithFilters(
        @Param("categoryId") Long categoryId,
        @Param("status") String status,
        @Param("stockFilter") String stockFilter,
        @Param("search") String search,
        Pageable pageable
    );
}
