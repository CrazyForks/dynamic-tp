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

package org.dromara.dynamictp.test.core.timer;

import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.dromara.dynamictp.core.notifier.manager.AlarmManager;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.dromara.dynamictp.core.support.ThreadPoolBuilder;
import org.dromara.dynamictp.core.timer.RunTimeoutTimerTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.dromara.dynamictp.common.em.NotifyItemEnum.RUN_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mockStatic;

/**
 * RunTimeoutTimerTask test
 */
class RunTimeoutTimerTaskTest {

    private DtpExecutor dtpExecutor;

    @AfterEach
    void tearDown() {
        if (dtpExecutor != null) {
            dtpExecutor.shutdownNow();
        }
    }

    @Test
    void testRunIncrementsRunTimeoutCountAndTriggersAlarm() throws Exception {
        dtpExecutor = ThreadPoolBuilder.newBuilder()
                .threadPoolName("run-timeout-task")
                .runTimeout(100)
                .buildDynamic();
        ExecutorWrapper wrapper = ExecutorWrapper.of(dtpExecutor);
        Runnable runnable = () -> { };
        Thread runningThread = new Thread();
        RunTimeoutTimerTask task = new RunTimeoutTimerTask(wrapper, runnable, runningThread);

        try (MockedStatic<AlarmManager> alarmManager = mockStatic(AlarmManager.class)) {
            alarmManager.when(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(RUN_TIMEOUT), same(runnable)))
                    .thenAnswer(invocation -> null);

            task.run(null);

            assertEquals(1, wrapper.getThreadPoolStatProvider().getRunTimeoutCount());
            assertFalse(runningThread.isInterrupted());
            alarmManager.verify(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(RUN_TIMEOUT), same(runnable)));
        }
    }

    @Test
    void testRunInterruptsThreadWhenTryInterruptEnabled() throws Exception {
        dtpExecutor = ThreadPoolBuilder.newBuilder()
                .threadPoolName("run-timeout-interrupt")
                .runTimeout(100)
                .tryInterrupt(true)
                .buildDynamic();
        ExecutorWrapper wrapper = ExecutorWrapper.of(dtpExecutor);
        Runnable runnable = () -> { };
        Thread runningThread = new Thread();
        RunTimeoutTimerTask task = new RunTimeoutTimerTask(wrapper, runnable, runningThread);

        try (MockedStatic<AlarmManager> alarmManager = mockStatic(AlarmManager.class)) {
            alarmManager.when(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(RUN_TIMEOUT), same(runnable)))
                    .thenAnswer(invocation -> null);

            task.run(null);

            assertEquals(1, wrapper.getThreadPoolStatProvider().getRunTimeoutCount());
            assertTrue(runningThread.isInterrupted());
            alarmManager.verify(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(RUN_TIMEOUT), same(runnable)));
        }
    }
}
