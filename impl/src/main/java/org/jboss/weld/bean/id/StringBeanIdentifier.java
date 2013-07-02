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

import javax.enterprise.inject.spi.BeanAttributes;

import org.jboss.weld.bean.SyntheticClassBean;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.jboss.weld.util.Beans;

public class StringBeanIdentifier implements BeanIdentifier {

    private static final long serialVersionUID = 9292732766921254L;

    public static StringBeanIdentifier of(BeanAttributes<?> attributes, Class<?> beanClass) {
        return new StringBeanIdentifier(new StringBuilder().append(SyntheticClassBean.class.getName()).append(BeanIdentifier.BEAN_ID_SEPARATOR)
                .append(beanClass.getName()).append(BeanIdentifier.BEAN_ID_SEPARATOR).append(Beans.createBeanAttributesId(attributes)).toString());
    }

    private final String id;
    private final int hashCode;

    public StringBeanIdentifier(String id) {
        this.id = id;
        this.hashCode = id.hashCode();
    }

    @Override
    public String asString() {
        return id;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BeanIdentifier) {
            BeanIdentifier that = (BeanIdentifier) obj;
            return this.asString().equals(that.asString());
        }
        return false;
    }

    @Override
    public String toString() {
        return asString();
    }
}
