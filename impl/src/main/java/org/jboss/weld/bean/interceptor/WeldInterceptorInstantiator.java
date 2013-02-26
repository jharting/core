package org.jboss.weld.bean.interceptor;

import javax.enterprise.context.spi.CreationalContext;

import org.jboss.weld.interceptor.spi.instance.InterceptorInstantiator;
import org.jboss.weld.interceptor.spi.metadata.InterceptorFactory;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * @param <T>
 * @author Marius Bogoevici
 */
public class WeldInterceptorInstantiator<T> implements InterceptorInstantiator<T, T> {

    private BeanManagerImpl manager;

    private CreationalContext<T> creationalContext;

    public WeldInterceptorInstantiator(BeanManagerImpl manager, CreationalContext<T> creationalContext) {
        this.manager = manager;
        this.creationalContext = creationalContext;
    }

    public T createFor(InterceptorFactory<T> interceptorReference) {
        return interceptorReference.create(creationalContext, manager);
    }
}
