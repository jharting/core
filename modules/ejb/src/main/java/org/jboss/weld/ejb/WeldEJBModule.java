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
package org.jboss.weld.ejb;

import org.jboss.weld.bootstrap.ContextHolder;
import org.jboss.weld.context.ejb.EjbLiteral;
import org.jboss.weld.context.ejb.EjbRequestContext;
import org.jboss.weld.context.ejb.EjbRequestContextImpl;
import org.jboss.weld.module.WeldModule;

public class WeldEJBModule implements WeldModule {

    @Override
    public String getName() {
        return "weld-name";
    }

    @Override
    public void postCreateContexts(ContextRegistrationContext ctx) {
        // Register the EJB Request context
        ctx.addContext(new ContextHolder<EjbRequestContext>(new EjbRequestContextImpl(ctx.getContextId()), EjbRequestContext.class, EjbLiteral.INSTANCE));
    }
}
