/*
 * Copyright 2015-2024 (c) CoralBlocks LLC - http://www.coralblocks.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.coralblocks.coralme.util;

/**
 * Returns the JVM monotonic nanosecond time via {@link System#nanoTime()}.
 *
 * Hot-path: zero allocations, single native call and a return.
 *
 * Note: this is NOT epoch-based; use only where a monotonic nanosecond timestamp is acceptable.
 */
public final class SystemTimestamper implements Timestamper {

    @Override
    public long nanoEpoch() {
        return System.nanoTime();
    }
}
