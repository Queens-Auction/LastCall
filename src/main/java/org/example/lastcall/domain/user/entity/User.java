package org.example.lastcall.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.user.enums.Role;

import java.util.UUID;


@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID publicId;

    @Column(name = "username", nullable = false, length = 10)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 30)
    private String email;

    @Column(name = "password", nullable = false, length = 50)
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

    // 기본 기능 구현 후 구현 예정
//    @Enumerated(EnumType.STRING)
//    @Column(name = "user_role", nullable = false, length = 10)
//    private UserRole userRole;
}
