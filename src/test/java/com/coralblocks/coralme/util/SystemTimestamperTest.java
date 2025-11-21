/* 
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.coralblocks.coralme.util;

import org.junit.Assert;
import org.junit.Test;

public class SystemTimestamperTest {
	
	private static final long NANOS_PER_MILLI = 1_000_000L;
	
	@Test
	public void testBasicTimestampProgression() {
		Timestamper t = new SystemTimestamper();
		
		long start = t.nanoEpoch();
		
		try {
			Thread.sleep(5);
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		long end = t.nanoEpoch();
		
		Assert.assertTrue("End timestamp should be greater than start", end > start);
		Assert.assertTrue("Difference should be at least 1ms", end - start > NANOS_PER_MILLI);
	}
	
	@Test
	public void testMonotonicBehavior() {
		Timestamper t = new SystemTimestamper();
		
		// Take multiple rapid timestamps and verify they are monotonically increasing
		long prev = t.nanoEpoch();
		for (int i = 0; i < 1000; i++) {
			long current = t.nanoEpoch();
			Assert.assertTrue("Timestamps should be monotonically increasing", current >= prev);
			prev = current;
		}
	}
	
	@Test
	public void testPrecisionBetterThanMillisecond() {
		Timestamper t = new SystemTimestamper();
		
		// Take two timestamps in rapid succession
		long t1 = t.nanoEpoch();
		long t2 = t.nanoEpoch();
		
		// The difference should be measurable in nanoseconds (typically < 1ms)
		long diff = t2 - t1;
		Assert.assertTrue("Difference should be positive", diff >= 0);
		// The difference should be small (less than 1ms for two immediate calls)
		Assert.assertTrue("Difference should be sub-millisecond for rapid calls", diff < NANOS_PER_MILLI);
	}
	
	@Test
	public void testEpochTimeReasonable() {
		Timestamper t = new SystemTimestamper();
		
		long timestamp = t.nanoEpoch();
		
		// Convert to milliseconds for comparison with System.currentTimeMillis()
		long timestampMillis = timestamp / NANOS_PER_MILLI;
		long systemMillis = System.currentTimeMillis();
		
		// Should be within 1 second of each other
		long diff = Math.abs(timestampMillis - systemMillis);
		Assert.assertTrue("Epoch time should be close to System.currentTimeMillis()", diff < 1000);
	}
	
	@Test
	public void testRecalibration() throws InterruptedException {
		Timestamper t = new SystemTimestamper();
		
		long t1 = t.nanoEpoch();
		
		// Sleep for more than the recalibration interval (1 second)
		Thread.sleep(1100);
		
		long t2 = t.nanoEpoch();
		
		// Verify timestamps still progress correctly after recalibration
		Assert.assertTrue("Timestamp after recalibration should be greater", t2 > t1);
		
		// Verify the difference is approximately 1.1 seconds (with some tolerance)
		long diffNanos = t2 - t1;
		long expectedNanos = 1100L * NANOS_PER_MILLI;
		long tolerance = 100L * NANOS_PER_MILLI; // 100ms tolerance
		Assert.assertTrue("Time difference should be approximately 1.1 seconds", 
			Math.abs(diffNanos - expectedNanos) < tolerance);
	}
	
	@Test
	public void testZeroGarbageBehavior() {
		// This test verifies that calling nanoEpoch many times doesn't cause issues
		// Actual zero-garbage verification is done by NoGCTest
		Timestamper t = new SystemTimestamper();
		
		// Call many times to ensure no accumulation issues
		for (int i = 0; i < 100000; i++) {
			long timestamp = t.nanoEpoch();
			Assert.assertTrue("Timestamp should be positive", timestamp > 0);
		}
	}
	
	@Test
	public void testMultipleInstances() {
		// Verify that multiple timestamper instances work correctly
		Timestamper t1 = new SystemTimestamper();
		Timestamper t2 = new SystemTimestamper();
		
		long ts1 = t1.nanoEpoch();
		long ts2 = t2.nanoEpoch();
		
		// Both should give similar timestamps (within a few milliseconds)
		long diff = Math.abs(ts1 - ts2);
		long tenMillisNanos = 10L * NANOS_PER_MILLI;
		Assert.assertTrue("Timestamps from different instances should be similar", diff < tenMillisNanos);
	}
	
	@Test
	public void testThreadSafety() throws InterruptedException {
		final Timestamper t = new SystemTimestamper();
		final int numThreads = 10;
		final int iterationsPerThread = 10000;
		final Thread[] threads = new Thread[numThreads];
		final long[][] results = new long[numThreads][iterationsPerThread];
		
		// Start multiple threads calling nanoEpoch concurrently
		for (int i = 0; i < numThreads; i++) {
			final int threadIndex = i;
			threads[i] = new Thread(() -> {
				for (int j = 0; j < iterationsPerThread; j++) {
					results[threadIndex][j] = t.nanoEpoch();
				}
			});
			threads[i].start();
		}
		
		// Wait for all threads to complete
		for (Thread thread : threads) {
			thread.join();
		}
		
		// Verify all timestamps are valid and reasonable
		long minTimestamp = Long.MAX_VALUE;
		long maxTimestamp = Long.MIN_VALUE;
		
		for (int i = 0; i < numThreads; i++) {
			for (int j = 0; j < iterationsPerThread; j++) {
				long ts = results[i][j];
				Assert.assertTrue("Timestamp should be positive", ts > 0);
				if (ts < minTimestamp) minTimestamp = ts;
				if (ts > maxTimestamp) maxTimestamp = ts;
			}
		}
		
		// All timestamps should be within a reasonable time window (a few seconds max)
		long spanNanos = maxTimestamp - minTimestamp;
		Assert.assertTrue("Timestamp span should be reasonable", spanNanos < 10_000_000_000L); // 10 seconds
		
		// Verify each thread's timestamps are monotonic
		for (int i = 0; i < numThreads; i++) {
			for (int j = 1; j < iterationsPerThread; j++) {
				Assert.assertTrue("Timestamps within same thread should be monotonic", 
					results[i][j] >= results[i][j-1]);
			}
		}
	}
}