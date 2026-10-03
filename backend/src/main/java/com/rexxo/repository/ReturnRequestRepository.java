package com.rexxo.repository;

import com.rexxo.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    Page<ReturnRequest> findByUserId(Long userId, Pageable pageable);

    Optional<ReturnRequest> findByIdAndUserId(Long id, Long userId);

    Optional<ReturnRequest> findByReturnNumber(String returnNumber);

    List<ReturnRequest> findByOrderId(Long orderId);

    Page<ReturnRequest> findByStatus(ReturnRequest.ReturnStatus status, Pageable pageable);

    long countByStatus(ReturnRequest.ReturnStatus status);

    boolean existsByOrderIdAndStatusNotIn(Long orderId, Collection<ReturnRequest.ReturnStatus> excludedStatuses);

    @Query("SELECT r FROM ReturnRequest r LEFT JOIN FETCH r.items i LEFT JOIN FETCH i.product WHERE r.id = :id")
    Optional<ReturnRequest> findByIdWithItems(@Param("id") Long id);
}
