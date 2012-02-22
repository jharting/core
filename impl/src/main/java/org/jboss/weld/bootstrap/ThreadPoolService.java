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
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.weld.bootstrap.api.Service;

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
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    }

    // TODO make this configurable
    // TODO consider using newCachedThreadPool instead
    public final int WORKERS = Runtime.getRuntime().availableProcessors() + 1;
    private static final int TERMINATION_TIMEOUT = 5;
    private final ExecutorService executor = Executors.newFixedThreadPool(WORKERS, new DeamonThreadFactory());

    public void execute(Runnable command) {
        executor.execute(command);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(task, result);
    }

    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    boolean isTerminated() {
        return executor.isTerminated();
    }

    /**
     * Submits the given tasks for execution in the given thread pool. The method call blocks until all the tasks have finished.
     *
     * A task may throw an exception. If multiple tasks throw an exception, the exception that occured first is rethrown once
     * all the tasks are finished.
     *
     * @param tasks tasks to be executed in the thread pool.
     */
    public void executeAndWait(Collection<? extends Runnable> tasks) {
        Queue<RuntimeException> exceptions = new LinkedBlockingQueue<RuntimeException>();
        CountDownLatch latch = new CountDownLatch(tasks.size());
        for (Runnable task : tasks) {
            execute(new BootstrapTask(latch, exceptions, task));
        }
        try {
            latch.await();
            if (!exceptions.isEmpty()) {
                throw exceptions.peek();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private class BootstrapTask implements Runnable {
        private final CountDownLatch latch;
        private final Queue<RuntimeException> exceptions;
        private final Runnable delegate;

        public BootstrapTask(CountDownLatch latch, Queue<RuntimeException> exceptions, Runnable delegate) {
            this.latch = latch;
            this.exceptions = exceptions;
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                delegate.run();
            } catch (RuntimeException e) {
                exceptions.add(e);
            } finally {
                latch.countDown();
            }
        }
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
