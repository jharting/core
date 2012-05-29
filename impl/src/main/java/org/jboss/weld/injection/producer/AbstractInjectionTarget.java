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
package org.jboss.weld.injection.producer;

import static org.jboss.weld.logging.messages.BeanMessage.INVOCATION_ERROR;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.runtime.RuntimeAnnotatedMembers;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.injection.FieldInjectionPoint;
import org.jboss.weld.injection.InjectionPointFactory;
import org.jboss.weld.injection.MethodInjectionPoint;
import org.jboss.weld.logging.messages.BeanMessage;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.InjectionPoints;

/**
 * @author Pete Muir
 * @author Jozef Hartinger
 */
public abstract class AbstractInjectionTarget<T> implements InjectionTarget<T> {

    protected final BeanManagerImpl beanManager;
    private final AnnotatedType<T> type;
    private final List<Set<FieldInjectionPoint<?, ?>>> injectableFields;
    private final List<Set<MethodInjectionPoint<?, ?>>> initializerMethods;
    private final List<AnnotatedMethod<? super T>> postConstructMethods;
    private final List<AnnotatedMethod<? super T>> preDestroyMethods;
    private final Set<InjectionPoint> injectionPoints;
    private final Bean<T> bean;

    // Instantiation
    private Instantiator<T> instantiator;

    public AbstractInjectionTarget(EnhancedAnnotatedType<T> type, Bean<T> bean, BeanManagerImpl beanManager) {
        this.beanManager = beanManager;
        this.type = type.slim();
        this.injectionPoints = new HashSet<InjectionPoint>();
        if (type.getJavaClass().isInterface()) {
            throw new DefinitionException(BeanMessage.INJECTION_TARGET_CANNOT_BE_CREATED_FOR_INTERFACE, type);
        }

        DefaultInstantiator<T> instantiator = new DefaultInstantiator<T>(type, bean, beanManager);
        injectionPoints.addAll(instantiator.getConstructor().getParameterInjectionPoints());
        this.instantiator = instantiator;

        this.injectableFields = InjectionPointFactory.instance().getFieldInjectionPoints(bean, type, beanManager);
        this.injectionPoints.addAll(InjectionPoints.flattenInjectionPoints(this.injectableFields));
        this.initializerMethods = Beans.getInitializerMethods(bean, type, beanManager);
        this.injectionPoints.addAll(InjectionPoints.flattenParameterInjectionPoints(initializerMethods));
        this.postConstructMethods = Beans.getPostConstructMethods(type);
        this.preDestroyMethods = Beans.getPreDestroyMethods(type);
        this.bean = bean;
    }

    public T produce(CreationalContext<T> ctx) {
        return instantiator.newInstance(ctx, beanManager);
    }

    public void postConstruct(T instance) {
        for (AnnotatedMethod<? super T> method : postConstructMethods) {
            if (method != null) {
                try {
                    // note: RI supports injection into @PreDestroy
                    RuntimeAnnotatedMembers.invokeMethod(method, instance);
                } catch (Exception e) {
                    throw new WeldException(INVOCATION_ERROR, e, method, instance);
                }
            }
        }
    }

    public void preDestroy(T instance) {
        for (AnnotatedMethod<? super T> method : preDestroyMethods) {
            if (method != null) {
                try {
                    // note: RI supports injection into @PreDestroy
                    RuntimeAnnotatedMembers.invokeMethod(method, instance);
                } catch (Exception e) {
                    throw new WeldException(INVOCATION_ERROR, e, method, instance);
                }
            }
        }
    }

    public void dispose(T instance) {
        // No-op
    }

    public Set<InjectionPoint> getInjectionPoints() {
        return injectionPoints;
    }

    protected AnnotatedType<T> getType() {
        return type;
    }

    protected BeanManagerImpl getBeanManager() {
        return beanManager;
    }

    protected List<Set<FieldInjectionPoint<?, ?>>> getInjectableFields() {
        return injectableFields;
    }

    protected List<Set<MethodInjectionPoint<?, ?>>> getInitializerMethods() {
        return initializerMethods;
    }

    public Instantiator<T> getInstantiator() {
        return instantiator;
    }

    public void setInstantiator(Instantiator<T> instantiator) {
        this.instantiator = instantiator;
    }

    public Bean<T> getBean() {
        return bean;
    }

    public boolean hasInterceptors() {
        return instantiator.hasInterceptors();
    }

    public boolean hasDecorators() {
        return instantiator.hasDecorators();
    }
}
