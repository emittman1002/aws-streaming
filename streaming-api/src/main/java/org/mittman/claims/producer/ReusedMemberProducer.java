package org.mittman.claims.producer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.mittman.domain.Member;

/**
 * A MemberProducer that returns an existing member some
 * percentage of the time rather than always creating a new one.
 * 
 * A reuse percent of 0 always creates a new member.
 * A reuse percent of 100 always returns the same member.
 * A reuse percent of 25 creates a new member 75% of the time.
 * 
 * There is a limit on the number of reusable members.  Once the limit
 * is reached, new members that are created will not be reusable.
 * 
 * The reuse rate defaults to 25, the member limit defaults to 1000.
 * 
 * @author Ed Mittman
 *
 */
public class ReusedMemberProducer extends MemberProducer {
	private static List<Member> members;
	{
		members = Collections.synchronizedList(new ArrayList<Member>());
	}
	
	private int reusePercent;
	private int reusableMemberLimit;
	
	
	public ReusedMemberProducer() {
		this(25, 1000);
	}
	
	public ReusedMemberProducer(int reusePercent, int reusableMemberLimit) {
		if (reusePercent<0 || reusePercent>100) {
			throw new IllegalArgumentException("Invalid reuse percent " + reusePercent);
		}
		
		if (reusableMemberLimit<0) {
			throw new IllegalArgumentException("Invalid reusableMemberLimit " + reusableMemberLimit);
		}
		
		this.reusePercent = reusePercent;
		this.reusableMemberLimit = reusableMemberLimit;
	}
	
	@Override
	public Member produce() {
		Member m = null;
		
		if (reusePercent>0 ) {
			boolean reuse = 
					!members.isEmpty() &&
					(reusePercent==100 ||
					ThreadLocalRandom.current().nextInt(100)<reusePercent);
			
			if (reuse) {
				int idx;
				if (reusePercent!=100) {
					idx = ThreadLocalRandom.current().nextInt(members.size());
				}
				else {
					idx = 0;
				}
				m = members.get(idx);
			}
		}

		if (m == null){
			m = super.produce();
			if (members.size() < reusableMemberLimit) {
				members.add(m);
			}
		}

		return m;
	}
}
