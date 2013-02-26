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
package org.jboss.weld.interceptor.chain;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.interceptor.InvocationContext;

import org.jboss.weld.interceptor.proxy.InterceptorInvocation;
import org.jboss.weld.interceptor.proxy.InterceptorMethodInvocation;
import org.jboss.weld.interceptor.spi.context.InterceptionChain;
import org.jboss.weld.util.reflection.SecureReflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 *
 */
public abstract class AbstractInterceptionChain implements InterceptionChain {

    private static final Logger log = LoggerFactory.getLogger(AbstractInterceptionChain.class);

    private final Object target;

    private final Method targetMethod;

    private int currentPosition;

    private final List<InterceptorMethodInvocation> interceptorMethodInvocations;

    protected AbstractInterceptionChain(Collection<InterceptorInvocation> interceptorInvocations, Object target, Method targetMethod) {
        this.target = target;
        this.targetMethod = targetMethod;
        this.currentPosition = 0;
        interceptorMethodInvocations = new ArrayList<InterceptorMethodInvocation>(interceptorInvocations.size());
        for (InterceptorInvocation interceptorInvocation : interceptorInvocations) {
            interceptorMethodInvocations.addAll(interceptorInvocation.getInterceptorMethodInvocations());
        }
    }

    public Object invokeNextInterceptor(InvocationContext invocationContext) throws Throwable {

        try {
            if (hasNextInterceptor()) {
                return invokeNext(invocationContext);
            } else {
                return interceptorChainCompleted(invocationContext);
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    protected Object invokeNext(InvocationContext invocationContext) throws Exception {
        int oldCurrentPosition = currentPosition;
        try {
            InterceptorMethodInvocation nextInterceptorMethodInvocation = interceptorMethodInvocations.get(currentPosition++);
            if (log.isTraceEnabled()) {
                log.trace("Invoking next interceptor in chain:" + nextInterceptorMethodInvocation.toString());
            }
            if (nextInterceptorMethodInvocation.expectsInvocationContext()) {
                return nextInterceptorMethodInvocation.invoke(invocationContext);
            } else {
                nextInterceptorMethodInvocation.invoke(null);
                while (hasNextInterceptor()) {
                    nextInterceptorMethodInvocation = interceptorMethodInvocations.get(currentPosition++);
                    nextInterceptorMethodInvocation.invoke(null);
                }
                return null;
            }
        } finally {
            currentPosition = oldCurrentPosition;
        }
    }

    protected Object interceptorChainCompleted(InvocationContext invocationContext) throws Exception {
        if (targetMethod != null) {
            SecureReflections.ensureAccessible(targetMethod);
            if (invocationContext.getMethod() != null) {
                return targetMethod.invoke(target, invocationContext.getParameters());
            } else {
                return targetMethod.invoke(target);
            }

        } else {
            return null;
        }
    }

    public boolean hasNextInterceptor() {
        return currentPosition < interceptorMethodInvocations.size();
    }
}
