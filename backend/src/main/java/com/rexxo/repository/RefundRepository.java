package com.rexxo.repository;

import com.rexxo.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByRefundId(String refundId);
    Page<Refund> findByOrderId(Long orderId, Pageable pageable);
    Page<Refund> findByStatus(Refund.RefundStatus status, Pageable pageable);
}
