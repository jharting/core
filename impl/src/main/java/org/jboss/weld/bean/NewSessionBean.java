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
package org.jboss.weld.bean;

import javax.enterprise.inject.spi.BeanAttributes;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.id.SessionBeanIdentifier;
import org.jboss.weld.ejb.InternalEjbDescriptor;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

/**
 * Represents a @New enterprise bean
 *
 * @author Nicklas Karlsson
 */
public class NewSessionBean<T> extends SessionBean<T> implements NewBean {

    /**
     * Creates an instance of a NewEnterpriseBean from an annotated class
     *
     * @param clazz       The annotated class
     * @param beanManager The Bean manager
     * @return a new NewEnterpriseBean instance
     */
    public static <T> NewSessionBean<T> of(BeanAttributes<T> attributes, InternalEjbDescriptor<T> ejbDescriptor, BeanManagerImpl beanManager) {
        EnhancedAnnotatedType<T> type = beanManager.getServices().get(ClassTransformer.class).getEnhancedAnnotatedType(ejbDescriptor.getBeanClass(), beanManager.getId());
        return new NewSessionBean<T>(attributes, type, ejbDescriptor, new SessionBeanIdentifier(type.slim().getIdentifier(), ejbDescriptor.getEjbName(), true), beanManager);
    }

    /**
     * Protected constructor
     *
     * @param type        An annotated class
     * @param beanManager The Bean manager
     */
    protected NewSessionBean(BeanAttributes<T> attributes, final EnhancedAnnotatedType<T> type, InternalEjbDescriptor<T> ejbDescriptor, SessionBeanIdentifier identifier, BeanManagerImpl beanManager) {
        super(attributes, type, ejbDescriptor, identifier, beanManager);
    }

    @Override
    public boolean isSpecializing() {
        return false;
    }

    @Override
    protected void checkScopeAllowed() {
        // No-op
    }

    @Override
    public String toString() {
        return "@New " + super.toString();
    }
}
