package org.mittman.claims.stream;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.mittman.claims.producer.ClaimProducer;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;

/**
 * Produces claims and streams them to Kinesis
 * 
 * @author Ed Mittman
 *
 */
public class KinesisClaimsProducer {
	private Properties properties;
	private String streamName;
	private int maxShardSize;
	private int maxRetries;
	private ClaimProducer claimProducer;

	public void produceAndStreamClaims() throws Exception {
		configure();

		AmazonKinesis kinesisClient = createClient();
		
		String prefix = Long.toString( System.currentTimeMillis() );
		
		Random random = new Random();
		
		try {
			for (int i = 0; i < 20; ++i) {
				int numRecs = random.nextInt(5000) + 1;
				streamClaims(kinesisClient, prefix+"."+(i+1), numRecs);
			}
		} finally {
			// kinesisClient.shutdown();
		}
	}

	private void configure() throws Exception {
		String filename = getClass().getSimpleName() + ".properties";
		InputStream in = null;

		try {
			in = getClass().getClassLoader().getResourceAsStream(filename);
			if (in == null) {
				try {
					in = new FileInputStream(filename);
				} catch (FileNotFoundException e) {
					throw e;
				}
			}

			properties = new Properties();
			properties.load(in);
		} finally {
			if (in != null) {
				in.close();
			}
		}

		streamName = properties.getProperty("aws.kinesis.stream.name");
		maxShardSize = Integer.valueOf(properties.getProperty("aws.kinesis.max.shard.size"));
		maxRetries = Integer.valueOf(properties.getProperty("max.retries"));
	}

	private AmazonKinesis createClient() throws Exception {
		return AmazonKinesisClientBuilder.standard().build();
	}

	private void streamClaims(AmazonKinesis kinesisClient, String prefix, int numRecordsToSend) throws Exception {		
		System.out.println("Sending " + numRecordsToSend + " claims");

		int shardNum = 0;
		int start = 0;
		int end = 0;
		while (numRecordsToSend > 0) {
			int shardSize;
			if (numRecordsToSend>maxShardSize) {
				shardSize = maxShardSize;
			}
			else {
				shardSize = numRecordsToSend;
			}		

			++shardNum;
			start = end+1;
			end = start + shardSize;
			sendShard(kinesisClient, prefix + "." + shardNum, start, end);	

			numRecordsToSend -= shardSize;
		}
	}
	
	private void sendShard(AmazonKinesis kinesisClient, String prefix, int start, int end) throws Exception {
		PutRecordsRequest putRecordsRequest = new PutRecordsRequest();
		putRecordsRequest.setStreamName(streamName);

		List<PutRecordsRequestEntry> requestList = new ArrayList<>();

		for (int i=start; i<end; i++) {
			PutRecordsRequestEntry requestEntry = new PutRecordsRequestEntry();
			String value = prefix + "." + i + "\n";
			requestEntry.setData(ByteBuffer.wrap(value.getBytes()));
			requestEntry.setPartitionKey(String.format("%s", value));
			
			requestList.add(requestEntry);
		}

		putRecordsRequest.setRecords(requestList);
		PutRecordsResult result = sendRecords(kinesisClient, putRecordsRequest);

		// Resend records that failed
		int retryCount = 0;
		int failedCt = result.getFailedRecordCount();
		
		while (failedCt != 0 && ++retryCount<=maxRetries) {
			List<PutRecordsResultEntry> resultEntryList = result.getRecords();
			
			List<PutRecordsRequestEntry> retryRequestEntryList = new ArrayList<>(failedCt);
			for (int i=0; i<resultEntryList.size(); ++i) {
				PutRecordsResultEntry resultEntry = resultEntryList.get(i);
				
				if (resultEntry.getSequenceNumber() != null || resultEntry.getShardId() != null) {
					retryRequestEntryList.add( requestList.get(i) );
				}
			}
			
			PutRecordsRequest retryRequest = new PutRecordsRequest();
			retryRequest.setStreamName(streamName);
			retryRequest.setRecords(retryRequestEntryList);
			requestList = retryRequestEntryList;

			result = sendRecords(kinesisClient, retryRequest);
			
			failedCt = result.getFailedRecordCount();
		}
		
		if (retryCount>5) {
			System.err.println(prefix + ":  " +
					"Giving up on " + failedCt + " entries");
		}
		else {
			System.out.println(prefix + ":  " +
					"All entries sent in " + retryCount + " retries");
		}
	}

	private PutRecordsResult sendRecords(AmazonKinesis kinesisClient, PutRecordsRequest putRecordsRequest) {
		
		return kinesisClient.putRecords(putRecordsRequest);
	}
	
	public static void main(String[] args) {
		System.out.println("Starting simulation...");
		
		KinesisClaimsProducer producer = new KinesisClaimsProducer();

		try {
			producer.produceAndStreamClaims();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Done.");
	}

}
