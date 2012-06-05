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

import static org.jboss.weld.logging.messages.BeanMessage.DELEGATE_NOT_ON_DECORATOR;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;

import org.jboss.weld.exceptions.DefinitionException;

public abstract class AbstractProducer<T> implements Producer<T> {

    protected void checkDelegateInjectionPoints() {
        for (InjectionPoint injectionPoint : getInjectionPoints()) {
            if (injectionPoint.isDelegate()) {
                throw new DefinitionException(DELEGATE_NOT_ON_DECORATOR, this);
            }
        }
    }
}
