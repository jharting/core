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
package org.jboss.weld.introspector.jlr;

import static org.jboss.weld.util.reflection.Reflections.EMPTY_ANNOTATIONS;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Qualifier;

import org.jboss.weld.introspector.WeldAnnotated;
import org.jboss.weld.introspector.jlr.temp.Annotations;
import org.jboss.weld.literal.DefaultLiteral;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.LazyValueHolder;
import org.jboss.weld.util.collections.ArraySet;
import org.jboss.weld.util.collections.Arrays2;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Represents functionality common for all annotated items, mainly different
 * mappings of the annotations and meta-annotations
 * <p/>
 * AbstractAnnotatedItem is an immutable class and therefore threadsafe
 *
 * @param <T>
 * @param <S>
 * @author Pete Muir
 * @author Nicklas Karlsson
 * @see org.jboss.weld.introspector.WeldAnnotated
 */
public abstract class AbstractWeldAnnotated<T, S> implements WeldAnnotated<T, S> {

    // The set of default binding types
    private static final Set<Annotation> DEFAULT_QUALIFIERS = Collections.<Annotation>singleton(DefaultLiteral.INSTANCE);

    private final LazyValueHolder<Annotations> annotations;

    private final Class<T> rawType;
    private final Type[] actualTypeArguments;
    private final Type type;
    private final LazyValueHolder<Set<Type>> typeClosure;

    /**
     * Constructor
     * <p/>
     * Also builds the meta-annotation map. Throws a NullPointerException if
     * trying to register a null map
     *
     * @param annotationMap A map of annotation to register
     */
    public AbstractWeldAnnotated(LazyValueHolder<Annotations> annotations, Class<T> rawType, Type type, final LazyValueHolder<Set<Type>> typeClosure) {
        // TODO check for annotations
//        if (annotationMap == null) {
//            throw new WeldException(ANNOTATION_MAP_NULL);
//        }
        this.annotations = annotations;

//        if (declaredAnnotationMap == null) {
//            throw new WeldException(DECLARED_ANNOTATION_MAP_NULL);
//        }
        this.rawType = rawType;
        this.type = type;
        if (type instanceof ParameterizedType) {
            this.actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
        } else {
            this.actualTypeArguments = new Type[0];
        }
        this.typeClosure = typeClosure;
    }

    public Class<T> getJavaClass() {
        return rawType;
    }

    public Type[] getActualTypeArguments() {
        return Arrays2.copyOf(actualTypeArguments, actualTypeArguments.length);
    }

    public Set<Type> getInterfaceClosure() {
        Set<Type> interfaces = new HashSet<Type>();
        for (Type t : getTypeClosure()) {
            if (Reflections.getRawType(t).isInterface()) {
                interfaces.add(t);
            }
        }
        return Collections.unmodifiableSet(interfaces);
    }

    public abstract S getDelegate();

    public boolean isParameterizedType() {
        return rawType.getTypeParameters().length > 0;
    }

    public boolean isPrimitive() {
        return getJavaClass().isPrimitive();
    }

    public Type getBaseType() {
        return type;
    }

    public Set<Type> getTypeClosure() {
        return typeClosure.get();
    }

    public Set<Annotation> getAnnotations() {
        // TODO cache instances
        return Collections.unmodifiableSet(new ArraySet<Annotation>(annotations.get().getAnnotationMap().values()));
    }

    public Set<Annotation> getMetaAnnotations(Class<? extends Annotation> metaAnnotationType) {
        return Collections.unmodifiableSet(new ArraySet<Annotation>(annotations.get().getMetaAnnotationMap().get(metaAnnotationType)));
    }

    @Deprecated
    public Set<Annotation> getQualifiers() {
        if (getMetaAnnotations(Qualifier.class).size() > 0) {
            return Collections.unmodifiableSet(getMetaAnnotations(Qualifier.class));
        } else {
            return Collections.unmodifiableSet(DEFAULT_QUALIFIERS);
        }
    }

    @Deprecated
    public Annotation[] getBindingsAsArray() {
        return getQualifiers().toArray(EMPTY_ANNOTATIONS);
    }


    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return annotationType.cast(annotations.get().getAnnotationMap().get(annotationType));
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return annotations.get().getAnnotationMap().containsKey(annotationType);
    }

    Map<Class<? extends Annotation>, Annotation> getAnnotationMap() {
        return annotations.get().getAnnotationMap();
    }

}
