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
 * High-performance, garbage-free timestamper with nanosecond precision.
 * 
 * <p>Combines System.currentTimeMillis() for epoch base with System.nanoTime() 
 * for high-precision offsets. Recalibrates every second for NTP adjustments.</p>
 * 
 * <p>Thread-safe with synchronized calibration.</p>
 */
public class SystemTimestamper implements Timestamper {
	
	private static final long RECALIBRATION_INTERVAL_NANOS = 1_000_000_000L;
	
	volatile long baseEpochNanos;
	volatile long baseNanoTime;
	volatile long lastCalibrationNanoTime;
	
	/**
	 * Creates a new SystemTimestamper and performs initial calibration.
	 */
	public SystemTimestamper() {
		calibrate();
	}
	
	private synchronized void calibrate() {
		long nanos = System.nanoTime();
		baseEpochNanos = System.currentTimeMillis() * 1_000_000L;
		baseNanoTime = nanos;
		lastCalibrationNanoTime = nanos;
	}
	
	/**
	 * Returns the current epoch timestamp in nanoseconds with high precision.
	 * 
	 * <p>Uses System.currentTimeMillis() for epoch base (recalibrated every second) 
	 * plus System.nanoTime() offset for sub-millisecond precision.</p>
	 * 
	 * <p>Thread-safe and garbage-free.</p>
	 * 
	 * @return the epoch timestamp in nanoseconds
	 */
	@Override
	public long nanoEpoch() {
		long currentNanoTime = System.nanoTime();
		long lastCalib = lastCalibrationNanoTime;
		
		// Recalibrate if more than 1 second elapsed or time went backwards
		if (currentNanoTime - lastCalib < 0 || 
		    currentNanoTime - lastCalib >= RECALIBRATION_INTERVAL_NANOS) {
			calibrate();
			currentNanoTime = System.nanoTime();
		}
		
		// Read base values together to ensure consistency
		long base = baseNanoTime;
		long epoch = baseEpochNanos;
		
		return epoch + (currentNanoTime - base);
	}
}