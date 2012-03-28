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
package org.jboss.weld.injection;

import java.io.Serializable;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import org.jboss.weld.bootstrap.events.ProcessInjectionPointImpl;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.jboss.weld.injection.attributes.ForwardingInjectionPointAttributes;
import org.jboss.weld.injection.attributes.ParameterInjectionPointAttributes;
import org.jboss.weld.introspector.WeldParameter;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.reflection.Reflections;

public class ParameterInjectionPointImpl<T, X> extends ForwardingInjectionPointAttributes<T, Object> implements ParameterInjectionPoint<T, X>, Serializable {

    private static final long serialVersionUID = -8354344628345860324L;

    /**
     * Creates an injection point without firing the {@link ProcessInjectionPoint} event.
     */
    public static <T, X> ParameterInjectionPointImpl<T, X> extension(ParameterInjectionPointAttributes<T, X> attributes, BeanManagerImpl manager) {
        return new ParameterInjectionPointImpl<T, X>(attributes);
    }

    public static <T, X> ParameterInjectionPointImpl<T, X> of(ParameterInjectionPointAttributes<T, X> attributes, BeanManagerImpl manager) {
        return new ParameterInjectionPointImpl<T, X>(ProcessInjectionPointImpl.fire(attributes, manager));
    }

    private final boolean cacheable;
    private transient Bean<?> cachedBean;

    private ParameterInjectionPointAttributes<T, X> attributes;

    private ParameterInjectionPointImpl(ParameterInjectionPointAttributes<T, X> attributes) {
        this.attributes = attributes;
        this.cacheable = !isDelegate() && !InjectionPoint.class.isAssignableFrom(getAnnotated().getJavaClass()) && !Instance.class.isAssignableFrom(getAnnotated().getJavaClass());
    }

    @Override
    protected ParameterInjectionPointAttributes<T, X> delegate() {
        return attributes;
    }

    public void inject(Object declaringInstance, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T getValueToInject(BeanManagerImpl manager, CreationalContext<?> creationalContext) {
        T objectToInject;
        if (!cacheable) {
            objectToInject = Reflections.<T> cast(manager.getInjectableReference(this, creationalContext));
        } else {
            if (cachedBean == null) {
                cachedBean = manager.resolve(manager.getBeans(this));
            }
            objectToInject = Reflections.<T> cast(manager.getReference(this, cachedBean, creationalContext));
        }
        return objectToInject;
    }

    public WeldParameter<T, X> getAnnotated() {
        return attributes.getAnnotated();
    }
}
