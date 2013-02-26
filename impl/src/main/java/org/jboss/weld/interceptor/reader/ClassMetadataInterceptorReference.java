/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.weld.interceptor.reader;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorReference;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.reflection.SecureReflections;

/**
 * {@link org.jboss.weld.interceptor.spi.metadata.ClassMetadata}-based implementation of {@link org.jboss.weld.interceptor.spi.metadata.InterceptorReference}
 * <p/>
 * This is used internally by the framework.
 */
public class ClassMetadataInterceptorReference<T> implements InterceptorReference<ClassMetadata<?>> {

    private static final long serialVersionUID = -619464974130150607L;

    public static <T> InterceptorReference<ClassMetadata<?>> of(ClassMetadata<T> classMetadata, BeanManagerImpl manager) {
        return new ClassMetadataInterceptorReference<T>(classMetadata, manager);
    }

    private final ClassMetadata<T> classMetadata;
    private final InjectionTarget<T> injectionTarget;

    private ClassMetadataInterceptorReference(ClassMetadata<T> classMetadata, BeanManagerImpl manager) {
        this.classMetadata = classMetadata;
        this.injectionTarget = manager.createInjectionTarget(manager.createAnnotatedType(classMetadata.getJavaClass()));
    }

    public ClassMetadata<?> getClassMetadata() {
        return classMetadata;
    }

    public ClassMetadata<?> getInterceptor() {
        // here the interceptor type is the class itself, so this duplicates getClassMetadata()
        return getClassMetadata();
    }

    public T create(CreationalContext<T> ctx, BeanManager manager) {
        try {
            T instance = SecureReflections.newInstance(classMetadata.getJavaClass()); // TODO: use special InjectionTarget (that does not apply interceptors) to instantiate the class
            injectionTarget.inject(instance, ctx);
            return instance;
        } catch (Exception e) {
            throw new WeldException(e); // TODO: add proper message
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ClassMetadataInterceptorReference<?>) {
            ClassMetadataInterceptorReference<?> that = (ClassMetadataInterceptorReference<?>) o;
            return this.classMetadata.equals(that.classMetadata);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return classMetadata.hashCode();
    }
}
