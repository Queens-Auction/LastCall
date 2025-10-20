package org.example.lastcall.domain.auth.repository;

import org.example.lastcall.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRepository extends JpaRepository<User, Long> {
}
