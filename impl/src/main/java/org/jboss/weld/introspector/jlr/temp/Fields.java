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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;

import org.jboss.weld.introspector.WeldClass;
import org.jboss.weld.introspector.WeldField;
import org.jboss.weld.introspector.jlr.WeldFieldImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.LazyValueHolder;
import org.jboss.weld.util.collections.ArraySet;
import org.jboss.weld.util.reflection.Reflections;
import org.jboss.weld.util.reflection.SecureReflections;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

public class Fields<T> {

    private static final Fields<Object> NO_FIELDS = new Fields<Object>();
    private static final LazyValueHolder<Fields<Object>> NO_FIELDS_HOLDER = new LazyValueHolder<Fields<Object>>() {
        @Override
        protected Fields<Object> computeValue() {
            return NO_FIELDS;
        }
    };

    public static <T> LazyValueHolder<Fields<T>> noFieldsHolder() {
        return Reflections.cast(NO_FIELDS_HOLDER);
    }

    // // The set of abstracted fields
    private final Set<WeldField<?, ? super T>> fields;
    // // The map from annotation type to abstracted field with annotation
    private final Multimap<Class<? extends Annotation>, WeldField<?, ? super T>> annotatedFields;

    // The set of abstracted fields
    private final Set<WeldField<?, ? super T>> declaredFields;
    // The map from annotation type to abstracted field with annotation
    private final Multimap<Class<? extends Annotation>, WeldField<?, ? super T>> declaredAnnotatedFields;

    private final WeldClass<T> weldClass;

    private Fields() {
        this.fields = Collections.emptySet();
        this.declaredFields = fields;
        ArrayListMultimap<Class<? extends Annotation>, WeldField<?, ? super T>> annotatedFieldsTemp = ArrayListMultimap.create(0, 0);
        annotatedFieldsTemp.trimToSize();
        this.annotatedFields = Multimaps.unmodifiableMultimap(annotatedFieldsTemp);
        this.declaredAnnotatedFields = annotatedFields;
        this.weldClass = null;
    }

    protected Fields(WeldClass<T> weldClass, ClassTransformer classTransformer) {
        this.weldClass = weldClass;
        ArraySet<WeldField<?, ? super T>> declaredFieldsTemp = new ArraySet<WeldField<?, ? super T>>();
        ArrayListMultimap<Class<? extends Annotation>, WeldField<?, ? super T>> declaredAnnotatedFieldsTemp = ArrayListMultimap.create();

        for (Field field : SecureReflections.getDeclaredFields(weldClass.getJavaClass())) {
            WeldField<?, T> annotatedField = WeldFieldImpl.of(field, weldClass, classTransformer);
            declaredFieldsTemp.add(annotatedField);
            for (Annotation annotation : annotatedField.getAnnotations()) {
                declaredAnnotatedFieldsTemp.put(annotation.annotationType(), annotatedField);
            }
        }
        this.declaredFields = Collections.unmodifiableSet(declaredFieldsTemp.trimToSize());
        declaredAnnotatedFieldsTemp.trimToSize();
        this.declaredAnnotatedFields = Multimaps.unmodifiableMultimap(declaredAnnotatedFieldsTemp);

        WeldClass<? super T> superclass = weldClass.getWeldSuperclass();
        Set<WeldField<?, ? super T>> superclassFields = Reflections.<Set<WeldField<?, ? super T>>> cast(superclass.getWeldFields());
        if ((superclass == null) || (superclass.getJavaClass() == Object.class) || superclassFields.isEmpty()) {
            this.fields = declaredFields;
            this.annotatedFields = declaredAnnotatedFields;
        } else {
            this.fields = Sets.union(declaredFields, superclassFields);
            this.annotatedFields = null;
        }
    }

    protected Fields(WeldClass<T> weldClass, AnnotatedType<T> annotatedType, ClassTransformer classTransformer) {
        this.weldClass = weldClass;
        ArraySet<WeldField<?, ? super T>> fieldsTemp = new ArraySet<WeldField<?, ? super T>>();
        ArraySet<WeldField<?, ? super T>> declaredFieldsTemp = new ArraySet<WeldField<?, ? super T>>();
        ArrayListMultimap<Class<? extends Annotation>, WeldField<?, ? super T>> annotatedFieldsTemp = ArrayListMultimap.create();
        ArrayListMultimap<Class<? extends Annotation>, WeldField<?, ? super T>> declaredAnnotatedFieldsTemp = ArrayListMultimap.create();

        for (AnnotatedField<? super T> annotatedField : annotatedType.getFields()) {
            WeldField<?, ? super T> weldField = WeldFieldImpl.of(annotatedField, weldClass, classTransformer);
            fieldsTemp.add(weldField);
            boolean declared = annotatedField.getDeclaringType().getJavaClass() == weldClass.getJavaClass();
            if (declared) {
                declaredFieldsTemp.add(weldField);
            }
            for (Annotation annotation : weldField.getAnnotations()) {
                annotatedFieldsTemp.put(annotation.annotationType(), weldField);
                if (declared) {
                    declaredAnnotatedFieldsTemp.put(annotation.annotationType(), weldField);
                }
            }
        }
        // TODO we could check if fields.equals(declaredFields) and if so, drop one of the collections
        // TODO even better, we should not even build the second structure unless really needed
        this.fields = Collections.unmodifiableSet(fieldsTemp.trimToSize());
        this.declaredFields = Collections.unmodifiableSet(declaredFieldsTemp.trimToSize());
        annotatedFieldsTemp.trimToSize();
        declaredAnnotatedFieldsTemp.trimToSize();
        this.annotatedFields = Multimaps.unmodifiableMultimap(annotatedFieldsTemp);
        this.declaredAnnotatedFields = Multimaps.unmodifiableMultimap(declaredAnnotatedFieldsTemp);
    }

    public Set<WeldField<?, ? super T>> getFields() {
        return fields;
    }

    public Set<WeldField<?, ? super T>> getDeclaredFields() {
        return declaredFields;
    }

    public Collection<WeldField<?, ? super T>> getWeldFields(Class<? extends Annotation> annotationType) {
        if (annotatedFields == null) {
            ArrayList<WeldField<?, ? super T>> aggregatedFields = new ArrayList<WeldField<?, ? super T>>(getDeclaredWeldFields(annotationType));
            aggregatedFields.addAll(weldClass.getWeldSuperclass().getWeldFields(annotationType));
            return Collections.unmodifiableList(aggregatedFields);
        } else {
            return annotatedFields.get(annotationType);
        }
    }

    public Collection<WeldField<?, ? super T>> getDeclaredWeldFields(Class<? extends Annotation> annotationType) {
        return declaredAnnotatedFields.get(annotationType);
    }

    private static class LazyFieldsHolder<T> extends LazyValueHolder<Fields<T>> {

        protected final WeldClass<T> weldClass;
        protected final ClassTransformer classTransformer;

        public LazyFieldsHolder(WeldClass<T> weldClass, ClassTransformer classTransformer) {
            this.weldClass = weldClass;
            this.classTransformer = classTransformer;
        }

        @Override
        protected Fields<T> computeValue() {
            return new Fields<T>(weldClass, classTransformer);
        }
    }

    private static class LazyExternalFieldsHolder<T> extends LazyFieldsHolder<T> {
        private final AnnotatedType<T> annotatedType;

        public LazyExternalFieldsHolder(WeldClass<T> weldClass, AnnotatedType<T> annotatedType, ClassTransformer classTransformer) {
            super(weldClass, classTransformer);
            this.annotatedType = annotatedType;
        }

        @Override
        protected Fields<T> computeValue() {
            return new Fields<T>(weldClass, annotatedType, classTransformer);
        }
    }

    public static <T> LazyValueHolder<Fields<T>> of(WeldClass<T> weldClass, ClassTransformer classTransformer) {
        if (Object.class.equals(weldClass.getJavaClass()) || (weldClass.getWeldSuperclass() == null && SecureReflections.getDeclaredFields(weldClass.getJavaClass()).length == 0)) {
            return noFieldsHolder();
        }
        return new LazyFieldsHolder<T>(weldClass, classTransformer);
    }

    public static <T> LazyValueHolder<Fields<T>> of(WeldClass<T> weldClass, AnnotatedType<T> annotatedType, ClassTransformer classTransformer) {
        if (annotatedType.getFields().isEmpty()) {
            return noFieldsHolder();
        }
        return new LazyExternalFieldsHolder<T>(weldClass, annotatedType, classTransformer);
    }
}
