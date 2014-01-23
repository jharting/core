package org.jboss.weld.interceptor.reader.cache;

import static org.jboss.weld.util.cache.LoadingCacheUtils.getCastCacheValue;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.InterceptorImpl;
import org.jboss.weld.bean.interceptor.CdiInterceptorFactory;
import org.jboss.weld.interceptor.reader.DefaultInterceptorMetadata;
import org.jboss.weld.interceptor.reader.InterceptorMetadataUtils;
import org.jboss.weld.interceptor.reader.PlainInterceptorFactory;
import org.jboss.weld.interceptor.reader.TargetClassInterceptorMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorFactory;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 *
 */
public class DefaultMetadataCachingReader implements MetadataCachingReader {

    private final BeanManagerImpl manager;

    public DefaultMetadataCachingReader(final BeanManagerImpl manager) {
        this.manager = manager;
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();

        this.plainInterceptorMetadataCache = cacheBuilder.build(new CacheLoader<Class<?>, InterceptorMetadata<?>>() {

            @Override
            public InterceptorMetadata<?> load(Class<?> key) throws Exception {
                EnhancedAnnotatedType<?> type = manager.getServices().get(ClassTransformer.class).getEnhancedAnnotatedType(key, manager.getId());
                InterceptorFactory<?> factory = PlainInterceptorFactory.of(key, manager);
                return new DefaultInterceptorMetadata(key, factory, InterceptorMetadataUtils.buildMethodMap(type, false, manager));
            }
        });
    }



    @Override
    public void cleanAfterBoot() {
    }

    private final LoadingCache<Class<?>, InterceptorMetadata<?>> plainInterceptorMetadataCache;

    @Override
    public <T> InterceptorMetadata<T> getPlainInterceptorMetadata(Class<T> clazz) {
        return getCastCacheValue(plainInterceptorMetadataCache, clazz);
    }

    @Override
    public <T> TargetClassInterceptorMetadata<T> getTargetClassInterceptorMetadata(EnhancedAnnotatedType<T> type) {
        return TargetClassInterceptorMetadata.of(type.getJavaClass(), InterceptorMetadataUtils.buildMethodMap(type, true, manager));
    }

    // TODO move to interceptor impl
    @Override
    public <T> InterceptorMetadata<T> getCdiInterceptorMetadata(InterceptorImpl<T> interceptor) {
        CdiInterceptorFactory<T> reference = new CdiInterceptorFactory<T>(interceptor);
        return new DefaultInterceptorMetadata<T>(interceptor.getBeanClass(), reference, InterceptorMetadataUtils.buildMethodMap(interceptor.getEnhancedAnnotated(), false, manager));
    }
}
