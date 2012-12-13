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
package org.jboss.weld.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedParameter;
import org.jboss.weld.literal.DefaultLiteral;
import org.jboss.weld.manager.BeanManagerImpl;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

/**
 * Represents an injection point for {@link BeanManager} with the {@link Default} qualifier.
 * @author Jozef Hartinger
 *
 * @param <X> the declaring type
 */
public class BeanManagerInjectionPoint<X> implements ParameterInjectionPoint<BeanManager, X> {

    public static <X> BeanManagerInjectionPoint<X> of(EnhancedAnnotatedParameter<?, X> parameter, Bean<?> declaringBean, BeanManagerImpl beanManager) {
        return new BeanManagerInjectionPoint<X>(beanManager, declaringBean, parameter.slim());
    }

    public static final Set<Annotation> QUALIFIERS = ImmutableSet.<Annotation>of(DefaultLiteral.INSTANCE);
    private final BeanManagerImpl beanManager;
    private final Bean<?> declaringBean;
    private final AnnotatedParameter<X> parameter;

    private BeanManagerInjectionPoint(BeanManagerImpl beanManager, Bean<?> declaringBean, AnnotatedParameter<X> parameter) {
        this.beanManager = beanManager;
        this.declaringBean = declaringBean;
        this.parameter = parameter;
    }

    @Override
    public void inject(Object declaringInstance, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A extends Annotation> A getQualifier(Class<A> annotationType) {
        if (annotationType.equals(Default.class)) {
            return annotationType.cast(DefaultLiteral.INSTANCE);
        }
        return null;
    }

    @Override
    public Type getType() {
        return BeanManager.class;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return QUALIFIERS;
    }

    @Override
    public Bean<?> getBean() {
        return declaringBean;
    }

    @Override
    public Member getMember() {
        return getAnnotated().getDeclaringCallable().getJavaMember();
    }

    @Override
    public boolean isDelegate() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public AnnotatedParameter<X> getAnnotated() {
        return parameter;
    }

    @Override
    public BeanManager getValueToInject(BeanManagerImpl manager, CreationalContext<?> creationalContext) {
        return beanManager;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(declaringBean, parameter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof BeanManagerInjectionPoint)) {
            return false;
        }
        BeanManagerInjectionPoint<?> that = (BeanManagerInjectionPoint<?>) obj;
        return (Objects.equal(this.declaringBean, that.declaringBean) && Objects.equal(this.parameter, that.parameter));
    }
}
