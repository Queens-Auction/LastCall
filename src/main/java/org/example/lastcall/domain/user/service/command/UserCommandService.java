package org.example.lastcall.domain.user.service.command;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.config.PasswordEncoder;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.repository.RefreshTokenRepository;
import org.example.lastcall.domain.user.dto.request.PasswordChangeRequest;
import org.example.lastcall.domain.user.dto.request.UserUpdateRequest;
import org.example.lastcall.domain.user.dto.response.UserProfileResponse;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.exception.UserErrorCode;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.example.lastcall.domain.auth.enums.RefreshTokenStatus.ACTIVE;
import static org.example.lastcall.domain.auth.enums.RefreshTokenStatus.REVOKED;
import static org.example.lastcall.domain.user.exception.UserErrorCode.USER_ALREADY_DELETED;

@Service
@RequiredArgsConstructor
@Transactional()
public class UserCommandService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserProfileResponse updateMyProfile(Long userId, UserUpdateRequest req) {
        if (req == null || req.isEmpty()) {
            throw new BusinessException(UserErrorCode.NO_FIELDS_TO_UPDATE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) throw new BusinessException(USER_ALREADY_DELETED);

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

    public void changeMyPassword(Long userId, PasswordChangeRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (user.isDeleted()) throw new BusinessException(USER_ALREADY_DELETED);

        user.validatePassword(passwordEncoder, req.oldPassword());
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.SAME_AS_OLD_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(req.newPassword()));
        user.markPasswordChangedNow();

        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), ACTIVE, REVOKED);
    }
}
