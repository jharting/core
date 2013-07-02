/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.bean.id;

import static com.google.common.base.Objects.equal;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.bean.SessionBean;

public class SessionBeanIdentifier extends AbstractWeldBeanIdentifier {

    private static final long serialVersionUID = -8582127778163107560L;

    private final String ejbName;
    private final boolean isNew;
    private final int hashCode;

    public SessionBeanIdentifier(AnnotatedTypeIdentifier delegate, String ejbName, boolean isNew) {
        super(delegate);
        this.ejbName = ejbName;
        this.isNew = isNew;
        this.hashCode = asString().hashCode();
    }

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder();
        builder.append(SessionBean.class.getName());
        builder.append(BEAN_ID_SEPARATOR);
        builder.append(getTypeIdentifier().asString());
        builder.append(BEAN_ID_SEPARATOR);
        builder.append(ejbName);
        builder.append(BEAN_ID_SEPARATOR);
        builder.append(isNew);
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SessionBeanIdentifier) {
            SessionBeanIdentifier that = (SessionBeanIdentifier) obj;
            return equal(this.ejbName, that.ejbName) && equal(this.getTypeIdentifier(), that.getTypeIdentifier()) && equal(this.isNew, that.isNew);
        }
        if (obj instanceof StringBeanIdentifier) {
            StringBeanIdentifier that = (StringBeanIdentifier) obj;
            return this.asString().equals(that.asString());
        }
        return false;
    }
}
