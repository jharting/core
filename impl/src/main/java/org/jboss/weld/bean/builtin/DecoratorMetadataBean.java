/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.bean.builtin;

import java.io.Serializable;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.bean.ForwardingDecorator;
import org.jboss.weld.bean.id.BuiltInBeanIdentifier;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.serialization.BeanHolder;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Allows a decorator to obtain information about itself.
 *
 * @author Jozef Hartinger
 * @see CDI-92
 *
 */
public class DecoratorMetadataBean extends AbstractBuiltInMetadataBean<Decorator<?>> {

    public DecoratorMetadataBean(BeanManagerImpl beanManager) {
        super(BuiltInBeanIdentifier.of(beanManager, Decorator.class), Reflections.<Class<Decorator<?>>>cast(Decorator.class), beanManager);
    }

    @Override
    protected Decorator<?> newInstance(InjectionPoint ip, CreationalContext<Decorator<?>> creationalContext) {
        Contextual<?> bean = getParentCreationalContext(creationalContext).getContextual();
        if (bean instanceof Decorator<?>) {
            return SerializableProxy.of((Decorator<?>) bean);
        }
        throw new IllegalArgumentException("Unable to inject " + bean + " into " + ip);
    }

    private static class SerializableProxy<T> extends ForwardingDecorator<T> implements Serializable {

        private static final long serialVersionUID = 398927939412634913L;

        public static <T> SerializableProxy<T> of(Bean<T> bean) {
            return new SerializableProxy<T>(bean);
        }

        private BeanHolder<T> holder;

        protected SerializableProxy(Bean<T> bean) {
            this.holder = new BeanHolder<T>(bean);
        }

        @Override
        protected Decorator<T> delegate() {
            return (Decorator<T>) holder.get();
        }
    }
}
