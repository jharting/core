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
package org.jboss.weld.bootstrap;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Requires;
import javax.enterprise.inject.Veto;
import javax.enterprise.inject.spi.AnnotatedType;

import org.jboss.weld.resources.ClassLoaderResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Class responsible for vetoing classes based on {@link Veto} or {@link Requires} annotations located on a bean class or
 * package. Package information is cached.
 *
 * This class it NOT threadsafe and MUST NOT be used concurrently.
 *
 * @author Jozef Hartinger
 * 
 */
public class VetoRepository {

    private final Map<Package, Boolean> packageCache = new HashMap<Package, Boolean>();

    /**
     * Returns true if a given {@link AnnotatedType} should be suppressed from being processed due to {@link Veto} or
     * unsatisfied {@link Requires} requirements.
     */
    public boolean isVetoed(AnnotatedType<?> type) {
        if (type.isAnnotationPresent(Veto.class)) {
            return true;
        }
        Class<?> javaClass = type.getJavaClass();
        ClassLoader classLoader = javaClass.getClassLoader();
        if (isRequirementMissing(type.getAnnotation(Requires.class), classLoader)) {
            return true;
        }
        return isPackageVetoed(javaClass.getPackage(), classLoader);
    }

    /**
     * Determines if any of the requirements cannot be fulfilled.
     */
    protected static boolean isRequirementMissing(Requires requires, ClassLoader classLoader) {
        if (requires == null) {
            return false;
        }
        ResourceLoader loader = new ClassLoaderResourceLoader(classLoader);
        for (String className : requires.value()) {
            if (!Reflections.isClassLoadable(className, loader)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isPackageVetoed(Package pkg, ClassLoader classLoader) {
        Boolean result = packageCache.get(pkg);
        if (result == null) {
            boolean vetoed = false;
            if (pkg.isAnnotationPresent(Veto.class)) {
                vetoed = true;
            }
            if (isRequirementMissing(pkg.getAnnotation(Requires.class), classLoader)) {
                vetoed = true;
            }
            packageCache.put(pkg, vetoed);
            result = vetoed;
        }
        return result;
    }
}
