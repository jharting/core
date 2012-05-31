/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.runtime.RuntimeAnnotatedMembers;
import org.jboss.weld.bean.proxy.DecoratorProxy;
import org.jboss.weld.bean.proxy.DecoratorProxyFactory;
import org.jboss.weld.bean.proxy.ProxyMethodHandler;
import org.jboss.weld.bean.proxy.ProxyObject;
import org.jboss.weld.bean.proxy.TargetBeanInstance;
import org.jboss.weld.injection.ConstructorInjectionPoint;
import org.jboss.weld.injection.FieldInjectionPoint;
import org.jboss.weld.injection.InjectionPointFactory;
import org.jboss.weld.injection.WeldInjectionPoint;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Decorators;

public class DecoratorInjectionTarget<T> extends DefaultInjectionTarget<T> {

    private final WeldInjectionPoint<?, ?> delegateInjectionPoint;

    public DecoratorInjectionTarget(EnhancedAnnotatedType<T> type, Bean<T> bean, BeanManagerImpl beanManager) {
        super(type, bean, beanManager);
        this.delegateInjectionPoint = Decorators.findDelegateInjectionPoint(type, getInjectionPoints());
    }

    @Override
    protected Instantiator<T> initInstantiator(EnhancedAnnotatedType<T> type, Bean<T> bean, BeanManagerImpl beanManager, Set<InjectionPoint> injectionPoints) {
        if (type.isAbstract()) {
            ConstructorInjectionPoint<T> originalConstructor = InjectionPointFactory.instance().createConstructorInjectionPoint(bean, type, beanManager);
            injectionPoints.addAll(originalConstructor.getParameterInjectionPoints());
            final WeldInjectionPoint<?, ?> delegateInjectionPoint = Decorators.findDelegateInjectionPoint(type, injectionPoints);
            return new SubclassedComponentInstantiator<T>(type, bean, originalConstructor, beanManager) {
                @Override
                protected Class<T> createEnhancedSubclass(AnnotatedType<T> type, Bean<?> bean) {
                    return new DecoratorProxyFactory<T>(type.getJavaClass(), delegateInjectionPoint, bean).getProxyClass();
                }
            };
        } else {
            DefaultInstantiator<T> instantiator = new DefaultInstantiator<T>(type, getBean(), beanManager);
            injectionPoints.addAll(instantiator.getConstructor().getParameterInjectionPoints());
            return instantiator;
        }
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {
        super.inject(instance, ctx);

        if (delegateInjectionPoint instanceof FieldInjectionPoint<?, ?>) {
            if (instance instanceof DecoratorProxy) {

                // this code is only applicable if the delegate is injected into a field
                // as the proxy can't intercept the delegate when setting the field
                // we need to now read the delegate from the field

                // this is only needed for fields, as constructor and method injection are handed
                // at injection time
                final Object delegate = RuntimeAnnotatedMembers.getFieldValue((AnnotatedField<?>) delegateInjectionPoint.getAnnotated(), instance);
                final ProxyMethodHandler handler = new ProxyMethodHandler(new TargetBeanInstance(delegate), getBean());
                ((ProxyObject) instance).setHandler(handler);
            }
        }
    }

    @Override
    public void initializeAfterBeanDiscovery(EnhancedAnnotatedType<T> annotatedType) {
        // noop
    }
}
