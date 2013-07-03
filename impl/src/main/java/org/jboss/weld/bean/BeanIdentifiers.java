/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import javax.enterprise.inject.spi.Extension;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedField;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.builtin.AbstractBuiltInBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.AnnotatedTypes;
import org.jboss.weld.util.Beans;

public class BeanIdentifiers {

    private BeanIdentifiers() {
    }

    public static final String SEPARATOR = "|";

    public static String forManagedBean(EnhancedAnnotatedType<?> type) {
        return new StringBuilder(ManagedBean.class.getName()).append(SEPARATOR).append(type.slim().getIdentifier().asString()).toString();
    }

    public static String forDecorator(EnhancedAnnotatedType<?> type) {
        return new StringBuilder(DecoratorImpl.class.getName()).append(SEPARATOR).append(type.slim().getIdentifier().asString()).toString();
    }

    public static String forInterceptor(EnhancedAnnotatedType<?> type) {
        return new StringBuilder(InterceptorImpl.class.getName()).append(SEPARATOR).append(type.slim().getIdentifier().asString()).toString();
    }

    public static String forNewManagedBean(EnhancedAnnotatedType<?> type) {
        return new StringBuilder(NewManagedBean.class.getName()).append(SEPARATOR).append(type.slim().getIdentifier().asString()).toString();
    }

    public static String forSessionBean(EnhancedAnnotatedType<?> type, EjbDescriptor<?> descriptor) {
        StringBuilder builder = new StringBuilder(SessionBean.class.getName()).append(SEPARATOR).append(descriptor.getEjbName());
        if (!type.isDiscovered()) {
            builder.append(SEPARATOR).append(type.slim().getIdentifier().asString());
        }
        return builder.toString();
    }

    public static String forNewSessionBean(EjbDescriptor<?> descriptor) {
        return new StringBuilder(NewSessionBean.class.getName()).append(SEPARATOR).append(descriptor.getEjbName()).toString();
    }

    public static String forProducerField(EnhancedAnnotatedField<?, ?> field, AbstractClassBean<?> declaringBean) {
        StringBuilder sb = new StringBuilder(ProducerField.class.getName()).append(SEPARATOR).append(declaringBean.getAnnotated().getIdentifier().asString())
                .append(SEPARATOR);
        if (declaringBean.getEnhancedAnnotated().isDiscovered()) {
            sb.append(field.getName());
        } else {
            sb.append(AnnotatedTypes.createFieldId(field));
        }
        return sb.toString();
    }

    public static String forProducerMethod(EnhancedAnnotatedMethod<?, ?> method, AbstractClassBean<?> declaringBean) {
        StringBuilder sb = new StringBuilder(ProducerMethod.class.getName()).append(SEPARATOR).append(declaringBean.getAnnotated().getIdentifier().asString())
                .append(SEPARATOR);
        if (declaringBean.getEnhancedAnnotated().isDiscovered()) {
            sb.append(method.getSignature().toString());
        } else {
            sb.append(AnnotatedTypes.createCallableId(method));
        }
        return sb.toString();
    }

    public static String forSyntheticBean(BeanAttributes<?> attributes, Class<?> beanClass) {
        return new StringBuilder().append(SyntheticClassBean.class.getName()).append(SEPARATOR).append(beanClass.getName()).append(SEPARATOR)
                .append(Beans.createBeanAttributesId(attributes)).toString();
    }

    public static String forBuiltInBean(BeanManagerImpl manager, Class<?> type, String suffix) {
        StringBuilder builder = new StringBuilder(AbstractBuiltInBean.class.getName()).append(SEPARATOR).append(manager.getId()).append(SEPARATOR).append(type.getSimpleName());
        if (suffix != null) {
            builder.append(SEPARATOR).append(suffix);
        }
        return builder.toString();
    }

    public static String forExtension(EnhancedAnnotatedType<?> type) {
        return new StringBuilder(Extension.class.getName()).append(SEPARATOR).append(type.slim().getIdentifier().asString()).toString();
    }
}
