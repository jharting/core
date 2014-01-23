/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.weld.interceptor.reader.cache;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.InterceptorImpl;
import org.jboss.weld.interceptor.reader.TargetClassInterceptorMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;

/**
 * @author: Marius Bogoevici
 */
public interface MetadataCachingReader {

    <T> InterceptorMetadata<T> getCdiInterceptorMetadata(InterceptorImpl<T> interceptor);
    <T> InterceptorMetadata<T> getPlainInterceptorMetadata(Class<T> clazz);
    <T> TargetClassInterceptorMetadata<T> getTargetClassInterceptorMetadata(EnhancedAnnotatedType<T> type);

    void cleanAfterBoot();

}
