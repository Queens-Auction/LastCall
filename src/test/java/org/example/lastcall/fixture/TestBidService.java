package org.example.lastcall.fixture;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestBidService {
	@Autowired
	BidRepository repository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Bid create(Auction auction, User user) {
		return repository.save( Bid.of(
                auction.getStartingBid() + auction.getBidStep(),
                auction, user));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Bid createMaxBid(Auction auction, User user) {
		return repository.save(
			Bid.of(auction.getStartingBid() + auction.getBidStep() + auction.getBidStep(),
                    auction, user));
	}
}
