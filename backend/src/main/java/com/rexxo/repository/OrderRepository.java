package com.rexxo.repository;

import com.rexxo.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Order> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status NOT IN ('CANCELLED', 'REFUNDED')")
    long countActiveOrders();

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.product.id = :productId")
    boolean existsByProductId(@Param("productId") Long productId);
}
