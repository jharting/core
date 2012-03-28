/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.exceptions;

import java.util.List;

/**
 * Thrown if an deployment exception occurs.
 *
 * @author Pete Muir
 * @author Jozef Hartinger
 */
public class DeploymentException extends javax.enterprise.inject.spi.DeploymentException {
    private static final long serialVersionUID = 8014646336322875707L;

    private WeldExceptionMessage message;

    /**
     * Creates a new exception with the given localized message key and optional
     * arguments for the message.
     *
     * @param <E>  The enumeration type for the message keys
     * @param key  The localized message to use
     * @param args Optional arguments to insert into the message
     */
    public <E extends Enum<?>> DeploymentException(E key, Object... args) {
        super(DeploymentException.class.getName()); // the no-args constructor is missing
        this.message = new WeldExceptionKeyMessage(key, args);
    }

    /**
     * Creates a new exception with the given localized message key, the cause
     * for this exception and optional arguments for the message.
     *
     * @param <E>       The enumeration type for the message keys
     * @param key       The localized message to use
     * @param throwable The cause for this exception
     * @param args      Optional arguments to insert into the message
     */
    public <E extends Enum<?>> DeploymentException(E key, Throwable throwable, Object... args) {
        super(throwable);
        this.message = new WeldExceptionStringMessage(throwable.getLocalizedMessage());
    }

    /**
     * Creates a new exception with the given cause.
     *
     * @param throwable The cause of the exception
     */
    public DeploymentException(Throwable throwable) {
        super(throwable);
        this.message = new WeldExceptionStringMessage(throwable.getLocalizedMessage());
    }

    /**
     * Creates a new exception based on a list of throwables.  The throwables are not
     * used as the cause, but the message from each throwable is included as the message
     * for this exception.
     *
     * @param errors A list of throwables to use in the message
     */
    public DeploymentException(List<? extends Throwable> errors) {
        super(DeploymentException.class.getName()); // the no-args constructor is missing
        this.message = new WeldExceptionListMessage(errors);
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

    @Override
    public String getMessage() {
        return message.getAsString();
    }
}
