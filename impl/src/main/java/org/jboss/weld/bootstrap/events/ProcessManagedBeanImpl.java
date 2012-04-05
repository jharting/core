/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.bootstrap.events;

import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.manager.BeanManagerImpl;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessManagedBean;
import java.lang.reflect.Type;

public class ProcessManagedBeanImpl<X> extends AbstractProcessClassBean<X, ManagedBean<X>> implements ProcessManagedBean<X> {

    public static <X> void fire(BeanManagerImpl beanManager, ManagedBean<X> bean) {
        if (beanManager.isBeanEnabled(bean)) {
            new ProcessManagedBeanImpl<X>(beanManager, bean) {
            }.fire();
        }
    }

    public ProcessManagedBeanImpl(BeanManagerImpl beanManager, ManagedBean<X> bean) {
        super(beanManager, ProcessManagedBean.class, new Type[]{bean.getAnnotated().getBaseType()}, bean);
    }

    public AnnotatedType<X> getAnnotatedBeanClass() {
        return getBean().getAnnotated();
    }

}
