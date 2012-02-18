/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.weld.introspector.jlr.temp;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedType;

import org.jboss.weld.introspector.ConstructorSignature;
import org.jboss.weld.introspector.WeldClass;
import org.jboss.weld.introspector.WeldConstructor;
import org.jboss.weld.introspector.jlr.WeldConstructorImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.LazyValueHolder;
import org.jboss.weld.util.collections.ArraySet;
import org.jboss.weld.util.reflection.Reflections;
import org.jboss.weld.util.reflection.SecureReflections;

public class WeldConstructors<T> {

    // The set of abstracted constructors
    private final Set<WeldConstructor<T>> constructors;
    private final Map<ConstructorSignature, WeldConstructor<T>> constructorsBySignature;

    private WeldConstructors(WeldClass<T> weldClass, ClassTransformer classTransformer) {
        ArraySet<WeldConstructor<T>> constructorsTemp = new ArraySet<WeldConstructor<T>>();
        Map<ConstructorSignature, WeldConstructor<T>> constructorsBySignatureTemp = new HashMap<ConstructorSignature, WeldConstructor<T>>();

        for (Constructor<?> constructor : SecureReflections.getDeclaredConstructors(weldClass.getJavaClass())) {
            Constructor<T> c = Reflections.cast(constructor);
            WeldConstructor<T> annotatedConstructor = WeldConstructorImpl.of(c, weldClass, classTransformer);
            constructorsTemp.add(annotatedConstructor);
            constructorsBySignatureTemp.put(annotatedConstructor.getSignature(), annotatedConstructor);
        }
        this.constructors = Collections.unmodifiableSet(constructorsTemp.trimToSize());
        this.constructorsBySignature = Collections.unmodifiableMap(constructorsBySignatureTemp);
    }

    private WeldConstructors(WeldClass<T> weldClass, AnnotatedType<T> annotatedType, ClassTransformer classTransformer) {
        ArraySet<WeldConstructor<T>> constructorsTemp = new ArraySet<WeldConstructor<T>>();
        Map<ConstructorSignature, WeldConstructor<T>> constructorsBySignatureTemp = new HashMap<ConstructorSignature, WeldConstructor<T>>();

        for (AnnotatedConstructor<T> constructor : annotatedType.getConstructors()) {
            WeldConstructor<T> weldConstructor = WeldConstructorImpl.of(constructor, weldClass, classTransformer);
            constructorsTemp.add(weldConstructor);
            constructorsBySignatureTemp.put(weldConstructor.getSignature(), weldConstructor);
        }
        this.constructors = Collections.unmodifiableSet(constructorsTemp.trimToSize());
        this.constructorsBySignature = Collections.unmodifiableMap(constructorsBySignatureTemp);
    }

    public WeldConstructor<T> getWeldConstructor(ConstructorSignature signature) {
        return constructorsBySignature.get(signature);
    }

    public Set<WeldConstructor<T>> getWeldConstructors() {
        return constructors;
    }

    /**
     * Gets constructors with given annotation type
     *
     * @param annotationType The annotation type to match
     * @return A set of abstracted constructors with given annotation type. If
     *         the constructors set is empty, initialize it first. Returns an
     *         empty set if there are no matches.
     * @see org.jboss.weld.introspector.WeldClass#getWeldConstructors(Class)
     */
    public Collection<WeldConstructor<T>> getWeldConstructors(Class<? extends Annotation> annotationType) {
        Set<WeldConstructor<T>> ret = new HashSet<WeldConstructor<T>>();
        for (WeldConstructor<T> constructor : constructors) {
            if (constructor.isAnnotationPresent(annotationType)) {
                ret.add(constructor);
            }
        }
        return ret;
    }

    public WeldConstructor<T> getNoArgsWeldConstructor() {
        for (WeldConstructor<T> constructor : constructors) {
            if (constructor.getJavaMember().getParameterTypes().length == 0) {
                return constructor;
            }
        }
        return null;
    }

    private static class LazyConstructorsHolder<T> extends LazyValueHolder<WeldConstructors<T>> {

        protected final WeldClass<T> weldClass;
        protected final ClassTransformer classTransformer;

        public LazyConstructorsHolder(WeldClass<T> weldClass, ClassTransformer classTransformer) {
            this.weldClass = weldClass;
            this.classTransformer = classTransformer;
        }

        @Override
        protected WeldConstructors<T> computeValue() {
            return new WeldConstructors<T>(weldClass, classTransformer);
        }
    }

    private static class LazyExternalConstructorsHolder<T> extends LazyConstructorsHolder<T> {
        private final AnnotatedType<T> annotatedType;

        public LazyExternalConstructorsHolder(WeldClass<T> weldClass, AnnotatedType<T> annotatedType, ClassTransformer classTransformer) {
            super(weldClass, classTransformer);
            this.annotatedType = annotatedType;
        }

        @Override
        protected WeldConstructors<T> computeValue() {
            return new WeldConstructors<T>(weldClass, annotatedType, classTransformer);
        }
    }

    public static <T> LazyValueHolder<WeldConstructors<T>> of(WeldClass<T> weldClass, ClassTransformer classTransformer) {
        return new LazyConstructorsHolder<T>(weldClass, classTransformer);
    }

    public static <T> LazyValueHolder<WeldConstructors<T>> of(WeldClass<T> weldClass, AnnotatedType<T> annotatedType, ClassTransformer classTransformer) {
        return new LazyExternalConstructorsHolder<T>(weldClass, annotatedType, classTransformer);
    }
}
