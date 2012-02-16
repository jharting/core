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

import static org.jboss.weld.introspector.WeldAnnotated.MAPPED_DECLARED_METAANNOTATIONS;
import static org.jboss.weld.introspector.WeldAnnotated.MAPPED_METAANNOTATIONS;
import static org.jboss.weld.logging.messages.ReflectionMessage.ANNOTATION_MAP_NULL;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.metadata.TypeStore;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.SharedObjectFacade;
import org.jboss.weld.util.LazyValueHolder;
import org.jboss.weld.util.collections.ArraySet;
import org.jboss.weld.util.collections.ArraySetMultimap;

import com.google.common.base.Function;

public class Annotations {

    /**
     * Builds the annotation map (annotation type -> annotation)
     *
     * @param annotations The array of annotations to map
     * @return The annotation map
     */
    protected static Map<Class<? extends Annotation>, Annotation> buildAnnotationMap(Annotation[] annotations) {
        Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
        for (Annotation annotation : annotations) {
            annotationMap.put(annotation.annotationType(), annotation);
        }
        return annotationMap;
    }

    /**
     * Builds the annotation map (annotation type -> annotation)
     *
     * @param annotations The array of annotations to map
     * @return The annotation map
     */
    protected static Map<Class<? extends Annotation>, Annotation> buildAnnotationMap(Iterable<Annotation> annotations) {
        Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<Class<? extends Annotation>, Annotation>();
        for (Annotation annotation : annotations) {
            annotationMap.put(annotation.annotationType(), annotation);
        }
        return annotationMap;
    }

    protected static void addMetaAnnotations(ArraySetMultimap<Class<? extends Annotation>, Annotation> metaAnnotationMap, Annotation annotation, Annotation[] metaAnnotations,
            boolean declared) {
        for (Annotation metaAnnotation : metaAnnotations) {
            addMetaAnnotation(metaAnnotationMap, annotation, metaAnnotation.annotationType(), declared);
        }
    }

    protected static void addMetaAnnotations(ArraySetMultimap<Class<? extends Annotation>, Annotation> metaAnnotationMap, Annotation annotation,
            Iterable<Annotation> metaAnnotations, boolean declared) {
        for (Annotation metaAnnotation : metaAnnotations) {
            addMetaAnnotation(metaAnnotationMap, annotation, metaAnnotation.annotationType(), declared);
        }
    }

    private static void addMetaAnnotation(ArraySetMultimap<Class<? extends Annotation>, Annotation> metaAnnotationMap, Annotation annotation,
            Class<? extends Annotation> metaAnnotationType, boolean declared) {
        // Only map meta-annotations we are interested in
        if (declared ? MAPPED_DECLARED_METAANNOTATIONS.contains(metaAnnotationType) : MAPPED_METAANNOTATIONS.contains(metaAnnotationType)) {
            metaAnnotationMap.putSingleElement(metaAnnotationType, annotation);
        }
    }

    // The annotation map (annotation type -> annotation) of the item
    private final Map<Class<? extends Annotation>, Annotation> annotationMap;
    // The meta-annotation map (annotation type -> set of annotations containing
    // meta-annotation) of the item
    private final ArraySetMultimap<Class<? extends Annotation>, Annotation> metaAnnotationMap;

    public Annotations(Set<Annotation> annotations, boolean declared, TypeStore typeStore) {
        this(buildAnnotationMap(annotations), declared, typeStore);
    }

    public Annotations(Annotation[] annotations, boolean declared, TypeStore typeStore) {
        this(buildAnnotationMap(annotations), declared, typeStore);
    }

    protected Annotations(Map<Class<? extends Annotation>, Annotation> annotationMap, boolean declared, TypeStore typeStore) {
        if (annotationMap == null) {
            throw new WeldException(ANNOTATION_MAP_NULL);
        }
        this.annotationMap = SharedObjectFacade.wrap(annotationMap);
        ArraySetMultimap<Class<? extends Annotation>, Annotation> metaAnnotationMap = new ArraySetMultimap<Class<? extends Annotation>, Annotation>();
        for (Annotation annotation : annotationMap.values()) {
            addMetaAnnotations(metaAnnotationMap, annotation, annotation.annotationType().getAnnotations(), declared);
            addMetaAnnotations(metaAnnotationMap, annotation, typeStore.get(annotation.annotationType()), declared);
        }
        metaAnnotationMap.trimToSize();
        this.metaAnnotationMap = SharedObjectFacade.wrap(metaAnnotationMap);
    }

    public Map<Class<? extends Annotation>, Annotation> getAnnotationMap() {
        return annotationMap;
    }

    public ArraySetMultimap<Class<? extends Annotation>, Annotation> getMetaAnnotationMap() {
        return metaAnnotationMap;
    }

    private abstract static class AbstractLazyAnnotationsHolder extends LazyValueHolder<Annotations> {
        protected final boolean declared;
        protected final TypeStore typeStore;

        public AbstractLazyAnnotationsHolder(boolean declared, TypeStore typeStore) {
            this.declared = declared;
            this.typeStore = typeStore;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (declared ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AbstractLazyAnnotationsHolder other = (AbstractLazyAnnotationsHolder) obj;
            if (declared != other.declared)
                return false;
            return true;
        }
    }

    private static class LazySetAnnotationsHolder extends AbstractLazyAnnotationsHolder {
        private final Set<Annotation> annotations;

        public LazySetAnnotationsHolder(Set<Annotation> annotations, boolean declared, TypeStore typeStore) {
            super(declared, typeStore);
            this.annotations = annotations;
        }

        @Override
        protected Annotations computeValue() {
            return new Annotations(annotations, declared, typeStore);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            LazySetAnnotationsHolder other = (LazySetAnnotationsHolder) obj;
            if (annotations == null) {
                if (other.annotations != null)
                    return false;
            } else if (!annotations.equals(other.annotations))
                return false;
            return true;
        }
    }

    private static class LazyArrayAnnotationsHolder extends AbstractLazyAnnotationsHolder {
        private final Annotation[] annotations;

        public LazyArrayAnnotationsHolder(Annotation[] annotations, boolean declared, TypeStore typeStore) {
            super(declared, typeStore);
            this.annotations = annotations;
        }

        @Override
        protected Annotations computeValue() {
            return new Annotations(annotations, declared, typeStore);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Arrays.hashCode(annotations);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            LazyArrayAnnotationsHolder other = (LazyArrayAnnotationsHolder) obj;
            if (!Arrays.equals(annotations, other.annotations))
                return false;
            return true;
        }
    }

    public static LazyValueHolder<Annotations> of(Set<Annotation> annotations, boolean declared, ClassTransformer classTransformer) {
        return SharedObjectFacade.wrap(new LazySetAnnotationsHolder(annotations, declared, classTransformer.getTypeStore()));
    }

    public static LazyValueHolder<Annotations> of(Annotation[] annotations, boolean declared, ClassTransformer classTransformer) {
        return SharedObjectFacade.wrap(new LazyArrayAnnotationsHolder(annotations, declared, classTransformer.getTypeStore()));
    }

    public static LazyValueHolder<Annotations> ofAnnotation(Class<? extends Annotation> annotationType, boolean declared, ClassTransformer classTransformer) {
        Annotation[] annotations = null;
        if (declared) {
            annotations = annotationType.getDeclaredAnnotations();
        } else {
            annotations = annotationType.getAnnotations();
        }
        Set<Annotation> extraAnnotations = classTransformer.getTypeStore().get(annotationType);
        if (extraAnnotations.isEmpty()) {
            return of(annotations, declared, classTransformer);
        } else {
            ArraySet<Annotation> mergedAnnotations = new ArraySet<Annotation>(Arrays.asList(annotations));
            mergedAnnotations.addAll(extraAnnotations);
            mergedAnnotations.trimToSize();
            return of(SharedObjectFacade.wrap(mergedAnnotations), declared, classTransformer);
        }
    }

    public static class AnnotationsHolderFunction implements Function<LazyValueHolder<Annotations>, LazyValueHolder<Annotations>> {
        @Override
        public LazyValueHolder<Annotations> apply(LazyValueHolder<Annotations> from) {
            return from;
        }
    }
}
