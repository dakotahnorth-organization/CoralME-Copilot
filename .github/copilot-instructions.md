# Copilot Instructions for CoralME

## Project Overview
CoralME is a simple, fast, and garbage-free matching engine order book for electronic exchanges. It maintains limit orders resting in an order book until they are either canceled or filled, based on price-time priority.

## Key Characteristics
- **Performance-Critical**: The codebase is designed to be zero-garbage and high-performance
- **Callback-Oriented**: Uses listener patterns for order state changes
- **Financial Domain**: Deals with orders, order books, price levels, executions, and matching

## Development Environment

### Build System
- **Build Tool**: Maven 3.8+
- **Java Version**: Java 17
- **Build Command**: `mvn clean compile`
- **Package Command**: `mvn clean package`
- **Test Command**: `mvn test`

### Dependencies
- CoralPool (1.4.1) - Object pooling library for zero-garbage operations
- CoralDS (1.2.3) - Data structures library
- JUnit 4 (4.13.1) - Testing framework
- Mockito (4.11.0) - Mocking framework for tests

### Repository Structure
```
src/
├── main/java/com/coralblocks/coralme/
│   ├── Order.java               - Order representation
│   ├── OrderBook.java           - Main order book implementation
│   ├── OrderBookListener.java   - Callback interface
│   ├── OrderBookLogger.java     - Logging listener
│   ├── PriceLevel.java          - Price level data structure
│   ├── example/                 - Example usage code
│   └── util/                    - Utility classes
└── test/java/com/coralblocks/coralme/
    ├── OrderBookTest.java       - Main test suite
    └── util/                    - Utility tests
```

## Coding Guidelines

### License Headers
**ALWAYS** include the Apache 2.0 license header in new Java files:
```java
/*
 * Copyright (c) 2015-2025 CoralBlocks LLC - http://www.coralblocks.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

### Performance Considerations
- **NEVER** create unnecessary objects in hot paths (matching logic, order processing)
- **AVOID** string concatenation in performance-critical code
- **PREFER** primitive types over boxed types (long vs Long, int vs Integer)
- **USE** object pooling patterns where objects are reused
- **MINIMIZE** allocations - the codebase aims for zero garbage collection

### Code Style
- Use descriptive variable names
- Follow existing naming conventions (camelCase for methods/variables, PascalCase for classes)
- Add comments for complex logic, but let the code be self-documenting where possible
- Keep methods focused and concise

### Testing
- Use JUnit 4 for unit tests
- Test files should mirror the structure of main source files
- Include tests for edge cases and error conditions
- Mock external dependencies using Mockito when appropriate
- Verify zero-garbage behavior for performance-critical paths

## Common Tasks

### Adding a New Feature
1. Understand the order lifecycle and callback patterns
2. Consider garbage-free design from the start
3. Add unit tests that verify functionality
4. Update relevant documentation if adding public APIs
5. Ensure the feature integrates with the callback system (OrderBookListener)

### Fixing Bugs
1. Write a failing test that reproduces the bug
2. Make the minimal change to fix the issue
3. Verify the test passes
4. Check that no garbage is created (if in performance-critical code)
5. Run the full test suite to ensure no regressions

### Modifying Order Book Logic
- Be extremely careful - this is the core matching engine
- Understand price-time priority matching
- Consider all order types: MARKET, LIMIT
- Consider all time-in-force types: IOC, GTC, DAY
- Verify MAKER and TAKER execution sides
- Test all order book states: NORMAL, CROSSED, LOCKED, ONESIDED, EMPTY

## Important Files

### Core Components
- `OrderBook.java` - The main order book with matching logic
- `Order.java` - Order representation and state management
- `PriceLevel.java` - Price level management for the order book
- `OrderBookListener.java` - Callback interface for order events

### Examples
- `src/main/java/com/coralblocks/coralme/example/Example.java` - Comprehensive usage examples
- `src/main/java/com/coralblocks/coralme/example/NoGCTest.java` - Zero-garbage verification

### Tests
- `src/test/java/com/coralblocks/coralme/OrderBookTest.java` - Main test suite

## Key Concepts

### Order Types
- **MARKET**: Execute immediately at best available price
- **LIMIT**: Execute only at specified price or better

### Time In Force
- **IOC** (Immediate Or Cancel): Execute immediately, cancel unfilled portion
- **GTC** (Good Till Cancel): Remain in book until filled or canceled
- **DAY**: Expire at end of trading day

### Execution Sides
- **MAKER**: Provides liquidity (resting order that gets filled)
- **TAKER**: Takes liquidity (incoming order that matches)

### Order Book States
- **NORMAL**: Bid below ask, valid spread
- **CROSSED**: Bid above ask (should trigger immediate matching)
- **LOCKED**: Bid equals ask
- **ONESIDED**: Only bids or only asks
- **EMPTY**: No orders

### Callbacks
The OrderBookListener interface provides callbacks for:
- `onOrderAccepted` - Order is accepted into the system
- `onOrderRested` - Order is placed in the book
- `onOrderExecuted` - Order is matched (partially or fully)
- `onOrderCanceled` - Order is canceled
- `onOrderReduced` - Order size is reduced
- `onOrderRejected` - Order is rejected
- `onOrderTerminated` - Order lifecycle is complete

## Things to Avoid
- **NEVER** introduce garbage collection in the core matching path
- **NEVER** remove or modify existing tests without understanding their purpose
- **NEVER** change public APIs without considering backward compatibility
- **AVOID** using Java streams in performance-critical code (creates garbage)
- **AVOID** string operations in hot paths
- **DO NOT** add logging to the core order matching logic (use callbacks instead)

## Additional Resources
- README.md - Comprehensive overview and code examples
- CONTRIBUTING.md - Contribution guidelines and CLA information
- bin/runGCTest.sh - Script to verify zero-garbage behavior
