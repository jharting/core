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
package org.jboss.weld.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jboss.weld.logging.ServletLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.servlet.spi.HttpContextActivationFilter;
import org.jboss.weld.util.servlet.ServletUtils;

/**
 * Filter that handles conversation context activation if mapped by the application. Otherwise, conversation context is
 * activated by {@link org.jboss.weld.servlet.WeldInitialListener} at the beginning of the request processing.
 *
 * @see org.jboss.weld.servlet.WeldInitialListener
 * @see ConversationContextActivator
 *
 * @author Jozef Hartinger
 *
 */
public class ConversationFilter implements Filter {

    public static final String CONVERSATION_FILTER_REGISTERED = ConversationFilter.class.getName() +  ".registered";

    @Inject
    private BeanManagerImpl manager;
    private HttpContextActivationFilter contextActivationFilter;

    private ConversationContextActivator conversationContextActivator;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.conversationContextActivator = new ConversationContextActivator(manager, false);
        filterConfig.getServletContext().setAttribute(CONVERSATION_FILTER_REGISTERED, Boolean.TRUE);
        contextActivationFilter = ServletUtils.getContextActivationFilter(manager, filterConfig.getServletContext());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            if (contextActivationFilter.accepts(httpRequest)) {
                conversationContextActivator.startConversationContext(httpRequest);
            }
            chain.doFilter(request, response);
            /*
             * We do not deactivate the conversation context in the filer. WeldListener takes care of that!
             */
        } else {
            throw ServletLogger.LOG.onlyHttpServletLifecycleDefined();
        }
    }

    @Override
    public void destroy() {
    }
}
