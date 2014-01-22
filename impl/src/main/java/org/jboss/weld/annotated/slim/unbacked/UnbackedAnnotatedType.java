package org.jboss.weld.annotated.slim.unbacked;

import static org.jboss.weld.util.collections.WeldCollections.immutableGuavaSet;
import static org.jboss.weld.util.reflection.Reflections.cast;

import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.annotated.slim.SlimAnnotatedType;
import org.jboss.weld.exceptions.InvalidObjectException;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.util.reflection.Formats;

import com.google.common.base.Objects;

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

    public static <X> UnbackedAnnotatedType<X> additionalAnnotatedType(String contextId, AnnotatedType<X> source, String bdaId, String suffix, SharedObjectCache cache) {
        return new UnbackedAnnotatedType<X>(source, AnnotatedTypeIdentifier.of(contextId, bdaId, source.getJavaClass().getName(), suffix, false), cache);
    }

    public static <X> UnbackedAnnotatedType<X> modifiedAnnotatedType(SlimAnnotatedType<X> originalType, AnnotatedType<X> source, SharedObjectCache cache) {
        AnnotatedTypeIdentifier identifier = AnnotatedTypeIdentifier.forModifiedAnnotatedType(originalType.getIdentifier());
        return new UnbackedAnnotatedType<X>(source, identifier, cache);
    }

    private final Class<X> javaClass;
    private final Set<AnnotatedConstructor<X>> constructors;
    private final Set<AnnotatedMethod<? super X>> methods;
    private final Set<AnnotatedField<? super X>> fields;
    private final AnnotatedTypeIdentifier identifier;

    private UnbackedAnnotatedType(AnnotatedType<X> source, AnnotatedTypeIdentifier identifier, SharedObjectCache cache) {
        super(source.getBaseType(), source.getTypeClosure(), source.getAnnotations());
        this.javaClass = source.getJavaClass();
        Set<AnnotatedConstructor<X>> constructors = new HashSet<AnnotatedConstructor<X>>(source.getConstructors().size());
        for (AnnotatedConstructor<X> constructor : source.getConstructors()) {
            constructors.add(UnbackedAnnotatedConstructor.of(constructor, this, cache));
        }
        this.constructors = immutableGuavaSet(constructors);
        Set<AnnotatedMethod<? super X>> methods = new HashSet<AnnotatedMethod<? super X>>(source.getMethods().size());
        for (AnnotatedMethod<? super X> originalMethod : source.getMethods()) {
            methods.add(UnbackedAnnotatedMethod.of(originalMethod, this, cache));
        }
        this.methods = immutableGuavaSet(methods);
        Set<AnnotatedField<? super X>> fields = new HashSet<AnnotatedField<? super X>>(source.getFields().size());
        for (AnnotatedField<? super X> originalField : source.getFields()) {
            fields.add(UnbackedAnnotatedField.of(originalField, this, cache));
        }
        this.fields = immutableGuavaSet(fields);
        this.identifier = identifier;
    }

    @Override
    public Class<X> getJavaClass() {
        return javaClass;
    }

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        return constructors;
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        return methods;
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        return fields;
    }

    @Override
    public String toString() {
        return Formats.formatAnnotatedType(this);
    }

    // Serialization

    private Object writeReplace() throws ObjectStreamException {
        return new SerializationProxy<X>(getIdentifier());
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw BeanLogger.LOG.proxyRequired();
    }

    @Override
    public void clear() {
        // noop
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UnbackedAnnotatedType<?>) {
            UnbackedAnnotatedType<?> that = cast(obj);
            return Objects.equal(this.identifier, that.identifier);
        }
        return false;
    }

    @Override
    public AnnotatedTypeIdentifier getIdentifier() {
        return identifier;
    }
}
