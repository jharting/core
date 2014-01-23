package org.jboss.weld.interceptor.reader;

import static org.jboss.weld.util.collections.WeldCollections.immutableMap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.interceptor.InvocationContext;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.annotated.enhanced.MethodSignature;
import org.jboss.weld.annotated.enhanced.jlr.MethodSignatureImpl;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorFactory;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.metadata.MethodMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;
import org.jboss.weld.interceptor.util.InterceptionTypeRegistry;
import org.jboss.weld.interceptor.util.InterceptorMetadataException;
import org.jboss.weld.logging.ValidatorLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.security.SetAccessibleAction;
import org.jboss.weld.util.BeanMethods;

/**
 * @author Marius Bogoevici
 * @author Marko Luksa
 */
public class InterceptorMetadataUtils {
    protected static final String OBJECT_CLASS_NAME = Object.class.getName();

    private InterceptorMetadataUtils() {
    }

    public static InterceptorMetadata readMetadataForInterceptorClass(InterceptorFactory<?> interceptorReference, BeanManagerImpl manager) {
        return new DefaultInterceptorMetadata(interceptorReference.getClassMetadata().getJavaClass(), interceptorReference, buildMethodMap(interceptorReference.getClassMetadata(), false, manager));
    }

//    public static <T> TargetClassInterceptorMetadata readMetadataForTargetClass(ClassMetadata<T> classMetadata, BeanManagerImpl manager) {
//        return new TargetClassInterceptorMetadata(classMetadata, buildMethodMap(classMetadata, true, manager));
//    }

    public static boolean isInterceptorMethod(InterceptionType interceptionType, MethodMetadata method, boolean forTargetClass) {
        if (!method.getSupportedInterceptionTypes().contains(interceptionType)) {
            return false;
        }

        if (interceptionType.isLifecycleCallback()) {
            if (forTargetClass) {
                return isValidTargetClassLifecycleInterceptorMethod(interceptionType, method);
            } else {
                return isValidInterceptorClassLifecycleInterceptorMethod(interceptionType, method);
            }
        } else {
            return isValidBusinessMethodInterceptorMethod(interceptionType, method);
        }
    }

    private static boolean isValidTargetClassLifecycleInterceptorMethod(InterceptionType interceptionType, MethodMetadata method) {
        Method javaMethod = method.getJavaMethod();
        /*
         * This check is relaxed (WELD-1399) because we are not able to distinguish a CDI managed bean from
         * an interceptor class bound using @Interceptors.
         *
         * This will be revisited as part of https://issues.jboss.org/browse/WELD-1401
         *
        if (interceptionType == InterceptionType.AROUND_CONSTRUCT) {
            throw new DefinitionException(ValidatorMessage.AROUND_CONSTRUCT_INTERCEPTOR_METHOD_NOT_ALLOWED_ON_TARGET_CLASS,
                    javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                    interceptionType.annotationClassName());
        }
        */
        /*
         * Again, we relax the check and allow both void and Object return types as we cannot distinguish between
         * a managed bean and an interceptor class.
         */
        if (!Void.TYPE.equals(method.getReturnType()) && !Object.class.equals(method.getReturnType())) {
            throw ValidatorLogger.LOG.interceptorMethodDoesNotHaveVoidReturnType(
                    javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                    interceptionType.annotationClassName(), Void.TYPE.getName());
        }
        Class<?>[] parameterTypes = javaMethod.getParameterTypes();
        if (parameterTypes.length > 1) {
            throw ValidatorLogger.LOG.interceptorMethodDoesNotHaveZeroParameters(
                    javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                    interceptionType.annotationClassName());
        }
        Class<?>[] exceptionTypes = javaMethod.getExceptionTypes();
        if (exceptionTypes.length != 0) {
            for (Class<?> exceptionType : exceptionTypes) {
                if (!RuntimeException.class.isAssignableFrom(exceptionType)) {
                    ValidatorLogger.LOG.interceptorMethodShouldNotThrowCheckedExceptions(
                            javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                            exceptionType.getName());
                }
            }
        }

        return parameterTypes.length == 0;
    }

    private static boolean isValidInterceptorClassLifecycleInterceptorMethod(InterceptionType interceptionType, MethodMetadata method) {
        Method javaMethod = method.getJavaMethod();
        if (!Object.class.equals(method.getReturnType()) && !Void.TYPE.equals(method.getReturnType())) {
            throw ValidatorLogger.LOG.interceptorMethodDoesNotReturnObjectOrVoid(
                    javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                    interceptionType.annotationClassName(), Void.TYPE.getName(), OBJECT_CLASS_NAME);
        }

        Class<?>[] parameterTypes = javaMethod.getParameterTypes();
        if (parameterTypes.length == 0) {
            return false;
        } else if (parameterTypes.length == 1) {
            if (InvocationContext.class.isAssignableFrom(parameterTypes[0])) {
                return true;
            }
            throw ValidatorLogger.LOG.interceptorMethodDoesNotHaveCorrectTypeOfParameter(
                    javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                    interceptionType.annotationClassName(), InvocationContext.class.getName());
        } else {
            throw ValidatorLogger.LOG.interceptorMethodDoesNotHaveExactlyOneParameter(
                    javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                    interceptionType.annotationClassName());
        }
    }

    private static boolean isValidBusinessMethodInterceptorMethod(InterceptionType interceptionType, MethodMetadata method) {
        Method javaMethod = method.getJavaMethod();
        if (!Object.class.equals(method.getReturnType())) {
            throw ValidatorLogger.LOG.interceptorMethodDoesNotReturnObject(
                    javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                    interceptionType.annotationClassName(), OBJECT_CLASS_NAME);
        }

        Class<?>[] parameterTypes = javaMethod.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw ValidatorLogger.LOG.interceptorMethodDoesNotHaveExactlyOneParameter(
                    javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                    interceptionType.annotationClassName());
        }
        if (!InvocationContext.class.isAssignableFrom(parameterTypes[0])) {
            throw ValidatorLogger.LOG.interceptorMethodDoesNotHaveCorrectTypeOfParameter(
                    javaMethod.getName(), javaMethod.getDeclaringClass().getName(),
                    interceptionType.annotationClassName(), InvocationContext.class.getName());
        }
        return true;
    }

    static Map<InterceptionType, List<MethodMetadata>> buildMethodMap(ClassMetadata<?> interceptorClass, boolean forTargetClass) {
        Map<InterceptionType, List<MethodMetadata>> methodMap = new HashMap<InterceptionType, List<MethodMetadata>>();
        ClassMetadata<?> currentClass = interceptorClass;
        Set<MethodSignature> foundMethods = new HashSet<MethodSignature>();
        do {
            Set<InterceptionType> detectedInterceptorTypes = new HashSet<InterceptionType>();

            for (MethodMetadata method : currentClass.getDeclaredMethods()) {
                boolean privateMethod = Modifier.isPrivate(method.getJavaMethod().getModifiers());
                MethodSignature methodSignature = new MethodSignatureImpl(method.getJavaMethod());
                if (privateMethod || !foundMethods.contains(methodSignature)) {
                    for (InterceptionType interceptionType : InterceptionTypeRegistry.getSupportedInterceptionTypes()) {
                        if (isInterceptorMethod(interceptionType, method, forTargetClass)) {
                            if (detectedInterceptorTypes.contains(interceptionType)) {
                                throw new InterceptorMetadataException("Same interception type cannot be specified twice on the same class");
                            }
                            detectedInterceptorTypes.add(interceptionType);

                            // add method in the list - if it is there already, it means that it has been added by a subclass
                            // final methods are treated separately, as a final method cannot override another method nor be
                            // overridden
                            AccessController.doPrivileged(SetAccessibleAction.of(method.getJavaMethod()));

                            List<MethodMetadata> methodList = methodMap.get(interceptionType);
                            if (methodList == null) {
                                methodList = new LinkedList<MethodMetadata>();
                                methodMap.put(interceptionType, methodList);
                            }
                            methodList.add(0, method);
                        }
                    }
                    // the method reference must be added anyway - overridden methods are not taken into consideration
                    if (!privateMethod) {
                        foundMethods.add(methodSignature);
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        while (currentClass != null && !OBJECT_CLASS_NAME.equals(currentClass.getJavaClass().getName()));
        return immutableMap(methodMap);
    }

    static Map<InterceptionType, List<MethodMetadata>> buildMethodMap(ClassMetadata<?> interceptorClass, boolean forTargetClass, BeanManagerImpl manager) {
        EnhancedAnnotatedType<?> type = manager.getServices().get(ClassTransformer.class).getEnhancedAnnotatedType(interceptorClass.getJavaClass(), manager.getId());
        Map<InterceptionType, List<MethodMetadata>> result = new HashMap<InterceptionType, List<MethodMetadata>>();
        for (InterceptionType interceptionType : InterceptionTypeRegistry.getSupportedInterceptionTypes()) {
            List<MethodMetadata> value = BeanMethods.getInterceptorMethods(type, interceptionType, forTargetClass);
            if (!value.isEmpty()) {
                result.put(interceptionType, value);
            }
        }
        return result;
    }

    public static Map<InterceptionType, List<MethodMetadata>> buildMethodMap(EnhancedAnnotatedType<?> type, boolean forTargetClass, BeanManagerImpl manager) {
        Map<InterceptionType, List<MethodMetadata>> result = new HashMap<InterceptionType, List<MethodMetadata>>();
        for (InterceptionType interceptionType : InterceptionTypeRegistry.getSupportedInterceptionTypes()) {
            List<MethodMetadata> value = BeanMethods.getInterceptorMethods(type, interceptionType, forTargetClass);
            if (!value.isEmpty()) {
                result.put(interceptionType, value);
            }
        }
        return result;
    }
}
