package org.example.lastcall.fixture;

import java.util.UUID;

import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;

public class UserFixture {
	public static User createUser() {
		return User.of(
			UUID.randomUUID(),
			"tester",
			"테스트유저",
			"user200@test.com",
			"encodedPassword",
			"서울시 테스트구 테스트로 123",
			"12345",
			"테스트아파트 101동 202호",
			"010-1234-5678",
			Role.USER);
	}

	public static User createUser(String email, String nickname) {
		return User.of(
			UUID.randomUUID(),
			"tester",
			nickname,
			email,
			"encodedPassword",
			"서울시 테스트구 테스트로 123",
			"12345",
			"테스트아파트 101동 202호",
			"010-1234-5678",
			Role.USER);
	}
}
