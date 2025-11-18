package org.example.lastcall.common.monitoring;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {
    private final UserRepository userRepository;

    @Override
    public Health health() {
        boolean dbUp = checkDatabaseConnection();
        if (dbUp) {
            return Health.up().withDetail("database", "OK").build();
        }

        return Health.down().withDetail("database", "DOWN").build();
    }

    private boolean checkDatabaseConnection() {
        long count = userRepository.count();

        return true;
    }
}
