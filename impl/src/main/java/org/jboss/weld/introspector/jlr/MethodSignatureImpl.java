/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.introspector.jlr;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jboss.weld.introspector.MethodSignature;
import org.jboss.weld.introspector.WeldMethod;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.resources.SharedObjectFacade;

public class MethodSignatureImpl implements MethodSignature {

    public static MethodSignature of(WeldMethod<?, ?> method) {
        ArrayList<String> parameterTypes = new ArrayList<String>(method.getWeldParameters().size());
        for (int i = 0; i < method.getWeldParameters().size(); i++) {
            parameterTypes.add(method.getWeldParameters().get(i).getJavaClass().getName());
        }
        parameterTypes.trimToSize();
        return of(method.getName(), parameterTypes);
    }

    public static MethodSignature of(Method method) {
        ArrayList<String> parameterTypes = new ArrayList<String>(method.getParameterTypes().length);
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            parameterTypes.add(method.getParameterTypes()[i].getName());
        }
        parameterTypes.trimToSize();
        return of(method.getName(), parameterTypes);
    }

    private static MethodSignature of(String name, List<String> parameterTypes) {
        SharedObjectCache cache = SharedObjectFacade.getSharedObjectCache();
        if (cache == null) {
            return new MethodSignatureImpl(name, parameterTypes);
        } else {
            return new MethodSignatureImpl(name, cache.getParameterTypes(parameterTypes));
        }
    }

    private static final long serialVersionUID = 870948075030895317L;

    private final String methodName;
    private final List<String> parameterTypes;

    public MethodSignatureImpl(String methodName, List<String> parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
        result = prime * result + ((parameterTypes == null) ? 0 : parameterTypes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MethodSignatureImpl other = (MethodSignatureImpl) obj;
        if (methodName == null) {
            if (other.methodName != null)
                return false;
        } else if (!methodName.equals(other.methodName))
            return false;
        if (parameterTypes == null) {
            if (other.parameterTypes != null)
                return false;
        } else if (!parameterTypes.equals(other.parameterTypes))
            return false;
        return true;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public String toString() {
        return new StringBuffer().append("method ").append(getMethodName()).append(getParameterTypes()).toString();
    }

}
