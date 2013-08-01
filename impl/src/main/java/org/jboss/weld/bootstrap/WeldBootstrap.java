/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
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

import static org.jboss.weld.logging.Category.BOOTSTRAP;
import static org.jboss.weld.logging.Category.VERSION;
import static org.jboss.weld.logging.LoggerFactory.loggerFactory;
import static org.jboss.weld.logging.messages.BootstrapMessage.DEPLOYMENT_ARCHIVE_NULL;
import static org.jboss.weld.logging.messages.BootstrapMessage.DEPLOYMENT_REQUIRED;
import static org.jboss.weld.logging.messages.BootstrapMessage.JTA_UNAVAILABLE;
import static org.jboss.weld.logging.messages.BootstrapMessage.MANAGER_NOT_INITIALIZED;
import static org.jboss.weld.logging.messages.BootstrapMessage.UNSPECIFIED_REQUIRED_SERVICE;
import static org.jboss.weld.logging.messages.BootstrapMessage.VALIDATING_BEANS;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.Interceptor;

import org.jboss.weld.Container;
import org.jboss.weld.ContainerState;
import org.jboss.weld.Weld;
import org.jboss.weld.annotated.slim.SlimAnnotatedTypeStore;
import org.jboss.weld.annotated.slim.SlimAnnotatedTypeStoreImpl;
import org.jboss.weld.bean.DecoratorImpl;
import org.jboss.weld.bean.InterceptorImpl;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.bean.builtin.BeanManagerBean;
import org.jboss.weld.bean.builtin.BeanManagerImplBean;
import org.jboss.weld.bean.builtin.ContextBean;
import org.jboss.weld.bean.proxy.util.SimpleProxyServices;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.CDI11Bootstrap;
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
import org.jboss.weld.bootstrap.events.BeforeShutdownImpl;
import org.jboss.weld.bootstrap.events.ContainerLifecycleEventPreloader;
import org.jboss.weld.bootstrap.events.ContainerLifecycleEvents;
import org.jboss.weld.bootstrap.events.SimpleAnnotationDiscovery;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.BootstrapConfiguration;
import org.jboss.weld.bootstrap.spi.CDI11Deployment;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.helpers.FileBasedBootstrapConfiguration;
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
import org.jboss.weld.context.http.HttpConversationContext;
import org.jboss.weld.context.http.HttpConversationContextImpl;
import org.jboss.weld.context.http.HttpLiteral;
import org.jboss.weld.context.http.HttpRequestContext;
import org.jboss.weld.context.http.HttpRequestContextImpl;
import org.jboss.weld.context.http.HttpSessionContext;
import org.jboss.weld.context.http.HttpSessionContextImpl;
import org.jboss.weld.context.http.HttpSessionDestructionContext;
import org.jboss.weld.context.unbound.ApplicationContextImpl;
import org.jboss.weld.context.unbound.DependentContextImpl;
import org.jboss.weld.context.unbound.RequestContextImpl;
import org.jboss.weld.context.unbound.SingletonContextImpl;
import org.jboss.weld.context.unbound.UnboundLiteral;
import org.jboss.weld.ejb.spi.EjbServices;
import org.jboss.weld.event.CurrentEventMetadata;
import org.jboss.weld.event.GlobalObserverNotifierService;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.jboss.weld.exceptions.IllegalStateException;
import org.jboss.weld.executor.ExecutorServicesFactory;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.SLSBInvocationInjectionPoint;
import org.jboss.weld.injection.producer.InjectionTargetService;
import org.jboss.weld.logging.messages.VersionMessage;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.BeanManagerLookupService;
import org.jboss.weld.manager.api.ExecutorServices;
import org.jboss.weld.metadata.TypeStore;
import org.jboss.weld.metadata.cache.MetaAnnotationStore;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.DefaultResourceLoader;
import org.jboss.weld.resources.MemberTransformer;
import org.jboss.weld.resources.ReflectionCache;
import org.jboss.weld.resources.ReflectionCacheFactory;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.resources.SingleThreadScheduledExecutorServiceFactory;
import org.jboss.weld.resources.WeldClassLoaderResourceLoader;
import org.jboss.weld.resources.spi.AnnotationDiscovery;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ScheduledExecutorServiceFactory;
import org.jboss.weld.serialization.ContextualStoreImpl;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.jboss.weld.servlet.ServletApi;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.jboss.weld.util.ServiceLoader;
import org.jboss.weld.util.reflection.Formats;
import org.jboss.weld.util.reflection.Reflections;
import org.jboss.weld.util.reflection.instantiation.InstantiatorFactory;
import org.jboss.weld.util.reflection.instantiation.LoaderInstantiatorFactory;
import org.jboss.weld.xml.BeansXmlParser;
import org.slf4j.cal10n.LocLogger;

import com.google.common.collect.ImmutableSet;

/**
 * Common bootstrapping functionality that is run at application startup and
 * detects and register beans
 *
 * @author Pete Muir
 * @author Ales Justin
 */
public class WeldBootstrap implements CDI11Bootstrap {

    private static final LocLogger log = loggerFactory().getLogger(BOOTSTRAP);

    /**
     * A Deployment visitor which can find the transitive closure of Bean
     * Deployment Archives
     *
     * @author pmuir
     */
    private static class DeploymentVisitor {

        private final BeanManagerImpl deploymentManager;
        private final Environment environment;
        private final Deployment deployment;
        private final Map<BeanDeploymentArchive, BeanDeployment> managerAwareBeanDeploymentArchives;
        private final Collection<ContextHolder<? extends Context>> contexts;

        public DeploymentVisitor(BeanManagerImpl deploymentManager, Environment environment, final Deployment deployment, Collection<ContextHolder<? extends Context>> contexts) {
            this.deploymentManager = deploymentManager;
            this.environment = environment;
            this.deployment = deployment;
            this.contexts = contexts;
            this.managerAwareBeanDeploymentArchives = new ConcurrentHashMap<BeanDeploymentArchive, BeanDeployment>();
        }

        public Map<BeanDeploymentArchive, BeanDeployment> visit() {
            for (BeanDeploymentArchive archive : deployment.getBeanDeploymentArchives()) {
                visit(archive, managerAwareBeanDeploymentArchives, new HashSet<BeanDeploymentArchive>(), true);
            }
            return managerAwareBeanDeploymentArchives;
        }

        private <T extends Service> void copyService(BeanDeploymentArchive archive, Class<T> serviceClass) {
            // for certain services we can fall back to deployment-level settings or defaults
            ServiceRegistry registry = archive.getServices();
            if (!registry.contains(serviceClass)) {
                T service = deployment.getServices().get(serviceClass);
                if (service != null) {
                    registry.add(serviceClass, service);
                }
            }
        }

        private BeanDeployment visit(BeanDeploymentArchive beanDeploymentArchive, Map<BeanDeploymentArchive, BeanDeployment> managerAwareBeanDeploymentArchives, Set<BeanDeploymentArchive> seenBeanDeploymentArchives, boolean validate) {
            copyService(beanDeploymentArchive, ResourceLoader.class);
            copyService(beanDeploymentArchive, InstantiatorFactory.class);
            // Check that the required services are specified
            if (validate) {
                verifyServices(beanDeploymentArchive.getServices(), environment.getRequiredBeanDeploymentArchiveServices());
            }

            // Check the id is not null
            if (beanDeploymentArchive.getId() == null) {
                throw new IllegalArgumentException(DEPLOYMENT_ARCHIVE_NULL, beanDeploymentArchive);
            }

            BeanDeployment parent = managerAwareBeanDeploymentArchives.get(beanDeploymentArchive);
            if (parent == null) {
                // Create the BeanDeployment
                parent = new BeanDeployment(beanDeploymentArchive, deploymentManager, deployment.getServices(), contexts);

                // Attach it
                managerAwareBeanDeploymentArchives.put(beanDeploymentArchive, parent);
            }
            seenBeanDeploymentArchives.add(beanDeploymentArchive);
            for (BeanDeploymentArchive archive : beanDeploymentArchive.getBeanDeploymentArchives()) {
                BeanDeployment child;
                // Cut any circularties
                if (!seenBeanDeploymentArchives.contains(archive)) {
                    child = visit(archive, managerAwareBeanDeploymentArchives, seenBeanDeploymentArchives, validate);
                } else {
                    // already visited
                    child = managerAwareBeanDeploymentArchives.get(archive);
                }
                parent.getBeanManager().addAccessibleBeanManager(child.getBeanManager());
            }
            return parent;
        }

    }

    static {
        loggerFactory().getLogger(VERSION).info(VersionMessage.VERSION, Formats.version(WeldBootstrap.class.getPackage()));
    }

    // The Bean manager
    private BeanManagerImpl deploymentManager;
    private Map<BeanDeploymentArchive, BeanDeployment> beanDeployments;
    private Environment environment;
    private Deployment deployment;
    private DeploymentVisitor deploymentVisitor;
    private final BeansXmlParser beansXmlParser;
    private Collection<ContextHolder<? extends Context>> contexts;
    private final ServiceRegistry initialServices = new SimpleServiceRegistry();
    private Iterable<Metadata<Extension>> extensions;

    public WeldBootstrap() {
        this.beansXmlParser = new BeansXmlParser();
    }

    @Override
    public TypeDiscoveryConfiguration startExtensions(Iterable<Metadata<Extension>> extensions) {
        synchronized (this) {
            this.extensions = extensions;
            // TODO: we should fire BeforeBeanDiscovery to allow extensions to register additional scopes
            final Set<Class<? extends Annotation>> scopes = ImmutableSet.of(Dependent.class, RequestScoped.class, ConversationScoped.class, SessionScoped.class, ApplicationScoped.class);
            return new TypeDiscoveryConfigurationImpl(scopes);
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
        ClassTransformer classTransformer = new ClassTransformer(store, cache, reflectionCache);
        initialServices.add(TypeStore.class, store);
        initialServices.add(SharedObjectCache.class, cache);
        initialServices.add(ReflectionCache.class, reflectionCache);
        initialServices.add(ClassTransformer.class, classTransformer);
    }

    public Bootstrap startContainer(Environment environment, Deployment deployment) {
        synchronized (this) {
            if (deployment == null) {
                throw new IllegalArgumentException(DEPLOYMENT_REQUIRED);
            }

            if (this.extensions == null) {
                this.extensions = deployment.getExtensions();
            }

            final ServiceRegistry registry = deployment.getServices();

            setupInitialServices();
            registry.addAll(initialServices.entrySet());

            if (!registry.contains(ResourceLoader.class)) {
                registry.add(ResourceLoader.class, DefaultResourceLoader.INSTANCE);
            }
            if (!registry.contains(InstantiatorFactory.class)) {
                registry.add(InstantiatorFactory.class, new LoaderInstantiatorFactory());
            }
            if (!registry.contains(ScheduledExecutorServiceFactory.class)) {
                registry.add(ScheduledExecutorServiceFactory.class, new SingleThreadScheduledExecutorServiceFactory());
            }
            if (!registry.contains(ProxyServices.class)) {
                registry.add(ProxyServices.class, new SimpleProxyServices());
            }
            if (!registry.contains(BootstrapConfiguration.class)) {
                registry.add(BootstrapConfiguration.class, new FileBasedBootstrapConfiguration(DefaultResourceLoader.INSTANCE));
            }

            verifyServices(registry, environment.getRequiredDeploymentServices());

            if (!registry.contains(TransactionServices.class)) {
                log.info(JTA_UNAVAILABLE);
            }

            // TODO Reinstate if we can find a good way to detect.
            // if (!deployment.getServices().contains(EjbServices.class))
            // {
            // log.info("EJB services not available. Session beans will be simple beans, CDI-style injection into non-contextual EJBs, injection of remote EJBs and injection of @EJB in simple beans will not be available");
            // }
            // if (!deployment.getServices().contains(JpaInjectionServices.class))
            // {
            // log.info("JPA services not available. Injection of @PersistenceContext will not occur. Entity beans will be discovered as simple beans.");
            // }
            // if
            // (!deployment.getServices().contains(ResourceInjectionServices.class))
            // {
            // log.info("@Resource injection not available.");
            // }

            this.deployment = deployment;
            addImplementationServices(registry);

            ServiceRegistry deploymentServices = new SimpleServiceRegistry();
            deploymentServices.add(ClassTransformer.class, registry.get(ClassTransformer.class));
            deploymentServices.add(SlimAnnotatedTypeStore.class, registry.get(SlimAnnotatedTypeStore.class));
            deploymentServices.add(MetaAnnotationStore.class, registry.get(MetaAnnotationStore.class));
            deploymentServices.add(TypeStore.class, registry.get(TypeStore.class));
            deploymentServices.add(ContextualStore.class, registry.get(ContextualStore.class));
            deploymentServices.add(CurrentInjectionPoint.class, registry.get(CurrentInjectionPoint.class));
            deploymentServices.add(GlobalObserverNotifierService.class, registry.get(GlobalObserverNotifierService.class));
            deploymentServices.add(ContainerLifecycleEvents.class, registry.get(ContainerLifecycleEvents.class));
            deploymentServices.add(SpecializationAndEnablementRegistry.class, registry.get(SpecializationAndEnablementRegistry.class));
            deploymentServices.add(ReflectionCache.class, registry.get(ReflectionCache.class));
            deploymentServices.add(GlobalEnablementBuilder.class, registry.get(GlobalEnablementBuilder.class));

            this.environment = environment;
            this.deploymentManager = BeanManagerImpl.newRootManager("deployment", deploymentServices);

            Container.initialize(deploymentManager, ServiceRegistries.unmodifiableServiceRegistry(registry));
            Container.instance().setState(ContainerState.STARTING);

            this.contexts = createContexts(deploymentServices);

            this.deploymentVisitor = new DeploymentVisitor(deploymentManager, environment, deployment, contexts);

            if (deployment instanceof CDI11Deployment) {
                registry.add(BeanManagerLookupService.class, new BeanManagerLookupService((CDI11Deployment) deployment, deploymentVisitor.managerAwareBeanDeploymentArchives));
            } else {
                log.warn("Legacy deployment metadata provided by the integrator. Certain functionality will not be available.");
            }

            // Read the deployment structure, this will be the physical structure
            // as caused by the presence of beans.xml
            beanDeployments = deploymentVisitor.visit();


            return this;
        }
    }

    private void addImplementationServices(ServiceRegistry services) {
        // Temporary workaround to provide context for building annotated class
        // TODO expose AnnotatedClass on SPI and allow container to provide impl
        // of this via ResourceLoader
        services.add(SlimAnnotatedTypeStore.class, new SlimAnnotatedTypeStoreImpl());
        if (services.get(ClassTransformer.class) == null) {
            throw new RuntimeException();
        }
        services.add(MemberTransformer.class, new MemberTransformer(services.get(ClassTransformer.class)));
        services.add(MetaAnnotationStore.class, new MetaAnnotationStore(services.get(ClassTransformer.class)));
        services.add(ContextualStore.class, new ContextualStoreImpl());
        services.add(CurrentInjectionPoint.class, new CurrentInjectionPoint());
        services.add(SLSBInvocationInjectionPoint.class, new SLSBInvocationInjectionPoint());
        services.add(CurrentEventMetadata.class, new CurrentEventMetadata());
        services.add(SpecializationAndEnablementRegistry.class, new SpecializationAndEnablementRegistry());
        services.add(MissingDependenciesRegistry.class, new MissingDependenciesRegistry());

        GlobalObserverNotifierService observerNotificationService = new GlobalObserverNotifierService(services);
        services.add(GlobalObserverNotifierService.class, observerNotificationService);

        /*
         * Setup ExecutorServices
         */
        ExecutorServices executor = ExecutorServicesFactory.create(DefaultResourceLoader.INSTANCE);
        if (executor != null) {
            services.add(ExecutorServices.class, executor);
        }

        if (!services.contains(AnnotationDiscovery.class)) {
            services.add(AnnotationDiscovery.class, new SimpleAnnotationDiscovery(services.get(ReflectionCache.class)));
        }

        /*
         * Setup Validator
         */
        BootstrapConfiguration bootstrapConfiguration = services.get(BootstrapConfiguration.class);
        if (bootstrapConfiguration.isConcurrentDeploymentEnabled() && services.contains(ExecutorServices.class)) {
            services.add(Validator.class, new ConcurrentValidator(executor));
        } else {
            services.add(Validator.class, new Validator());
        }

        /*
         * Preloader for container lifecycle events
         */
        ContainerLifecycleEventPreloader preloader = null;
        int preloaderThreadPoolSize = bootstrapConfiguration.getPreloaderThreadPoolSize();
        if (preloaderThreadPoolSize > 0) {
            preloader = new ContainerLifecycleEventPreloader(preloaderThreadPoolSize, observerNotificationService.getGlobalLenientObserverNotifier());
        }
        services.add(ContainerLifecycleEvents.class, new ContainerLifecycleEvents(preloader, services.get(AnnotationDiscovery.class)));
        services.add(GlobalEnablementBuilder.class, new GlobalEnablementBuilder());
    }

    public BeanManagerImpl getManager(BeanDeploymentArchive beanDeploymentArchive) {
        synchronized (this) {
            BeanDeployment beanDeployment = beanDeployments.get(beanDeploymentArchive);
            if (beanDeployment != null) {
                return beanDeployment.getBeanManager().getCurrent();
            } else {
                return null;
            }
        }
    }

    public Bootstrap startInitialization() {
        synchronized (this) {
            if (deploymentManager == null) {
                throw new IllegalStateException(MANAGER_NOT_INITIALIZED);
            }

            // we need to know which BDAs are physical so that we fire ProcessModule for there archives only
            Set<BeanDeployment> physicalBeanDeploymentArchives = new HashSet<BeanDeployment>(beanDeployments.values());

            ExtensionBeanDeployer extensionBeanDeployer = new ExtensionBeanDeployer(deploymentManager, deployment, beanDeployments, contexts);
            extensionBeanDeployer.addExtensions(extensions);
            extensionBeanDeployer.deployBeans();

            // Add the Deployment BeanManager Bean to the Deployment BeanManager
            deploymentManager.addBean(new BeanManagerBean(deploymentManager));
            deploymentManager.addBean(new BeanManagerImplBean(deploymentManager));

            // Re-Read the deployment structure, this will be the physical
            // structure, and will add in BDAs for any extensions outside a
            // physical BDA
            beanDeployments = deploymentVisitor.visit();

            BeforeBeanDiscoveryImpl.fire(deploymentManager, deployment, beanDeployments, contexts);

            // for each physical BDA transform its classes into AnnotatedType instances
            for (BeanDeployment beanDeployment : physicalBeanDeploymentArchives) {
                beanDeployment.createClasses();
            }


            // Re-Read the deployment structure, this will be the physical
            // structure, extensions and any classes added using addAnnotatedType
            // outside the physical BDA
            beanDeployments = deploymentVisitor.visit();

            for (BeanDeployment beanDeployment : beanDeployments.values()) {
                beanDeployment.createTypes();
            }

            AfterTypeDiscoveryImpl.fire(deploymentManager, deployment, beanDeployments, contexts);

            for (BeanDeployment beanDeployment : beanDeployments.values()) {
                beanDeployment.createEnabled();
            }
        }
        return this;
    }

    public Bootstrap deployBeans() {
        synchronized (this) {
            for (BeanDeployment deployment : beanDeployments.values()) {
                deployment.createBeans(environment);
            }
            // we must use separate loops, otherwise cyclic specialization would not work
            for (BeanDeployment deployment : beanDeployments.values()) {
                deployment.getBeanDeployer().processClassBeanAttributes();
                deployment.getBeanDeployer().createProducersAndObservers();
            }
            for (BeanDeployment deployment : beanDeployments.values()) {
                deployment.getBeanDeployer().processProducerAttributes();
                deployment.getBeanDeployer().createNewBeans();
            }

            for (Entry<BeanDeploymentArchive, BeanDeployment> entry : beanDeployments.entrySet()) {
                entry.getValue().deploySpecialized(environment);
            }
            // TODO keep a list of new bdas, add them all in, and deploy beans for
            // them, then merge into existing
            for (Entry<BeanDeploymentArchive, BeanDeployment> entry : beanDeployments.entrySet()) {
                entry.getValue().deployBeans(environment);
            }
            AfterBeanDiscoveryImpl.fire(deploymentManager, deployment, beanDeployments, contexts);
            // Re-read the deployment structure, this will be the physical
            // structure, extensions, classes, and any beans added using addBean
            // outside the physical structure
            beanDeployments = deploymentVisitor.visit();
            for (Entry<BeanDeploymentArchive, BeanDeployment> entry : beanDeployments.entrySet()) {
                entry.getValue().getBeanManager().getServices().get(InjectionTargetService.class).initialize();
                entry.getValue().afterBeanDiscovery(environment);
            }
            Container.instance().putBeanDeployments(beanDeployments);
            Container.instance().setState(ContainerState.DEPLOYED);
        }
        return this;
    }

    public Bootstrap validateBeans() {
        synchronized (this) {
            log.debug(VALIDATING_BEANS);
            for (Entry<BeanDeploymentArchive, BeanDeployment> entry : beanDeployments.entrySet()) {
                BeanManagerImpl beanManager = entry.getValue().getBeanManager();
                beanManager.getBeanResolver().clear();
                deployment.getServices().get(Validator.class).validateDeployment(beanManager, entry.getValue());
                beanManager.getServices().get(InjectionTargetService.class).validate();
            }
            Container.instance().setState(ContainerState.VALIDATED);
            AfterDeploymentValidationImpl.fire(deploymentManager);
        }
        return this;
    }

    public Bootstrap endInitialization() {
        // TODO rebuild the manager accessibility graph if the bdas have changed
        synchronized (this) {
            // Register the managers so external requests can handle them
            // clear the TypeSafeResolvers, so data that is only used at startup
            // is not kept around using up memory
            deploymentManager.getBeanResolver().clear();
            deploymentManager.getAccessibleLenientObserverNotifier().clear();
            deploymentManager.getGlobalStrictObserverNotifier().clear();
            deploymentManager.getGlobalLenientObserverNotifier().clear();
            deploymentManager.getDecoratorResolver().clear();
            deploymentManager.getServices().cleanupAfterBoot();
            for (Entry<BeanDeploymentArchive, BeanDeployment> entry : beanDeployments.entrySet()) {
                BeanManagerImpl beanManager = entry.getValue().getBeanManager();
                beanManager.getBeanResolver().clear();
                beanManager.getAccessibleLenientObserverNotifier().clear();
                beanManager.getDecoratorResolver().clear();
                beanManager.getInterceptorMetadataReader().cleanAfterBoot();
                beanManager.getServices().cleanupAfterBoot();
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
            for (BeanDeployment deployment : beanDeployments.values()) {
                deployment.getBeanDeployer().cleanup();
            }
            Container.instance().setState(ContainerState.INITIALIZED);
            return this;
        }
    }

    protected Collection<ContextHolder<? extends Context>> createContexts(ServiceRegistry services) {
        List<ContextHolder<? extends Context>> contexts = new ArrayList<ContextHolder<? extends Context>>();

        /*
        * Register a full set of bound and unbound contexts. Although we may not use all of
        * these (e.g. if we are running in a servlet environment) they may be
        * useful for an application.
        */
        contexts.add(new ContextHolder<ApplicationContext>(new ApplicationContextImpl(), ApplicationContext.class, UnboundLiteral.INSTANCE));
        contexts.add(new ContextHolder<SingletonContext>(new SingletonContextImpl(), SingletonContext.class, UnboundLiteral.INSTANCE));
        contexts.add(new ContextHolder<BoundSessionContext>(new BoundSessionContextImpl(), BoundSessionContext.class, BoundLiteral.INSTANCE));
        contexts.add(new ContextHolder<BoundConversationContext>(new BoundConversationContextImpl(), BoundConversationContext.class, BoundLiteral.INSTANCE));
        contexts.add(new ContextHolder<BoundRequestContext>(new BoundRequestContextImpl(), BoundRequestContext.class, BoundLiteral.INSTANCE));
        contexts.add(new ContextHolder<RequestContext>(new RequestContextImpl(), RequestContext.class, UnboundLiteral.INSTANCE));
        contexts.add(new ContextHolder<DependentContext>(new DependentContextImpl(services.get(ContextualStore.class)), DependentContext.class, UnboundLiteral.INSTANCE));

        if (Reflections.isClassLoadable(ServletApi.SERVLET_CONTEXT_CLASS_NAME, WeldClassLoaderResourceLoader.INSTANCE)) {
            // Register the Http contexts if not in
            contexts.add(new ContextHolder<HttpSessionContext>(new HttpSessionContextImpl(), HttpSessionContext.class, HttpLiteral.INSTANCE));
            contexts.add(new ContextHolder<HttpSessionDestructionContext>(new HttpSessionDestructionContext(), HttpSessionDestructionContext.class, HttpLiteral.INSTANCE));
            contexts.add(new ContextHolder<HttpConversationContext>(new HttpConversationContextImpl(), HttpConversationContext.class, HttpLiteral.INSTANCE));
            contexts.add(new ContextHolder<HttpRequestContext>(new HttpRequestContextImpl(), HttpRequestContext.class, HttpLiteral.INSTANCE));
        }

        if (deployment.getServices().contains(EjbServices.class)) {
            // Register the EJB Request context if EjbServices are available
            contexts.add(new ContextHolder<EjbRequestContext>(new EjbRequestContextImpl(), EjbRequestContext.class, EjbLiteral.INSTANCE));
        }

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

    public void shutdown() {
        synchronized (this) {
            try {
                // First, the container must destroy all contexts.
                deploymentManager.instance().select(ApplicationContext.class).get().invalidate();
            } finally {
                try {
                    // Finally, the container must fire an event of type BeforeShutdown.
                    BeforeShutdownImpl.fire(deploymentManager, beanDeployments);
                } finally {
                    Container.instance().setState(ContainerState.SHUTDOWN);
                    Container.instance().cleanup();
                    // remove BeanManager references
                    try {
                        CDI<?> cdi = CDI.current();
                        if (cdi instanceof Weld) {
                            ((Weld) cdi).cleanup();
                        }
                    } catch (java.lang.IllegalStateException ignored) {
                    }
                }
            }
        }
    }

    protected static void verifyServices(ServiceRegistry services, Set<Class<? extends Service>> requiredServices) {
        for (Class<? extends Service> serviceType : requiredServices) {
            if (!services.contains(serviceType)) {
                throw new IllegalStateException(UNSPECIFIED_REQUIRED_SERVICE, serviceType.getName());
            }
        }
    }

    public BeansXml parse(Iterable<URL> urls) {
        return parse(urls, false);
    }

    public BeansXml parse(Iterable<URL> urls, boolean removeDuplicates) {
        return beansXmlParser.parse(urls, removeDuplicates);
    }

    public BeansXml parse(URL url) {
        return beansXmlParser.parse(url);
    }

    public Iterable<Metadata<Extension>> loadExtensions(ClassLoader classLoader) {
        return ServiceLoader.load(Extension.class, classLoader);
    }

}
