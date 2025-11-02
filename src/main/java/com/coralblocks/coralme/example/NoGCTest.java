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
package com.coralblocks.coralme.example;

import com.coralblocks.coralme.Order;
import com.coralblocks.coralme.Order.Side;
import com.coralblocks.coralme.Order.TimeInForce;
import com.coralblocks.coralme.OrderBook;
import com.coralblocks.coralme.OrderBookAdapter;
import com.coralblocks.coralme.OrderBookListener;

/**
 * <p>Run this test with <b>-verbose:gc</b> and look for any GC activity. <b>You must not see any.</b></p>
 * 
 * <p>Alternatively you can pass <i>true</i> to createGarbage to see a lot of GC activity.</p>
 * 
 * <p>You should also decrease the max size of your heap memory so that if the GC has to kick in, it will do it sooner than later.</p>
 * 
 * <p>A good command-line example is:  <b><code>java -verbose:gc -Xms128m -Xmx256m -cp target/classes com.coralblocks.coralme.example.NoGCTest</code></b></p>
 * 
 * <p>This test also measures and reports performance metrics including execution time and memory usage for before/after comparison.</p>
 */
public class NoGCTest {
	
	/**
	 * Performance metrics collector. Instance is created before the measured test loop.
	 * Reporting methods (printMetrics, toStorageFormat) are called after the test completes,
	 * so any garbage they create does not affect the zero-garbage validation of the test loop.
	 */
	private static class PerformanceMetrics {
		long startTimeNanos;
		long endTimeNanos;
		long startMemoryBytes;
		long endMemoryBytes;
		long peakMemoryBytes;
		int iterations;
		boolean createdGarbage;
		
		void printMetrics() {
			System.out.println("\n================== Performance Metrics ==================");
			System.out.println("Iterations: " + iterations);
			System.out.println("Garbage Creation Enabled: " + createdGarbage);
			System.out.println();
			
			double durationSeconds = (endTimeNanos - startTimeNanos) / 1_000_000_000.0;
			System.out.println("Execution Time: " + String.format("%.3f", durationSeconds) + " seconds");
			if (durationSeconds > 0) {
				System.out.println("Throughput: " + String.format("%.0f", iterations / durationSeconds) + " iterations/second");
			} else {
				System.out.println("Throughput: N/A (execution time too short to measure)");
			}
			System.out.println();
			
			double startMemoryMB = startMemoryBytes / (1024.0 * 1024.0);
			double endMemoryMB = endMemoryBytes / (1024.0 * 1024.0);
			double peakMemoryMB = peakMemoryBytes / (1024.0 * 1024.0);
			double memoryDeltaMB = endMemoryMB - startMemoryMB;
			
			System.out.println("Memory Usage:");
			System.out.println("  Start: " + String.format("%.2f", startMemoryMB) + " MB");
			System.out.println("  End: " + String.format("%.2f", endMemoryMB) + " MB");
			System.out.println("  Peak: " + String.format("%.2f", peakMemoryMB) + " MB");
			System.out.println("  Delta: " + String.format("%.2f", memoryDeltaMB) + " MB");
			System.out.println("========================================================");
		}
		
		String toStorageFormat() {
			StringBuilder sb = new StringBuilder();
			sb.append("# NoGCTest Performance Metrics\n");
			sb.append("# Generated at: ").append(System.currentTimeMillis()).append("\n");
			sb.append("\n");
			sb.append("iterations=").append(iterations).append("\n");
			sb.append("garbage_creation_enabled=").append(createdGarbage).append("\n");
			long durationNanos = endTimeNanos - startTimeNanos;
			sb.append("execution_time_nanos=").append(durationNanos).append("\n");
			double durationSeconds = durationNanos / 1_000_000_000.0;
			sb.append("execution_time_seconds=").append(String.format("%.3f", durationSeconds)).append("\n");
			if (durationSeconds > 0) {
				sb.append("throughput_iterations_per_second=").append(String.format("%.0f", iterations / durationSeconds)).append("\n");
			} else {
				sb.append("throughput_iterations_per_second=N/A\n");
			}
			sb.append("start_memory_bytes=").append(startMemoryBytes).append("\n");
			sb.append("end_memory_bytes=").append(endMemoryBytes).append("\n");
			sb.append("peak_memory_bytes=").append(peakMemoryBytes).append("\n");
			sb.append("memory_delta_bytes=").append(endMemoryBytes - startMemoryBytes).append("\n");
			return sb.toString();
		}
	}
	
	private static final long CLIENT_ID = 1002L;

	private static final boolean USE_BAD_SYSTEM_OUT_PRINT = false; // turn this on and you will see a lot of garbage from System.out.print
	private static final StringBuilder sb = new StringBuilder(1024);
	private static long orderId = 1;
	
	private static CharSequence getClientOrderId() {
		sb.setLength(0);
		sb.append(orderId);
		return sb;
	}
	
	private static void printWithoutGarbage(CharSequence cs) {
		int size = cs.length();
		for(int i = 0; i < size; i++) System.out.write(cs.charAt(i));
		System.out.flush();
	}
	
	private static void printIteration(int x) {
		
		sb.setLength(0);
		sb.append('\r').append(x); // does not produce garbage
		
		if (USE_BAD_SYSTEM_OUT_PRINT) {
			System.out.print(sb); // produces garbage!
		} else {
			printWithoutGarbage(sb);
		}
	}
	
	public static void main(String[] args) {
		
		boolean createGarbage = args.length >= 1 ? Boolean.parseBoolean(args[0]) : false;
		int iterations = args.length >= 2 ? Integer.parseInt(args[1]) : 1000000;
		
		// Initialize performance metrics
		PerformanceMetrics metrics = new PerformanceMetrics();
		metrics.iterations = iterations;
		metrics.createdGarbage = createGarbage;
		
		// Collect initial memory state
		Runtime runtime = Runtime.getRuntime();
		// Note: System.gc() call is BEFORE measurement starts and is only for establishing a clean baseline.
		// The actual test loop measured below will produce zero garbage (when createGarbage=false).
		System.gc();
		try { 
			Thread.sleep(100); 
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // Restore interrupt status
		}
		metrics.startMemoryBytes = runtime.totalMemory() - runtime.freeMemory();
		metrics.peakMemoryBytes = metrics.startMemoryBytes;
		
		// Start timing - all code after this point until metrics.endTimeNanos is the measured section
		metrics.startTimeNanos = System.nanoTime();
		
		OrderBookListener noOpListener = new OrderBookAdapter();
		
		OrderBook book = new OrderBook("AAPL", noOpListener);
		
		for(int i = 1; i <= iterations; i++) {
			
			printIteration(i);
			
			// Track peak memory usage periodically (every 10000 iterations to minimize overhead)
			if (i % 10000 == 0) {
				long currentMemory = runtime.totalMemory() - runtime.freeMemory();
				if (currentMemory > metrics.peakMemoryBytes) {
					metrics.peakMemoryBytes = currentMemory;
				}
			}
			
			// Bids:
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.BUY, 1000, 100.00, TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.BUY,  900,  99.00, TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.BUY,  800,  98.00, TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.BUY,  700,  97.00, TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.BUY,  500,  95.00, TimeInForce.DAY);
			
			// Asks:
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.SELL,  500, 102.00, TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.SELL,  400, 104.00, TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.SELL,  800, 105.00, TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.SELL,  700, 108.00, TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++, Side.SELL,  500, 115.00, TimeInForce.DAY);
			
			// Hit top of book with IOCs:
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.BUY,  600, 103.00, TimeInForce.IOC);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.SELL, 900, 96.00,  TimeInForce.IOC);
			
			// Reduce and cancel top of book orders
			Order bidOrder = book.getBestBidOrder();
			Order askOrder = book.getBestAskOrder();
			
			if (createGarbage) {
				// create some garbage for the garbage collector
				sb.setLength(0);
				sb.append("someGarbage"); // appending a CharSequence does not produce garbage
				for(int x = 0; x < 10; x++) sb.toString(); // but this produces garbage
			}
			
			bidOrder.reduceTo(100);
			askOrder.reduceTo(100);
			
			bidOrder.cancel();
			askOrder.cancel();
			
			// Order rejects due odd lot
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.BUY,  620, 103.00, TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.SELL, 940, 96.00,  TimeInForce.DAY);
			
			// Add a couple of more orders in the middle of the book
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.BUY,  600, 96.00,  TimeInForce.DAY);
			book.createLimit(CLIENT_ID, getClientOrderId(),  orderId++,  Side.SELL, 990, 111.00, TimeInForce.DAY);
			
			// Now use a market order to remove all liquidity from both sides
			book.createMarket(CLIENT_ID, getClientOrderId(),  orderId++,  Side.BUY,  15000);
			book.createMarket(CLIENT_ID, getClientOrderId(),  orderId++,  Side.SELL, 15000);
			
			// Book must now be empty
			if (!book.isEmpty()) throw new IllegalStateException("Book must be empty here!");
		}
		
		// End timing and collect final memory state
		metrics.endTimeNanos = System.nanoTime();
		metrics.endMemoryBytes = runtime.totalMemory() - runtime.freeMemory();
		
		// Update peak if final memory is higher
		if (metrics.endMemoryBytes > metrics.peakMemoryBytes) {
			metrics.peakMemoryBytes = metrics.endMemoryBytes;
		}
		
		System.out.println(" ... DONE!");
		
		// Note: Metrics reporting below occurs AFTER the measured test loop completes.
		// Some garbage may be created during reporting, but this does not affect the
		// zero-garbage validation of the actual test loop above.
		
		// Print performance metrics
		metrics.printMetrics();
		
		// Store metrics in human-readable format
		String metricsData = metrics.toStorageFormat();
		System.out.print("\n");
		printWithoutGarbage(metricsData);
	}
}
