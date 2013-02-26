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
package org.jboss.weld.injection.producer;

import java.util.List;

import javax.enterprise.context.spi.CreationalContext;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.proxy.CombinedInterceptorAndDecoratorStackMethodHandler;
import org.jboss.weld.bean.proxy.ProxyObject;
import org.jboss.weld.exceptions.DeploymentException;
import org.jboss.weld.interceptor.proxy.DefaultInvocationContextFactory;
import org.jboss.weld.interceptor.proxy.InterceptionChainInvoker;
import org.jboss.weld.interceptor.proxy.InterceptionContext;
import org.jboss.weld.interceptor.proxy.InterceptorMethodHandler;
import org.jboss.weld.interceptor.reader.TargetClassInterceptorMetadata;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.interceptor.spi.model.InterceptionType;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * A wrapper over {@link SubclassedComponentInstantiator} that registers interceptors within the method handler. This class is
 * thread-safe.
 *
 * @author Jozef Hartinger
 *
 * @param <T>
 */
public class InterceptorApplyingInstantiator<T> implements Instantiator<T> {

    private final TargetClassInterceptorMetadata<T> targetClassInterceptorMetadata;
    private final InterceptionModel<ClassMetadata<?>, ?> interceptionModel;
    private final Instantiator<T> delegate;

    public InterceptorApplyingInstantiator(EnhancedAnnotatedType<T> type, Instantiator<T> delegate, BeanManagerImpl manager) {
        this.targetClassInterceptorMetadata = manager.getInterceptorMetadataReader().getTargetClassInterceptorMetadata(manager.getInterceptorMetadataReader().getClassMetadata(type.getJavaClass()));
        this.interceptionModel = manager.getInterceptorModelRegistry().get(type.getJavaClass());
        this.delegate = delegate;
    }

    @Override
    public T newInstance(CreationalContext<T> ctx, BeanManagerImpl manager) {
        InterceptionContext interceptionContext = new InterceptionContext(targetClassInterceptorMetadata, interceptionModel, ctx, manager);

        performAroundConstructInterception(interceptionContext);

        T instance = delegate.newInstance(ctx, manager);
        applyInterceptors(instance, interceptionContext);
        return instance;
    }

    protected void performAroundConstructInterception(InterceptionContext interceptionContext) {
        List<? extends InterceptorMetadata<?>> interceptors = interceptionModel.getInterceptors(InterceptionType.AROUND_CONSTRUCT, null);
        if (!interceptors.isEmpty()) {
            // TODO
        }
    }

    protected T applyInterceptors(T instance, InterceptionContext interceptionContext) {
        try {
            InterceptionChainInvoker invocation = new InterceptionChainInvoker(interceptionContext, new DefaultInvocationContextFactory());
            InterceptorMethodHandler methodHandler = new InterceptorMethodHandler(invocation);
            CombinedInterceptorAndDecoratorStackMethodHandler wrapperMethodHandler = (CombinedInterceptorAndDecoratorStackMethodHandler) ((ProxyObject) instance).getHandler();
            wrapperMethodHandler.setInterceptorMethodHandler(methodHandler);
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
        return instance;
    }

    @Override
    public String toString() {
        return "InterceptorApplyingInstantiator for " + delegate;
    }

    @Override
    public boolean hasInterceptorSupport() {
        return true;
    }

    @Override
    public boolean hasDecoratorSupport() {
        return delegate.hasDecoratorSupport();
    }
}
