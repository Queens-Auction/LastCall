package org.example.lastcall.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceApi {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {

        return userRepository.findById(id);
    }
}
