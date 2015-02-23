/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.web;

import java.util.LinkedList;
import java.util.List;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.spi.Context;

import org.jboss.weld.bootstrap.ContextHolder;
import org.jboss.weld.context.http.HttpConversationContext;
import org.jboss.weld.context.http.HttpLiteral;
import org.jboss.weld.context.http.HttpRequestContext;
import org.jboss.weld.context.http.HttpRequestContextImpl;
import org.jboss.weld.context.http.HttpSessionContext;
import org.jboss.weld.context.http.HttpSessionContextImpl;
import org.jboss.weld.context.http.HttpSessionDestructionContext;
import org.jboss.weld.context.http.LazyHttpConversationContextImpl;
import org.jboss.weld.el.WeldELResolver;
import org.jboss.weld.el.WeldExpressionFactory;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.ExpressionLanguageService;
import org.jboss.weld.module.WeldModule;
import org.jboss.weld.resources.WeldClassLoaderResourceLoader;
import org.jboss.weld.serialization.BeanIdentifierIndex;
import org.jboss.weld.servlet.ServletApiAbstraction;
import org.jboss.weld.servlet.ServletContextService;
import org.jboss.weld.util.reflection.Reflections;

public class WeldWebModule implements WeldModule {

    // TODO: remove this
    public static final ExpressionLanguageService EL_SERVICE = new ExpressionLanguageService() {
        @Override
        public void cleanup() {
        }

        @Override
        public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory) {
            return new WeldExpressionFactory(expressionFactory);
        }

        @Override
        public ELResolver createElResolver(BeanManagerImpl manager) {
            return new WeldELResolver(manager);
        }
    };

    @Override
    public String getName() {
        return "weld-web";
    }

    private final List<ContextHolder<? extends Context>> contexts = new LinkedList<>();

    public void register(RegistrationContext ctx) {
        ctx.getServices().add(ExpressionLanguageService.class, EL_SERVICE);
        ctx.getServices().add(ServletContextService.class, new ServletContextService());
        ctx.getServices().add(ServletApiAbstraction.class, new ServletApiAbstraction(WeldClassLoaderResourceLoader.INSTANCE));
    }

    public void postCreateContexts(ContextRegistrationContext ctx) {
        final BeanIdentifierIndex index = ctx.getServices().get(BeanIdentifierIndex.class);
        final String contextId = ctx.getContextId();
        if (Reflections.isClassLoadable(ServletApiAbstraction.SERVLET_CONTEXT_CLASS_NAME, WeldClassLoaderResourceLoader.INSTANCE)) {
            // Register the Http contexts if not in
            contexts.add(new ContextHolder<HttpSessionContext>(new HttpSessionContextImpl(contextId, index), HttpSessionContext.class, HttpLiteral.INSTANCE));
            contexts.add(new ContextHolder<HttpSessionDestructionContext>(new HttpSessionDestructionContext(contextId, index), HttpSessionDestructionContext.class, HttpLiteral.INSTANCE));
            contexts.add(new ContextHolder<HttpConversationContext>(new LazyHttpConversationContextImpl(contextId, index), HttpConversationContext.class, HttpLiteral.INSTANCE));
            contexts.add(new ContextHolder<HttpRequestContext>(new HttpRequestContextImpl(contextId), HttpRequestContext.class, HttpLiteral.INSTANCE));
        }

        for (ContextHolder<? extends Context> context : contexts) {
            ctx.addContext(context);
        }
    }

    public void preDeployBeans(PreBeanRegistrationContext ctx) {
        if (Reflections.isClassLoadable(ServletApiAbstraction.SERVLET_CONTEXT_CLASS_NAME, WeldClassLoaderResourceLoader.INSTANCE)) {
            ctx.registerBean(new HttpServletRequestBean(ctx.getBeanManager()));
            ctx.registerBean(new HttpSessionBean(ctx.getBeanManager()));
            ctx.registerBean(new ServletContextBean(ctx.getBeanManager()));
        }
    }
}
