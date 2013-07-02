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

import com.google.common.base.Objects;

public class SyntheticBeanIdentifier implements BeanIdentifier {

    private static final long serialVersionUID = 4972950382153266536L;

    public static SyntheticBeanIdentifier of(BeanAttributes<?> attributes, Class<?> beanClass) {
        return new SyntheticBeanIdentifier(new StringBuilder().append(SyntheticClassBean.class.getName()).append(BeanIdentifier.BEAN_ID_SEPARATOR)
                .append(beanClass.getName()).append(Beans.createBeanAttributesId(attributes)).toString());
    }

    private final String id;

    public SyntheticBeanIdentifier(String id) {
        this.id = id;
    }

    @Override
    public String asString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SyntheticBeanIdentifier) {
            SyntheticBeanIdentifier that = (SyntheticBeanIdentifier) obj;
            return Objects.equal(this.id, that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return asString();
    }
}
