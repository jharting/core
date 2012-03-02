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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.exceptions.DeploymentException;
import org.jboss.weld.logging.messages.BootstrapMessage;
import org.slf4j.cal10n.LocLogger;

/**
 * A centralized thread pool used by Weld for parallel bootstrap.
 *
 * @author Jozef Hartinger
 *
 */
public class ThreadPoolService implements Service {

    /**
     * Use deamon threads so that Weld does not hang e.g. in a SE environment.
     */
    private static class DeamonThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private static final String THREAD_NAME_PREFIX = "weld-worker-";
        private final ThreadFactory delegate = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = delegate.newThread(r);
            thread.setDaemon(true);
            thread.setName(THREAD_NAME_PREFIX + threadNumber.getAndIncrement());
            return thread;
        }
    }

    private static final LocLogger log = loggerFactory().getLogger(BOOTSTRAP);
    private static final int TERMINATION_TIMEOUT = 5;
    private static final String THREAD_POOL_SIZE_VARIABLE_NAME = "WELD_THREAD_POOL_SIZE";
    public static final int SIZE;

    static {
        String value = System.getenv(THREAD_POOL_SIZE_VARIABLE_NAME);
        int size = Runtime.getRuntime().availableProcessors() + 1;
        if (value != null) {
            try {
                size = Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        SIZE = size;
        log.info(BootstrapMessage.THREADS_IN_USE, SIZE);
    }

    private final ExecutorService executor;

    public ThreadPoolService() {
        this.executor = Executors.newFixedThreadPool(SIZE, new DeamonThreadFactory());
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks);
    }

    /**
     * Executes the given tasks and blocks until they all finish. If a task throws an exception, the exception is rethrown by
     * this method. If multiple tasks throw exceptions, there is no guarantee about which of the exceptions is rethrown by this
     * method.
     */
    public void invokeAllAndCheckForExceptions(Collection<? extends Callable<Void>> tasks) {
        try {
            List<Future<Void>> results = executor.invokeAll(tasks);
            for (Future<Void> result : results) {
                try {
                    result.get();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new DeploymentException(e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
        }
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public void cleanup() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(TERMINATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
