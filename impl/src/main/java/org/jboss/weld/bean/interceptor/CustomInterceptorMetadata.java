package org.jboss.weld.bean.interceptor;

import org.jboss.weld.interceptor.proxy.CustomInterceptorInvocation;
import org.jboss.weld.interceptor.proxy.InterceptorInvocation;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;

/**
 * @author Marius Bogoevici
 */
public class CustomInterceptorMetadata<T> implements InterceptorMetadata<T> {

    private static final long serialVersionUID = -4399216536392687374L;

    private CdiInterceptorFactory<T> factory;

    private ClassMetadata<?> classMetadata;

    public CustomInterceptorMetadata(CdiInterceptorFactory<T> factory, ClassMetadata<?> classMetadata) {
        this.factory = factory;
        this.classMetadata = classMetadata;
    }

    public CdiInterceptorFactory<T> getInterceptorFactory() {
       return factory;
    }

    public ClassMetadata<?> getInterceptorClass() {
        return classMetadata;
    }

    public boolean isEligible(InterceptionType interceptionType) {
        return factory.getInterceptor().intercepts(javax.enterprise.inject.spi.InterceptionType.valueOf(interceptionType.name()));
    }

    public InterceptorInvocation getInterceptorInvocation(Object interceptorInstance, InterceptorMetadata interceptorReference, InterceptionType interceptionType) {
        return new CustomInterceptorInvocation(factory.getInterceptor(), interceptorInstance, javax.enterprise.inject.spi.InterceptionType.valueOf(interceptionType.name()));
    }

    @Override
    public String toString() {
        return "CustomInterceptorMetadata [" + getInterceptorClass().getClassName() + "]";
    }
}
