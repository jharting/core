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

import static org.jboss.weld.logging.Category.BOOTSTRAP;
import static org.jboss.weld.logging.LoggerFactory.loggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.enterprise.inject.spi.Bean;

import org.jboss.weld.bean.RIBean;
import org.jboss.weld.exceptions.DeploymentException;
import org.jboss.weld.manager.BeanManagerImpl;
import org.slf4j.cal10n.LocLogger;

public class OptimizedConcurrentValidator extends ConcurrentValidator {

    private final ThreadPoolService executor;
    private static final LocLogger log = loggerFactory().getLogger(BOOTSTRAP);

    public OptimizedConcurrentValidator(ThreadPoolService executor) {
        super(executor);
        this.executor = executor;
    }

    @Override
    public void validateBeans(Collection<? extends Bean<?>> beans, BeanManagerImpl manager) {
        List<List<? extends Bean<?>>> splitBeans = split((List<? extends Bean<?>>) beans, ThreadPoolService.SIZE);
        List<ValidationTask> tasks = new ArrayList<ValidationTask>(ThreadPoolService.SIZE);
        for (int i = 0; i < ThreadPoolService.SIZE; i++) {
            tasks.add(new ValidationTask(splitBeans.get(i), manager));
        }
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (ValidationTask task : tasks) {
            if (!task.getProblems().isEmpty()) {
                if (task.getProblems().size() == 1) {
                    throw task.getProblems().get(0);
                } else {
                    throw new DeploymentException(task.getProblems());
                }
            }
        }
    }

    private List<List<? extends Bean<?>>> split(List<? extends Bean<?>> beans, int number) {
        List<List<? extends Bean<?>>> result = new ArrayList<List<? extends Bean<?>>>(number);
        int part = beans.size() / number;
        int remainder = beans.size() % number;
        for (int i = 0; i < number; i++) {
            if (i + 1 == number) {
                result.add(new ArrayList<Bean<?>>(beans.subList(i * part, (i + 1) * part + remainder)));
            } else {
                result.add(new ArrayList<Bean<?>>(beans.subList(i * part, (i + 1) * part)));
            }
        }
        return result;
    }

    private class ValidationTask implements Callable<Void> {
        private final Collection<? extends Bean<?>> beans;
        private final BeanManagerImpl manager;
        private final List<RuntimeException> problems = new LinkedList<RuntimeException>();
        private final Set<RIBean<?>> specializedBeans = new HashSet<RIBean<?>>();

        public ValidationTask(Collection<? extends Bean<?>> beans, BeanManagerImpl manager) {
            this.beans = beans;
            this.manager = manager;
        }

        @Override
        public Void call() {
            for (Bean<?> bean : beans) {
                validateBean(bean, specializedBeans, manager, problems);
            }
            return null;
        }

        public List<RuntimeException> getProblems() {
            return problems;
        }
    }
}
