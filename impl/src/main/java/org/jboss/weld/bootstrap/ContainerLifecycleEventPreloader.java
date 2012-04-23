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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.util.reflection.ParameterizedTypeImpl;

/**
 * Allows observer methods for container lifecycle events to be resolved upfront while the deployment is waiting for classloader
 * or reflection API.
 *
 * @author Jozef Hartinger
 *
 */
public class ContainerLifecycleEventPreloader implements Service {

    /**
     * Use daemon threads so that Weld does not hang e.g. in a SE environment.
     */
    private static class DeamonThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private static final String THREAD_NAME_PREFIX = "weld-preloader-";
        private static final ThreadGroup THREAD_GROUP = new ThreadGroup("weld-preloaders");

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(THREAD_GROUP, r, THREAD_NAME_PREFIX + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    private class PreloadingTask implements Callable<Void> {

        private final Type type;
        private final BeanManager manager;

        public PreloadingTask(Type type, BeanManager manager) {
            this.type = type;
            this.manager = manager;
        }

        @Override
        public Void call() throws Exception {
            manager.resolveObserverMethods(type);
            return null;
        }
    }

    private final ExecutorService executor;

    public ContainerLifecycleEventPreloader() {
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new DeamonThreadFactory());
    }

    public void preloadContainerLifecycleEvent(BeanManager manager, Class<?> eventRawType, Type... typeParameters) {
        executor.submit(new PreloadingTask(new ParameterizedTypeImpl(eventRawType, typeParameters, null), manager));
    }

    @Override
    public void cleanup() {
        // TODO
        executor.shutdownNow();
    }
}
