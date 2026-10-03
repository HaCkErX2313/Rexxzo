package com.rexxo.repository;

import com.rexxo.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.items i LEFT JOIN FETCH i.product p LEFT JOIN FETCH p.images WHERE w.user.id = :userId")
    Optional<Wishlist> findByUserIdWithItems(@Param("userId") Long userId);

    Optional<Wishlist> findByUserId(Long userId);
}
