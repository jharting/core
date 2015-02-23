/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.bootstrap;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.Interceptor;

import org.jboss.weld.Container;
import org.jboss.weld.ContainerState;
import org.jboss.weld.annotated.slim.SlimAnnotatedTypeStore;
import org.jboss.weld.annotated.slim.SlimAnnotatedTypeStoreImpl;
import org.jboss.weld.bean.DecoratorImpl;
import org.jboss.weld.bean.InterceptorImpl;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.bean.builtin.BeanManagerBean;
import org.jboss.weld.bean.builtin.BeanManagerImplBean;
import org.jboss.weld.bean.builtin.ContextBean;
import org.jboss.weld.bean.proxy.ProtectionDomainCache;
import org.jboss.weld.bean.proxy.ProxyInstantiator;
import org.jboss.weld.bean.proxy.util.SimpleProxyServices;
import org.jboss.weld.bootstrap.api.Environment;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.TypeDiscoveryConfiguration;
import org.jboss.weld.bootstrap.api.helpers.ServiceRegistries;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.enablement.GlobalEnablementBuilder;
import org.jboss.weld.bootstrap.events.AfterBeanDiscoveryImpl;
import org.jboss.weld.bootstrap.events.AfterDeploymentValidationImpl;
import org.jboss.weld.bootstrap.events.AfterTypeDiscoveryImpl;
import org.jboss.weld.bootstrap.events.BeforeBeanDiscoveryImpl;
import org.jboss.weld.bootstrap.events.ContainerLifecycleEventPreloader;
import org.jboss.weld.bootstrap.events.ContainerLifecycleEvents;
import org.jboss.weld.bootstrap.events.RequiredAnnotationDiscovery;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BootstrapConfiguration;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.config.ConfigurationKey;
import org.jboss.weld.config.WeldConfiguration;
import org.jboss.weld.configuration.spi.ExternalConfiguration;
import org.jboss.weld.context.ApplicationContext;
import org.jboss.weld.context.DependentContext;
import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.SingletonContext;
import org.jboss.weld.context.bound.BoundConversationContext;
import org.jboss.weld.context.bound.BoundConversationContextImpl;
import org.jboss.weld.context.bound.BoundLiteral;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.context.bound.BoundRequestContextImpl;
import org.jboss.weld.context.bound.BoundSessionContext;
import org.jboss.weld.context.bound.BoundSessionContextImpl;
import org.jboss.weld.context.ejb.EjbLiteral;
import org.jboss.weld.context.ejb.EjbRequestContext;
import org.jboss.weld.context.ejb.EjbRequestContextImpl;
import org.jboss.weld.context.unbound.ApplicationContextImpl;
import org.jboss.weld.context.unbound.DependentContextImpl;
import org.jboss.weld.context.unbound.RequestContextImpl;
import org.jboss.weld.context.unbound.SingletonContextImpl;
import org.jboss.weld.context.unbound.UnboundLiteral;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.event.CurrentEventMetadata;
import org.jboss.weld.event.DefaultObserverNotifierFactory;
import org.jboss.weld.event.GlobalObserverNotifierService;
import org.jboss.weld.executor.ExecutorServicesFactory;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.SLSBInvocationInjectionPoint;
import org.jboss.weld.injection.producer.InjectionTargetService;
import org.jboss.weld.logging.BootstrapLogger;
import org.jboss.weld.logging.VersionLogger;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.BeanManagerLookupService;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.metadata.TypeStore;
import org.jboss.weld.metadata.cache.MetaAnnotationStore;
import org.jboss.weld.module.ObserverNotifierFactory;
import org.jboss.weld.module.WeldExtensionRegistrar;
import org.jboss.weld.module.WeldModules;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.DefaultResourceLoader;
import org.jboss.weld.resources.MemberTransformer;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.resources.ReflectionCacheFactory;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.resources.SingleThreadScheduledExecutorServiceFactory;
import org.jboss.weld.resources.spi.ClassFileServices;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ScheduledExecutorServiceFactory;
import org.jboss.weld.serialization.BeanIdentifierIndex;
import org.jboss.weld.serialization.ContextualStoreImpl;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.servlet.spi.HttpContextActivationFilter;
import org.jboss.weld.servlet.spi.helpers.AcceptingHttpContextActivationFilter;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.jboss.weld.util.Permissions;
import org.jboss.weld.util.collections.ImmutableSet;
import org.jboss.weld.util.reflection.Formats;
import org.jboss.weld.util.reflection.Reflections;

/**
 * Common bootstrapping functionality that is run at application startup and
 * detects and register beans
 *
 * @author Pete Muir
 * @author Ales Justin
 * @author Marko Luksa
 */
@SuppressWarnings("deprecation")
public class WeldStartup {

    static {
        VersionLogger.LOG.version(Formats.version(WeldBootstrap.class.getPackage()));
    }

    private BeanManagerImpl deploymentManager;
    private BeanDeploymentArchiveMapping bdaMapping;
    private Collection<ContextHolder<? extends Context>> contexts;
    private Iterable<Metadata<Extension>> extensions;
    private Environment environment;
    private Deployment deployment;
    private DeploymentVisitor deploymentVisitor;
    private final ServiceRegistry initialServices = new SimpleServiceRegistry();
    private String contextId;


    public WeldStartup() {
    }

    public WeldRuntime startContainer(String contextId, Environment environment, Deployment deployment) {
        if (deployment == null) {
            throw BootstrapLogger.LOG.deploymentRequired();
        }

        checkApiVersion();

        Container.currentId.set(contextId);
        this.contextId = contextId;

        if (this.extensions == null) {
            this.extensions = deployment.getExtensions();
        }

        final ServiceRegistry registry = deployment.getServices();

        setupInitialServices();
        registry.addAll(initialServices.entrySet());

        new AdditionalServiceLoader(deployment).loadAdditionalServices(registry);

        if (!registry.contains(ResourceLoader.class)) {
            registry.add(ResourceLoader.class, DefaultResourceLoader.INSTANCE);
        }

        // Weld configuration - must be set after the ResourceLoader fallback
        WeldConfiguration configuration = new WeldConfiguration(registry.get(BootstrapConfiguration.class), registry.get(ExternalConfiguration.class), deployment);
        registry.add(WeldConfiguration.class, configuration);

        if (!registry.contains(ScheduledExecutorServiceFactory.class)) {
            registry.add(ScheduledExecutorServiceFactory.class, new SingleThreadScheduledExecutorServiceFactory());
        }
        if (!registry.contains(ProxyServices.class)) {
            registry.add(ProxyServices.class, new SimpleProxyServices());
        }

        addImplementationServices(registry, contextId);

        verifyServices(registry, environment.getRequiredDeploymentServices());
        if (!registry.contains(TransactionServices.class)) {
            BootstrapLogger.LOG.jtaUnavailable();
        }

        this.deployment = deployment;
        this.environment = environment;
        this.deploymentManager = BeanManagerImpl.newRootManager(contextId, "deployment", registry);

        Container.initialize(contextId, deploymentManager, ServiceRegistries.unmodifiableServiceRegistry(deployment.getServices()));
        getContainer().setState(ContainerState.STARTING);

        this.contexts = createContexts(registry);

        this.bdaMapping = new BeanDeploymentArchiveMapping();
        this.deploymentVisitor = new DeploymentVisitor(deploymentManager, environment, deployment, contexts, bdaMapping);

        if (deployment instanceof CDI11Deployment) {
            registry.add(BeanManagerLookupService.class, new BeanManagerLookupService((CDI11Deployment) deployment, bdaMapping.getBdaToBeanManagerMap()));
        } else {
            BootstrapLogger.LOG.legacyDeploymentMetadataProvided();
        }

        // Read the deployment structure, bdaMapping will be the physical structure
        // as caused by the presence of beans.xml
        deploymentVisitor.visit();

        Container.currentId.remove();

        return new WeldRuntime(contextId, deploymentManager, bdaMapping.getBdaToBeanManagerMap());
    }

    private void checkApiVersion() {
        if (Bean.class.getInterfaces().length == 1) {
            // this means Bean only extends Contextual - since CDI 1.1 Bean also extends BeanAttributes
            // CDI 1.0 API is detected on classpath - since that would result in obscure exception later, we
            // throw an appropriate exception right now
            throw BootstrapLogger.LOG.cdiApiVersionMismatch();
        }
    }

    private void setupInitialServices() {
        if (initialServices.contains(TypeStore.class)) {
            return;
        }
        // instantiate initial services which we need for this phase
        TypeStore store = new TypeStore();
        SharedObjectCache cache = new SharedObjectCache();
        ReflectionCache reflectionCache = ReflectionCacheFactory.newInstance(store);
        ClassTransformer classTransformer = new ClassTransformer(store, cache, reflectionCache, contextId);
        initialServices.add(TypeStore.class, store);
        initialServices.add(SharedObjectCache.class, cache);
        initialServices.add(ReflectionCache.class, reflectionCache);
        initialServices.add(ClassTransformer.class, classTransformer);
    }

    private void addImplementationServices(ServiceRegistry services, String contextId) {
        final WeldConfiguration configuration = services.get(WeldConfiguration.class);
        services.add(SlimAnnotatedTypeStore.class, new SlimAnnotatedTypeStoreImpl());
        if (services.get(ClassTransformer.class) == null) {
            throw new IllegalStateException(ClassTransformer.class.getSimpleName() + " not installed.");
        }
        services.add(MemberTransformer.class, new MemberTransformer(services.get(ClassTransformer.class)));
        services.add(MetaAnnotationStore.class, new MetaAnnotationStore(services.get(ClassTransformer.class)));
        BeanIdentifierIndex beanIdentifierIndex = new BeanIdentifierIndex();
        services.add(BeanIdentifierIndex.class, beanIdentifierIndex);
        services.add(ContextualStore.class, new ContextualStoreImpl(contextId, beanIdentifierIndex));
        services.add(CurrentInjectionPoint.class, new CurrentInjectionPoint());
        services.add(SLSBInvocationInjectionPoint.class, new SLSBInvocationInjectionPoint());
        services.add(CurrentEventMetadata.class, new CurrentEventMetadata());
        services.add(SpecializationAndEnablementRegistry.class, new SpecializationAndEnablementRegistry());
        services.add(MissingDependenciesRegistry.class, new MissingDependenciesRegistry());

        services.add(ObserverNotifierFactory.class, DefaultObserverNotifierFactory.INSTANCE);

        /*
         * Setup ExecutorServices
         */
        ExecutorServices executor = services.get(ExecutorServices.class);
        if (executor == null) {
            executor = ExecutorServicesFactory.create(DefaultResourceLoader.INSTANCE, configuration);
            if (executor != null) {
                services.add(ExecutorServices.class, executor);
            }
        }

        services.add(RequiredAnnotationDiscovery.class, new RequiredAnnotationDiscovery(services.get(ReflectionCache.class)));

        /*
         * Setup Validator
         */
        if (configuration.getBooleanProperty(ConfigurationKey.CONCURRENT_DEPLOYMENT) && services.contains(ExecutorServices.class)) {
            services.add(Validator.class, new ConcurrentValidator(executor));
        } else {
            services.add(Validator.class, new Validator());
        }

        services.add(GlobalEnablementBuilder.class, new GlobalEnablementBuilder());
        if (!services.contains(HttpContextActivationFilter.class)) {
            services.add(HttpContextActivationFilter.class, AcceptingHttpContextActivationFilter.INSTANCE);
        }
        services.add(ProtectionDomainCache.class, new ProtectionDomainCache());

        services.add(ProxyInstantiator.class, ProxyInstantiator.Factory.create(configuration));

        WeldExtensionRegistrar.register(services, contextId);

        GlobalObserverNotifierService observerNotificationService = new GlobalObserverNotifierService(services, contextId);
        services.add(GlobalObserverNotifierService.class, observerNotificationService);

        /*
         * Preloader for container lifecycle events
         */
        ContainerLifecycleEventPreloader preloader = null;
        int preloaderThreadPoolSize = configuration.getIntegerProperty(ConfigurationKey.PRELOADER_THREAD_POOL_SIZE);
        if (preloaderThreadPoolSize > 0 && Permissions.hasPermission(Permissions.MODIFY_THREAD_GROUP)) {
            preloader = new ContainerLifecycleEventPreloader(preloaderThreadPoolSize, observerNotificationService.getGlobalLenientObserverNotifier());
        }
        services.add(ContainerLifecycleEvents.class, new ContainerLifecycleEvents(preloader, services.get(RequiredAnnotationDiscovery.class)));




    }

    // needs to be resolved once extension beans are deployed
    private void installFastProcessAnnotatedTypeResolver(ServiceRegistry services) {
        ClassFileServices classFileServices = services.get(ClassFileServices.class);
        if (classFileServices != null) {
            final GlobalObserverNotifierService observers = services.get(GlobalObserverNotifierService.class);
            try {
                final FastProcessAnnotatedTypeResolver resolver = new FastProcessAnnotatedTypeResolver(observers.getAllObserverMethods());
                services.add(FastProcessAnnotatedTypeResolver.class, resolver);
            } catch (UnsupportedObserverMethodException e) {
                BootstrapLogger.LOG.notUsingFastResolver(e.getObserver());
                return;
            }
        }
    }

    public void startInitialization() {
        if (deploymentManager == null) {
            throw BootstrapLogger.LOG.managerNotInitialized();
        }

        Set<BeanDeployment> physicalBeanDeploymentArchives = new HashSet<BeanDeployment>(getBeanDeployments());

        ExtensionBeanDeployer extensionBeanDeployer = new ExtensionBeanDeployer(deploymentManager, deployment, bdaMapping, contexts);
        extensionBeanDeployer.addExtensions(extensions);
        extensionBeanDeployer.deployBeans();

        installFastProcessAnnotatedTypeResolver(deploymentManager.getServices());

        // Add the Deployment BeanManager Bean to the Deployment BeanManager
        deploymentManager.addBean(new BeanManagerBean(deploymentManager));
        deploymentManager.addBean(new BeanManagerImplBean(deploymentManager));

        // Re-Read the deployment structure, bdaMapping will be the physical
        // structure, and will add in BDAs for any extensions outside a
        // physical BDA
        deploymentVisitor.visit();

        BeforeBeanDiscoveryImpl.fire(deploymentManager, deployment, bdaMapping, contexts);

        // for each physical BDA transform its classes into AnnotatedType instances
        for (BeanDeployment beanDeployment : physicalBeanDeploymentArchives) {
            beanDeployment.createClasses();
        }

        // Re-Read the deployment structure, bdaMapping will be the physical
        // structure, extensions and any classes added using addAnnotatedType
        // outside the physical BDA
        deploymentVisitor.visit();

        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            beanDeployment.createTypes();
        }

        AfterTypeDiscoveryImpl.fire(deploymentManager, deployment, bdaMapping, contexts);

        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            beanDeployment.createEnablement();
        }
    }


    public void deployBeans() {
        for (BeanDeployment deployment : getBeanDeployments()) {
            deployment.createBeans(environment);
        }
        // we must use separate loops, otherwise cyclic specialization would not work
        for (BeanDeployment deployment : getBeanDeployments()) {
            deployment.getBeanDeployer().processClassBeanAttributes();
            deployment.getBeanDeployer().createProducersAndObservers();
        }
        for (BeanDeployment deployment : getBeanDeployments()) {
            deployment.getBeanDeployer().processProducerAttributes();
            deployment.getBeanDeployer().createNewBeans();
        }

        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            beanDeployment.deploySpecialized(environment);
        }

        // TODO keep a list of new bdas, add them all in, and deploy beans for them, then merge into existing
        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            beanDeployment.deployBeans(environment);
        }

        getContainer().setState(ContainerState.DISCOVERED);

        // Flush caches for BeanManager.getBeans() to be usable in ABD (WELD-1729)
        flushCaches();

        AfterBeanDiscoveryImpl.fire(deploymentManager, deployment, bdaMapping, contexts);

        // Extensions may have registered beans / observers. We need to flush caches.
        flushCaches();

        // Re-read the deployment structure, bdaMapping will be the physical
        // structure, extensions, classes, and any beans added using addBean
        // outside the physical structure
        deploymentVisitor.visit();

        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            beanDeployment.getBeanManager().getServices().get(InjectionTargetService.class).initialize();
            beanDeployment.afterBeanDiscovery(environment);
        }
        getContainer().putBeanDeployments(bdaMapping);
        getContainer().setState(ContainerState.DEPLOYED);
    }

    public void validateBeans() {
        BootstrapLogger.LOG.validatingBeans();
        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            BeanManagerImpl beanManager = beanDeployment.getBeanManager();
            beanManager.getBeanResolver().clear();
            deployment.getServices().get(Validator.class).validateDeployment(beanManager, beanDeployment);
            beanManager.getServices().get(InjectionTargetService.class).validate();
        }
        getContainer().setState(ContainerState.VALIDATED);
        AfterDeploymentValidationImpl.fire(deploymentManager);
    }

    public void endInitialization() {

        // Build a special index of bean identifiers
        deploymentManager.getServices().get(BeanIdentifierIndex.class).build(getBeansForBeanIdentifierIndex());

        // TODO rebuild the manager accessibility graph if the bdas have changed
        // Register the managers so external requests can handle them
        // clear the TypeSafeResolvers, so data that is only used at startup
        // is not kept around using up memory
        flushCaches();
        deploymentManager.getServices().cleanupAfterBoot();
        deploymentManager.cleanupAfterBoot();
        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            BeanManagerImpl beanManager = beanDeployment.getBeanManager();
            beanManager.getInterceptorMetadataReader().cleanAfterBoot();
            beanManager.getServices().cleanupAfterBoot();
            beanManager.cleanupAfterBoot();
            // clean up beans
            for (Bean<?> bean : beanManager.getBeans()) {
                if (bean instanceof RIBean<?>) {
                    RIBean<?> riBean = (RIBean<?>) bean;
                    riBean.cleanupAfterBoot();
                }
            }
            // clean up decorators
            for (Decorator<?> decorator : beanManager.getDecorators()) {
                if (decorator instanceof DecoratorImpl<?>) {
                    Reflections.<DecoratorImpl<?>>cast(decorator).cleanupAfterBoot();
                }
            }
            // clean up interceptors
            for (Interceptor<?> interceptor : beanManager.getInterceptors()) {
                if (interceptor instanceof InterceptorImpl<?>) {
                    Reflections.<InterceptorImpl<?>>cast(interceptor).cleanupAfterBoot();
                }
            }
        }
        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            beanDeployment.getBeanDeployer().cleanup();
        }

        getContainer().setState(ContainerState.INITIALIZED);
    }

    private void flushCaches() {
        deploymentManager.getBeanResolver().clear();
        deploymentManager.getAccessibleLenientObserverNotifier().clear();
        deploymentManager.getGlobalStrictObserverNotifier().clear();
        deploymentManager.getGlobalLenientObserverNotifier().clear();
        deploymentManager.getDecoratorResolver().clear();
        deploymentManager.getInterceptorResolver().clear();
        deploymentManager.getNameBasedResolver().clear();
        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            BeanManagerImpl beanManager = beanDeployment.getBeanManager();
            beanManager.getBeanResolver().clear();
            beanManager.getAccessibleLenientObserverNotifier().clear();
            beanManager.getDecoratorResolver().clear();
            beanManager.getInterceptorResolver().clear();
            beanManager.getNameBasedResolver().clear();
        }
    }

    private Collection<BeanDeployment> getBeanDeployments() {
        return bdaMapping.getBeanDeployments();
    }

    private Container getContainer() {
        return Container.instance(contextId);
    }

    protected Collection<ContextHolder<? extends Context>> createContexts(ServiceRegistry services) {
        List<ContextHolder<? extends Context>> contexts = new ArrayList<ContextHolder<? extends Context>>();

        BeanIdentifierIndex beanIdentifierIndex = services.get(BeanIdentifierIndex.class);

        /*
        * Register a full set of bound and unbound contexts. Although we may not use all of
        * these (e.g. if we are running in a servlet environment) they may be
        * useful for an application.
        */
        contexts.add(new ContextHolder<ApplicationContext>(new ApplicationContextImpl(contextId), ApplicationContext.class, UnboundLiteral.INSTANCE));
        contexts.add(new ContextHolder<SingletonContext>(new SingletonContextImpl(contextId), SingletonContext.class, UnboundLiteral.INSTANCE));
        contexts.add(new ContextHolder<BoundSessionContext>(new BoundSessionContextImpl(contextId, beanIdentifierIndex), BoundSessionContext.class, BoundLiteral.INSTANCE));
        contexts.add(new ContextHolder<BoundConversationContext>(new BoundConversationContextImpl(contextId, beanIdentifierIndex), BoundConversationContext.class, BoundLiteral.INSTANCE));
        contexts.add(new ContextHolder<BoundRequestContext>(new BoundRequestContextImpl(contextId), BoundRequestContext.class, BoundLiteral.INSTANCE));
        contexts.add(new ContextHolder<RequestContext>(new RequestContextImpl(contextId), RequestContext.class, UnboundLiteral.INSTANCE));
        contexts.add(new ContextHolder<DependentContext>(new DependentContextImpl(services.get(ContextualStore.class)), DependentContext.class, UnboundLiteral.INSTANCE));

        if (isEjbServicesRegistered()) {
            // Register the EJB Request context if EjbServices are available
            contexts.add(new ContextHolder<EjbRequestContext>(new EjbRequestContextImpl(contextId), EjbRequestContext.class, EjbLiteral.INSTANCE));
        }

        WeldExtensionRegistrar.postCreateContexts(services, contextId);
        contexts.addAll(services.get(WeldModules.class).getAdditionalContexts());

        /*
        * Register the contexts with the bean manager and add the beans to the
        * deployment manager so that they are easily accessible (contexts are app
        * scoped)
        */
        for (ContextHolder<? extends Context> context : contexts) {
            deploymentManager.addContext(context.getContext());
            deploymentManager.addBean(ContextBean.of(context, deploymentManager));
        }

        return contexts;
    }

    protected static void verifyServices(ServiceRegistry services, Set<Class<? extends Service>> requiredServices) {
        for (Class<? extends Service> serviceType : requiredServices) {
            if (!services.contains(serviceType)) {
                throw BootstrapLogger.LOG.unspecifiedRequiredService(serviceType.getName());
            }
        }
    }

    public TypeDiscoveryConfiguration startExtensions(Iterable<Metadata<Extension>> extensions) {
        this.extensions = extensions;
        // TODO: we should fire BeforeBeanDiscovery to allow extensions to register additional scopes
        final Set<Class<? extends Annotation>> beanDefiningAnnotations = ImmutableSet.of(
                // built-in scopes
                Dependent.class, RequestScoped.class, ConversationScoped.class, SessionScoped.class, ApplicationScoped.class,
                javax.interceptor.Interceptor.class, javax.decorator.Decorator.class,
                // built-in stereotype
                Model.class,
                // meta-annotations
                NormalScope.class, Stereotype.class);
        return new TypeDiscoveryConfigurationImpl(beanDefiningAnnotations);
    }

    public BeanManagerImpl getManager(BeanDeploymentArchive beanDeploymentArchive) {
        BeanDeployment beanDeployment = bdaMapping.getBeanDeployment(beanDeploymentArchive);
        return beanDeployment == null ? null : beanDeployment.getBeanManager().getCurrent();
    }

    /**
     * Right now we only index all session and conversation scoped beans.
     *
     * @return the set of beans the index should be built from
     */
    private Set<Bean<?>> getBeansForBeanIdentifierIndex() {
        Set<Bean<?>> beans = new HashSet<Bean<?>>();
        for (BeanDeployment beanDeployment : getBeanDeployments()) {
            for (Bean<?> bean : beanDeployment.getBeanManager().getBeans()) {
                if (bean.getScope().equals(SessionScoped.class) || bean.getScope().equals(ConversationScoped.class)) {
                    beans.add(bean);
                }
            }
        }
        return beans;
    }

    private boolean isEjbServicesRegistered() {
        if(deployment.getServices().contains(EjbServices.class)) {
            // For backwards compatibility with older integrators that register EjbServices
            // as a deployment service
            return true;
        }
        // EjbServices is a bean deployment archive service
        for (BeanDeploymentArchive beanDeploymentArchive : deployment.getBeanDeploymentArchives()) {
            if (beanDeploymentArchive.getServices().contains(EjbServices.class)) {
                return true;
            }
        }
        return false;
    }
}
