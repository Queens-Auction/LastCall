package org.example.lastcall.domain.product.repository;

import org.example.lastcall.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findAllByUserId(Long userId, Pageable pageable);

    //상품 소유자 검증
    @Query("SELECT p " +
            "FROM Product p " +
            "JOIN FETCH p.user " +
            "WHERE p.id = :productId")
    Optional<Product> findByIdWithUser(Long productId);
}
