package org.example.lastcall.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // 기본 기능 구현 후 구현 예정
//    @Enumerated(EnumType.STRING)
//    @Column(name = "user_role", nullable = false, length = 10)
//    private UserRole userRole;
}
