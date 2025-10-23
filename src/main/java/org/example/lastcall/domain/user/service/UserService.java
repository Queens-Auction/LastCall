package org.example.lastcall.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.user.dto.request.UserUpdateRequest;
import org.example.lastcall.domain.user.dto.response.UserProfileResponse;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.exception.UserErrorCode;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceApi {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(UserErrorCode.USER_ALREADY_DELETED);
        }
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UserUpdateRequest req) {
        if (req == null || req.isEmpty()) {
            throw new BusinessException(UserErrorCode.NO_FIELDS_TO_UPDATE); // 없으면 추가 추천
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) throw new BusinessException(UserErrorCode.USER_ALREADY_DELETED);

        // nickname 변경 시 중복 방지
        if (req.nickname() != null && !req.nickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(req.nickname())) {
                throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
            }
            user.changeNickname(req.nickname());
        }

        if (req.phoneNumber() != null) {
            user.changePhoneNumber(req.phoneNumber());
        }

        if (req.addressInfo() != null) {
            var a = req.addressInfo();
            user.changeAddress(a.address(), a.postcode(), a.detailAddress());
        }

        return UserProfileResponse.from(user);
    }
}
