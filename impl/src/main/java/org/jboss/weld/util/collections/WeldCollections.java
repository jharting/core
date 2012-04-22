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
package org.jboss.weld.util.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.weld.util.reflection.Reflections;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

/**
 * Collection utilities.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldCollections {

    public static final Map<Object, List<Object>> EMPTY_ARRAY_SET_MULTIMAP = Collections.unmodifiableMap(new ArraySetMultimap<Object, Object>().trimToSize());
    public static final SetMultimap<Object, Object> EMPTY_SET_MULTIMAP = Multimaps.unmodifiableSetMultimap(HashMultimap.create());

    private WeldCollections() {
    }

    /**
     * Returns an immutable view of a given set. If the given set is empty, a shared instance is returned. If the given set is
     * an instance of {@link ArraySet}, it is trimmed.
     */
    public static <T> Set<T> immutableSet(Set<T> set) {
        if (set.isEmpty()) {
            return Collections.emptySet();
        }
        if (set instanceof ImmutableSet<?>) {
            return set;
        }
        if (set instanceof ArraySet<?>) {
            ArraySet.class.cast(set).trimToSize();
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Returns an immutable view of a given list. If the given list is empty, a shared instance is returned. If the given list
     * is an instance of {@link ArrayList}, it is trimmed.
     */
    public static <T> List<T> immutableList(List<T> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        if (list instanceof ImmutableList<?>) {
            return list;
        }
        if (list instanceof ArrayList<?>) {
            ArrayList.class.cast(list).trimToSize();
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns an immutable view of a given map. If the given map is empty, a shared instance is returned.
     */
    public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
        if (map.isEmpty()) {
            if (map instanceof ArraySetMultimap<?, ?>) {
                return Reflections.cast(EMPTY_ARRAY_SET_MULTIMAP);
            }
            return Collections.emptyMap();
        }
        if (map instanceof ImmutableMap<?, ?>) {
            return map;
        }
        if (map instanceof ArraySetMultimap<?, ?>) {
            ArraySetMultimap.class.cast(map).trimToSize();
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns an immutable view of a given {@link SetMultimap}. If the given multimap is empty, a shared instance is returned.
     */
    public static <K, V> SetMultimap<K, V> immutableSetMultimap(SetMultimap<K, V> multimap) {
        if (multimap.isEmpty()) {
            return Reflections.cast(EMPTY_SET_MULTIMAP);
        }
        return Multimaps.unmodifiableSetMultimap(multimap);
    }
}
