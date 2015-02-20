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

import org.jboss.weld.bean.builtin.AbstractBuiltInBean;
import org.jboss.weld.bootstrap.BeanDeployer;
import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.WeldModule.PreBeanRegistrationContext;
import org.jboss.weld.module.WeldModule.RegistrationContext;
import org.jboss.weld.util.ServiceLoader;

public class WeldExtensionRegistrar {

    private WeldExtensionRegistrar() {
    }

    public static void register(final ServiceRegistry services) {
        final RegistrationContext ctx = new RegistrationContext() {
            @Override
            public ObserverNotifierFactory getObserverNotifierFactory() {
                return services.get(ObserverNotifierFactory.class);
            }
            @Override
            public void setObserverNotifierFactory(ObserverNotifierFactory factory) {
                services.add(ObserverNotifierFactory.class, factory);
            }
            @Override
            public ServiceRegistry getServices() {
                return services;
            }
        };
        for (Metadata<WeldModule> extension : ServiceLoader.load(WeldModule.class, WeldModule.class.getClassLoader())) {
            extension.getValue().register(ctx);
        }
    }

    public static void registerBeans(final ServiceRegistry services, final BeanManagerImpl manager, final Environment environment, final BeanDeployer deployer) {
        for (Metadata<WeldModule> extension : ServiceLoader.load(WeldModule.class, WeldModule.class.getClassLoader())) {
            PreBeanRegistrationContext ctx = new PreBeanRegistrationContext() {

                @Override
                public void registerBean(AbstractBuiltInBean<?> bean) {
                    deployer.addBuiltInBean(bean);
                }

                @Override
                public ServiceRegistry getServices() {
                    return services;
                }

                @Override
                public Environment getEnvironment() {
                    return environment;
                }

                @Override
                public BeanManagerImpl getBeanManager() {
                    return manager;
                }
            };
            extension.getValue().preDeployBeans(ctx);
        }
    }
}
