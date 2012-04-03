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
package org.jboss.weld.event;

import static org.jboss.weld.logging.messages.EventMessage.INVALID_DISPOSES_PARAMETER;
import static org.jboss.weld.logging.messages.EventMessage.INVALID_INITIALIZER;
import static org.jboss.weld.logging.messages.EventMessage.INVALID_INJECTION_POINT;
import static org.jboss.weld.logging.messages.EventMessage.INVALID_PRODUCER;
import static org.jboss.weld.logging.messages.EventMessage.INVALID_SCOPED_CONDITIONAL_OBSERVER;
import static org.jboss.weld.logging.messages.EventMessage.MULTIPLE_EVENT_PARAMETERS;
import static org.jboss.weld.logging.messages.ValidatorMessage.NON_FIELD_INJECTION_POINT_CANNOT_USE_NAMED;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.ObserverException;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedParameter;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.injection.MethodInjectionPoint;
import org.jboss.weld.injection.ParameterInjectionPoint;
import org.jboss.weld.injection.WeldInjectionPoint;
import org.jboss.weld.injection.attributes.SpecialParameterInjectionPoint;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Observers;

/**
 * <p>
 * Reference implementation for the ObserverMethod interface, which represents
 * an observer method. Each observer method has an event type which is the class
 * of the event object being observed, and event binding types that are
 * annotations applied to the event parameter to narrow the event notifications
 * delivered.
 * </p>
 *
 * @author David Allen
 * @author Jozef Hartinger
 */
public class ObserverMethodImpl<T, X> implements ObserverMethod<T> {

    public static final String ID_PREFIX = ObserverMethodImpl.class.getPackage().getName();

    public static final String ID_SEPARATOR = "-";

    private final Set<Annotation> bindings;
    private final Type eventType;
    protected final BeanManagerImpl beanManager;
    private final Reception reception;
    protected final RIBean<X> declaringBean;
    protected final MethodInjectionPoint<T, ? super X> observerMethod;
    protected TransactionPhase transactionPhase;
    private final String id;

    private final Set<WeldInjectionPoint<?, ?>> injectionPoints;
    private final Set<WeldInjectionPoint<?, ?>> newInjectionPoints;

    /**
     * Creates an Observer which describes and encapsulates an observer method
     * (8.5).
     *
     * @param observer      The observer
     * @param declaringBean The observer bean
     * @param manager       The Bean manager
     */
    protected ObserverMethodImpl(final EnhancedAnnotatedMethod<T, ? super X> observer, final RIBean<X> declaringBean, final BeanManagerImpl manager) {
        this.beanManager = manager;
        this.declaringBean = declaringBean;
        this.observerMethod = MethodInjectionPoint.ofObserverOrDisposerMethod(observer, declaringBean, manager);
        this.eventType = observerMethod.getAnnotated().getEnhancedParameters(Observes.class).get(0).getBaseType();
        this.id = new StringBuilder().append(ID_PREFIX).append(ID_SEPARATOR)/*.append(manager.getId()).append(ID_SEPARATOR)*/.append(ObserverMethod.class.getSimpleName()).append(ID_SEPARATOR).append(declaringBean.getBeanClass().getName()).append(".").append(observer.getSignature()).toString();
        this.bindings = new HashSet<Annotation>(observerMethod.getAnnotated().getEnhancedParameters(Observes.class).get(0).getMetaAnnotations(Qualifier.class));
        Observes observesAnnotation = observerMethod.getAnnotated().getEnhancedParameters(Observes.class).get(0).getAnnotation(Observes.class);
        this.reception = observesAnnotation.notifyObserver();
        transactionPhase = TransactionPhase.IN_PROGRESS;

        this.injectionPoints = new HashSet<WeldInjectionPoint<?, ?>>();
        this.newInjectionPoints = new HashSet<WeldInjectionPoint<?, ?>>();
        for (ParameterInjectionPoint<?, ?> injectionPoint : observerMethod.getParameterInjectionPoints()) {
            if (injectionPoint instanceof SpecialParameterInjectionPoint) {
                continue;
            }
            if (injectionPoint.getQualifier(New.class) != null) {
                this.newInjectionPoints.add(injectionPoint);
            }
            injectionPoints.add(injectionPoint);
        }
    }

    public Set<WeldInjectionPoint<?, ?>> getInjectionPoints() {
        return Collections.unmodifiableSet(injectionPoints);
    }

    public Set<WeldInjectionPoint<?, ?>> getNewInjectionPoints() {
        return Collections.unmodifiableSet(newInjectionPoints);
    }

    /**
     * Performs validation of the observer method for compliance with the
     * specifications.
     */
    private void checkObserverMethod() {
        // Make sure exactly one and only one parameter is annotated with Observes
        List<?> eventObjects = this.observerMethod.getAnnotated().getEnhancedParameters(Observes.class);
        if (this.reception.equals(Reception.IF_EXISTS) && declaringBean.getScope().equals(Dependent.class)) {
            throw new DefinitionException(INVALID_SCOPED_CONDITIONAL_OBSERVER, this);
        }
        if (eventObjects.size() > 1) {
            throw new DefinitionException(MULTIPLE_EVENT_PARAMETERS, this);
        }
        // Check for parameters annotated with @Disposes
        List<?> disposeParams = this.observerMethod.getAnnotated().getEnhancedParameters(Disposes.class);
        if (disposeParams.size() > 0) {
            throw new DefinitionException(INVALID_DISPOSES_PARAMETER, this);
        }
        // Check annotations on the method to make sure this is not a producer
        // method, initializer method, or destructor method.
        if (this.observerMethod.getAnnotated().isAnnotationPresent(Produces.class)) {
            throw new DefinitionException(INVALID_PRODUCER, this);
        }
        if (this.observerMethod.getAnnotated().isAnnotationPresent(Inject.class)) {
            throw new DefinitionException(INVALID_INITIALIZER, this);
        }
        boolean containerLifecycleObserverMethod = Observers.isContainerLifecycleObserverMethod(this);
        for (EnhancedAnnotatedParameter<?, ?> parameter : getMethod().getAnnotated().getEnhancedParameters()) {
            if (parameter.isAnnotationPresent(Named.class) && parameter.getAnnotation(Named.class).value().equals("")) {
                throw new DefinitionException(NON_FIELD_INJECTION_POINT_CANNOT_USE_NAMED, getMethod());
            }
            // if this is an observer method for container lifecycle event, it must not inject anything besides BeanManager
            if (containerLifecycleObserverMethod && !parameter.isAnnotationPresent(Observes.class) && !BeanManager.class.equals(parameter.getBaseType())) {
                throw new DefinitionException(INVALID_INJECTION_POINT, this);
            }
        }

    }

    public Class<X> getBeanClass() {
        return declaringBean.getType();
    }

    public RIBean<X> getDeclaringBean() {
        return declaringBean;
    }

    public Annotation[] getBindingsAsArray() {
        return bindings.toArray(new Annotation[0]);
    }

    public Reception getReception() {
        return reception;
    }

    public Set<Annotation> getObservedQualifiers() {
        return bindings;
    }

    public Type getObservedType() {
        return eventType;
    }

    public TransactionPhase getTransactionPhase() {
        return transactionPhase;
    }

    /**
     * @return the observerMethod
     */
    public MethodInjectionPoint<T, ? super X> getMethod() {
        return observerMethod;
    }

    /**
     * Completes initialization of the observer and allows derived types to
     * override behavior.
     */
    public void initialize() {
        checkObserverMethod();
    }

    public void notify(final T event) {
        sendEvent(event);
    }

    public void notify(T event, Set<Annotation> qualifiers) {
        notify(event);
    }

    /**
     * Invokes the observer method immediately passing the event.
     *
     * @param event The event to notify observer with
     */
    protected void sendEvent(final T event) {
        if (observerMethod.getAnnotated().isStatic()) {
            sendEvent(event, null, beanManager.createCreationalContext(declaringBean));
        } else if (reception.equals(Reception.IF_EXISTS)) {
            Object receiver = getReceiverIfExists();
            // The observer is conditional, and there is no existing bean
            if (receiver == null) {
                return;
            } else {
                sendEvent(event, receiver, null);
            }
        } else {
            CreationalContext<?> creationalContext = beanManager.createCreationalContext(declaringBean);
            sendEvent(event, getReceiver(creationalContext), creationalContext);
        }

    }

    protected void sendEvent(T event, Object receiver, CreationalContext<?> creationalContext) {
        try {
            preNotify(event, receiver);
            if (receiver == null) {
                observerMethod.invokeWithSpecialValue(receiver, Observes.class, event, beanManager, creationalContext, ObserverException.class);
            } else {
                // As we are working with the contextual instance, we may not have the
                // actual object, but a container proxy (e.g. EJB)
                observerMethod.invokeOnInstanceWithSpecialValue(receiver, Observes.class, event, beanManager, creationalContext, ObserverException.class);
            }
        } finally {
            postNotify(event, receiver);
            if (creationalContext != null && Dependent.class.equals(declaringBean.getScope())) {
                creationalContext.release();
            }
        }
    }

    /**
     * Hooks allowing subclasses to perform additional logic just before and just after an event is delivered to an observer method.
     */
    protected void preNotify(T event, Object receiver) {
    }

    protected void postNotify(T event, Object receiver) {
    }

    protected Object getReceiverIfExists() {
        try {
            return beanManager.getReference(declaringBean, null, false);
        } catch (ContextNotActiveException e) {
            return null;
        }
    }

    protected Object getReceiver(CreationalContext<?> ctx) {
        return beanManager.getReference(declaringBean, ctx, false);
    }

    @Override
    public String toString() {
        return observerMethod.toString();
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ObserverMethodImpl<?, ?>) {
            ObserverMethodImpl<?, ?> that = (ObserverMethodImpl<?, ?>) obj;
            return this.getId().equals(that.getId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
