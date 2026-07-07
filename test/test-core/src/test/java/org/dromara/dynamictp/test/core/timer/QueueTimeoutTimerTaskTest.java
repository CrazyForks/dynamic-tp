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
import org.dromara.dynamictp.core.timer.QueueTimeoutTimerTask;
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

import static org.dromara.dynamictp.common.em.NotifyItemEnum.QUEUE_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mockStatic;

/**
 * QueueTimeoutTimerTask test
 */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("SPRING_CONTEXT_HOLDER")
class QueueTimeoutTimerTaskTest {

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
    void testRunIncrementsQueueTimeoutCountAndTriggersAlarm() throws Exception {
        dtpExecutor = ThreadPoolBuilder.newBuilder()
                .threadPoolName("queue-timeout-task")
                .queueTimeout(100)
                .buildDynamic();
        ExecutorWrapper wrapper = ExecutorWrapper.of(dtpExecutor);
        Runnable runnable = () -> { };
        QueueTimeoutTimerTask task = new QueueTimeoutTimerTask(wrapper, runnable);

        try (MockedStatic<AlarmManager> alarmManager = mockStatic(AlarmManager.class)) {
            alarmManager.when(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(QUEUE_TIMEOUT), same(runnable)))
                    .thenAnswer(invocation -> null);

            task.run(null);

            assertEquals(1, wrapper.getThreadPoolStatProvider().getQueueTimeoutCount());
            alarmManager.verify(() -> AlarmManager.tryAlarmAsync(same(wrapper), same(QUEUE_TIMEOUT), same(runnable)));
        }
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
