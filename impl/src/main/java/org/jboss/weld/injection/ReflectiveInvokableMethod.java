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
package org.jboss.weld.injection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.Collections;
import java.util.Map;

import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.security.MethodLookupAction;
import org.jboss.weld.security.SetAccessibleAction;

import com.google.common.collect.ImmutableMap;

public class ReflectiveInvokableMethod implements InvokableMethod {

    private final Method delegate;
    private volatile Map<Class<?>, Method> methods;

    public ReflectiveInvokableMethod(Method delegate) {
        this.delegate = delegate;
        this.methods = Collections.emptyMap();
    }

    @Override
    public Object invoke(Object instance, Object... parameters) throws InvocationTargetException, IllegalAccessException, IllegalArgumentException, NoSuchMethodException{
        final Method method = getMethodFromClass(instance.getClass());
        return method.invoke(instance, parameters);
    }

    private Method getMethodFromClass(Class<?> clazz) throws NoSuchMethodException {
        final Map<Class<?>, Method> methods = this.methods;
        Method method = this.methods.get(clazz);
        if (method == null) {
            // the same method may be written to the map twice, but that is ok
            // lookupMethod is very slow
            try {
                method = AccessController.doPrivileged(new MethodLookupAction(clazz, delegate.getName(), delegate.getParameterTypes()));
                method = AccessController.doPrivileged(SetAccessibleAction.of(method));
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof NoSuchMethodException) {
                    throw (NoSuchMethodException) e.getCause();
                }
                throw new WeldException(e.getCause());
            }
            final Map<Class<?>, Method> newMethods = ImmutableMap.<Class<?>, Method>builder().putAll(methods).put(clazz, method).build();
            this.methods = newMethods;
        }
        return method;
    }
}
