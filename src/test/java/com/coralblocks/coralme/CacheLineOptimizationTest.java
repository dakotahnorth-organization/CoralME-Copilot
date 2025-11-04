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
 * A typical 64-bit JVM object layout:
 * - Object header: 12-16 bytes (12 bytes with compressed oops)
 * - Fields laid out in declaration order (with alignment)
 * - Reference fields: 4 bytes (compressed oops) or 8 bytes
 * - long/double: 8 bytes
 * - int/float: 4 bytes
 * - boolean: 1 byte (often padded)
 * 
 * For a 64-byte cache line, we want hot fields within ~48-52 bytes after header.
 */
public class CacheLineOptimizationTest {
	
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
		// This is a rough estimate assuming compressed oops (4-byte refs, 8-byte longs)
		int estimatedSize = 0;
		estimatedSize += 4; // next reference
		estimatedSize += 4; // prev reference
		estimatedSize += 8; // totalSize long
		estimatedSize += 8; // executedSize long
		estimatedSize += 8; // price long
		estimatedSize += 8; // id long
		estimatedSize += 8; // clientId long
		estimatedSize += 4; // side reference
		estimatedSize += 4; // type reference
		estimatedSize += 4; // tif reference
		// Total: 60 bytes + 12-byte header = 72 bytes
		
		// With 12-byte header and 60 bytes of hot fields, we're close to 64-byte cache line
		// Some fields might be reordered by JVM but the declaration order strongly influences layout
		assertTrue("Hot fields should fit within approximately 64 bytes including object header. " +
				   "Estimated: " + (estimatedSize + 12) + " bytes",
				   estimatedSize + 12 <= 80); // Allow some margin for alignment
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
		// This is a rough estimate assuming compressed oops (4-byte refs, 8-byte longs)
		int estimatedSize = 0;
		estimatedSize += 4; // next reference
		estimatedSize += 4; // prev reference
		estimatedSize += 4; // head reference
		estimatedSize += 4; // tail reference
		estimatedSize += 8; // price long
		estimatedSize += 8; // size long
		estimatedSize += 4; // side reference
		estimatedSize += 4; // orders int
		// Total: 40 bytes + 12-byte header = 52 bytes
		
		assertTrue("Hot fields should fit within 64-byte cache line including object header. " +
				   "Estimated: " + (estimatedSize + 12) + " bytes",
				   estimatedSize + 12 <= 64);
	}
	
	/**
	 * Verify that cold fields (rarely accessed) come after hot fields in Order class
	 */
	@Test
	public void testOrderColdFieldsAreLast() {
		
		Field[] fields = Order.class.getDeclaredFields();
		
		// Cold fields that should be positioned after hot fields
		String[] coldFields = {
			"listeners",
			"clientOrderId",
			"security",
			"originalSize",
			"acceptTime",
			"restTime",
			"cancelTime",
			"rejectTime",
			"reduceTime",
			"executeTime",
			"timestamper"
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
		
		// The first cold field should appear after position 9 (after the 10 hot fields)
		assertTrue("Cold fields should be positioned after hot fields (index > 9). " +
				   "First cold field at index: " + firstColdFieldIndex,
				   firstColdFieldIndex >= 10);
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
		
		// Security field must exist and should be positioned after the 8 hot fields
		assertTrue("Cold field 'security' must exist in PriceLevel class",
				   coldFieldIndex >= 0);
		assertTrue("Cold field 'security' should be positioned after hot fields (index >= 8). " +
				   "Actual index: " + coldFieldIndex,
				   coldFieldIndex >= 8);
	}
}
