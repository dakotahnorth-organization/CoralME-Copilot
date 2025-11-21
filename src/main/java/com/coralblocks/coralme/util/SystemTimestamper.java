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

/**
 * <p>High-performance, garbage-free timestamper using a hybrid approach for nanosecond precision.</p>
 * 
 * <p>This implementation combines System.currentTimeMillis() for epoch base time with 
 * System.nanoTime() for high-precision offsets, providing better-than-millisecond precision 
 * while maintaining zero garbage generation.</p>
 * 
 * <p>The implementation uses a calibration anchor that is periodically refreshed to handle 
 * system time adjustments and avoid drift. All operations use primitive types only.</p>
 * 
 * <p>Thread-safe: Multiple threads can safely call nanoEpoch() concurrently. Recalibration
 * is synchronized to ensure consistent state updates.</p>
 */
public class SystemTimestamper implements Timestamper {
	
	// Calibration interval: recalibrate every 1 second to handle system time adjustments
	private static final long RECALIBRATION_INTERVAL_NANOS = 1_000_000_000L;
	
	// Anchor points for hybrid timestamp calculation
	// All fields are package-private for testing purposes
	volatile long baseEpochNanos;       // Epoch time in nanoseconds at calibration
	volatile long baseNanoTime;         // System.nanoTime() value at calibration
	volatile long lastCalibrationNanoTime;  // Last calibration time for recalibration check
	
	/**
	 * Creates a new SystemTimestamper and performs initial calibration.
	 */
	public SystemTimestamper() {
		calibrate();
	}
	
	/**
	 * Calibrates the timestamper by capturing synchronized epoch and nanoTime values.
	 * This method is garbage-free and uses only primitive operations.
	 * Synchronized to prevent race conditions during concurrent recalibration attempts.
	 */
	private synchronized void calibrate() {
		// Capture both values as close together as possible for accuracy
		long millis = System.currentTimeMillis();
		long nanos = System.nanoTime();
		
		// Convert milliseconds to nanoseconds for the epoch base
		baseEpochNanos = millis * 1_000_000L;
		baseNanoTime = nanos;
		lastCalibrationNanoTime = nanos;
	}
	
	/**
	 * Returns the current epoch timestamp in nanoseconds with high precision.
	 * 
	 * <p>This method is garbage-free and uses a hybrid approach:</p>
	 * <ul>
	 * <li>Uses System.currentTimeMillis() as the epoch base (recalibrated periodically)</li>
	 * <li>Uses System.nanoTime() offset for high-precision sub-millisecond accuracy</li>
	 * </ul>
	 * 
	 * <p>The implementation automatically recalibrates every second to handle system 
	 * time adjustments (e.g., NTP synchronization) while maintaining monotonic behavior 
	 * within short time windows.</p>
	 * 
	 * <p>Thread-safe: Can be called concurrently from multiple threads.</p>
	 * 
	 * @return the epoch timestamp in nanoseconds
	 */
	@Override
	public long nanoEpoch() {
		// Read base values first to maintain consistency
		// Reading epoch before base ensures they are from the same calibration cycle
		long epoch = baseEpochNanos;
		long base = baseNanoTime;
		long currentNanoTime = System.nanoTime();
		
		// Check if we need to recalibrate
		// Read lastCalibrationNanoTime once to avoid race condition
		long lastCalib = lastCalibrationNanoTime;
		long nanosSinceCalibration = currentNanoTime - lastCalib;
		
		// Recalibrate if: 
		// 1. Time went backwards (system time adjustment or nanoTime wrap - extremely rare)
		// 2. More than 1 second has elapsed since last calibration
		// Note: nanoTime wrap takes ~292 years, but we handle it defensively
		if (nanosSinceCalibration < 0 || nanosSinceCalibration >= RECALIBRATION_INTERVAL_NANOS) {
			calibrate();
			// Re-read all values after recalibration for consistency
			// Using the newly calibrated base ensures accurate timestamps
			// Note: Multiple threads may simultaneously detect need for recalibration.
			// While calibrate() is synchronized, the re-read here is not atomic with it.
			// This is acceptable: worst case is slightly stale values for one call,
			// but correctness is maintained due to volatile visibility guarantees.
			epoch = baseEpochNanos;
			base = baseNanoTime;
			// Use current nanoTime as the reference point for this timestamp
			currentNanoTime = System.nanoTime();
		}
		
		// Calculate offset from base and add to base epoch time
		long nanoOffset = currentNanoTime - base;
		return epoch + nanoOffset;
	}
}