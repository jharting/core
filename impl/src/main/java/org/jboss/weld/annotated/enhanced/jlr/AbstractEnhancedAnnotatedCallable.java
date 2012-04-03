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
package org.jboss.weld.annotated.enhanced.jlr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.Map;

import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedCallable;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedMember;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.logging.messages.ReflectionMessage;
import org.jboss.weld.resources.ClassTransformer;

/**
 * @author Pete Muir
 * @author Jozef Hartinger
 */
public abstract class AbstractEnhancedAnnotatedCallable<T, X, S extends Member> extends AbstractEnhancedAnnotatedMember<T, X, S> implements EnhancedAnnotatedCallable<T, X, S> {

    protected AbstractEnhancedAnnotatedCallable(AnnotatedCallable<X> annotatedCallable, Map<Class<? extends Annotation>, Annotation> annotationMap, Map<Class<? extends Annotation>, Annotation> declaredAnnotationMap, ClassTransformer classTransformer, EnhancedAnnotatedType<X> declaringType) {
        super(annotatedCallable, annotationMap, declaredAnnotationMap, classTransformer, declaringType);
    }

    protected static void validateParameterCount(AnnotatedCallable<?> callable) {
        if (callable instanceof BackedAnnotatedMember) {
            return; // do not validate backed implementation
        }
        Class<?>[] parameterTypes = null;
        if (callable instanceof AnnotatedConstructor<?>) {
            parameterTypes = AnnotatedConstructor.class.cast(callable).getJavaMember().getParameterTypes();
        } else {
            parameterTypes = AnnotatedMethod.class.cast(callable).getJavaMember().getParameterTypes();
        }
        if (callable.getParameters().size() != parameterTypes.length) {
            throw new DefinitionException(ReflectionMessage.INCORRECT_NUMBER_OF_ANNOTATED_PARAMETERS_METHOD, callable.getParameters().size(), callable, callable.getParameters(), Arrays.asList(parameterTypes));
        }
    }
}
