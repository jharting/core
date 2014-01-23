/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.weld.interceptor.proxy.InterceptorInvocation;
import org.jboss.weld.interceptor.proxy.SimpleInterceptorInvocation;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.metadata.MethodMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;


/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 * @author Jozef Hartinger
 */
public abstract class AbstractInterceptorMetadata<T> implements InterceptorMetadata<T> {

    protected final Map<InterceptionType, List<MethodMetadata>> interceptorMethodMap;

    private final ClassMetadata<?> classMetadata;

    public AbstractInterceptorMetadata(ClassMetadata<?> classMetadata, Map<InterceptionType, List<MethodMetadata>> interceptorMethodMap) {
        this.classMetadata = classMetadata;
        this.interceptorMethodMap = interceptorMethodMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassMetadata<?> getInterceptorClass() {
        return classMetadata;
    }

    public List<MethodMetadata> getInterceptorMethods(InterceptionType interceptionType) {
        if (interceptorMethodMap != null) {
            List<MethodMetadata> methods = interceptorMethodMap.get(interceptionType);
            return methods == null ? Collections.<MethodMetadata>emptyList() : methods;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEligible(InterceptionType interceptionType) {
        if (this.interceptorMethodMap == null) {
            return false;
        }
        List<MethodMetadata> interceptorMethods = this.interceptorMethodMap.get(interceptionType);
        // return true if there are any interceptor methods for this interception type
        return (interceptorMethods != null && !(interceptorMethods.isEmpty()));
    }

    @Override
    public InterceptorInvocation getInterceptorInvocation(Object interceptorInstance, InterceptionType interceptionType) {
        return new SimpleInterceptorInvocation(interceptorInstance, interceptionType, getInterceptorMethods(interceptionType), isTargetClassInterceptor());
    }

    protected abstract boolean isTargetClassInterceptor();
}
