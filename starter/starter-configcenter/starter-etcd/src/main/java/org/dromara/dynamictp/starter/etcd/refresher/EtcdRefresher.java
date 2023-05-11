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

package org.dromara.dynamictp.starter.etcd.refresher;

import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.common.properties.DtpProperties;
import org.dromara.dynamictp.core.refresher.AbstractRefresher;
import org.dromara.dynamictp.core.spring.BinderHelper;
import org.dromara.dynamictp.core.spring.PropertiesBinder;
import org.dromara.dynamictp.starter.etcd.util.EtcdUtil;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;

import java.util.Map;
import java.util.Objects;

/**
 * @author Redick01
 */
@Slf4j
public class EtcdRefresher extends AbstractRefresher implements InitializingBean, Ordered, DisposableBean {

    @Override
    public void afterPropertiesSet() {
        DtpProperties.Etcd etcd = dtpProperties.getEtcd();
        Map<Object, Object> map = loadConfig(etcd);
        if (map.size() > 0) {
            EtcdUtil.initWatcher(this, dtpProperties, map);
        }
    }

    public void refresh(final DtpProperties dtpProperties) {
        doRefresh(dtpProperties);
    }

    /**
     * load config.
     * @param etcd {@link DtpProperties.Etcd}
     */
    private Map<Object, Object> loadConfig(final DtpProperties.Etcd etcd) {
        Map<Object, Object> properties = EtcdUtil.getConfigMap(etcd, dtpProperties.getConfigType());
        final PropertiesBinder binder = BinderHelper.getBinder();
        if (Objects.isNull(binder)) {
            return properties;
        }
        binder.bindDtpProperties(properties, dtpProperties);
        return properties;
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public void destroy() {
        EtcdUtil.close();
    }

}
