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
package org.jboss.weld.util;

import java.lang.reflect.Method;
import java.util.Set;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.interceptor.spi.metadata.MethodMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;
import org.jboss.weld.interceptor.util.InterceptionTypeRegistry;

import com.google.common.collect.ImmutableSet;

class InterceptorMethod implements MethodMetadata {

    private final Set<InterceptionType> supportedInterceptorTypes;
    private final Method method;

    public InterceptorMethod(EnhancedAnnotatedMethod<?, ?> annotatedMethod) {
        this.method = annotatedMethod.getJavaMember();
        ImmutableSet.Builder<InterceptionType> builder = ImmutableSet.builder();
        for (InterceptionType interceptionType : InterceptionTypeRegistry.getSupportedInterceptionTypes()) {
            if (annotatedMethod.isAnnotationPresent(InterceptionTypeRegistry.getAnnotationClass(interceptionType))) {
                builder.add(interceptionType);
            }
        }
        this.supportedInterceptorTypes = builder.build();
        if (supportedInterceptorTypes.isEmpty()) {
            throw new IllegalStateException("Interceptor method does not support interception of any kind " + annotatedMethod);
        }

        method.setAccessible(true); // TODO: fix me
    }

    @Override
    public Method getJavaMethod() {
        return method;
    }

    @Override
    public Set<InterceptionType> getSupportedInterceptionTypes() {
        return supportedInterceptorTypes;
    }
}
