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

package org.dromara.dynamictp.test.core.notify.manager;

import org.dromara.dynamictp.common.em.NotifyItemEnum;
import org.dromara.dynamictp.common.entity.NotifyItem;
import org.dromara.dynamictp.common.entity.TpMainFields;
import org.dromara.dynamictp.common.pattern.filter.Invoker;
import org.dromara.dynamictp.common.pattern.filter.InvokerChain;
import org.dromara.dynamictp.core.notifier.context.BaseNotifyCtx;
import org.dromara.dynamictp.core.notifier.context.NoticeCtx;
import org.dromara.dynamictp.core.notifier.manager.NoticeManager;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NoticeManager test.
 *
 * @author codex
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = NoticeManagerTest.ContextConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
class NoticeManagerTest {

    private final AtomicReference<BaseNotifyCtx> noticeContext = new AtomicReference<>();

    private final AtomicInteger noticeCount = new AtomicInteger();

    private InvokerChain<BaseNotifyCtx> noticeInvokerChain;

    private Invoker<BaseNotifyCtx> originalHead;

    private Field headField;

    private ThreadPoolExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        executor = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        noticeInvokerChain = getNoticeInvokerChain();
        headField = InvokerChain.class.getDeclaredField("head");
        headField.setAccessible(true);
        originalHead = (Invoker<BaseNotifyCtx>) headField.get(noticeInvokerChain);
        Invoker<BaseNotifyCtx> testInvoker = context -> {
            noticeContext.set(context);
            noticeCount.incrementAndGet();
        };
        headField.set(noticeInvokerChain, testInvoker);
    }

    @AfterEach
    void tearDown() throws Exception {
        headField.set(noticeInvokerChain, originalHead);
        executor.shutdownNow();
    }

    @Test
    void testDoTryNoticeTriggersWhenChangeNotifyItemExists() {
        NotifyItem notifyItem = notifyItem(NotifyItemEnum.CHANGE);
        ExecutorWrapper wrapper = executorWrapper("notice-change", notifyItem);
        TpMainFields oldFields = new TpMainFields();
        List<String> diffKeys = Collections.singletonList("corePoolSize");

        NoticeManager.doTryNotice(wrapper, oldFields, diffKeys);

        assertEquals(1, noticeCount.get());
        assertTrue(noticeContext.get() instanceof NoticeCtx);
        NoticeCtx noticeCtx = (NoticeCtx) noticeContext.get();
        assertSame(notifyItem, noticeCtx.getNotifyItem());
        assertEquals(NotifyItemEnum.CHANGE, noticeCtx.getNotifyItemEnum());
        assertEquals("notice-change", noticeCtx.getExecutorWrapper().getThreadPoolName());
        assertSame(oldFields, noticeCtx.getOldFields());
        assertSame(diffKeys, noticeCtx.getDiffs());
    }

    @Test
    void testDoTryNoticeSkipsWhenChangeNotifyItemMissing() {
        ExecutorWrapper wrapper = executorWrapper("notice-missing", notifyItem(NotifyItemEnum.LIVENESS));

        NoticeManager.doTryNotice(wrapper, new TpMainFields(), Collections.singletonList("maximumPoolSize"));

        assertNoNotice();
    }

    private void assertNoNotice() {
        assertEquals(0, noticeCount.get());
        assertNull(noticeContext.get());
    }

    private NotifyItem notifyItem(NotifyItemEnum notifyItemEnum) {
        NotifyItem notifyItem = new NotifyItem();
        notifyItem.setType(notifyItemEnum.getValue());
        notifyItem.setPlatformIds(Collections.singletonList("platform"));
        return notifyItem;
    }

    private ExecutorWrapper executorWrapper(String poolName, NotifyItem... notifyItems) {
        ExecutorWrapper wrapper = new ExecutorWrapper(poolName, executor);
        wrapper.setNotifyItems(Arrays.asList(notifyItems));
        wrapper.setNotifyEnabled(true);
        return wrapper;
    }

    private InvokerChain<BaseNotifyCtx> getNoticeInvokerChain() throws Exception {
        Field field = NoticeManager.class.getDeclaredField("NOTICE_INVOKER_CHAIN");
        field.setAccessible(true);
        return (InvokerChain<BaseNotifyCtx>) field.get(null);
    }

    @Configuration
    static class ContextConfig {

        @Bean
        org.dromara.dynamictp.spring.holder.SpringContextHolder springContextHolder() {
            return new org.dromara.dynamictp.spring.holder.SpringContextHolder();
        }
    }
}
