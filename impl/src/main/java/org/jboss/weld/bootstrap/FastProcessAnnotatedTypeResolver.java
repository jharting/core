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
package org.jboss.weld.bootstrap;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.jboss.weld.event.ExtensionObserverMethodImpl;
import org.jboss.weld.resolution.TypeSafeObserverResolver;
import org.jboss.weld.resources.spi.ClassFileInfo;
import org.jboss.weld.resources.spi.ClassFileServices;
import org.jboss.weld.util.Types;
import org.jboss.weld.util.reflection.Reflections;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * ProcessAnnotatedType observer method resolver. It uses {@link ClassFileServices} for resolution and thus entirely avoids loading the classes which speeds up
 * especially large deployments.
 *
 * Although this resolver covers most of the possible PAT observer method types, there are several cases when {@link ClassFileInfo} used by this resolver is not
 * sufficient to perform observer method resolution correctly. If such observer method is present in the deployment, the constructor of this class throws
 * {@link UnsupportedObserverMethodException}. This exception is expected to be caught by the deployer and observer method resolution using the default
 * {@link TypeSafeObserverResolver} is performed instead.
 *
 * @author Jozef Hartinger
 *
 */
public class FastProcessAnnotatedTypeResolver {

    private static class ExactTypePredicate implements Predicate<ClassFileInfo> {
        private final Class<?> type;

        public ExactTypePredicate(Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean apply(ClassFileInfo input) {
            return type.getName().equals(input.getClassName());
        }
    }

    private static class AssignableToPredicate implements Predicate<ClassFileInfo> {

        private final Class<?> type;

        public AssignableToPredicate(Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean apply(ClassFileInfo input) {
            return input.isAssignableTo(type);
        }
    }

    @SuppressWarnings("unchecked")
    private static class CompositePredicate implements Predicate<ClassFileInfo> {

        private static CompositePredicate assignable(Class<?>[] classes) {
            Predicate<ClassFileInfo>[] predicates = new Predicate[classes.length];
            for (int i = 0; i < classes.length; i++) {
                predicates[i] = new AssignableToPredicate(classes[i]);
            }
            return new CompositePredicate(predicates);
        }

        private final Predicate<ClassFileInfo>[] predicates;

        public CompositePredicate(Predicate<ClassFileInfo>[] predicates) {
            this.predicates = predicates;
        }

        @Override
        public boolean apply(ClassFileInfo input) {
            for (Predicate<ClassFileInfo> predicate : predicates) {
                if (!predicate.apply(input)) {
                    return false;
                }
            }
            return true;
        }
    }

    private final ClassFileServices classFileInfoServices;
    private final Set<ExtensionObserverMethodImpl<?, ?>> catchAllObservers;
    private final Map<ExtensionObserverMethodImpl<?, ?>, Predicate<ClassFileInfo>> observers;

    public FastProcessAnnotatedTypeResolver(ClassFileServices classFileInfoServices, Iterable<ObserverMethod<?>> observers)
            throws UnsupportedObserverMethodException {
        this.classFileInfoServices = classFileInfoServices;
        this.catchAllObservers = Sets.newHashSet();
        this.observers = new LinkedHashMap<ExtensionObserverMethodImpl<?, ?>, Predicate<ClassFileInfo>>();
        for (ObserverMethod<?> o : observers) {
            if (o instanceof ExtensionObserverMethodImpl<?, ?>) {
                process((ExtensionObserverMethodImpl<?, ?>) o, o.getObservedType());
            }
        }
    }

    private void process(ExtensionObserverMethodImpl<?, ?> observer, Type observedType) throws UnsupportedObserverMethodException {
        if (Object.class.equals(observedType)) {
            // void observe(Object event)
            catchAllObservers.add(observer);
        } else if (ProcessAnnotatedType.class.equals(observedType)) {
            // void observe(ProcessAnnotatedType event)
            catchAllObservers.add(observer);
        } else if (observedType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) observedType;
            if (ProcessAnnotatedType.class.equals(type.getRawType())) {
                Type typeParameter = type.getActualTypeArguments()[0];
                if (typeParameter instanceof Class<?>) {
                    this.observers.put(observer, new ExactTypePredicate(Reflections.getRawType(typeParameter)));
                } else if (typeParameter instanceof ParameterizedType) {
                    // void observe(ProcessAnnotatedType<Iterable<?>> event)
                    ParameterizedType parameterizedType = (ParameterizedType) typeParameter;
                    for (Type t : parameterizedType.getActualTypeArguments()) {
                        if (!Reflections.isUnboundedTypeVariable(t) && !Reflections.isUnboundedWildcard(t)) {
                            return; // skip - this observer would never be invoked per spec
                        }
                        this.observers.put(observer, new ExactTypePredicate(Reflections.getRawType(typeParameter)));
                    }
                } else if (typeParameter instanceof WildcardType) {
                    // void observe(ProcessAnnotatedType<?> event)
                    WildcardType wildCard = (WildcardType) typeParameter;
                    checkBounds(observer, wildCard.getUpperBounds());
                    this.observers.put(observer, CompositePredicate.assignable(Types.getRawTypes(wildCard.getUpperBounds())));
                } else if (typeParameter instanceof TypeVariable<?>) {
                    // <T> void observe(ProcessAnnotatedType<T> event)
                    TypeVariable<?> variable = (TypeVariable<?>) typeParameter;
                    checkBounds(observer, variable.getBounds());
                    this.observers.put(observer, CompositePredicate.assignable(Types.getRawTypes(variable.getBounds())));
                }
            }
        } else if (observedType instanceof TypeVariable<?>) {
            defaultRules(observer, observedType);
        }
    }

    private void checkBounds(ExtensionObserverMethodImpl<?, ?> observer, Type[] bounds) throws UnsupportedObserverMethodException {
        for (Type type : bounds) {
            if (!(type instanceof Class<?>)) {
                throw new UnsupportedObserverMethodException(observer);
            }
        }
    }

    private void defaultRules(ExtensionObserverMethodImpl<?, ?> observer, Type observedType) throws UnsupportedObserverMethodException {
        if (ProcessAnnotatedType.class.equals(observedType)) {
            catchAllObservers.add(observer);
        } else if (observedType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) observedType;
            if (ProcessAnnotatedType.class.equals(parameterizedType.getRawType())) {
                Type argument = parameterizedType.getActualTypeArguments()[0];
                if (argument instanceof Class<?>) {
                    this.observers.put(observer, new AssignableToPredicate(Reflections.getRawType(argument)));
                } else {
                    throw new UnsupportedObserverMethodException(observer);
                }
            }
        } else if (observedType instanceof TypeVariable) {
            final TypeVariable<?> typeVariable = (TypeVariable<?>) observedType;
            if (Reflections.isUnboundedTypeVariable(observedType)) {
                // <T> void observe(T event)
                catchAllObservers.add(observer);
            } else {
                if (typeVariable.getBounds().length == 1) { // here we expect that a PAT impl only implements the PAT interface
                    defaultRules(observer, typeVariable.getBounds()[0]);
                }
            }
        }
    }

    public Set<ExtensionObserverMethodImpl<?, ?>> resolveProcessAnnotatedTypeObservers(String className) {
        Set<ExtensionObserverMethodImpl<?, ?>> result = new HashSet<ExtensionObserverMethodImpl<?, ?>>();
        result.addAll(catchAllObservers);

        ClassFileInfo classInfo = classFileInfoServices.getClassFileInfo(className);
        for (Map.Entry<ExtensionObserverMethodImpl<?, ?>, Predicate<ClassFileInfo>> entry : observers.entrySet()) {
            ExtensionObserverMethodImpl<?, ?> observer = entry.getKey();
            if (containsRequiredAnnotation(classInfo, observer) && entry.getValue().apply(classInfo)) {
                result.add(observer);
            }
        }
        return result;
    }

    private boolean containsRequiredAnnotation(ClassFileInfo classInfo, ExtensionObserverMethodImpl<?, ?> observer) {
        if (observer.getRequiredAnnotations().isEmpty()) {
            return true;
        }
        for (Class<? extends Annotation> annotation : observer.getRequiredAnnotations()) {
            if (classInfo.containsAnnotation(annotation)) {
                return true;
            }
        }
        return false;
    }
}
