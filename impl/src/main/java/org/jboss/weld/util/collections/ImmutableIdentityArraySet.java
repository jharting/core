/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.util.collections;

import static org.jboss.weld.util.reflection.Reflections.cast;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.jboss.weld.interceptor.util.ArrayIterator;
import org.jboss.weld.resources.ReflectionCache;

public class ImmutableIdentityArraySet<T> extends AbstractSet<T> implements Serializable {

    private static final long serialVersionUID = 7469940310665094245L;

    public static ImmutableIdentityArraySet<Annotation> of(Set<Annotation> annotations, ReflectionCache cache) {
        if (annotations instanceof ImmutableIdentityArraySet<?>) {
            return cast(annotations);
        }
        Annotation[] canonicalAnnotations = new Annotation[annotations.size()];
        int i = 0;
        for (Annotation annotation : annotations) {
            canonicalAnnotations[i++] = cache.getCanonicalAnnotationInstance(annotation);
        }
        return new ImmutableIdentityArraySet<Annotation>(canonicalAnnotations);
    }

    public static ImmutableIdentityArraySet<Annotation> of(Annotation[] annotations, ReflectionCache cache) {
        Annotation[] canonicalAnnotations = new Annotation[annotations.length];
        for (int i = 0; i < annotations.length; i++) {
            canonicalAnnotations[i] = cache.getCanonicalAnnotationInstance(annotations[i]);
        }
        return new ImmutableIdentityArraySet<Annotation>(canonicalAnnotations);
    }

    private final T[] values;
    private final int hashCode;

    private ImmutableIdentityArraySet(T[] values) {
        this.values = values;
        this.hashCode = Arrays.hashCode(values);
    }

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public boolean isEmpty() {
        return values.length == 0;
    }

    @Override
    public boolean contains(Object o) {
        for (T value : values) {
            if (value.equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator<T>(values);
    }

    @Override
    public Object[] toArray() {
        return values.clone();
    }

    @Override
    public boolean add(T e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ImmutableIdentityArraySet<?>) {
            if (this == o) {
                return true;
            }
            ImmutableIdentityArraySet<?> other = (ImmutableIdentityArraySet<?>) o;
            if (values.length != other.values.length) {
                return false;
            }
            for (T value : values) {
                boolean found = false;
                for (Object otherValue : other.values) {
                    if (value == otherValue) {
                        found = true;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        } else {
            return super.equals(o);
        }
    }
}
