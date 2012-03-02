/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.weld.bootstrap;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Interceptor;

import org.jboss.weld.bean.RIBean;
import org.jboss.weld.exceptions.DeploymentException;
import org.jboss.weld.manager.BeanManagerImpl;

import com.google.common.collect.Sets;

/**
 * Processes validation of beans, decorators and interceptors in parallel.
 *
 * @author Jozef Hartinger
 *
 */
public class ConcurrentValidator extends Validator {

    private final ThreadPoolService executor;

    public ConcurrentValidator(ThreadPoolService executor) {
        this.executor = executor;
    }

    @Override
    public void validateBeans(Collection<? extends Bean<?>> beans, final BeanManagerImpl manager) {
        final Set<RIBean<?>> specializedBeans = Sets.newSetFromMap(new ConcurrentHashMap<RIBean<?>, Boolean>());
        final List<RuntimeException> problems = Collections.synchronizedList(new LinkedList<RuntimeException>());

        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
        for (final Bean<?> bean : beans) {
            tasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    validateBean(bean, specializedBeans, manager, problems);
                    return null;
                }
            });
        }

        executor.invokeAllAndCheckForExceptions(tasks);

        if (!problems.isEmpty()) {
            if (problems.size() == 1) {
                throw problems.get(0);
            } else {
                throw new DeploymentException(problems);
            }
        }
    }

    @Override
    public void validateInterceptors(Collection<? extends Interceptor<?>> interceptors) {
        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
        for (final Interceptor<?> interceptor : interceptors) {
            tasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    validateInterceptor(interceptor);
                    return null;
                }
            });
        }

        executor.invokeAllAndCheckForExceptions(tasks);
    }

    @Override
    public void validateDecorators(Collection<? extends Decorator<?>> decorators, final BeanManagerImpl manager) {
        final Set<RIBean<?>> specializedBeans = Sets.newSetFromMap(new ConcurrentHashMap<RIBean<?>, Boolean>());

        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
        for (final Decorator<?> decorator : decorators) {
            tasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    validateDecorator(decorator, specializedBeans, manager);
                    return null;
                }
            });
        }

        executor.invokeAllAndCheckForExceptions(tasks);
    }
}
