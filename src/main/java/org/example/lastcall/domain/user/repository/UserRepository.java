package org.example.lastcall.domain.user.repository;

import org.example.lastcall.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndDeletedFalse(String email);

    @Query("SELECT u FROM User u JOIN RefreshToken r ON u.id = r.userId WHERE r.token = :refreshToken")
    Optional<User> findByRefreshToken(@Param("refreshToken") String refreshToken);
}
