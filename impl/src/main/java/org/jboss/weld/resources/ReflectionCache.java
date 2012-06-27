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

import org.jboss.weld.Container;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.manager.BeanManagerImpl;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

public class ReflectionCache implements Service {

    private static class AnnotationsForAnnotatedElement implements Function<AnnotatedElement, Annotation[]> {
        @Override
        public Annotation[] apply(AnnotatedElement element) {
            return element.getAnnotations();
        }
    }

    private static class DeclaredAnnotationsForAnnotatedElement implements Function<AnnotatedElement, Annotation[]> {
        @Override
        public Annotation[] apply(AnnotatedElement element) {
            return element.getDeclaredAnnotations();
        }
    }

    public static ReflectionCache instance() {
        return Container.instance().services().get(ReflectionCache.class);
    }

    public static ReflectionCache instance(BeanManagerImpl manager) {
        return manager.getServices().get(ReflectionCache.class);
    }

    private final Map<AnnotatedElement, Annotation[]> annotations;
    private final Map<AnnotatedElement, Annotation[]> declaredAnnotations;

    public ReflectionCache() {
        MapMaker maker = new MapMaker();
        this.annotations = maker.makeComputingMap(new AnnotationsForAnnotatedElement());
        this.declaredAnnotations = maker.makeComputingMap(new DeclaredAnnotationsForAnnotatedElement());
    }

    public Annotation[] getAnnotations(AnnotatedElement element) {
        return annotations.get(element);
    }

    public Annotation[] getDeclaredAnnotations(AnnotatedElement element) {
        return declaredAnnotations.get(element);
    }

    @Override
    public void cleanup() {
        annotations.clear();
        declaredAnnotations.clear();
    }
}
