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
package org.jboss.weld.bootstrap.events;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.event.ExtensionObserverMethodImpl;
import org.jboss.weld.util.reflection.Reflections;

public class ContainerLifecycleEventObservers implements Service {

    private boolean everythingObserved;
    private boolean processAnnotatedTypeObserved;
    private boolean processBeanObserved;
    private boolean processBeanAttributesObserved;

    public void processObserverMethod(ObserverMethod<?> observer) {
        if (observer instanceof ExtensionObserverMethodImpl<?, ?>) {
            processObserverMethodType(observer.getObservedType());
        }
    }

    protected void processObserverMethodType(Type observedType) {
        if (everythingObserved) {
            return;
        }

        Class<?> rawType = Reflections.getRawType(observedType);
        if (Object.class.equals(rawType)) {
            this.everythingObserved = true;
        } else if (!processAnnotatedTypeObserved && ProcessAnnotatedType.class.isAssignableFrom(rawType)) {
            processAnnotatedTypeObserved = true;
        } else if (!processBeanObserved && ProcessBean.class.isAssignableFrom(rawType)) {
            processBeanObserved = true;
        } else if (!processBeanAttributesObserved && ProcessBeanAttributes.class.isAssignableFrom(rawType)) {
            processBeanAttributesObserved = true;
        }
    }

    public boolean isProcessAnnotatedTypeObserved() {
        return everythingObserved || processAnnotatedTypeObserved;
    }

    public boolean isProcessBeanObserved() {
        return everythingObserved || processBeanObserved;
    }

    public boolean isProcessBeanAttributesObserved() {
        return everythingObserved || processBeanAttributesObserved;
    }

    @Override
    public void cleanup() {
    }
}
