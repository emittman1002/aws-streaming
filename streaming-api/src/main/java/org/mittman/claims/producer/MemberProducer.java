package org.mittman.claims.producer;

import java.util.concurrent.atomic.AtomicLong;

import org.mittman.domain.Member;

import lombok.Getter;
import lombok.Setter;

public class MemberProducer implements Producer<Member> {
	@Getter@Setter
	private String firstName = "Member";
	private String lastNamePrefix = "Number";
	
	private static AtomicLong nextMemberId;
	
	{
		nextMemberId = new AtomicLong();
	}
	
	@Override
	public Member produce() {
		Member m = new Member();
		long id = nextMemberId.incrementAndGet();
		m.setId(id);
		m.setFirstName(firstName);
		m.setLastName(lastNamePrefix + id );
		m.setMemberNumber( Long.toHexString(id) );
		
		return m;
	}
}
