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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;

import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.events.ProcessAnnotatedTypeFactory;
import org.jboss.weld.bootstrap.events.ProcessAnnotatedTypeImpl;
import org.jboss.weld.ejb.EjbDescriptors;
import org.jboss.weld.ejb.InternalEjbDescriptor;
import org.jboss.weld.introspector.DiscoveredExternalAnnotatedType;
import org.jboss.weld.introspector.ExternalAnnotatedType;
import org.jboss.weld.introspector.WeldClass;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.collections.ConcurrentHashSetSupplier;
import org.jboss.weld.util.reflection.Reflections;

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
        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();

        for (final String className : c) {
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    addClass(className);
                    return null;
                }
            });
        }

        executor.invokeAllAndCheckForExceptions(tasks);
        return this;
    }

    @Override
    public void processAnnotatedTypes() {
        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();

        for (final WeldClass<?> clazz : getEnvironment().getClasses()) {
            tasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    WeldClass<?> weldClass = clazz;
                    // fire event
                    boolean synthetic = getEnvironment().getSource(weldClass) != null;
                    ProcessAnnotatedTypeImpl<?> event;
                    if (synthetic) {
                        event = ProcessAnnotatedTypeFactory.create(getManager(), weldClass, getEnvironment().getSource(weldClass));
                    } else {
                        event = ProcessAnnotatedTypeFactory.create(getManager(), weldClass);
                    }
                    event.fire();
                    // process the result
                    if (event.isVeto()) {
                        getEnvironment().vetoClass(weldClass);
                    } else {
                        boolean dirty = event.isDirty();
                        if (dirty) {
                            getEnvironment().removeClass(weldClass); // remove the original class
                            AnnotatedType<?> modifiedType;
                            if (synthetic) {
                                modifiedType = ExternalAnnotatedType.of(event.getAnnotatedType());
                            } else {
                                modifiedType = DiscoveredExternalAnnotatedType.of(event.getAnnotatedType());
                            }
                            weldClass = classTransformer.loadClass(modifiedType);
                        }

                        // vetoed due to @Veto or @Requires
                        boolean vetoed = Beans.isVetoed(weldClass);

                        if (dirty && !vetoed) {
                            getEnvironment().addClass(weldClass); // add a replacement for the removed class
                        }
                        if (!dirty && vetoed) {
                            getEnvironment().vetoClass(weldClass);
                        }
                    }
                    return null;
                }
            });
        }

        executor.invokeAllAndCheckForExceptions(tasks);
    }

    @Override
    public void createClassBeans() {
        final Multimap<Class<?>, WeldClass<?>> otherWeldClasses = Multimaps.newSetMultimap(new ConcurrentHashMap<Class<?>, Collection<WeldClass<?>>>(),
                new ConcurrentHashSetSupplier<WeldClass<?>>());

        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
        for (final WeldClass<?> weldClass : getEnvironment().getClasses()) {
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    createClassBean(weldClass, otherWeldClasses);
                    return null;
                }
            });
        }

        executor.invokeAllAndCheckForExceptions(tasks);

        List<Callable<Void>> ejbTasks = new LinkedList<Callable<Void>>();
        for (final InternalEjbDescriptor<?> descriptor : getEnvironment().getEjbDescriptors()) {
            ejbTasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    if (getEnvironment().isVetoed(descriptor.getBeanClass())) {
                        return null;
                    }
                    if (descriptor.isSingleton() || descriptor.isStateful() || descriptor.isStateless()) {
                        if (otherWeldClasses.containsKey(descriptor.getBeanClass())) {
                            for (WeldClass<?> c : otherWeldClasses.get(descriptor.getBeanClass())) {
                                createSessionBean(descriptor, Reflections.<WeldClass> cast(c));
                            }
                        } else {
                            createSessionBean(descriptor);
                        }
                    }
                    return null;
                }
            });
        }
        executor.invokeAllAndCheckForExceptions(ejbTasks);
    }

    @Override
    public void createProducersAndObservers() {
        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
        for (final AbstractClassBean<?> bean : getEnvironment().getClassBeanMap().values()) {
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    createObserversProducersDisposers(bean);
                    return null;
                }
            });
        }
        executor.invokeAllAndCheckForExceptions(tasks);
    }

    @Override
    public void doAfterBeanDiscovery(List<? extends Bean<?>> beanList) {
        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
        for (final Bean<?> bean : beanList) {
            tasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    if (bean instanceof RIBean<?>) {
                        ((RIBean<?>) bean).initializeAfterBeanDiscovery();
                    }
                    return null;
                }
            });
        }
        executor.invokeAllAndCheckForExceptions(tasks);
    }

    @Override
    public AbstractBeanDeployer<BeanDeployerEnvironment> initializeBeans() {
        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
        for (final RIBean<?> bean : getEnvironment().getBeans()) {
            tasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    bean.initialize(getEnvironment());
                    return null;
                }
            });
        }
        executor.invokeAllAndCheckForExceptions(tasks);
        return this;
    }

    @Override
    public AbstractBeanDeployer<BeanDeployerEnvironment> fireBeanEvents() {
        List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
        for (final RIBean<?> bean : getEnvironment().getBeans()) {
            tasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    fireBeanEvents(bean);
                    return null;
                }
            });
        }
        executor.invokeAllAndCheckForExceptions(tasks);
        return this;
    }
}
