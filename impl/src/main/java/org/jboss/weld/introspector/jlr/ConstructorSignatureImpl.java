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

import java.util.ArrayList;
import java.util.List;

import org.jboss.weld.introspector.ConstructorSignature;
import org.jboss.weld.introspector.WeldConstructor;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.resources.SharedObjectFacade;

public class ConstructorSignatureImpl implements ConstructorSignature {

    public static ConstructorSignature of(WeldConstructor<?> method) {
        ArrayList<String> parameterTypes = new ArrayList<String>(method.getWeldParameters().size());
        for (int i = 0; i < method.getWeldParameters().size(); i++) {
            parameterTypes.add(method.getWeldParameters().get(i).getJavaClass().getName());
        }
        parameterTypes.trimToSize();

        SharedObjectCache cache = SharedObjectFacade.getSharedObjectCache();
        if (cache == null) {
            // test run
            return new ConstructorSignatureImpl(parameterTypes);
        } else {
            return cache.getConstructorSignature(parameterTypes);
        }
    }

    private static final long serialVersionUID = -9111642596078876778L;

    private final List<String> parameterTypes;

    public ConstructorSignatureImpl(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    @Override
    public int hashCode() {
        return parameterTypes.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConstructorSignatureImpl other = (ConstructorSignatureImpl) obj;
        if (parameterTypes == null) {
            if (other.parameterTypes != null)
                return false;
        } else if (!parameterTypes.equals(other.parameterTypes))
            return false;
        return true;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

}
