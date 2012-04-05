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

import static org.jboss.weld.injection.Exceptions.rethrowException;

import java.io.Serializable;
import java.lang.reflect.Field;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.annotated.runtime.RuntimeAnnotatedMembers;
import org.jboss.weld.bean.proxy.DecoratorProxy;
import org.jboss.weld.bootstrap.events.ProcessInjectionPointImpl;
import org.jboss.weld.injection.attributes.FieldInjectionPointAttributes;
import org.jboss.weld.injection.attributes.ForwardingInjectionPointAttributes;
import org.jboss.weld.injection.attributes.WeldInjectionPointAttributes;
import org.jboss.weld.interceptor.util.proxy.TargetInstanceProxy;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.reflection.Reflections;

public class FieldInjectionPoint<T, X> extends ForwardingInjectionPointAttributes<T, Field> implements WeldInjectionPoint<T, Field>, Serializable {

    private static final long serialVersionUID = 6645272914499045953L;

    private final boolean cacheable;
    private transient Bean<?> cachedBean;

    private final FieldInjectionPointAttributes<T, X> attributes;

    public static <T, X> FieldInjectionPoint<T, X> of(FieldInjectionPointAttributes<T, X> attributes, BeanManagerImpl manager) {
        return new FieldInjectionPoint<T, X>(ProcessInjectionPointImpl.fire(attributes, manager));
    }

    protected FieldInjectionPoint(FieldInjectionPointAttributes<T, X> attributes) {
        this.attributes = attributes;
        this.cacheable = isCacheableInjectionPoint(attributes);
    }

    protected static boolean isCacheableInjectionPoint(WeldInjectionPointAttributes<?, ?> attributes) {
        if (attributes.isDelegate()) {
            return false;
        }
        Class<?> rawType = Reflections.getRawType(attributes.getType());
        return !InjectionPoint.class.isAssignableFrom(rawType) && !Instance.class.isAssignableFrom(rawType);
    }

    public void inject(Object declaringInstance, BeanManagerImpl manager, CreationalContext<?> creationalContext) {
        try {
            Object instanceToInject = declaringInstance;
            if (!(instanceToInject instanceof DecoratorProxy)) {
                // if declaringInstance is a proxy, unwrap it
                if (declaringInstance instanceof TargetInstanceProxy) {
                    instanceToInject = Reflections.<TargetInstanceProxy<T>> cast(declaringInstance).getTargetInstance();
                }
            }
            Object objectToInject;
            if (!cacheable) {
                objectToInject = manager.getInjectableReference(this, creationalContext);
            } else {
                if (cachedBean == null) {
                    cachedBean = manager.resolve(manager.getBeans(this));
                }
                objectToInject = manager.getReference(this, cachedBean, creationalContext);
            }
            RuntimeAnnotatedMembers.setFieldValue(getAnnotated(), instanceToInject, objectToInject);
        } catch (IllegalArgumentException e) {
            rethrowException(e);
        } catch (IllegalAccessException e) {
            rethrowException(e);
        }
    }

    public void inject(Object declaringInstance, Object value) {
        try {
            Object instanceToInject = declaringInstance;
            if (!(instanceToInject instanceof DecoratorProxy)) {
                // if declaringInstance is a proxy, unwrap it
                if (instanceToInject instanceof TargetInstanceProxy)
                    instanceToInject = Reflections.<TargetInstanceProxy<T>> cast(declaringInstance).getTargetInstance();
            }
            RuntimeAnnotatedMembers.setFieldValue(getAnnotated(), instanceToInject, value);
        } catch (IllegalArgumentException e) {
            rethrowException(e);
        } catch (IllegalAccessException e) {
            rethrowException(e);
        }
    }

    @Override
    protected FieldInjectionPointAttributes<T, X> delegate() {
        return attributes;
    }

    @Override
    public AnnotatedField<X> getAnnotated() {
        return attributes.getAnnotated();
    }
}
