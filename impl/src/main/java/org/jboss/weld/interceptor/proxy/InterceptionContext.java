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
package org.jboss.weld.interceptor.proxy;

import static org.jboss.weld.util.collections.WeldCollections.immutableMap;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.spi.CreationalContext;

import org.jboss.weld.interceptor.reader.TargetClassInterceptorMetadata;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Holds interceptor metadata and interceptor instances throughout the lifecycle of the intercepted instance.
 *
 * @author Jozef Hartinger
 *
 */
public class InterceptionContext {

    private final TargetClassInterceptorMetadata<?> targetClassInterceptorMetadata;
    private final InterceptionModel<ClassMetadata<?>, ?> interceptionModel;
    private final Map<InterceptorMetadata<?>, Object> interceptorInstances;

    public InterceptionContext(TargetClassInterceptorMetadata<?> targetClassInterceptorMetadata,
            InterceptionModel<ClassMetadata<?>, ?> interceptionModel, CreationalContext<?> ctx, BeanManagerImpl manager) {
        this.targetClassInterceptorMetadata = targetClassInterceptorMetadata;
        this.interceptionModel = interceptionModel;
        this.interceptorInstances = initInterceptorInstanceMap(interceptionModel.getAllInterceptors(), ctx, manager);
    }

    private Map<InterceptorMetadata<?>, Object> initInterceptorInstanceMap(Iterable<? extends InterceptorMetadata<?>> interceptorMetadata, CreationalContext ctx, BeanManagerImpl manager) {
        Map<InterceptorMetadata<?>, Object> interceptorInstances = new HashMap<InterceptorMetadata<?>, Object>();
        for (InterceptorMetadata<?> interceptor : interceptorMetadata) {
            interceptorInstances.put(interceptor, interceptor.getInterceptorFactory().create(ctx, manager));
        }
        return immutableMap(interceptorInstances);
    }

    public TargetClassInterceptorMetadata<?> getTargetClassInterceptorMetadata() {
        return targetClassInterceptorMetadata;
    }

    public InterceptionModel<ClassMetadata<?>, ?> getInterceptionModel() {
        return interceptionModel;
    }

    public Map<InterceptorMetadata<?>, Object> getInterceptorInstances() {
        return interceptorInstances;
    }
}
