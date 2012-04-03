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
package org.jboss.weld.util.annotated;

import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedParameter;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedCallable;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedParameter;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;


public abstract class ForwardingWeldParameter<T, X> extends ForwardingWeldAnnotated<T, Object> implements EnhancedAnnotatedParameter<T, X> {

    @Override
    protected abstract EnhancedAnnotatedParameter<T, X> delegate();

    public AnnotatedCallable<X> getDeclaringCallable() {
        return delegate().getDeclaringCallable();
    }

    public int getPosition() {
        return delegate().getPosition();
    }

    public EnhancedAnnotatedCallable<?, X, ?> getDeclaringEnhancedCallable() {
        return delegate().getDeclaringEnhancedCallable();
    }

    public EnhancedAnnotatedType<X> getDeclaringType() {
        return delegate().getDeclaringType();
    }

    @Override
    public AnnotatedParameter<X> slim() {
        return delegate().slim();
    }
}
