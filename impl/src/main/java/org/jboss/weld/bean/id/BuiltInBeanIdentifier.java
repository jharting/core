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

import org.jboss.weld.bean.BeanIdentifier;
import org.jboss.weld.bean.builtin.AbstractBuiltInBean;
import org.jboss.weld.manager.BeanManagerImpl;

import com.google.common.base.Objects;

public class BuiltInBeanIdentifier implements BeanIdentifier {

    private static final long serialVersionUID = 902099265887139677L;

    public static BuiltInBeanIdentifier of(BeanManagerImpl manager, Class<?> clazz) {
        return new BuiltInBeanIdentifier(manager.getId(), clazz.getName());
    }

    public static BuiltInBeanIdentifier of(BeanManagerImpl manager, Class<?> clazz, String suffix) {
        return new BuiltInBeanIdentifier(manager.getId(), clazz.getName(), suffix);
    }

    private final String bdaId;
    private final String className;
    private final String suffix;

    public BuiltInBeanIdentifier(String bdaId, String className, String suffix) {
        this.bdaId = bdaId;
        this.className = className;
        this.suffix = suffix;
    }

    public BuiltInBeanIdentifier(String bdaId, String className) {
        this(bdaId, className, null);
    }

    @Override
    public String asString() {
        return new StringBuilder().append(AbstractBuiltInBean.class.getName()).append(BEAN_ID_SEPARATOR).append(bdaId).append(BEAN_ID_SEPARATOR)
                .append(className).append(BEAN_ID_SEPARATOR).append(suffix).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bdaId, className, suffix);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BuiltInBeanIdentifier) {
            BuiltInBeanIdentifier that = (BuiltInBeanIdentifier) obj;
            return equal(this.bdaId, that.bdaId) && equal(this.className, that.className) && equal(this.suffix, that.suffix);
        }
        return false;
    }

    @Override
    public String toString() {
        return asString();
    }
}
