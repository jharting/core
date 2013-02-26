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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.weld.interceptor.reader.TargetClassInterceptorMetadata;
import org.jboss.weld.interceptor.spi.context.InvocationContextFactory;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;

public class InterceptionChainInvoker {

    private final InterceptionContext ctx;
    private final InvocationContextFactory factory;

    public InterceptionChainInvoker(InterceptionContext ctx, InvocationContextFactory factory) {
        this.ctx = ctx;
        this.factory = factory;
    }

    public Object executeInterception(Object instance, Method method, Object[] args, InterceptionType interceptionType) throws Throwable {
        List<? extends InterceptorMetadata<?>> interceptorList = ctx.getInterceptionModel().getInterceptors(interceptionType, method);
        Collection<InterceptorInvocation> interceptorInvocations = new ArrayList<InterceptorInvocation>(interceptorList.size());
        for (InterceptorMetadata<?> interceptorMetadata : interceptorList) {
            interceptorInvocations.add(interceptorMetadata.getInterceptorInvocation(ctx.getInterceptorInstances().get(interceptorMetadata), interceptorMetadata, interceptionType));
        }
        TargetClassInterceptorMetadata<?> targetClassInterceptorMetadata = ctx.getTargetClassInterceptorMetadata();
        if (targetClassInterceptorMetadata != null && targetClassInterceptorMetadata.isEligible(interceptionType)) {
            interceptorInvocations.add(targetClassInterceptorMetadata.getInterceptorInvocation(instance, targetClassInterceptorMetadata, interceptionType));
        }
        SimpleInterceptionChain chain = new SimpleInterceptionChain(interceptorInvocations);
        return chain.invokeNextInterceptor(factory.newInvocationContext(chain, instance, method, args));
    }

    public InterceptionContext getInterceptionContext() {
        return ctx;
    }
}
