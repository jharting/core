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
package org.jboss.weld.bootstrap.events;

import static org.jboss.weld.logging.messages.BootstrapMessage.ANNOTATION_TYPE_NULL;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resolution.Resolvable;
import org.jboss.weld.resources.ClassTransformer;

/**
 * Container lifecycle event for each Java class or interface discovered by
 * the container.
 *
 * @author pmuir
 * @author David Allen
 */
@SuppressWarnings("rawtypes")
public class ProcessAnnotatedTypeImpl<X> extends AbstractDefinitionContainerEvent implements ProcessAnnotatedType<X> {

    private SlimAnnotatedType<X> annotatedType;
    private boolean veto;
    private boolean dirty;
    private final Resolvable resolvable;

    public ProcessAnnotatedTypeImpl(BeanManagerImpl beanManager, SlimAnnotatedType<X> annotatedType, AnnotationDiscovery discovery) {
        this(beanManager, annotatedType, ProcessAnnotatedType.class, annotatedType.getJavaClass(), discovery);
    }

    protected ProcessAnnotatedTypeImpl(BeanManagerImpl beanManager, SlimAnnotatedType<X> annotatedType, Class<? extends ProcessAnnotatedType> rawType, Class<?> typeArgument, AnnotationDiscovery discovery) {
        super(beanManager, rawType, new Type[] { typeArgument });
        this.annotatedType = annotatedType;
        this.resolvable = createResolvable(typeArgument, discovery);
    }

    public SlimAnnotatedType<X> getAnnotatedType() {
        return annotatedType;
    }

    public void setAnnotatedType(AnnotatedType<X> type) {
        if (type == null) {
            throw new IllegalArgumentException(ANNOTATION_TYPE_NULL, this);
        }
        this.annotatedType = getBeanManager().getServices().get(ClassTransformer.class).getAnnotatedType(type);
        this.dirty = true;
    }

    protected Resolvable createResolvable(Class<?> typeArgument, AnnotationDiscovery discovery) {
        return ProcessAnnotatedTypeEventResolvable.forProcessAnnotatedType(typeArgument, discovery);
    }

    @Override
    public void fire() {
        Set<ObserverMethod<? super Object>> observers = getBeanManager().getGlobalLenientObserverNotifier().resolveObserverMethods(resolvable);
        if (!observers.isEmpty()) {
            annotatedType.init();
            try {
                for (ObserverMethod<? super Object> observer : observers) {
                    observer.notify(this, Collections.<Annotation> emptySet());
                }
            } catch (Exception e) {
                getErrors().add(e);
            }
        }
        if (!getErrors().isEmpty()) {
            throw new DefinitionException(getErrors());
        }
    }

    public void veto() {
        this.veto = true;
    }

    public boolean isVeto() {
        return veto;
    }

    public boolean isDirty() {
        return dirty;
    }

    @Override
    public String toString() {
        return annotatedType.toString();
    }

}
