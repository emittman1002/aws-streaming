package org.mittman.domain;

import java.time.LocalDate;

import lombok.Data;

@Data
public class Claim implements Identifiable {
	private static final long serialVersionUID = 1L;
	
	private Long claimId;
	private Member member;
	private ClaimServiceCode claimServiceCode;
	private String description;
	private LocalDate startDateOfService;
	private LocalDate endDateOfService;
	
	
	public Long getId() {
		return claimId;
	}
	public void setId(Long id) {
		claimId = id;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((claimId == null) ? 0 : claimId.hashCode());
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
		Claim other = (Claim) obj;
		if (claimId == null) {
			if (other.claimId != null)
				return false;
		} else if (!claimId.equals(other.claimId))
			return false;
		return true;
	}

}
