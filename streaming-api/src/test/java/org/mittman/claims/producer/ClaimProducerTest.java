package org.mittman.claims.producer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mittman.domain.Claim;

import junit.framework.TestCase;

public class ClaimProducerTest extends TestCase {
	private final int NUM_PRODUCERS = 10000;
	private final int NUM_CLAIMS = 1000;
	
	private final int MBR_REUSE_PCT = 25;
	private final int MBR_LIMIT = 5000;

	private volatile AtomicInteger finishedProducers;
	private volatile Exception exceptions[];
	private volatile Map<Long, Long> ids;


	private class ProducerRunnable implements Runnable {
		private int id;
		private ClaimProducer producer;

		@Override
		public void run() {
			try {
				// Wait until all producers have been created
				synchronized(exceptions) {
					exceptions.wait();
				}

				// Produce the objects
				for(int i=0; i<NUM_CLAIMS; ++i) {
					Claim c = producer.produce();

					if (ids.put(c.getId(), c.getId()) != null) {
						long threadId = Thread.currentThread().getId();
						exceptions[id] = new Exception("Thread " + threadId + " generated duplicate claim " + c.getId());
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

		ids = new ConcurrentHashMap<Long, Long>(NUM_PRODUCERS*NUM_CLAIMS);

		for (int i=0; i<NUM_PRODUCERS; ++i) {
			exceptions[i] = null;
			
			ProducerRunnable runnable = new ProducerRunnable();
			runnable.id = i;
			ClaimProducer producer = new ClaimProducer();
			producer.setMemberProducer(new ReusedMemberProducer(MBR_REUSE_PCT, MBR_LIMIT) );
			runnable.producer = producer;
			
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
