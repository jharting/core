/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.tests.producer.method.parameterized.variable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.BeanArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for a producer method where the producer type uses a type variable.
 * 
 * @author Ricardo Campos
 * @author Jozef Hartinger
 * @see https://community.jboss.org/message/774533
 * 
 */
@RunWith(Arquillian.class)
public class ProducerWithTypeVariableTest {

    @Inject
    private InjectedBean bean;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(BeanArchive.class).addPackage(ProducerWithTypeVariableTest.class.getPackage());
    }

    @Test
    public void testListFoo() {
        assertNotNull(bean);
        Foo<List<String>> listFoo = bean.getListBean();
        assertNotNull(listFoo);
        List<String> list = new ArrayList<String>();
        listFoo.set(list);
        assertEquals(list, listFoo.get());

    }

    @Test
    public void testIntegerFoo() {
        assertNotNull(bean);
        Foo<Integer> integerFoo = bean.getIntegerBean();
        assertNotNull(integerFoo);
        integerFoo.set(8);
        assertEquals(Integer.valueOf(8), integerFoo.get());
    }
}
