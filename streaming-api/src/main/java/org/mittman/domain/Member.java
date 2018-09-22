package org.mittman.domain;

import lombok.Data;

@Data
public class Member implements Identifiable {
	private static final long serialVersionUID = 1L;
	
	private Long memberId;
	private String firstName;
	private String lastName;
	private String memberNumber;
	
	
	public Long getId() {
		return memberId;
	}
	public void setId(Long id) {
		memberId = id;		
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((memberId == null) ? 0 : memberId.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Member other = (Member) obj;
		if (memberId == null) {
			if (other.memberId != null)
				return false;
		} else if (!memberId.equals(other.memberId))
			return false;
		return true;
	}
	
}
