package org.example.lastcall.domain.product.repository;

import org.example.lastcall.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p JOIN FETCH p.user WHERE p.id = :productId")
    Optional<Product> findByIdWithUser(Long productId);

    Page<Product> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Optional<Product> findByIdAndDeletedFalse(Long productId);
}
