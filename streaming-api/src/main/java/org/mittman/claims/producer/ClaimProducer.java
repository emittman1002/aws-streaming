package org.mittman.claims.producer;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.text.WordUtils;
import org.mittman.domain.Claim;
import org.mittman.domain.ClaimServiceCode;
import org.mittman.domain.Member;

import lombok.Getter;
import lombok.Setter;

public class ClaimProducer implements Producer<Claim> {
	private static AtomicLong nextClaimId;
	{
		nextClaimId = new AtomicLong();
	}
	
	private final int NUM_CLAIM_CODES;
	
	@Getter@Setter
	private MemberProducer memberProducer;
	
	
	public ClaimProducer() {
		NUM_CLAIM_CODES = ClaimServiceCode.values().length;
		memberProducer = new MemberProducer();
	}
	
	@Override
	public Claim produce() {
		long id = nextClaimId.incrementAndGet();
		
		Claim c = new Claim();
		c.setId(id);
		
		Member m = memberProducer.produce();
		c.setMember(m);
		
		int ordinal = ThreadLocalRandom.current().nextInt(NUM_CLAIM_CODES);
		c.setClaimServiceCode( ClaimServiceCode.values()[ordinal] );
		
		c.setDescription( WordUtils.capitalizeFully(c.getClaimServiceCode().toString()) );
		
		LocalDate now = LocalDate.now();
		int startDateDifference = startDateDifference();
		int endDateDifference = endDateDifference(startDateDifference);
		
		c.setStartDateOfService(now.minusDays(startDateDifference));
		c.setEndDateOfService(now.minusDays(endDateDifference));
		
		return c;
	}

	int startDateDifference() {
		return ThreadLocalRandom.current().nextInt(2*365);
	}
	
	int endDateDifference(int startDateDifference) {
		if (startDateDifference!=0) {
			return ThreadLocalRandom.current().nextInt(startDateDifference);
		}
		return startDateDifference;
	}
}
