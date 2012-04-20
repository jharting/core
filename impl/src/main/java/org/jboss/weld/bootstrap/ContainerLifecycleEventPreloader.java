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

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.executor.FixedThreadPoolExecutorServices;
import org.jboss.weld.executor.ProfilingExecutorServices;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.util.reflection.ParameterizedTypeImpl;

/**
 * Allows observer methods for container lifecycle events to be resolved upfront while the deployment is waiting for classloader
 * or reflection API.
 *
 * @author Jozef Hartinger
 *
 */
public class ContainerLifecycleEventPreloader implements Service {

    private static class PreloadingTask {

        public static final PreloadingTask STOPPER = new PreloadingTask(null, null);

        private final Type type;
        private final BeanManager manager;

        public PreloadingTask(Type type, BeanManager manager) {
            this.type = type;
            this.manager = manager;
        }
    }

    private class Worker implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            for (PreloadingTask task = preloaderQueue.take(); task != PreloadingTask.STOPPER; task = preloaderQueue.take()) {
                task.manager.resolveObserverMethods(task.type);
            }
            return null;
        }
    }

    private final BlockingQueue<PreloadingTask> preloaderQueue = new LinkedBlockingQueue<PreloadingTask>();
    private final boolean enabled;

    public ContainerLifecycleEventPreloader(ExecutorServices executor) {
        if (executor instanceof ProfilingExecutorServices) {
            executor = ProfilingExecutorServices.class.cast(executor).getDelegate();
        }
        if (executor instanceof FixedThreadPoolExecutorServices && FixedThreadPoolExecutorServices.class.cast(executor).getThreadPoolSize() > 1) {
            FixedThreadPoolExecutorServices.class.cast(executor).getTaskExecutor().submit(new Worker());
            enabled = true;
        } else {
            // not a multithreaded environment - no preloading
            enabled = false;
        }
    }

    public void preloadContainerLifecycleEvent(BeanManager manager, Class<?> eventRawType, Type... typeParameters) {
        if (enabled) {
            preloaderQueue.add(new PreloadingTask(new ParameterizedTypeImpl(eventRawType, typeParameters, null), manager));
        }
    }

    @Override
    public void cleanup() {
        if (enabled) {
            preloaderQueue.clear();
            preloaderQueue.add(PreloadingTask.STOPPER);
        }
    }
}
