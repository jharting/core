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

import static org.jboss.weld.logging.messages.BeanMessage.NON_SERIALIZABLE_CONSTRUCTOR_PARAM_INJECTION_ERROR;
import static org.jboss.weld.logging.messages.BeanMessage.NON_SERIALIZABLE_FIELD_INJECTION_ERROR;
import static org.jboss.weld.logging.messages.BeanMessage.NON_SERIALIZABLE_INITIALIZER_PARAM_INJECTION_ERROR;
import static org.jboss.weld.logging.messages.BeanMessage.NON_SERIALIZABLE_PRODUCER_PARAM_INJECTION_ERROR;
import static org.jboss.weld.logging.messages.BeanMessage.NON_SERIALIZABLE_PRODUCT_ERROR;
import static org.jboss.weld.logging.messages.BeanMessage.NULL_NOT_ALLOWED_FROM_PRODUCER;
import static org.jboss.weld.logging.messages.BeanMessage.PRODUCER_CAST_ERROR;
import static org.jboss.weld.logging.messages.BeanMessage.PRODUCER_METHOD_CANNOT_HAVE_A_WILDCARD_RETURN_TYPE;
import static org.jboss.weld.logging.messages.BeanMessage.PRODUCER_METHOD_WITH_TYPE_VARIABLE_RETURN_TYPE_MUST_BE_DEPENDENT;
import static org.jboss.weld.logging.messages.BeanMessage.RETURN_TYPE_MUST_BE_CONCRETE;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Inject;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMember;
import org.jboss.weld.bootstrap.BeanDeployerEnvironment;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.exceptions.IllegalProductException;
import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.producer.AbstractMemberProducer;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.metadata.cache.MetaAnnotationStore;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.reflection.Reflections;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

/**
 * The implicit producer bean
 *
 * @param <X>
 * @param <T>
 * @param <S>
 * @author Gavin King
 * @author David Allen
 * @author Jozef Hartinger
 */
public abstract class AbstractProducerBean<X, T, S extends Member> extends AbstractBean<T, S> {

    private static final Function<Class<?>, Boolean> SERIALIZABLE_CHECK = new Function<Class<?>, Boolean>() {

        public Boolean apply(Class<?> from) {
            return Reflections.isSerializable(from);
        }

    };

    // Underlying Producer represented by this bean
    private Producer<T> producer;

    private final AbstractClassBean<X> declaringBean;

    // Passivation flags
    private boolean passivationCapableBean;
    private boolean passivationCapableDependency;

    // Serialization cache for produced types at runtime
    private ConcurrentMap<Class<?>, Boolean> serializationCheckCache;

    /**
     * Constructor
     *
     * @param declaringBean The declaring bean
     * @param beanManager   The Bean manager
     */
    public AbstractProducerBean(BeanAttributes<T> attributes, String idSuffix, AbstractClassBean<X> declaringBean, BeanManagerImpl beanManager, ServiceRegistry services) {
        super(attributes, idSuffix, beanManager);
        this.declaringBean = declaringBean;
        serializationCheckCache = new MapMaker().makeComputingMap(SERIALIZABLE_CHECK);
    }

    @Override
    // Overriden to provide the class of the bean that declares the producer
    // method/field
    public Class<?> getBeanClass() {
        return getDeclaringBean().getBeanClass();
    }

    /**
     * Initializes the type
     */
    protected void initType() {
        try {
            this.type = getEnhancedAnnotated().getJavaClass();
        } catch (ClassCastException e) {
            Type type = Beans.getDeclaredBeanType(getClass());
            throw new WeldException(PRODUCER_CAST_ERROR, e, getEnhancedAnnotated().getJavaClass(), (type == null ? " unknown " : type));
        }
    }

    /**
     * Validates the producer method
     */
    protected void checkProducerReturnType() {
        if ((getEnhancedAnnotated().getBaseType() instanceof TypeVariable<?>) || (getEnhancedAnnotated().getBaseType() instanceof WildcardType)) {
            throw new DefinitionException(RETURN_TYPE_MUST_BE_CONCRETE, getEnhancedAnnotated().getBaseType());
        } else if (getEnhancedAnnotated().isParameterizedType()) {
            for (Type type : getEnhancedAnnotated().getActualTypeArguments()) {
                if (!Dependent.class.equals(getScope()) && type instanceof TypeVariable<?>) {
                    throw new DefinitionException(PRODUCER_METHOD_WITH_TYPE_VARIABLE_RETURN_TYPE_MUST_BE_DEPENDENT, getEnhancedAnnotated());
                } else if (type instanceof WildcardType) {
                    throw new DefinitionException(PRODUCER_METHOD_CANNOT_HAVE_A_WILDCARD_RETURN_TYPE, getEnhancedAnnotated());
                }
            }
        }
    }

    /**
     * Initializes the bean and its metadata
     */
    @Override
    public void internalInitialize(BeanDeployerEnvironment environment) {
        getDeclaringBean().initialize(environment);
        super.internalInitialize(environment);
        checkProducerReturnType();
        initPassivationCapable();
    }

    private void initPassivationCapable() {
        if (getEnhancedAnnotated().isFinal() && !Serializable.class.isAssignableFrom(getEnhancedAnnotated().getJavaClass())) {
            this.passivationCapableBean = false;
        } else {
            this.passivationCapableBean = true;
        }
        if (isNormalScoped()) {
            this.passivationCapableDependency = true;
        } else if (getScope().equals(Dependent.class) && passivationCapableBean) {
            this.passivationCapableDependency = true;
        } else {
            this.passivationCapableDependency = false;
        }
    }

    @Override
    public boolean isPassivationCapableBean() {
        return passivationCapableBean;
    }

    @Override
    public boolean isPassivationCapableDependency() {
        return passivationCapableDependency;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return getProducer().getInjectionPoints();
    }

    /**
     * Validates the return value
     *
     * @param instance The instance to validate
     */
    protected void checkReturnValue(T instance) {
        if (instance == null && !isDependent()) {
            throw new IllegalProductException(NULL_NOT_ALLOWED_FROM_PRODUCER, getProducer());
        } else if (instance != null) {
            boolean passivating = beanManager.getServices().get(MetaAnnotationStore.class).getScopeModel(getScope()).isPassivating();
            boolean instanceSerializable = isTypeSerializable(instance.getClass());
            if (passivating && !instanceSerializable) {
                throw new IllegalProductException(NON_SERIALIZABLE_PRODUCT_ERROR, getProducer());
            }
            InjectionPoint injectionPoint = beanManager.getServices().get(CurrentInjectionPoint.class).peek();
            if (injectionPoint != null && injectionPoint.getBean() != null) {
                if (!instanceSerializable && Beans.isPassivatingScope(injectionPoint.getBean(), beanManager)) {
                    if (injectionPoint.getMember() instanceof Field) {
                        if (!injectionPoint.isTransient() && instance != null && !instanceSerializable) {
                            throw new IllegalProductException(NON_SERIALIZABLE_FIELD_INJECTION_ERROR, this, injectionPoint);
                        }
                    } else if (injectionPoint.getMember() instanceof Method) {
                        Method method = (Method) injectionPoint.getMember();
                        if (method.isAnnotationPresent(Inject.class)) {
                            throw new IllegalProductException(NON_SERIALIZABLE_INITIALIZER_PARAM_INJECTION_ERROR, this, injectionPoint);
                        }
                        if (method.isAnnotationPresent(Produces.class)) {
                            throw new IllegalProductException(NON_SERIALIZABLE_PRODUCER_PARAM_INJECTION_ERROR, this, injectionPoint);
                        }
                    } else if (injectionPoint.getMember() instanceof Constructor<?>) {
                        throw new IllegalProductException(NON_SERIALIZABLE_CONSTRUCTOR_PARAM_INJECTION_ERROR, this, injectionPoint);
                    }
                }
            }
        }
    }

    @Override
    protected void checkType() {

    }

    protected boolean isTypeSerializable(final Class<?> clazz) {
        return serializationCheckCache.get(clazz);
    }

    /**
     * This operation is *not* threadsafe, and should not be called outside
     * bootstrap
     *
     * @param producer
     */
    public void setProducer(Producer<T> producer) {
        this.producer = producer;
    }

    public Producer<T> getProducer() {
        return producer;
    }

    /**
     * Creates an instance of the bean
     *
     * @returns The instance
     */
    public T create(final CreationalContext<T> creationalContext) {
        T instance = getProducer().produce(creationalContext);
        checkReturnValue(instance);
        return instance;
    }

    public void destroy(T instance, CreationalContext<T> creationalContext) {
        try {
            if (producer instanceof AbstractMemberProducer<?, ?>) {
                Reflections.<AbstractMemberProducer<?, T>>cast(producer).dispose(instance, creationalContext);
            } else {
                producer.dispose(instance);
            }
        } finally {
            if (getDeclaringBean().isDependent()) {
                creationalContext.release();
            }
        }
    }

    /**
     * Returns the declaring bean
     *
     * @return The bean representation
     */
    public AbstractClassBean<X> getDeclaringBean() {
        return declaringBean;
    }

    @Override
    public abstract AnnotatedMember<? super X> getAnnotated();

    @Override
    public abstract EnhancedAnnotatedMember<T, ?, S> getEnhancedAnnotated();
}
