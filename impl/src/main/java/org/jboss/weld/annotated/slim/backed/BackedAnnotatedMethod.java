package org.jboss.weld.annotated.slim.backed;

import static org.jboss.weld.util.reflection.Reflections.cast;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;

import org.jboss.weld.util.reflection.Formats;

import com.google.common.collect.ImmutableSet;

public class BackedAnnotatedMethod<X> extends BackedAnnotatedMember<X> implements AnnotatedMethod<X> {

    public static <X, Y extends X> AnnotatedMethod<X> of(Method method, BackedAnnotatedType<Y> declaringType) {
        BackedAnnotatedType<X> downcastDeclaringType = cast(declaringType);
        return new BackedAnnotatedMethod<X>(method, downcastDeclaringType);
    }

    private final Method method;
    private final List<AnnotatedParameter<X>> parameters;

    public BackedAnnotatedMethod(Method method, BackedAnnotatedType<X> declaringType) {
        super(method.getGenericReturnType(), declaringType);
        this.method = method;

        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        final Type[] genericParameterTypes = method.getGenericParameterTypes();

        List<AnnotatedParameter<X>> parameters = new ArrayList<AnnotatedParameter<X>>(genericParameterTypes.length);

        for (int i = 0; i < genericParameterTypes.length; i++) {
            Type parameterType = genericParameterTypes[i];
            Set<Annotation> annotations = null;
            if (parameterAnnotations[i].length > 0) {
                annotations = ImmutableSet.copyOf(parameterAnnotations[i]);
            } else {
                annotations = Collections.emptySet();
            }
            parameters.add(BackedAnnotatedParameter.of(parameterType, annotations, i, this));
        }
        this.parameters = Collections.unmodifiableList(parameters);
    }

    public Method getJavaMember() {
        return method;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return method.getAnnotation(annotationType);
    }

    public Set<Annotation> getAnnotations() {
        return ImmutableSet.copyOf(method.getAnnotations());
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return method.isAnnotationPresent(annotationType);
    }

    public List<AnnotatedParameter<X>> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return Formats.formatAnnotatedMethod(this);
    }
}
