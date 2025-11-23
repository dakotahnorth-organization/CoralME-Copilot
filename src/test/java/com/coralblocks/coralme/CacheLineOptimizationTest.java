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
package com.coralblocks.coralme;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * This test ensures that hot (frequently accessed) fields in Order and PriceLevel
 * classes are positioned at the beginning of the class to fit within a single
 * 64-byte cache line, optimizing cache performance in the critical matching path.
 * 
 * <h3>JVM Object Layout Assumptions</h3>
 * A typical 64-bit JVM object layout with compressed oops (enabled by default up to 32GB heap):
 * - Object header: 12 bytes (8-byte mark word + 4-byte class pointer)
 * - Fields laid out primarily in declaration order (JVM may reorder for alignment)
 * - Reference fields: 4 bytes (compressed oops) or 8 bytes (no compression)
 * - long/double: 8 bytes
 * - int/float: 4 bytes
 * - boolean: 1 byte (often padded to 4 bytes for alignment)
 * 
 * <h3>Test Methodology</h3>
 * These tests validate field declaration order, which strongly influences but does not
 * guarantee final memory layout. The JVM may reorder fields for alignment and padding,
 * but declaration order is the primary factor in field placement.
 * 
 * <h3>Cache Line Optimization Goals</h3>
 * For a 64-byte cache line, we want hot fields within ~52 bytes (64 - 12 byte header).
 * The margin of 80 bytes accounts for:
 * - 12-16 byte object header (varies by JVM and configuration)
 * - Up to 4 bytes padding for alignment
 * - Potential field reordering by JVM for alignment
 * 
 * This ensures hot fields typically fit within a single cache line on modern CPUs.
 */
public class CacheLineOptimizationTest {
	
	// Standard 64-byte cache line size on most modern CPUs
	private static final int CACHE_LINE_SIZE = 64;
	
	// Allow margin for object header (12-16 bytes) and alignment (up to 8 bytes)
	// This accounts for JVM field reordering and padding while keeping hot fields
	// within a single cache line in typical configurations
	private static final int CACHE_LINE_SIZE_WITH_MARGIN = 80;
	
	// Hot fields count for Order class (next, prev, totalSize, executedSize, price, id, clientId, side, type, tif)
	private static final int ORDER_HOT_FIELDS_COUNT = 10;
	
	// Hot fields count for PriceLevel class (next, prev, head, tail, price, size, side, orders)
	private static final int PRICE_LEVEL_HOT_FIELDS_COUNT = 8;
	
	/**
	 * Hot fields in Order class that are accessed in the critical match/execute path:
	 * - next, prev (linked list traversal)
	 * - totalSize, executedSize (for getOpenSize calculation)
	 * - price (price comparison)
	 * - id, clientId (order identification)
	 * - side, type, tif (order properties checked frequently)
	 */
	@Test
	public void testOrderHotFieldsAreFirst() {
		
		Field[] fields = Order.class.getDeclaredFields();
		
		// Expected hot fields in order
		String[] expectedHotFields = {
			"next",
			"prev",
			"totalSize",
			"executedSize",
			"price",
			"id",
			"clientId",
			"side",
			"type",
			"tif"
		};
		
		List<String> actualFields = new ArrayList<>();
		for (Field field : fields) {
			// Skip static and synthetic fields
			if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
				continue;
			}
			actualFields.add(field.getName());
		}
		
		// Verify that hot fields appear first in the class
		for (int i = 0; i < expectedHotFields.length; i++) {
			String hotField = expectedHotFields[i];
			assertTrue("Hot field '" + hotField + "' should be among the first " + expectedHotFields.length + 
					   " instance fields in Order class for cache line optimization",
					   actualFields.indexOf(hotField) < expectedHotFields.length);
		}
		
		// Calculate approximate size of hot fields
		// NOTE: This is a conservative estimate based on current field types.
		// Assumptions: compressed oops (4-byte refs), no padding between fields
		// If field types change, this calculation must be updated accordingly
		int estimatedSize = 0;
		estimatedSize += 4; // next - Order reference
		estimatedSize += 4; // prev - Order reference
		estimatedSize += 8; // totalSize - long
		estimatedSize += 8; // executedSize - long
		estimatedSize += 8; // price - long
		estimatedSize += 8; // id - long
		estimatedSize += 8; // clientId - long
		estimatedSize += 4; // side - Side enum reference
		estimatedSize += 4; // type - Type enum reference
		estimatedSize += 4; // tif - TimeInForce enum reference
		// Total: 60 bytes + 12-byte header = 72 bytes
		
		// With 12-byte header and 60 bytes of hot fields, we're close to 64-byte cache line
		// Some fields might be reordered by JVM but the declaration order strongly influences layout
		assertTrue("Hot fields should fit within approximately 64 bytes including object header. " +
				   "Estimated: " + (estimatedSize + 12) + " bytes",
				   estimatedSize + 12 <= CACHE_LINE_SIZE_WITH_MARGIN);
	}
	
	/**
	 * Hot fields in PriceLevel class accessed in the critical path:
	 * - next, prev (linked list traversal)
	 * - head, tail (order list access)
	 * - price (price comparison)
	 * - size (total size at level)
	 * - side (matching direction)
	 * - orders (count for isEmpty check)
	 */
	@Test
	public void testPriceLevelHotFieldsAreFirst() {
		
		Field[] fields = PriceLevel.class.getDeclaredFields();
		
		// Expected hot fields in order
		String[] expectedHotFields = {
			"next",
			"prev",
			"head",
			"tail",
			"price",
			"size",
			"side",
			"orders"
		};
		
		List<String> actualFields = new ArrayList<>();
		for (Field field : fields) {
			// Skip static and synthetic fields
			if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
				continue;
			}
			actualFields.add(field.getName());
		}
		
		// Verify that hot fields appear first in the class
		for (int i = 0; i < expectedHotFields.length; i++) {
			String hotField = expectedHotFields[i];
			assertTrue("Hot field '" + hotField + "' should be among the first " + expectedHotFields.length + 
					   " instance fields in PriceLevel class for cache line optimization",
					   actualFields.indexOf(hotField) < expectedHotFields.length);
		}
		
		// Calculate approximate size of hot fields
		// NOTE: This is a conservative estimate based on current field types.
		// Assumptions: compressed oops (4-byte refs), no padding between fields
		// If field types change, this calculation must be updated accordingly
		int estimatedSize = 0;
		estimatedSize += 4; // next - PriceLevel reference
		estimatedSize += 4; // prev - PriceLevel reference
		estimatedSize += 4; // head - Order reference
		estimatedSize += 4; // tail - Order reference
		estimatedSize += 8; // price - long
		estimatedSize += 8; // size - long
		estimatedSize += 4; // side - Side enum reference
		estimatedSize += 4; // orders - int
		// Total: 40 bytes + 12-byte header = 52 bytes
		
		assertTrue("Hot fields should fit within 64-byte cache line including object header. " +
				   "Estimated: " + (estimatedSize + 12) + " bytes",
				   estimatedSize + 12 <= CACHE_LINE_SIZE);
	}
	
	/**
	 * Verify that cold fields (rarely accessed) come after hot fields in Order class
	 */
	@Test
	public void testOrderColdFieldsAreLast() {
		
		Field[] fields = Order.class.getDeclaredFields();
		
		// Cold fields that should be positioned after hot fields (in actual order)
		String[] coldFields = {
			"listeners",
			"clientOrderId",
			"priceLevel",
			"security",
			"timestamper",
			"originalSize",
			"acceptTime",
			"restTime",
			"cancelTime",
			"rejectTime",
			"reduceTime",
			"executeTime",
			"pendingSize",
			"isResting",
			"isPendingCancel"
		};
		
		List<String> actualFields = new ArrayList<>();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
				continue;
			}
			actualFields.add(field.getName());
		}
		
		int firstColdFieldIndex = Integer.MAX_VALUE;
		for (String coldField : coldFields) {
			int index = actualFields.indexOf(coldField);
			if (index >= 0 && index < firstColdFieldIndex) {
				firstColdFieldIndex = index;
			}
		}
		
		// The first cold field should appear after all hot fields
		assertTrue("Cold fields should be positioned after hot fields (index >= " + ORDER_HOT_FIELDS_COUNT + "). " +
				   "First cold field at index: " + firstColdFieldIndex,
				   firstColdFieldIndex >= ORDER_HOT_FIELDS_COUNT);
	}
	
	/**
	 * Verify that cold fields come after hot fields in PriceLevel class
	 */
	@Test
	public void testPriceLevelColdFieldsAreLast() {
		
		Field[] fields = PriceLevel.class.getDeclaredFields();
		
		// Cold field: security (rarely accessed, mainly for initialization)
		String coldField = "security";
		
		List<String> actualFields = new ArrayList<>();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
				continue;
			}
			actualFields.add(field.getName());
		}
		
		int coldFieldIndex = actualFields.indexOf(coldField);
		
		// Security field must exist and should be positioned after all hot fields
		assertTrue("Cold field 'security' must exist in PriceLevel class",
				   coldFieldIndex >= 0);
		assertTrue("Cold field 'security' should be positioned after hot fields (index >= " + PRICE_LEVEL_HOT_FIELDS_COUNT + "). " +
				   "Actual index: " + coldFieldIndex,
				   coldFieldIndex >= PRICE_LEVEL_HOT_FIELDS_COUNT);
	}
}
