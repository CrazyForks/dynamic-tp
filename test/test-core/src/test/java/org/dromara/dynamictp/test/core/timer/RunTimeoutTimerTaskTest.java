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
import org.dromara.dynamictp.spring.holder.SpringContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.dromara.dynamictp.common.em.NotifyItemEnum.RUN_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mockStatic;

/**
 * RunTimeoutTimerTask test
 */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("SPRING_CONTEXT_HOLDER")
class RunTimeoutTimerTaskTest {

    private static final long WAIT_TIMEOUT_SECONDS = 1;

    private DtpExecutor dtpExecutor;

    private GenericApplicationContext context;

    private ApplicationContext originalContext;

    @BeforeEach
    void setUp() throws Exception {
        originalContext = springContext();
        context = new GenericApplicationContext();
        context.refresh();
        new SpringContextHolder().setApplicationContext(context);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dtpExecutor != null) {
            dtpExecutor.shutdownNow();
        }
        if (context != null) {
            context.close();
        }
        setSpringContext(originalContext);
    }

    @Test
    void testRunIncrementsRunTimeoutCountAndTriggersAlarm() throws Exception {
        dtpExecutor = ThreadPoolBuilder.newBuilder()
                .threadPoolName("run-timeout-task")
                .runTimeout(100)
                .buildDynamic();
        ExecutorWrapper wrapper = ExecutorWrapper.of(dtpExecutor);
        Runnable runnable = () -> { };
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        Thread runningThread = newInterruptWaitingThread("run-timeout-task-worker", started, interrupted);
        runningThread.start();
        assertTrue(started.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        try {
            RunTimeoutTimerTask task = new RunTimeoutTimerTask(wrapper, runnable, runningThread);
            try (MockedStatic<AlarmManager> alarmManager = mockStatic(AlarmManager.class)) {
                alarmManager.when(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(RUN_TIMEOUT), same(runnable)))
                        .thenAnswer(invocation -> null);

                task.run(null);

                assertEquals(1, wrapper.getThreadPoolStatProvider().getRunTimeoutCount());
                assertFalse(runningThread.isInterrupted());
                alarmManager.verify(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(RUN_TIMEOUT), same(runnable)));
            }
        } finally {
            runningThread.interrupt();
            runningThread.join(TimeUnit.SECONDS.toMillis(WAIT_TIMEOUT_SECONDS));
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
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        Thread runningThread = newInterruptWaitingThread("run-timeout-interrupt-worker", started, interrupted);
        runningThread.start();
        assertTrue(started.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));

        try {
            RunTimeoutTimerTask task = new RunTimeoutTimerTask(wrapper, runnable, runningThread);
            try (MockedStatic<AlarmManager> alarmManager = mockStatic(AlarmManager.class)) {
                alarmManager.when(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(RUN_TIMEOUT), same(runnable)))
                        .thenAnswer(invocation -> null);

                task.run(null);

                assertEquals(1, wrapper.getThreadPoolStatProvider().getRunTimeoutCount());
                assertTrue(interrupted.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS));
                alarmManager.verify(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(RUN_TIMEOUT), same(runnable)));
            }
        } finally {
            runningThread.interrupt();
            runningThread.join(TimeUnit.SECONDS.toMillis(WAIT_TIMEOUT_SECONDS));
        }
    }

    private Thread newInterruptWaitingThread(String name, CountDownLatch started, CountDownLatch interrupted) {
        return new Thread(() -> {
            started.countDown();
            while (!Thread.currentThread().isInterrupted()) {
                LockSupport.park();
            }
            interrupted.countDown();
        }, name);
    }

    private ApplicationContext springContext() throws Exception {
        Field field = SpringContextHolder.class.getDeclaredField("context");
        field.setAccessible(true);
        return (ApplicationContext) field.get(null);
    }

    private void setSpringContext(ApplicationContext applicationContext) throws Exception {
        Field field = SpringContextHolder.class.getDeclaredField("context");
        field.setAccessible(true);
        field.set(null, applicationContext);
    }
}
