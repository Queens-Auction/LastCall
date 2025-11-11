package org.example.lastcall.fixture;

import java.time.LocalDateTime;

import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestAuctionService {
	@Autowired
	AuctionRepository repository;

	@Autowired
	ProductRepository productRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Auction create(User user) {
		Product product = productRepository.save(
			Product.of(user, "test_product", Category.ACCESSORY, "test_description"));
		return repository.save(Auction.of(user, product, AuctionFixture.createRequest()));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Auction create(User user, Long bidStep) {
		Product product = productRepository.save(
			Product.of(user, "test_product", Category.ACCESSORY, "test_description"));
		return repository.save(Auction.of(user, product, AuctionFixture.createRequest(bidStep)));
	}

	static class AuctionFixture {
		public static AuctionCreateRequest createRequest() {
			AuctionCreateRequest request = new AuctionCreateRequest();
			ReflectionTestUtils.setField(request, "startingBid", 1000L);
			ReflectionTestUtils.setField(request, "bidStep", 100L);
			ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().minusDays(1));
			ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().plusDays(2));
			return request;
		}

		public static AuctionCreateRequest createRequest(Long bidStep) {
			AuctionCreateRequest request = new AuctionCreateRequest();
			ReflectionTestUtils.setField(request, "startingBid", 1000L);
			ReflectionTestUtils.setField(request, "bidStep", bidStep);
			ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().minusDays(1));
			ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().plusDays(2));
			return request;
		}
	}
}
