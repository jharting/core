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
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;

import org.jboss.weld.bean.BeanIdentifiers;
import org.jboss.weld.bean.StringBeanIdentifier;
import org.jboss.weld.context.WeldCreationalContext;
import org.jboss.weld.literal.InterceptedLiteral;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.jboss.weld.util.bean.SerializableForwardingBean;

/**
 * Allows an interceptor to obtain information about the bean it intercepts.
 *
 * @author Jozef Hartinger
 * @see CDI-92
 *
 */
public class InterceptedBeanMetadataBean extends BeanMetadataBean {

    public InterceptedBeanMetadataBean(BeanManagerImpl beanManager) {
        this(new StringBeanIdentifier(BeanIdentifiers.forBuiltInBean(beanManager, Bean.class, Intercepted.class.getSimpleName())), beanManager);
    }

    protected InterceptedBeanMetadataBean(BeanIdentifier identifier, BeanManagerImpl beanManager) {
        super(identifier, beanManager);
    }

    @Override
    protected Bean<?> newInstance(InjectionPoint ip, CreationalContext<Bean<?>> ctx) {
        checkInjectionPoint(ip);

        WeldCreationalContext<?> interceptorContext = getParentCreationalContext(ctx);
        WeldCreationalContext<?> interceptedBeanContext = getParentCreationalContext(interceptorContext);
        Contextual<?> interceptedContextual = interceptedBeanContext.getContextual();

        if (interceptedContextual instanceof Bean<?>) {
            Bean<?> bean = (Bean<?>) interceptedContextual;
            if (bean instanceof Serializable) {
                return bean;
            } else {
                return SerializableForwardingBean.of(bean);
            }
        } else {
            throw new IllegalArgumentException("Unable to determine @Intercepted Bean<?> for this interceptor.");
        }
    }

    protected void checkInjectionPoint(InjectionPoint ip) {
        if (!(ip.getBean() instanceof Interceptor<?>)) {
            throw new IllegalArgumentException("@Intercepted Bean<?> can only be injected into an interceptor.");
        }
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.<Annotation> singleton(InterceptedLiteral.INSTANCE);
    }

    @Override
    public String toString() {
        return "Implicit Bean [javax.enterprise.inject.spi.Bean] with qualifiers [@Intercepted]";
    }
}
