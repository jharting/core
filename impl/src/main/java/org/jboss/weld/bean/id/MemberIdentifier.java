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

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.jboss.weld.annotated.Identifier;

import com.google.common.base.Objects;

public class MemberIdentifier implements Identifier {

    private static final long serialVersionUID = 2412407623750100391L;

    private static final String METHOD_IDENTIFIER = "()";

    private final Member member;

    public MemberIdentifier(Member member) {
        this.member = member;
    }

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder();
        builder.append(member.getDeclaringClass());
        builder.append(ID_SEPARATOR);
        builder.append(member.getName());
        if (member instanceof Method) {
            builder.append(METHOD_IDENTIFIER);
            Method method = (Method) member;
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> parameterType : parameterTypes) {
                builder.append(ID_SEPARATOR);
                builder.append(parameterType.getName());
            }
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return member.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return false;
        }
        if (obj instanceof MemberIdentifier) {
            MemberIdentifier that = (MemberIdentifier) obj;
            return Objects.equal(this.member, that.member);
        }
        return false;
    }

    @Override
    public String toString() {
        return asString();
    }

}
