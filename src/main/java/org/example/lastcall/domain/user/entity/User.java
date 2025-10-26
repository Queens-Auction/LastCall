package org.example.lastcall.domain.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.example.lastcall.common.config.PasswordEncoder;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.exception.AuthErrorCode;
import org.example.lastcall.domain.user.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID publicId;

    @Column(name = "username", nullable = false, length = 10)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 30)
    private String email;

    @Column(name = "password", nullable = false, length = 60) // Bcrypt
    private String password;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @Column(name = "address", nullable = false, length = 50)
    private String address;

    @Column(name = "postcode", nullable = false, length = 5)
    private String postcode;

    @Column(name = "detail_address", nullable = false, length = 50)
    private String detailAddress;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 5)
    private Role userRole; // ENUM(ADMIN, USER)

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    private User(UUID publicId,
                 String username,
                 String nickname,
                 String email,
                 String password,
                 String address,
                 String postcode,
                 String detailAddress,
                 String phoneNumber,
                 Role userRole) {
        this.publicId = publicId;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.address = address;
        this.postcode = postcode;
        this.detailAddress = detailAddress;
        this.phoneNumber = phoneNumber;
        this.userRole = userRole;
    }

    public static User createForSignUp(UUID publicId,
                                       String username,
                                       String nickname,
                                       String email,
                                       String encodedPassword,
                                       String address,
                                       String postcode,
                                       String detailAddress,
                                       String phoneNumber,
                                       Role userRole)
    {
        return new User(publicId,
                username,
                nickname,
                email,
                encodedPassword,
                address,
                postcode,
                detailAddress,
                phoneNumber,
                userRole);
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;

    }
    public void changeAddress(String address,
                              String postcode,
                              String detailAddress) {
        if (address != null)
        {
            this.address = address;
        }
        if (postcode != null) {
            this.postcode = postcode;
        }
        if (detailAddress != null) {
            this.detailAddress = detailAddress;
        }
    }

    public void validatePassword(PasswordEncoder passwordEncoder, String requestedPassword) {
        if (!passwordEncoder.matches(requestedPassword, password)) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
        }
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void markPasswordChangedNow() {
        this.passwordChangedAt = LocalDateTime.now();
    }

}
