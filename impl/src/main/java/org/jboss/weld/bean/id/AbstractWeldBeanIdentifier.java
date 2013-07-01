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
package org.jboss.weld.bean.id;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.bean.BeanIdentifier;

public abstract class AbstractWeldBeanIdentifier implements BeanIdentifier {

    private static final long serialVersionUID = 5863801570327390141L;

    private final AnnotatedTypeIdentifier typeIdentifier;

    public AbstractWeldBeanIdentifier(AnnotatedTypeIdentifier delegate) {
        this.typeIdentifier = delegate;
    }

    protected abstract String getPrefix();

    protected AnnotatedTypeIdentifier getTypeIdentifier() {
        return typeIdentifier;
    }

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getPrefix());
        builder.append(BEAN_ID_SEPARATOR);
        builder.append(typeIdentifier.asString());
        return builder.toString();
    }

    @Override
    public String toString() {
        return asString();
    }
}
