package org.jboss.weld.interceptor.proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.weld.bean.proxy.MethodHandler;
import org.jboss.weld.bean.proxy.ProxyObject;
import org.jboss.weld.interceptor.reader.TargetClassInterceptorMetadata;
import org.jboss.weld.interceptor.spi.context.InvocationContextFactory;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.metadata.MethodMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;
import org.jboss.weld.interceptor.util.InterceptionUtils;
import org.jboss.weld.util.reflection.SecureReflections;

/**
 * @author Marius Bogoevici
 * @author Marko Luksa
 * @author Jozef Hartinger
 */
public class InterceptorMethodHandler implements MethodHandler, Serializable {

    private static final MethodHandler DEFAULT_METHOD_HANDLER = new MethodHandler() {

        public Object invoke(Object self, Method m,
                             Method proceed, Object[] args)
                throws Exception {
            return proceed.invoke(self, args);
        }
    };

    private final InterceptionContext ctx;
    private final Object targetInstance;
    private final InvocationContextFactory invocationContextFactory;

    public InterceptorMethodHandler(Object targetInstance, InterceptionContext ctx, InvocationContextFactory invocationContextFactory) {
        this.targetInstance = targetInstance;
        this.ctx = ctx;
        this.invocationContextFactory = invocationContextFactory;
    }

    protected boolean isProxy() {
        return targetInstance != null;
    }

    public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        SecureReflections.ensureAccessible(thisMethod);
        if (proceed == null) {
            if (thisMethod.getName().equals(InterceptionUtils.POST_CONSTRUCT)) {
                return executeInterception(isProxy() ? null : self, null, null, null, InterceptionType.POST_CONSTRUCT);
            } else if (thisMethod.getName().equals(InterceptionUtils.PRE_DESTROY)) {
                return executeInterception(isProxy() ? null : self, null, null, null, InterceptionType.PRE_DESTROY);
            }
        } else {
            if (isInterceptorMethod(thisMethod)) {
                if (isProxy()) {
                    return thisMethod.invoke(targetInstance, args);
                } else {
                    return proceed.invoke(self, args);
                }
            }
            return executeInterception(isProxy() ? null : self, thisMethod, thisMethod, args, InterceptionType.AROUND_INVOKE);
        }
        return null;
    }

    private boolean isInterceptorMethod(Method method) {
        MethodMetadata methodMetadata = ctx.getTargetClassInterceptorMetadata().getInterceptorClass().getDeclaredMethod(method);
        return methodMetadata != null && methodMetadata.isInterceptorMethod();
    }

    private Object executeInterception(Object self, Method proceedingMethod, Method thisMethod, Object[] args, InterceptionType interceptionType) throws Throwable {

        List<? extends InterceptorMetadata<?>> interceptorList = ctx.getInterceptionModel().getInterceptors(interceptionType, thisMethod);
        Collection<InterceptorInvocation> interceptorInvocations = new ArrayList<InterceptorInvocation>(interceptorList.size());
        for (InterceptorMetadata interceptorReference : interceptorList) {
            interceptorInvocations.add(interceptorReference.getInterceptorInvocation(ctx.getInterceptorInstances().get(interceptorReference), interceptorReference, interceptionType));
        }
        TargetClassInterceptorMetadata<?> targetClassInterceptorMetadata = ctx.getTargetClassInterceptorMetadata();
        if (targetClassInterceptorMetadata != null && targetClassInterceptorMetadata.isEligible(interceptionType)) {
            interceptorInvocations.add(targetClassInterceptorMetadata.getInterceptorInvocation(isProxy() ? targetInstance : self, targetClassInterceptorMetadata, interceptionType));
        }
        SimpleInterceptionChain chain = new SimpleInterceptionChain(interceptorInvocations, isProxy() ? targetInstance : self, isProxy() ? thisMethod : proceedingMethod);
        return chain.invokeNextInterceptor(invocationContextFactory.newInvocationContext(chain, isProxy() ? targetInstance : self, isProxy() ? thisMethod : proceedingMethod, args));
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        try {
            executeInterception(isProxy() ? targetInstance : null, null, null, null, InterceptionType.PRE_PASSIVATE);
            objectOutputStream.defaultWriteObject();
        } catch (Throwable throwable) {
            throw new IOException("Error while serializing class", throwable);
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException {
        try {
            objectInputStream.defaultReadObject();
            if (isProxy() && targetInstance instanceof ProxyObject && ((ProxyObject) targetInstance).getHandler() == null) {
                ((ProxyObject) targetInstance).setHandler(DEFAULT_METHOD_HANDLER);
            }
            executeInterception(isProxy() ? targetInstance : null, null, null, null, InterceptionType.POST_ACTIVATE);
        } catch (Throwable throwable) {
            throw new IOException("Error while deserializing class", throwable);
        }
    }

}
