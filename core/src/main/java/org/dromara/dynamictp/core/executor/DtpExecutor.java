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

package org.dromara.dynamictp.core.executor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dromara.dynamictp.common.em.NotifyItemEnum;
import org.dromara.dynamictp.common.entity.NotifyItem;
import org.dromara.dynamictp.core.aware.AwareManager;
import org.dromara.dynamictp.core.aware.TaskEnhanceAware;
import org.dromara.dynamictp.core.spring.SpringExecutor;
import org.dromara.dynamictp.core.support.ExecutorAdapter;
import org.dromara.dynamictp.core.support.task.wrapper.TaskWrapper;
import org.slf4j.MDC;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.dromara.dynamictp.common.constant.DynamicTpConst.TRACE_ID;

/**
 * Dynamic ThreadPoolExecutor, extending ThreadPoolExecutor, implements some new features
 *
 * @author yanhom
 * @since 1.0.0
 **/
@Slf4j
public class DtpExecutor extends ThreadPoolExecutor
        implements SpringExecutor, TaskEnhanceAware, ExecutorAdapter<ThreadPoolExecutor> {

    /**
     * The name of the thread pool.
     */
    protected String threadPoolName;

    /**
     * Notify items, see {@link NotifyItemEnum}.
     */
    private List<NotifyItem> notifyItems;

    /**
     * Notify platform ids.
     */
    private List<String> platformIds;

    /**
     * Task wrappers, do sth enhanced.
     */
    private List<TaskWrapper> taskWrappers = Lists.newArrayList();

    /**
     * Plugin names.
     */
    private Set<String> pluginNames = Sets.newHashSet();

    /**
     * RejectHandler type.
     */
    private String rejectHandlerType;

    public DtpExecutor(int corePoolSize,
                       int maximumPoolSize,
                       long keepAliveTime,
                       TimeUnit unit,
                       BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), new AbortPolicy());
    }

    public DtpExecutor(int corePoolSize,
                       int maximumPoolSize,
                       long keepAliveTime,
                       TimeUnit unit,
                       BlockingQueue<Runnable> workQueue,
                       ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, new AbortPolicy());
    }

    public DtpExecutor(int corePoolSize,
                       int maximumPoolSize,
                       long keepAliveTime,
                       TimeUnit unit,
                       BlockingQueue<Runnable> workQueue,
                       RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }

    public DtpExecutor(int corePoolSize,
                       int maximumPoolSize,
                       long keepAliveTime,
                       TimeUnit unit,
                       BlockingQueue<Runnable> workQueue,
                       ThreadFactory threadFactory,
                       RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public ThreadPoolExecutor getOriginal() {
        return this;
    }
    
    @Override
    public void execute(Runnable task, long startTimeout) {
        execute(task);
    }

    @Override
    public void execute(Runnable command) {
        command = getEnhancedTask(command);
        AwareManager.execute(this, command);
        super.execute(command);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        AwareManager.beforeExecute(this, t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        AwareManager.afterExecute(this, r, t);
        tryPrintError(r, t);
        clearContext();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        AwareManager.shutdown(this);
    }

    @Override
    public List<Runnable> shutdownNow() {
        val tasks = super.shutdownNow();
        AwareManager.shutdownNow(this, tasks);
        return tasks;
    }

    @Override
    protected void terminated() {
        super.terminated();
        AwareManager.terminated(this);
    }

    private void tryPrintError(Runnable r, Throwable t) {
        if (Objects.nonNull(t)) {
            log.error("DynamicTp execute, thread {} throw exception, traceId {}",
                    Thread.currentThread(), MDC.get(TRACE_ID), t);
            return;
        }
        if (r instanceof FutureTask) {
            try {
                Future<?> future = (Future<?>) r;
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("DynamicTp execute, thread {} throw exception, traceId {}",
                        Thread.currentThread(), MDC.get(TRACE_ID), e);
            }
        }
    }

    private void clearContext() {
        MDC.remove(TRACE_ID);
    }

    public String getThreadPoolName() {
        return threadPoolName;
    }

    public void setThreadPoolName(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    public List<NotifyItem> getNotifyItems() {
        return notifyItems;
    }

    public void setNotifyItems(List<NotifyItem> notifyItems) {
        this.notifyItems = notifyItems;
    }

    public List<String> getPlatformIds() {
        return platformIds;
    }

    public void setPlatformIds(List<String> platformIds) {
        this.platformIds = platformIds;
    }

    @Override
    public List<TaskWrapper> getTaskWrappers() {
        return taskWrappers;
    }

    @Override
    public void setTaskWrappers(List<TaskWrapper> taskWrappers) {
        this.taskWrappers = taskWrappers;
    }

    public Set<String> getPluginNames() {
        return pluginNames;
    }

    public void setPluginNames(Set<String> pluginNames) {
        this.pluginNames = pluginNames;
    }

    @Override
    public String getRejectHandlerType() {
        return rejectHandlerType;
    }

    public void setRejectHandlerType(String rejectHandlerType) {
        this.rejectHandlerType = rejectHandlerType;
    }

    /**
     * In order for the field can be assigned by reflection.
     *
     * @param allowCoreThreadTimeOut allowCoreThreadTimeOut
     */
    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        allowCoreThreadTimeOut(allowCoreThreadTimeOut);
    }
}
