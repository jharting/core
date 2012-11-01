package org.jboss.weld.annotated.slim.unbacked;

import static org.jboss.weld.logging.messages.BeanMessage.PROXY_REQUIRED;
import static org.jboss.weld.util.collections.WeldCollections.immutableSet;

import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.IdentifiedAnnotatedType;

import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.exceptions.InvalidObjectException;
import org.jboss.weld.util.AnnotatedTypes;
import org.jboss.weld.util.reflection.Formats;
import org.jboss.weld.util.reflection.Reflections;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Wrapper for extension-provided {@link AnnotatedType}. This may seem unnecessary, however it does mean we are providing a
 * consistent view for debugging, error reporting etc. This implementation is also serializable no matter if the original
 * extension-provided {@link AnnotatedType} implementation is.
 *
 * @author Pete Muir
 * @author Jozef Hartinger
 *
 * @param <X> the type
 */
@SuppressWarnings(value = { "SE_NO_SUITABLE_CONSTRUCTOR", "SE_NO_SERIALVERSIONID" }, justification = "False positive from FindBugs - serialization is handled by SerializationProxy.")
public class UnbackedAnnotatedType<X> extends UnbackedAnnotated implements SlimAnnotatedType<X>, Serializable {

    public static <X> UnbackedAnnotatedType<X> of(AnnotatedType<X> originalType) {
        if (originalType instanceof IdentifiedAnnotatedType<?>) {
            return UnbackedAnnotatedType.of(originalType, Reflections.<IdentifiedAnnotatedType<?>>cast(originalType).getID());
        }
        return UnbackedAnnotatedType.of(originalType, AnnotatedTypes.createTypeId(originalType));
    }

    public static <X> UnbackedAnnotatedType<X> of(AnnotatedType<X> originalType, String id) {
        return new UnbackedAnnotatedType<X>(originalType.getBaseType(), originalType.getTypeClosure(), originalType.getAnnotations(), originalType.getJavaClass(),
                originalType.getConstructors(), originalType.getMethods(), originalType.getFields(), id);
    }

    private final Class<X> javaClass;
    private final Set<AnnotatedConstructor<X>> constructors;
    private final Set<AnnotatedMethod<? super X>> methods;
    private final Set<AnnotatedField<? super X>> fields;
    private final String id;

    public UnbackedAnnotatedType(Type baseType, Set<Type> typeClosure, Set<Annotation> annotations, Class<X> javaClass, Set<AnnotatedConstructor<X>> originalConstructors,
            Set<AnnotatedMethod<? super X>> originalMethods, Set<AnnotatedField<? super X>> originalFields, String id) {
        super(baseType, typeClosure, annotations);
        this.javaClass = javaClass;
        Set<AnnotatedConstructor<X>> constructors = new HashSet<AnnotatedConstructor<X>>(originalConstructors.size());
        for (AnnotatedConstructor<X> originalConstructor : originalConstructors) {
            constructors.add(UnbackedAnnotatedConstructor.of(originalConstructor, this));
        }
        this.constructors = immutableSet(constructors);
        Set<AnnotatedMethod<? super X>> methods = new HashSet<AnnotatedMethod<? super X>>(originalMethods.size());
        for (AnnotatedMethod<? super X> originalMethod : originalMethods) {
            methods.add(UnbackedAnnotatedMethod.of(originalMethod, this));
        }
        this.methods = immutableSet(methods);
        Set<AnnotatedField<? super X>> fields = new HashSet<AnnotatedField<? super X>>(originalFields.size());
        for (AnnotatedField<? super X> originalField : originalFields) {
            fields.add(UnbackedAnnotatedField.of(originalField, this));
        }
        this.fields = immutableSet(fields);
        this.id = id;
    }

    public Class<X> getJavaClass() {
        return javaClass;
    }

    public Set<AnnotatedConstructor<X>> getConstructors() {
        return constructors;
    }

    public Set<AnnotatedMethod<? super X>> getMethods() {
        return methods;
    }

    public Set<AnnotatedField<? super X>> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return Formats.formatAnnotatedType(this);
    }

    // Serialization

    private Object writeReplace() throws ObjectStreamException {
        return new IdentifiedAnnotatedTypeSerializationProxy<X>(getID());
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException(PROXY_REQUIRED);
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public void init() {
        // noop
    }

    @Override
    public void clear() {
        // noop
    }

    @Override
    public int hashCode() {
        return getID().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SlimAnnotatedType<?>)) {
            return false;
        }
        SlimAnnotatedType<?> other = (SlimAnnotatedType<?>) obj;
        return other.getID().equals(getID());
    }
}
