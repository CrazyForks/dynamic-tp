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

package org.dromara.dynamictp.test.common.manager;

import org.dromara.dynamictp.common.manager.ContextManagerHelper;
import org.dromara.dynamictp.spring.holder.SpringContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ContextManagerHelperTest related.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ContextManagerHelperTest {

    private static final String TEST_PROPERTY_KEY = "context.manager.test.key";

    private GenericApplicationContext context;

    private ApplicationContext originalContext;

    @BeforeEach
    void setUp() throws Exception {
        originalContext = springContext();
        context = new GenericApplicationContext();
        context.getBeanFactory().registerSingleton("sampleBean", new SampleBean());
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("contextManagerTest", Collections.singletonMap(TEST_PROPERTY_KEY, "test-value")));
        context.refresh();
        new SpringContextHolder().setApplicationContext(context);
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
        setSpringContext(originalContext);
    }

    @Test
    void testDelegateToSpringContextManagerWhenImplementationFound() {
        SampleBean sampleBean = ContextManagerHelper.getBean(SampleBean.class);

        assertSame(sampleBean, ContextManagerHelper.getBean("sampleBean", SampleBean.class));
        assertTrue(ContextManagerHelper.getBeansOfType(SampleBean.class).containsValue(sampleBean));
        assertSame(context.getEnvironment(), ContextManagerHelper.getEnvironment());
        assertEquals("test-value", ContextManagerHelper.getEnvironmentProperty(TEST_PROPERTY_KEY));
        assertEquals("test-value", ContextManagerHelper.getEnvironmentProperty(TEST_PROPERTY_KEY, context.getEnvironment()));
        assertEquals("default", ContextManagerHelper.getEnvironmentProperty("missing.key", "default"));
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

    private static class SampleBean {
    }
}
