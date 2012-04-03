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
package org.jboss.weld.annotated.enhanced.jlr;

import org.jboss.weld.annotated.enhanced.ConstructorSignature;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedConstructor;
import org.jboss.weld.util.collections.Arrays2;

import java.util.Arrays;

public class ConstructorSignatureImpl implements ConstructorSignature {

    private static final long serialVersionUID = -9111642596078876778L;

    private final String[] parameterTypes;

    public ConstructorSignatureImpl(EnhancedAnnotatedConstructor<?> method) {
        this.parameterTypes = new String[method.getEnhancedParameters().size()];
        for (int i = 0; i < method.getEnhancedParameters().size(); i++) {
            parameterTypes[i] = method.getEnhancedParameters().get(i).getJavaClass().getName();
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConstructorSignature) {
            ConstructorSignature that = (ConstructorSignature) obj;
            return Arrays.equals(this.getParameterTypes(), that.getParameterTypes());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(parameterTypes);
    }

    public String[] getParameterTypes() {
        return Arrays2.copyOf(parameterTypes, parameterTypes.length);
    }

}
