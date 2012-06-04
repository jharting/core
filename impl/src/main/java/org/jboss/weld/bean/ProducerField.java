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

import static org.jboss.weld.logging.messages.BeanMessage.PRODUCER_FIELD_ON_SESSION_BEAN_MUST_BE_STATIC;

import java.lang.reflect.Field;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedField;
import org.jboss.weld.bootstrap.BeanDeployerEnvironment;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.injection.producer.ProducerFieldProducer;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.AnnotatedTypes;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.Proxies;
import org.jboss.weld.util.reflection.Formats;

/**
 * Represents a producer field
 *
 * @param <T>
 * @author Pete Muir
 */
public class ProducerField<X, T> extends AbstractProducerBean<X, T, Field> {

    // The underlying field
    private final AnnotatedField<? super X> annotatedField;
    private volatile EnhancedAnnotatedField<T, ? super X> enhancedAnnotatedField;
    private final boolean proxiable;

    /**
     * Creates a producer field
     *
     * @param field         The underlying method abstraction
     * @param declaringBean The declaring bean abstraction
     * @param beanManager   the current manager
     * @return A producer field
     */
    public static <X, T> ProducerField<X, T> of(BeanAttributes<T> attributes, EnhancedAnnotatedField<T, ? super X> field, AbstractClassBean<X> declaringBean, DisposalMethod<X, ?> disposalMethod, BeanManagerImpl beanManager, ServiceRegistry services) {
        return new ProducerField<X, T>(attributes, field, declaringBean, disposalMethod, beanManager, services);
    }


    /**
     * Constructor
     *
     * @param method        The producer field abstraction
     * @param declaringBean The declaring bean
     * @param manager       The Bean manager
     */
    protected ProducerField(BeanAttributes<T> attributes, EnhancedAnnotatedField<T, ? super X> field, AbstractClassBean<X> declaringBean, DisposalMethod<X, ?> disposalMethod, BeanManagerImpl manager, ServiceRegistry services) {
        super(attributes, createId(field, declaringBean), declaringBean, manager, services);
        this.enhancedAnnotatedField = field;
        this.annotatedField = field.slim();
        initType();
        this.proxiable = Proxies.isTypesProxyable(field.getTypeClosure());
        setProducer(new ProducerFieldProducer<X, T>(disposalMethod) {

            @Override
            public AnnotatedField<? super X> getAnnotated() {
                return annotatedField;
            }

            @Override
            public BeanManagerImpl getBeanManager() {
                return beanManager;
            }

            @Override
            public Bean<X> getDeclaringBean() {
                return ProducerField.this.getDeclaringBean();
            }

            @Override
            public Bean<T> getBean() {
                return ProducerField.this;
            }

            @Override
            protected boolean isTypeSerializable(Object object) {
                return isTypeSerializable(object.getClass());
            }

        });
    }

    protected static String createId(EnhancedAnnotatedField<?, ?> field, AbstractClassBean<?> declaringBean) {
        if (declaringBean.getEnhancedAnnotated().isDiscovered()) {
            StringBuilder sb = new StringBuilder();
            sb.append(ProducerField.class.getSimpleName());
            sb.append(BEAN_ID_SEPARATOR);
            sb.append(declaringBean.getEnhancedAnnotated().getName());
            sb.append(".");
            sb.append(field.getName());
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(ProducerField.class.getSimpleName());
            sb.append(BEAN_ID_SEPARATOR);
            sb.append(AnnotatedTypes.createTypeId(declaringBean.getEnhancedAnnotated()));
            sb.append(".");
            sb.append(AnnotatedTypes.createFieldId(field));
            return sb.toString();
        }
    }

    @Override
    public void internalInitialize(BeanDeployerEnvironment environment) {
        super.internalInitialize(environment);
        checkProducerField();
    }


    protected void checkProducerField() {
        if (getDeclaringBean() instanceof SessionBean<?> && !getEnhancedAnnotated().isStatic()) {
            throw new DefinitionException(PRODUCER_FIELD_ON_SESSION_BEAN_MUST_BE_STATIC, getEnhancedAnnotated(), getEnhancedAnnotated().getDeclaringType());
        }
    }

    @Override
    public AnnotatedField<? super X> getAnnotated() {
        return annotatedField;
    }

    /**
     * Gets the annotated item representing the field
     *
     * @return The annotated item
     */
    @Override
    public EnhancedAnnotatedField<T, ? super X> getEnhancedAnnotated() {
        return Beans.checkEnhancedAnnotatedAvailable(enhancedAnnotatedField);
    }

    @Override
    public void cleanupAfterBoot() {
//        super.cleanupAfterBoot();
        this.enhancedAnnotatedField = null;
    }


    @Override
    public AbstractBean<?, ?> getSpecializedBean() {
        return null;
    }

    @Override
    public boolean isSpecializing() {
        return false;
    }

    @Override
    public String toString() {
        return "Producer Field [" + Formats.formatType(getAnnotated().getBaseType()) + "] with qualifiers [" + Formats.formatAnnotations(getQualifiers()) + "] declared as [" + getAnnotated() + "]";
    }

    @Override
    public boolean isProxyable() {
        return proxiable;
    }

}
