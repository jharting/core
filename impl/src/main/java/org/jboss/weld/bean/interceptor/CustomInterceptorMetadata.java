package org.jboss.weld.bean.interceptor;

import org.jboss.weld.interceptor.proxy.CustomInterceptorInvocation;
import org.jboss.weld.interceptor.proxy.InterceptorInvocation;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;

/**
 * @author Marius Bogoevici
 */
public class CustomInterceptorMetadata<T> implements InterceptorMetadata<T> {

    private final CdiInterceptorFactory<T> factory;
    private final Class<T> javaClass;

    public CustomInterceptorMetadata(CdiInterceptorFactory<T> factory, Class<T> javaClass) {
        this.factory = factory;
        this.javaClass = javaClass;
    }

    @Override
    public CdiInterceptorFactory<T> getInterceptorFactory() {
       return factory;
    }

    @Override
    public boolean isEligible(InterceptionType interceptionType) {
        return factory.getInterceptor().intercepts(javax.enterprise.inject.spi.InterceptionType.valueOf(interceptionType.name()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public InterceptorInvocation getInterceptorInvocation(Object interceptorInstance, InterceptionType interceptionType) {
        return new CustomInterceptorInvocation<T>(factory.getInterceptor(), (T) interceptorInstance, javax.enterprise.inject.spi.InterceptionType.valueOf(interceptionType.name()));
    }

    @Override
    public String toString() {
        return "CustomInterceptorMetadata [" + getJavaClass().getName() + "]";
    }

    @Override
    public Class<T> getJavaClass() {
        return javaClass;
    }
}
