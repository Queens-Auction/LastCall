package org.example.lastcall.fixture;

import java.util.UUID;

import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;

public class UserFixture {
	public static User createUser() {
		return User.of(
			UUID.randomUUID(),          // publicId
			"tester",                   // username
			"테스트유저",                 // nickname
			"user200@test.com",            // email
			"encodedPassword",          // 이미 인코딩된 비밀번호 (테스트용)
			"서울시 테스트구 테스트로 123", // address
			"12345",                    // postcode
			"테스트아파트 101동 202호",     // detailAddress
			"010-1234-5678",            // phoneNumber
			Role.USER                   // userRole
		);
	}

	public static User createUser(String email, String nickname) {
		return User.of(
			UUID.randomUUID(),          // publicId
			"tester",                   // username
			nickname,                 // nickname
			email,
			"encodedPassword",          // 이미 인코딩된 비밀번호 (테스트용)
			"서울시 테스트구 테스트로 123", // address
			"12345",                    // postcode
			"테스트아파트 101동 202호",     // detailAddress
			"010-1234-5678",            // phoneNumber
			Role.USER                   // userRole
		);
	}
}
