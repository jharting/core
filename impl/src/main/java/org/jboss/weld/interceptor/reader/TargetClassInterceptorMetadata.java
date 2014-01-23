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
package org.jboss.weld.interceptor.reader;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.jboss.weld.interceptor.spi.metadata.InterceptorFactory;
import org.jboss.weld.interceptor.spi.metadata.MethodMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;

public class TargetClassInterceptorMetadata<T> extends AbstractInterceptorMetadata<T> {

    public static <T> TargetClassInterceptorMetadata<T> of(Class<T> javaClass, Map<InterceptionType, List<MethodMetadata>> interceptorMethodMap) {
        return new TargetClassInterceptorMetadata<T>(javaClass, interceptorMethodMap);
    }

    private final Class<T> javaClass;

    public TargetClassInterceptorMetadata(Class<T> javaClass, Map<InterceptionType, List<MethodMetadata>> interceptorMethodMap) {
        super(interceptorMethodMap);
        this.javaClass = javaClass;
    }

    @Override
    public InterceptorFactory<T> getInterceptorFactory() {
        return null;
    }

    @Override
    protected boolean isTargetClassInterceptor() {
        return true;
    }

    @Override
    public Class<T> getJavaClass() {
        return javaClass;
    }

    // TODO: fixme!!!
    public boolean isInterceptorMethod(Method method) {
        for (List<MethodMetadata> list : interceptorMethodMap.values()) {
            for (MethodMetadata methodMetadata : list) {
                if (methodMetadata.getJavaMethod().equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }
}
