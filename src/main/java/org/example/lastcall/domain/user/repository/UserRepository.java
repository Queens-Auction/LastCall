package org.example.lastcall.domain.user.repository;

import org.example.lastcall.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u JOIN RefreshToken r ON u.id = r.userId WHERE r.token = :refreshToken")
    Optional<User> findByRefreshToken(@Param("refreshToken") String refreshToken);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update User u
           set u.deleted   = true,
               u.deletedAt = CURRENT_TIMESTAMP
         where u.id = :id and u.deleted = false
    """)
    int softDeleteById(@Param("id") Long id);
}
