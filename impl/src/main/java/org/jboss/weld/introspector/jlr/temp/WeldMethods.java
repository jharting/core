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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import org.jboss.weld.introspector.WeldClass;
import org.jboss.weld.introspector.WeldMethod;
import org.jboss.weld.introspector.jlr.WeldMethodImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.LazyValueHolder;
import org.jboss.weld.util.collections.ArraySet;
import org.jboss.weld.util.reflection.Reflections;
import org.jboss.weld.util.reflection.SecureReflections;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

public class WeldMethods<T> {

    private static final WeldMethods<Object> NO_METHODS = new WeldMethods<Object>();
    private static final LazyValueHolder<WeldMethods<Object>> NO_METHODS_HOLDER = new LazyValueHolder<WeldMethods<Object>>() {
        @Override
        protected WeldMethods<Object> computeValue() {
            return NO_METHODS;
        }
    };

    // The set of abstracted methods
    private final Set<WeldMethod<?, ? super T>> methods;
    // The map from annotation type to abstracted method with annotation
    private final Multimap<Class<? extends Annotation>, WeldMethod<?, ? super T>> annotatedMethods;

    // The set of abstracted methods
    private final Set<WeldMethod<?, ? super T>> declaredMethods;
    // The map from annotation type to abstracted method with annotation
    private final Multimap<Class<? extends Annotation>, WeldMethod<?, ? super T>> declaredAnnotatedMethods;
    // The map from annotation type to method with a parameter with annotation
    private final Multimap<Class<? extends Annotation>, WeldMethod<?, ? super T>> declaredMethodsByAnnotatedParameters;

    private final WeldClass<T> weldClass;

    private WeldMethods(WeldClass<T> weldClass, ClassTransformer classTransformer) {
        this.weldClass = weldClass;
        ArraySet<WeldMethod<?, ? super T>> declaredMethodsTemp = new ArraySet<WeldMethod<?, ? super T>>();
        ArrayListMultimap<Class<? extends Annotation>, WeldMethod<?, ? super T>> declaredAnnotatedMethodsTemp = ArrayListMultimap.create();
        ArrayListMultimap<Class<? extends Annotation>, WeldMethod<?, ? super T>> declaredMethodsByAnnotatedParametersTemp = ArrayListMultimap.create();

        for (Method method : SecureReflections.getDeclaredMethods(weldClass.getJavaClass())) {
            WeldMethod<?, T> weldMethod = WeldMethodImpl.of(method, weldClass, classTransformer);
            declaredMethodsTemp.add(weldMethod);
            for (Annotation annotation : weldMethod.getAnnotations()) {
                declaredAnnotatedMethodsTemp.put(annotation.annotationType(), weldMethod);
            }
            for (Class<? extends Annotation> annotationType : WeldMethod.MAPPED_PARAMETER_ANNOTATIONS) {
                if (weldMethod.getWeldParameters(annotationType).size() > 0) {
                    declaredMethodsByAnnotatedParametersTemp.put(annotationType, weldMethod);
                }
            }
        }

        this.declaredMethods = Collections.unmodifiableSet(declaredMethodsTemp.trimToSize());
        declaredAnnotatedMethodsTemp.trimToSize();
        this.declaredAnnotatedMethods = Multimaps.unmodifiableListMultimap(declaredAnnotatedMethodsTemp);
        declaredMethodsByAnnotatedParametersTemp.trimToSize();
        this.declaredMethodsByAnnotatedParameters = Multimaps.unmodifiableListMultimap(declaredMethodsByAnnotatedParametersTemp);

        if (weldClass.getWeldSuperclass() == null || weldClass.getWeldSuperclass().getWeldMethods().size() == 0) {
            this.methods = declaredMethods;
            this.annotatedMethods = declaredAnnotatedMethods;
        } else {
            // TODO
            Set<WeldMethod<?, ? super T>> superclassMethods = Reflections.cast(weldClass.getWeldSuperclass().getWeldMethods());
            methods = Sets.union(declaredMethods, superclassMethods);
            annotatedMethods = null;
        }
    }

    private WeldMethods(WeldClass<T> weldClass, AnnotatedType<T> annotatedType, ClassTransformer classTransformer) {
        this.weldClass = weldClass;
        ArraySet<WeldMethod<?, ? super T>> methodsTemp = new ArraySet<WeldMethod<?, ? super T>>();
        ArrayListMultimap<Class<? extends Annotation>, WeldMethod<?, ? super T>> annotatedMethodsTemp = ArrayListMultimap.create();
        ArraySet<WeldMethod<?, ? super T>> declaredMethodsTemp = new ArraySet<WeldMethod<?, ? super T>>();
        ArrayListMultimap<Class<? extends Annotation>, WeldMethod<?, ? super T>> declaredAnnotatedMethodsTemp = ArrayListMultimap.create();
        ArrayListMultimap<Class<? extends Annotation>, WeldMethod<?, ? super T>> declaredMethodsByAnnotatedParametersTemp = ArrayListMultimap.create();

        for (AnnotatedMethod<? super T> method : annotatedType.getMethods()) {
            WeldMethod<?, ? super T> weldMethod = WeldMethodImpl.of(method, weldClass, classTransformer);
            methodsTemp.add(weldMethod);
            boolean declared = method.getDeclaringType().getJavaClass() == weldClass.getJavaClass();
            if (declared) {
                declaredMethodsTemp.add(weldMethod);
            }
            for (Annotation annotation : weldMethod.getAnnotations()) {
                annotatedMethodsTemp.put(annotation.annotationType(), weldMethod);
                if (declared) {
                    declaredAnnotatedMethodsTemp.put(annotation.annotationType(), weldMethod);
                }
            }
            for (Class<? extends Annotation> annotationType : WeldMethod.MAPPED_PARAMETER_ANNOTATIONS) {
                if (weldMethod.getWeldParameters(annotationType).size() > 0) {
                    if (declared) {
                        declaredMethodsByAnnotatedParametersTemp.put(annotationType, weldMethod);
                    }
                }
            }
        }
        this.methods = Collections.unmodifiableSet(methodsTemp.trimToSize());
        this.declaredMethods = Collections.unmodifiableSet(declaredMethodsTemp.trimToSize());
        annotatedMethodsTemp.trimToSize();
        declaredAnnotatedMethodsTemp.trimToSize();
        declaredMethodsByAnnotatedParametersTemp.trimToSize();
        this.annotatedMethods = Multimaps.unmodifiableListMultimap(annotatedMethodsTemp);
        this.declaredAnnotatedMethods = Multimaps.unmodifiableListMultimap(declaredAnnotatedMethodsTemp);
        this.declaredMethodsByAnnotatedParameters = Multimaps.unmodifiableListMultimap(declaredMethodsByAnnotatedParametersTemp);
    }

    private WeldMethods() {
        this.weldClass = null;
        this.methods = Collections.emptySet();
        this.declaredMethods = this.methods;
        this.annotatedMethods = Multimaps.unmodifiableListMultimap(ArrayListMultimap.<Class<? extends Annotation>, WeldMethod<?, ? super T>> create());
        this.declaredAnnotatedMethods = annotatedMethods;
        this.declaredMethodsByAnnotatedParameters = annotatedMethods;
    }

    public Collection<WeldMethod<?, ? super T>> getDeclaredWeldMethods(Class<? extends Annotation> annotationType) {
        return declaredAnnotatedMethods.get(annotationType);
    }

    public Collection<WeldMethod<?, ? super T>> getDeclaredWeldMethodsWithAnnotatedParameters(Class<? extends Annotation> annotationType) {
        return declaredMethodsByAnnotatedParameters.get(annotationType);
    }

    public Collection<WeldMethod<?, ? super T>> getWeldMethods() {
        return methods;
    }

    public Collection<WeldMethod<?, ? super T>> getDeclaredWeldMethods() {
        return declaredMethods;
    }

    public Collection<WeldMethod<?, ? super T>> getWeldMethods(Class<? extends Annotation> annotationType) {
        if (annotatedMethods == null) {
            ArrayList<WeldMethod<?, ? super T>> aggregateMethods = new ArrayList<WeldMethod<?, ? super T>>(declaredAnnotatedMethods.get(annotationType));
            aggregateMethods.addAll(weldClass.getWeldSuperclass().getWeldMethods(annotationType));
            return Collections.unmodifiableCollection(aggregateMethods);
        } else {
            return annotatedMethods.get(annotationType);
        }
    }

    private static class LazyMethodsHolder<T> extends LazyValueHolder<WeldMethods<T>> {

        protected final WeldClass<T> weldClass;
        protected final ClassTransformer classTransformer;

        public LazyMethodsHolder(WeldClass<T> weldClass, ClassTransformer classTransformer) {
            this.weldClass = weldClass;
            this.classTransformer = classTransformer;
        }

        @Override
        protected WeldMethods<T> computeValue() {
            return new WeldMethods<T>(weldClass, classTransformer);
        }
    }

    private static class LazyExternalMethodsHolder<T> extends LazyMethodsHolder<T> {
        private final AnnotatedType<T> annotatedType;

        public LazyExternalMethodsHolder(WeldClass<T> weldClass, AnnotatedType<T> annotatedType, ClassTransformer classTransformer) {
            super(weldClass, classTransformer);
            this.annotatedType = annotatedType;
        }

        @Override
        protected WeldMethods<T> computeValue() {
            return new WeldMethods<T>(weldClass, annotatedType, classTransformer);
        }
    }

    public static <T> LazyValueHolder<WeldMethods<T>> of(WeldClass<T> weldClass, ClassTransformer classTransformer) {
        if (weldClass.getJavaClass().equals(Object.class)) {
            return Reflections.cast(NO_METHODS_HOLDER);
        }
        return new LazyMethodsHolder<T>(weldClass, classTransformer);
    }

    public static <T> LazyValueHolder<WeldMethods<T>> of(WeldClass<T> weldClass, AnnotatedType<T> annotatedType, ClassTransformer classTransformer) {
        if (weldClass.getJavaClass().equals(Object.class)) {
            return Reflections.cast(NO_METHODS_HOLDER);
        }
        return new LazyExternalMethodsHolder<T>(weldClass, annotatedType, classTransformer);
    }
}
