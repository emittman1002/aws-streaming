package org.mittman.claims.producer;

import org.mittman.domain.Identifiable;

public interface Producer<T extends Identifiable> {
	T produce();
}
