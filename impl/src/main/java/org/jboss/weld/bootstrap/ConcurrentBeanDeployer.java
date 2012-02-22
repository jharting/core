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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.ejb.EjbDescriptors;
import org.jboss.weld.ejb.InternalEjbDescriptor;
import org.jboss.weld.introspector.WeldClass;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.collections.ConcurrentHashSetSupplier;
import org.jboss.weld.util.reflection.Reflections;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * BeanDeployer that processes some of the deployment tasks in parallel. A threadsafe instance of
 * {@link BeanDeployerEnvironment} is used.
 *
 * @author Jozef Hartinger
 *
 */
public class ConcurrentBeanDeployer extends BeanDeployer {

    private final ThreadPoolService executor;

    public ConcurrentBeanDeployer(BeanManagerImpl manager, EjbDescriptors ejbDescriptors, ServiceRegistry services) {
        super(manager, ejbDescriptors, services, BeanDeployerEnvironment.newConcurrentEnvironment(ejbDescriptors, manager));
        this.executor = services.get(ThreadPoolService.class);
    }

    @Override
    public BeanDeployer addClasses(Iterable<String> c) {
        final Queue<String> classNames = new LinkedBlockingQueue<String>();
        for (String clazz : c) {
            classNames.add(clazz);
        }

        List<Runnable> tasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            tasks.add(new WeldClassLoadingTask(classNames));
        }
        executor.executeAndWait(tasks);
        return this;
    }

    @Override
    public void createClassBeans() {
        final Multimap<Class<?>, WeldClass<?>> otherWeldClasses = Multimaps.newSetMultimap(new ConcurrentHashMap<Class<?>, Collection<WeldClass<?>>>(),
                new ConcurrentHashSetSupplier<WeldClass<?>>());
        final Queue<WeldClass<?>> classes = new LinkedBlockingQueue<WeldClass<?>>(getEnvironment().getClasses());

        // create managed beans, decorators and interceptors
        List<Runnable> tasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            tasks.add(new ClassBeanCreationTask(classes, otherWeldClasses));
        }
        executor.executeAndWait(tasks);

        // create session beans
        final Queue<InternalEjbDescriptor<?>> ejbDescriptors = new LinkedBlockingQueue<InternalEjbDescriptor<?>>();
        Iterables.addAll(ejbDescriptors, getEnvironment().getEjbDescriptors());
        List<Runnable> ejbTasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            ejbTasks.add(new SessionBeanCreationTask(ejbDescriptors, otherWeldClasses));
        }
        executor.executeAndWait(ejbTasks);
    }

    private class WeldClassLoadingTask implements Runnable {
        private final Queue<String> classNames;

        public WeldClassLoadingTask(Queue<String> classNames) {
            this.classNames = classNames;
        }

        @Override
        public void run() {
            String className = classNames.poll();
            Thread thread = Thread.currentThread();
            while (className != null && !thread.isInterrupted()) {
                addClass(className);
                className = classNames.poll();
            }
        }
    }

    private class ClassBeanCreationTask implements Runnable {
        private final Queue<WeldClass<?>> classes;
        private final Multimap<Class<?>, WeldClass<?>> otherWeldClasses;

        public ClassBeanCreationTask(Queue<WeldClass<?>> classes, Multimap<Class<?>, WeldClass<?>> otherWeldClasses) {
            this.classes = classes;
            this.otherWeldClasses = otherWeldClasses;
        }

        @Override
        public void run() {
            WeldClass<?> weldClass = classes.poll();
            Thread thread = Thread.currentThread();
            while (weldClass != null && !thread.isInterrupted()) {
                createClassBean(weldClass, otherWeldClasses);
                weldClass = classes.poll();
            }
        }
    }

    private class SessionBeanCreationTask implements Runnable {
        private final Queue<InternalEjbDescriptor<?>> ejbDescriptors;
        private final Multimap<Class<?>, WeldClass<?>> otherWeldClasses;

        public SessionBeanCreationTask(Queue<InternalEjbDescriptor<?>> ejbDescriptors, Multimap<Class<?>, WeldClass<?>> otherWeldClasses) {
            this.ejbDescriptors = ejbDescriptors;
            this.otherWeldClasses = otherWeldClasses;
        }

        @Override
        public void run() {
            InternalEjbDescriptor<?> ejbDescriptor = ejbDescriptors.poll();
            Thread thread = Thread.currentThread();
            while (ejbDescriptor != null && !thread.isInterrupted()) {
                if (getEnvironment().isVetoed(ejbDescriptor.getBeanClass())) {
                    continue;
                }
                if (ejbDescriptor.isSingleton() || ejbDescriptor.isStateful() || ejbDescriptor.isStateless()) {
                    if (otherWeldClasses.containsKey(ejbDescriptor.getBeanClass())) {
                        for (WeldClass<?> c : otherWeldClasses.get(ejbDescriptor.getBeanClass())) {
                            createSessionBean(ejbDescriptor, Reflections.<WeldClass> cast(c));
                        }
                    } else {
                        createSessionBean(ejbDescriptor);
                    }
                }
                ejbDescriptor = ejbDescriptors.poll();
            }
        }
    }
}
