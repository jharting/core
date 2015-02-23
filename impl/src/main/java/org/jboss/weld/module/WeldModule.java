/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.module;

import javax.enterprise.context.spi.Context;

import org.jboss.weld.bean.builtin.AbstractBuiltInBean;
import org.jboss.weld.bootstrap.ContextHolder;
import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.manager.BeanManagerImpl;

public interface WeldModule {

    String getName();

    default void register(RegistrationContext ctx) {
    }

    interface RegistrationContext {

        String getContextId();
        ObserverNotifierFactory getObserverNotifierFactory();
        void setObserverNotifierFactory(ObserverNotifierFactory factory);
        ServiceRegistry getServices();
    }

    default void postCreateContexts(ContextRegistrationContext ctx) {
    }

    interface ContextRegistrationContext {
        String getContextId();
        ServiceRegistry getServices();
        void addContext(ContextHolder<? extends Context> context);
    }

    default void preDeployBeans(PreBeanRegistrationContext ctx) {
    }

    interface PreBeanRegistrationContext {
        ServiceRegistry getServices();
        Environment getEnvironment();
        BeanManagerImpl getBeanManager();
        void registerBean(AbstractBuiltInBean<?> bean);
    }
}
