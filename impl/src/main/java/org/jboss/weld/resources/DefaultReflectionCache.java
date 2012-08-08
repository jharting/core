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
package org.jboss.weld.resources;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
import java.util.Set;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.util.collections.ImmutableIdentityArraySet;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

public class DefaultReflectionCache implements Service, ReflectionCache {

    protected Annotation[] internalGetAnnotations(AnnotatedElement element) {
        return element.getAnnotations();
    }

    protected Annotation[] internalGetDeclaredAnnotations(AnnotatedElement element) {
        return element.getDeclaredAnnotations();
    }

    private final Map<Annotation, Annotation> canonicalAnnotationsByIdentity;
    private final Map<Annotation, Annotation> canonicalAnnotations;

    private final Map<AnnotatedElement, Set<Annotation>> annotations;
    private final Map<AnnotatedElement, Set<Annotation>> declaredAnnotations;
    private final Map<Set<Annotation>, Set<Annotation>> sharedAnnotationSets;

    public DefaultReflectionCache() {
        MapMaker maker = new MapMaker();
        this.annotations = maker.makeComputingMap(new Function<AnnotatedElement, Set<Annotation>>() {
            @Override
            public Set<Annotation> apply(AnnotatedElement input) {
                return sharedAnnotationSets.get(ImmutableIdentityArraySet.of(internalGetAnnotations(input), DefaultReflectionCache.this));
            }
        });
        this.declaredAnnotations = maker.makeComputingMap(new Function<AnnotatedElement, Set<Annotation>>() {
            @Override
            public Set<Annotation> apply(AnnotatedElement input) {
                return sharedAnnotationSets.get(ImmutableIdentityArraySet.of(internalGetDeclaredAnnotations(input), DefaultReflectionCache.this));
            }
        });
        this.canonicalAnnotations = maker.makeComputingMap(new Function<Annotation, Annotation>() {
            @Override
            public Annotation apply(Annotation input) {
                return input;
            }
        });
        this.canonicalAnnotationsByIdentity = new MapMaker().weakKeys().makeComputingMap(new Function<Annotation, Annotation>() {
            @Override
            public Annotation apply(Annotation input) {
                return canonicalAnnotations.get(input);
            }
        });
        this.sharedAnnotationSets = maker.makeComputingMap(new Function<Set<Annotation>, Set<Annotation>>() {
            @Override
            public Set<Annotation> apply(Set<Annotation> input) {
                return input;
            }
        });
    }

    public Set<Annotation> getAnnotations(AnnotatedElement element) {
        return annotations.get(element);
    }

    public Set<Annotation> getDeclaredAnnotations(AnnotatedElement element) {
        return declaredAnnotations.get(element);
    }

    @Override
    public void cleanup() {
        annotations.clear();
        declaredAnnotations.clear();
    }

    @Override
    public Annotation getCanonicalAnnotationInstance(Annotation annotation) {
        return canonicalAnnotationsByIdentity.get(annotation);
    }

    @Override
    public Set<Annotation> getSharedAnnotationSet(Annotation[] annotations) {
        return sharedAnnotationSets.get(ImmutableIdentityArraySet.of(annotations, this));
    }

    @Override
    public Set<Annotation> getSharedAnnotationSet(Set<Annotation> annotations) {
        return sharedAnnotationSets.get(ImmutableIdentityArraySet.of(annotations, this));
    }
}
