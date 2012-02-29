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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;

import org.jboss.weld.bean.AbstractBean;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.bootstrap.ThreadPoolService.LoopDecompositionTask;
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
import org.jboss.weld.util.BeansClosure;
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
        final Queue<String> classNames = new ConcurrentLinkedQueue<String>();
        Iterables.addAll(classNames, c);

        List<Runnable> tasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            tasks.add(new LoopDecompositionTask<String>(classNames) {

                @Override
                protected void doWork(String className) {
                    addClass(className);
                }
            });
        }
        executor.executeAndWait(tasks);
        return this;
    }

    @Override
    public void processAnnotatedTypes() {
        Queue<WeldClass<?>> classes = new ConcurrentLinkedQueue<WeldClass<?>>(getEnvironment().getClasses());

        List<Runnable> tasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            tasks.add(new LoopDecompositionTask<WeldClass<?>>(classes) {

                @Override
                protected void doWork(WeldClass<?> weldClass) {
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
                }
            });
        }
        executor.executeAndWait(tasks);
    }

    @Override
    public void createClassBeans() {
        final Multimap<Class<?>, WeldClass<?>> otherWeldClasses = Multimaps.newSetMultimap(new ConcurrentHashMap<Class<?>, Collection<WeldClass<?>>>(),
                new ConcurrentHashSetSupplier<WeldClass<?>>());
        final Queue<WeldClass<?>> classes = new ConcurrentLinkedQueue<WeldClass<?>>(getEnvironment().getClasses());

        // create managed beans, decorators and interceptors
        List<Runnable> tasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            tasks.add(new LoopDecompositionTask<WeldClass<?>>(classes) {

                @Override
                protected void doWork(WeldClass<?> weldClass) {
                    createClassBean(weldClass, otherWeldClasses);
                }
            });
        }
        executor.executeAndWait(tasks);

        // create session beans
        final Queue<InternalEjbDescriptor<?>> ejbDescriptors = new ConcurrentLinkedQueue<InternalEjbDescriptor<?>>();
        Iterables.addAll(ejbDescriptors, getEnvironment().getEjbDescriptors());
        List<Runnable> ejbTasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            ejbTasks.add(new LoopDecompositionTask<InternalEjbDescriptor<?>>(ejbDescriptors) {

                @Override
                protected void doWork(InternalEjbDescriptor<?> descriptor) {
                    if (getEnvironment().isVetoed(descriptor.getBeanClass())) {
                        return;
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
                }
            });
        }
        executor.executeAndWait(ejbTasks);
    }

    @Override
    protected void processBeanAttributes(Collection<? extends AbstractBean<?, ?>> beans) {
        final Queue<AbstractBean<?, ?>> queue = new ConcurrentLinkedQueue<AbstractBean<?,?>>(beans);

        List<Runnable> tasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            tasks.add(new LoopDecompositionTask<AbstractBean<?, ?>>(queue) {

                @Override
                protected void doWork(AbstractBean<?, ?> bean) {
                    boolean vetoed = fireProcessBeanAttributes(bean);
                    if (vetoed) {
                        if (bean.isSpecializing()) {
                            BeansClosure.getClosure(getManager()).removeSpecialized(bean.getSpecializedBean());
                            queue.add(bean.getSpecializedBean());
                        }
                        getEnvironment().vetoBean(bean);
                    } else {
                        // now that we know that the bean won't be vetoed, it's the right time to register @New injection points
                        getEnvironment().addNewBeansFromInjectionPoints(bean);
                    }
                }
            });
        }
        executor.executeAndWait(tasks);
    }

    @Override
    public void createProducersAndObservers() {
        Queue<AbstractClassBean<?>> beans = new ConcurrentLinkedQueue<AbstractClassBean<?>>(getEnvironment().getClassBeanMap().values());

        List<Runnable> tasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            tasks.add(new LoopDecompositionTask<AbstractClassBean<?>>(beans) {

                @Override
                protected void doWork(AbstractClassBean<?> bean) {
                    createObserversProducersDisposers(bean);
                }
            });
        }
        executor.executeAndWait(tasks);
    }

    @Override
    public void doAfterBeanDiscovery(List<? extends Bean<?>> beanList) {
        Queue<Bean<?>> queue = new ConcurrentLinkedQueue<Bean<?>>(beanList);

        List<Runnable> tasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            tasks.add(new LoopDecompositionTask<Bean<?>>(queue) {

                @Override
                protected void doWork(Bean<?> bean) {
                    if (bean instanceof RIBean<?>) {
                        ((RIBean<?>) bean).initializeAfterBeanDiscovery();
                    }
                }
            });
        }

        executor.executeAndWait(tasks);
    }

    @Override
    public AbstractBeanDeployer<BeanDeployerEnvironment> fireBeanEvents() {
        Queue<RIBean<?>> queue = new ConcurrentLinkedQueue<RIBean<?>>(getEnvironment().getBeans());

        List<Runnable> tasks = new LinkedList<Runnable>();
        for (int i = 0; i < executor.WORKERS; i++) {
            tasks.add(new LoopDecompositionTask<RIBean<?>>(queue) {

                @Override
                protected void doWork(RIBean<?> bean) {
                    fireBeanEvents(bean);
                }
            });
        }
        executor.executeAndWait(tasks);
        return this;
    }
}
