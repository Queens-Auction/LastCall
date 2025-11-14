package org.example.lastcall.fixture;

import java.time.LocalDateTime;
import java.util.Optional;

import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Auction createOngoingAuction(User seller, Category category, Long startingBid, Long bidStep) {
        Product product = productRepository.save(
                Product.of(seller, "ongoing_product", category, "ongoing_auction"));

        AuctionCreateRequest request = new AuctionCreateRequest();
        ReflectionTestUtils.setField(request, "startingBid", startingBid);
        ReflectionTestUtils.setField(request, "bidStep", bidStep);
        ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().minusMinutes(5));
        ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().plusDays(1));

        Auction auction = Auction.of(seller, product, request);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.ONGOING);

        return repository.saveAndFlush(auction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Auction save(Auction auction) {
        return repository.saveAndFlush(auction);
    }

    public Auction findById(Long id) {
        Optional<Auction> auction = repository.findById(id);

        return auction.orElseThrow(() -> new IllegalStateException("테스트용 경매가 존재하지 않습니다."));
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
