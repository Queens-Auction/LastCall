package org.example.lastcall.fixture;

import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestUserService {
	@Autowired
	private UserRepository repository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public User saveTestUser() {
		return repository.save(UserFixture.createUser());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public User saveTestUser(String email, String nickname) {
		return repository.save(UserFixture.createUser(email, nickname));
	}
}
