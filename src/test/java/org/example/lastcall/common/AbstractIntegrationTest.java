package org.example.lastcall.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test") // ✅ application-test.yml 사용
@TestPropertySource(properties = {
	"spring.cloud.aws.region.static=dummy",
	"jwt.secret=u9vL0W5FvC47qkSmFl8D9DzRrXz7pXzMwx3xYV0I6MMktQoOE6rwpOBjXDEj9ywr4K9Q2CFw5UazUvH1wj3jbg==",
	"auth.baseUrl=dummy",
	"auth.security.cookie.secure=false",
	"spring.mail.username=dummy",
	"spring.mail.password=dummy",
	"spring.mail.host=dummy",
	"spring.mail.port=587",
	"spring.mail.transport.protocol=smtp",
	"spring.mail.smtp.auth=false",
	"spring.mail.smtp.starttls.enable=false",
	"spring.mail.smtp.connectiontimeout=5s",
	"spring.mail.smtp.timeout=5s",
	"spring.mail.smtp.writetimeout=5s",
	"spring.cloud.aws.s3.bucket=dummy",
})
public abstract class AbstractIntegrationTest {

}
