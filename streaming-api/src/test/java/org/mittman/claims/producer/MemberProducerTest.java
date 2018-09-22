package org.mittman.claims.producer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mittman.domain.Member;

import junit.framework.TestCase;

public class MemberProducerTest extends TestCase {
	private final int NUM_PRODUCERS = 1000;
	private final int NUM_MEMBERS = 1000;

	private volatile AtomicInteger finishedProducers;
	private volatile Exception exceptions[];
	private volatile Map<Long, Long> memberIds;

	private class ProducerRunnable implements Runnable {
		private int id;
		private MemberProducer producer;

		@Override
		public void run() {
			try {
				// Wait until all producers have been created
				synchronized(exceptions) {
					exceptions.wait();
				}

				// Generate the members
				for(int i=0; i<NUM_MEMBERS; ++i) {
					Member m = producer.produce();

					if (memberIds.put(m.getId(), m.getId()) != null) {
						long threadId = Thread.currentThread().getId();
						exceptions[id] = new Exception("Thread " + threadId + " generated duplicate member " + m.getId());
						break;
					}
				}
			}
			catch(InterruptedException e) {
				System.err.println("Thread " + Thread.currentThread().getId() + " was interrupted");
			}

			producer = null;
			finishedProducers.incrementAndGet();		
		}

	}

	@Test
	public void testProduce() throws Exception {
		exceptions = new Exception[NUM_PRODUCERS];
		finishedProducers = new AtomicInteger();

		memberIds = new ConcurrentHashMap<Long, Long>(NUM_PRODUCERS*NUM_MEMBERS);

		for (int i=0; i<NUM_PRODUCERS; ++i) {
			exceptions[i] = null;
			
			ProducerRunnable runnable = new ProducerRunnable();
			runnable.id = i;
			runnable.producer = new MemberProducer();
			
			new Thread(runnable).start();
		}

		try {
			// Wait for all of the producers to start
			// and block before notifying
			Thread.sleep(10);
			
			// Notify to start the simulation
			synchronized(exceptions) {
				exceptions.notifyAll();
			}

			// Wait until all producers are done
			int done = 0;
			do {
				Thread.sleep(10);
				done = finishedProducers.get();
			}
			while (done!=NUM_PRODUCERS);
		}
		catch(InterruptedException e) {
			System.err.println("Main thread was interrupted from sleep(");
		}
		
		// Check for problems
		for(int i=0; i<NUM_PRODUCERS; ++i) {
			if (exceptions[i] !=null) {
				throw exceptions[i];
			}
		}
	}

}
