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

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.ejb.EjbDescriptors;
import org.jboss.weld.introspector.WeldClass;
import org.jboss.weld.manager.BeanManagerImpl;

public class ParallelBeanDeployer extends BeanDeployer {

    private final Queue<String> classNames;
    private final BootstrapExecutorService executor;

    public ParallelBeanDeployer(BeanManagerImpl manager, EjbDescriptors ejbDescriptors, ServiceRegistry services) {
        super(manager, ejbDescriptors, services);
        this.classNames = new LinkedBlockingQueue<String>();
        this.executor = services.get(BootstrapExecutorService.class);
    }

    @Override
    public BeanDeployer addClasses(Iterable<String> c) {
        for (String clazz : c) {
            classNames.add(clazz);
        }
        final CountDownLatch latch = new CountDownLatch(executor.WORKERS);
        for (int i = 0; i < executor.WORKERS; i++) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    String className = classNames.poll();
                    while (className != null) {
                        addClass(className);
                        className = classNames.poll();
                    }
                    latch.countDown();
                }

            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            // TODO
        }
        return this;
    }

    @Override
    public void addClass(WeldClass<?> weldClass) {
        synchronized (this) {
            super.addClass(weldClass);
        }
    }
}
