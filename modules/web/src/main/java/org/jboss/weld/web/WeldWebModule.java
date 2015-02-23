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

import javax.el.ELResolver;
import javax.el.ExpressionFactory;

import org.jboss.weld.el.WeldELResolver;
import org.jboss.weld.el.WeldExpressionFactory;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.ExpressionLanguageService;
import org.jboss.weld.module.WeldModule;

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

    public void register(RegistrationContext ctx) {
        ctx.getServices().add(ExpressionLanguageService.class, EL_SERVICE);
    }
}
