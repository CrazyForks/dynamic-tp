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

package org.dromara.dynamictp.core.spring;

import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.common.timer.HashedWheelTimer;
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.monitor.DtpEndpoint;
import org.dromara.dynamictp.core.monitor.DtpMonitor;
import org.dromara.dynamictp.core.support.DtpBannerPrinter;
import org.dromara.dynamictp.core.thread.NamedThreadFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import java.util.concurrent.TimeUnit;

/**
 * DtpBaseBeanConfiguration related
 *
 * @author yanhom
 * @since 1.0.0
 **/
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DtpProperties.class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class DtpBaseBeanConfiguration {

    @Bean
    public DtpLifecycle dtpLifecycle() {
        return new DtpLifecycle();
    }

    @Bean
    public DtpRegistry dtpRegistry(DtpProperties dtpProperties) {
        return new DtpRegistry(dtpProperties);
    }

    @Bean
    public DtpMonitor dtpMonitor(DtpProperties dtpProperties) {
        return new DtpMonitor(dtpProperties);
    }

    @Bean
    @ConditionalOnAvailableEndpoint
    public DtpEndpoint dtpEndpoint() {
        return new DtpEndpoint();
    }

    @Bean
    public DtpBannerPrinter dtpBannerPrinter() {
        return new DtpBannerPrinter();
    }

    @Bean
    public HashedWheelTimer hashedWheelTimer() {
        return new HashedWheelTimer(new NamedThreadFactory("dtpRunnable-timeout", true), 10, TimeUnit.MILLISECONDS);
    }

}
