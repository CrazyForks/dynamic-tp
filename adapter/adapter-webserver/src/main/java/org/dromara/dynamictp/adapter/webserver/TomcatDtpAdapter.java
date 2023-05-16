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

package org.dromara.dynamictp.adapter.webserver;

import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.core.support.ExecutorAdapter;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * TomcatDtpAdapter related
 *
 * @author yanhom
 * @author dragon-zhang
 * @since 1.0.0
 */
@Slf4j
public class TomcatDtpAdapter extends AbstractWebServerDtpAdapter<Executor> {

    private static final String POOL_NAME = "tomcatTp";

    @Override
    public ExecutorWrapper doInitExecutorWrapper(WebServer webServer) {
        TomcatWebServer tomcatWebServer = (TomcatWebServer) webServer;
        final TomcatExecutorAdapter adapter = new TomcatExecutorAdapter(
                tomcatWebServer.getTomcat().getConnector().getProtocolHandler().getExecutor());
        return new ExecutorWrapper(POOL_NAME, adapter);
    }

    @Override
    public void refresh(DtpProperties dtpProperties) {
        refresh(POOL_NAME, executors.get(getTpName()), dtpProperties.getPlatforms(), dtpProperties.getTomcatTp());
    }

    @Override
    protected String getTpName() {
        return POOL_NAME;
    }

    /**
     * TomcatExecutorAdapter implements ExecutorAdapter, the goal of this class
     * is to be compatible with {@link org.apache.tomcat.util.threads.ThreadPoolExecutor}.
     **/
    private static class TomcatExecutorAdapter implements ExecutorAdapter<Executor> {
        
        private final Executor executor;
        
        TomcatExecutorAdapter(Executor executor) {
            this.executor = executor;
        }
        
        @Override
        public Executor getOriginal() {
            return this.executor;
        }

        public org.apache.tomcat.util.threads.ThreadPoolExecutor getTomcatExecutor() {
            return (org.apache.tomcat.util.threads.ThreadPoolExecutor) this.executor;
        }
        
        @Override
        public int getCorePoolSize() {
            return getTomcatExecutor().getCorePoolSize();
        }
        
        @Override
        public void setCorePoolSize(int corePoolSize) {
            getTomcatExecutor().setCorePoolSize(corePoolSize);
        }
        
        @Override
        public int getMaximumPoolSize() {
            return getTomcatExecutor().getMaximumPoolSize();
        }
        
        @Override
        public void setMaximumPoolSize(int maximumPoolSize) {
            getTomcatExecutor().setMaximumPoolSize(maximumPoolSize);
        }
        
        @Override
        public int getPoolSize() {
            return getTomcatExecutor().getPoolSize();
        }
        
        @Override
        public int getActiveCount() {
            return getTomcatExecutor().getActiveCount();
        }
        
        @Override
        public int getLargestPoolSize() {
            return getTomcatExecutor().getLargestPoolSize();
        }
        
        @Override
        public long getTaskCount() {
            return getTomcatExecutor().getTaskCount();
        }
        
        @Override
        public long getCompletedTaskCount() {
            return getTomcatExecutor().getCompletedTaskCount();
        }
        
        @Override
        public BlockingQueue<Runnable> getQueue() {
            return getTomcatExecutor().getQueue();
        }
    
        @Override
        public String getRejectHandlerType() {
            if (executor instanceof ThreadPoolExecutor) {
                return ((ThreadPoolExecutor) executor).getRejectedExecutionHandler().getClass().getSimpleName();
            }
            return getTomcatExecutor().getRejectedExecutionHandler().getClass().getSimpleName();
        }
        
        @Override
        public boolean allowsCoreThreadTimeOut() {
            return getTomcatExecutor().allowsCoreThreadTimeOut();
        }
        
        @Override
        public void allowCoreThreadTimeOut(boolean value) {
            getTomcatExecutor().allowCoreThreadTimeOut(value);
        }
        
        @Override
        public long getKeepAliveTime(TimeUnit unit) {
            return getTomcatExecutor().getKeepAliveTime(unit);
        }
        
        @Override
        public void setKeepAliveTime(long time, TimeUnit unit) {
            getTomcatExecutor().setKeepAliveTime(time, unit);
        }
    }
}
