package org.example.lastcall.domain.user.service;

import org.example.lastcall.domain.user.entity.User;

import java.util.Optional;

public interface UserServiceApi {
    Optional<User> findById(Long id);
}
