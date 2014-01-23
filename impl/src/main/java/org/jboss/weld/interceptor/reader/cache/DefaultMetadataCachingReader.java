package org.jboss.weld.interceptor.reader.cache;

import static org.jboss.weld.util.cache.LoadingCacheUtils.getCacheValue;
import static org.jboss.weld.util.cache.LoadingCacheUtils.getCastCacheValue;

import org.jboss.weld.interceptor.reader.ClassMetadataInterceptorFactory;
import org.jboss.weld.interceptor.reader.InterceptorMetadataUtils;
import org.jboss.weld.interceptor.reader.ReflectiveClassMetadata;
import org.jboss.weld.interceptor.reader.TargetClassInterceptorMetadata;
import org.jboss.weld.interceptor.spi.metadata.ClassMetadata;
import org.jboss.weld.interceptor.spi.metadata.InterceptorFactory;
import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.manager.BeanManagerImpl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 *
 */
public class DefaultMetadataCachingReader implements MetadataCachingReader {

    private final LoadingCache<InterceptorFactory<?>, InterceptorMetadata<?>> interceptorMetadataCache;

    private final LoadingCache<ClassMetadata<?>, InterceptorMetadata<?>> classMetadataInterceptorMetadataCache;

    private final LoadingCache<Class<?>, ClassMetadata<?>> reflectiveClassMetadataCache;

    private final BeanManagerImpl manager;

    public DefaultMetadataCachingReader(final BeanManagerImpl manager) {
        this.manager = manager;
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();

        this.interceptorMetadataCache = cacheBuilder.build(new CacheLoader<InterceptorFactory<?>, InterceptorMetadata<?>>() {
            @Override
            public InterceptorMetadata<?> load(InterceptorFactory<?> from) {
                return InterceptorMetadataUtils.readMetadataForInterceptorClass(from, manager);
            }
        });

        this.classMetadataInterceptorMetadataCache = cacheBuilder
                .build(new CacheLoader<ClassMetadata<?>, InterceptorMetadata<?>>() {
                    @Override
                    public InterceptorMetadata<?> load(ClassMetadata<?> from) {
                return InterceptorMetadataUtils.readMetadataForTargetClass(from, manager);
            }
        });

        this.reflectiveClassMetadataCache = cacheBuilder.build(new CacheLoader<Class<?>, ClassMetadata<?>>() {
            @Override
            public ClassMetadata<?> load(Class<?> from) {
                return ReflectiveClassMetadata.of(from);
            }
        });
    }

    @Override
    public <T> TargetClassInterceptorMetadata<T> getTargetClassInterceptorMetadata(ClassMetadata<T> classMetadata) {
        return getCastCacheValue(classMetadataInterceptorMetadataCache, classMetadata);
    }

    @Override
    public <T> InterceptorMetadata<T> getInterceptorMetadata(Class<T> clazz) {
        return getCastCacheValue(interceptorMetadataCache, ClassMetadataInterceptorFactory.of(getCacheValue(reflectiveClassMetadataCache, clazz), manager));
    }

    @Override
    public <T> ClassMetadata<T> getClassMetadata(Class<T> clazz) {
        return getCastCacheValue(reflectiveClassMetadataCache, clazz);
    }

    @Override
    public void cleanAfterBoot() {
        classMetadataInterceptorMetadataCache.invalidateAll();
    }
}
