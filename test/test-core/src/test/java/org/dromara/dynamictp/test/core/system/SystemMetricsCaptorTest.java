/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.dynamictp.test.core.system;

import org.dromara.dynamictp.core.system.CpuMetricsCaptor;
import org.dromara.dynamictp.core.system.MemoryMetricsCaptor;
import org.dromara.dynamictp.core.system.OperatingSystemBeanManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

/**
 * System metrics captor test.
 *
 * @author codex
 */
class SystemMetricsCaptorTest {

    @Test
    void testCpuMetricsCaptorInitialValue() {
        CpuMetricsCaptor captor = new CpuMetricsCaptor();

        assertEquals(-1.0, captor.getProcessCpuUsage(), 0.001);
    }

    @Test
    void testCpuMetricsCaptorRunDoesNotThrow() {
        CpuMetricsCaptor captor = new CpuMetricsCaptor();

        assertDoesNotThrow(captor::run);
        assertFalse(Double.isNaN(captor.getProcessCpuUsage()));
    }

    @Test
    void testCpuMetricsCaptorRunUpdatesProcessCpuUsage() throws Exception {
        CpuMetricsCaptor captor = new CpuMetricsCaptor();
        long previousProcessCpuTime = TimeUnit.MILLISECONDS.toNanos(200);
        long newProcessCpuTime = TimeUnit.MILLISECONDS.toNanos(260);
        long previousUpTime = Math.max(0L, getRuntimeUpTime() - 100L);
        setField(captor, "prevProcessCpuTime", previousProcessCpuTime);
        setField(captor, "prevUpTime", previousUpTime);

        try (MockedStatic<OperatingSystemBeanManager> mocked = mockStatic(OperatingSystemBeanManager.class)) {
            mocked.when(OperatingSystemBeanManager::getProcessCpuTime).thenReturn(newProcessCpuTime);

            captor.run();

            mocked.verify(OperatingSystemBeanManager::getProcessCpuTime);
        }

        long capturedUpTime = (long) getField(captor, "prevUpTime");
        long elapsedTime = capturedUpTime - previousUpTime;
        int cpuCores = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getAvailableProcessors();
        double expectedUsage = (double) TimeUnit.NANOSECONDS.toMillis(newProcessCpuTime - previousProcessCpuTime)
                / elapsedTime / cpuCores;

        assertTrue(elapsedTime > 0);
        assertEquals(newProcessCpuTime, getField(captor, "prevProcessCpuTime"));
        assertEquals(expectedUsage, captor.getProcessCpuUsage(), 0.000001);
        assertTrue(Double.isFinite(captor.getProcessCpuUsage()));
    }

    @Test
    void testCpuMetricsCaptorRunKeepsPreviousValueWhenCpuTimeFails() throws Exception {
        CpuMetricsCaptor captor = new CpuMetricsCaptor();
        double previousUsage = 0.42D;
        setField(captor, "currProcessCpuUsage", previousUsage);
        setField(captor, "prevProcessCpuTime", 10L);
        setField(captor, "prevUpTime", 20L);

        try (MockedStatic<OperatingSystemBeanManager> mocked = mockStatic(OperatingSystemBeanManager.class)) {
            mocked.when(OperatingSystemBeanManager::getProcessCpuTime)
                    .thenThrow(new IllegalStateException("cpu time unavailable"));

            assertDoesNotThrow(captor::run);
        }

        assertEquals(previousUsage, captor.getProcessCpuUsage(), 0.001);
        assertEquals(10L, getField(captor, "prevProcessCpuTime"));
        assertEquals(20L, getField(captor, "prevUpTime"));
    }

    @Test
    void testMemoryMetricsCaptorInitialValue() {
        MemoryMetricsCaptor captor = new MemoryMetricsCaptor();

        assertEquals(-1.0, captor.getLongLivedMemoryUsage(), 0.001);
    }

    @Test
    void testMemoryMetricsCaptorRunDoesNotThrow() {
        MemoryMetricsCaptor captor = new MemoryMetricsCaptor();

        assertDoesNotThrow(captor::run);
        double usage = captor.getLongLivedMemoryUsage();
        assertTrue(usage == -1.0 || usage >= 0.0);
    }

    private static long getRuntimeUpTime() {
        RuntimeMXBean runtimeBean = ManagementFactory.getPlatformMXBean(RuntimeMXBean.class);
        return runtimeBean.getUptime();
    }

    private static Object getField(Object target, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
