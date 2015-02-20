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
package org.jboss.weld.jta;

import org.jboss.weld.module.WeldModule;
import org.jboss.weld.transaction.spi.TransactionServices;

public class WeldTransactionsModule implements WeldModule {

    @Override
    public String getName() {
        return "weld-jta";
    }

    @Override
    public void register(RegistrationContext ctx) {
        if (ctx.getServices().contains(TransactionServices.class)) {
            ctx.setObserverNotifierFactory(TransactionalObserverNotifier.FACTORY);
        }
    }

    @Override
    public void preDeployBeans(PreBeanRegistrationContext ctx) {
        if (ctx.getServices().contains(TransactionServices.class)) {
            ctx.registerBean(new UserTransactionBean(ctx.getBeanManager()));
        }
    }

}
