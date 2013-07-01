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

import java.lang.reflect.Member;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMember;
import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bean.AbstractProducerBean;

import com.google.common.base.Objects;

public class ProducerIdentifier extends AbstractWeldBeanIdentifier {

    private static final long serialVersionUID = 7231465544854248132L;

    public static ProducerIdentifier of(AbstractClassBean<?> declaringBean, EnhancedAnnotatedMember<?, ?, ? extends Member> member) {
        return new ProducerIdentifier(declaringBean.getAnnotated().getIdentifier(), new MemberIdentifier(member.getJavaMember()));
    }

    private final MemberIdentifier memberIdentifier;

    public ProducerIdentifier(AnnotatedTypeIdentifier typeIdentifier, MemberIdentifier memberIdentifier) {
        super(typeIdentifier);
        this.memberIdentifier = memberIdentifier;
    }

    @Override
    protected String getPrefix() {
        return AbstractProducerBean.class.getName();
    }

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getPrefix());
        builder.append(BEAN_ID_SEPARATOR);
        builder.append(getTypeIdentifier().asString());
        builder.append(BEAN_ID_SEPARATOR);
        builder.append(memberIdentifier.asString());
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getTypeIdentifier(), memberIdentifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ProducerIdentifier) {
            ProducerIdentifier that = (ProducerIdentifier) obj;
            return equal(this.getTypeIdentifier(), that.getTypeIdentifier()) && equal(this.memberIdentifier, that.memberIdentifier);
        }
        return false;
    }

}
