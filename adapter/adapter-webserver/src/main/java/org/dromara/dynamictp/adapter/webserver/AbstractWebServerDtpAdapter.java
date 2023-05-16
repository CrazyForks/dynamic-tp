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
import org.dromara.dynamictp.adapter.common.AbstractDtpAdapter;
import org.dromara.dynamictp.common.ApplicationContextHolder;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.core.converter.ExecutorConverter;
import org.dromara.dynamictp.core.support.ExecutorWrapper;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;

import java.util.concurrent.Executor;

/**
 * AbstractWebServerDtpAdapter related
 *
 * @author yanhom
 * @author dragon-zhang
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractWebServerDtpAdapter<A extends Executor> extends AbstractDtpAdapter {

    @Override
    public boolean supportsEventType(ResolvableType resolvableType) {
        Class<?> type = resolvableType.getRawClass();
        if (type != null) {
            return WebServerInitializedEvent.class.isAssignableFrom(type);
        }
        return false;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof WebServerInitializedEvent) {
            try {
                DtpProperties dtpProperties = ApplicationContextHolder.getBean(DtpProperties.class);
                initialize();
                refresh(dtpProperties);
            } catch (Exception e) {
                log.error("Init web server thread pool failed.", e);
            }
        }
    }

    @Override
    protected void initialize() {
        if (executors.get(getTpName()) == null) {
            ApplicationContext applicationContext = ApplicationContextHolder.getInstance();
            WebServer webServer = ((WebServerApplicationContext) applicationContext).getWebServer();
            ExecutorWrapper wrapper = doInitExecutorWrapper(webServer);
            initNotifyItems(wrapper.getThreadPoolName(), wrapper);
            executors.put(getTpName(), wrapper);
            log.info("DynamicTp adapter, web server executor init end, executor: {}",
                    ExecutorConverter.toMainFields(wrapper));
        }
    }

    /**
     * Do init thread pool executor wrapper.
     *
     * @param webServer webServer
     * @return the Executor instance
     */
    protected abstract ExecutorWrapper doInitExecutorWrapper(WebServer webServer);

    /**
     * Refresh thread pool executor wrapper.
     * @return the thread pool name
     */
    protected abstract String getTpName();
}
