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
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.cal10n.LocLogger;

public class ProfiledThreadPoolService extends ThreadPoolService {

    private static final String PROFILING_VARIABLE_NAME = "WELD_THREAD_POOL_PROFILING";
    private static final LocLogger log = loggerFactory().getLogger(BOOTSTRAP);

    private final AtomicInteger execution = new AtomicInteger();
    private volatile long start = 0;
    private volatile long exceptionCheckStart = 0;
    private final boolean enabled;

    public ProfiledThreadPoolService() {
        enabled = "true".equals(System.getenv(PROFILING_VARIABLE_NAME));
    }

    protected void startProfiling() {
        if (enabled) {
            if (start != 0) {
                throw new IllegalStateException();
            }
            start = System.currentTimeMillis();
            log.info("ThreadPool task execution #" + execution.incrementAndGet() + " started.");
        }
    }

    protected void startExceptionCheck() {
        if (enabled) {
            if (exceptionCheckStart != 0) {
                throw new IllegalStateException();
            }
            exceptionCheckStart = System.currentTimeMillis();
        }
    }

    protected void stopProfiling() {
        if (enabled) {
            if (start == 0) {
                throw new IllegalStateException();
            }
            final long current = System.currentTimeMillis();
            String message = "ThreadPool task execution #" + execution.get() + " took " + (current - start) + " ms";
            if (exceptionCheckStart != 0) {
                message += ", out of which checking for exceptions took " + (current - exceptionCheckStart) + " ms";
            }
            log.info(message);
            start = 0;
            exceptionCheckStart = 0;
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        startProfiling();
        try {
            return super.invokeAll(tasks);
        } finally {
            stopProfiling();
        }
    }

    @Override
    public void invokeAllAndCheckForExceptions(Collection<? extends Callable<Void>> tasks) {
        startProfiling();
        try {
            super.invokeAllAndCheckForExceptions(tasks);
        } finally {
            stopProfiling();
        }
    }

    @Override
    protected void checkForExceptions(List<Future<Void>> futures) {
        startExceptionCheck();
        super.checkForExceptions(futures);
    }
}
