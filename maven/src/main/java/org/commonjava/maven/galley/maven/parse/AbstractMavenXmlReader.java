package org.commonjava.maven.galley.maven.parse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.galley.model.Location;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class AbstractMavenXmlReader<T extends ProjectRef>
{

    private final Cache<DocCacheKey<T>, DocRef<T>> cache;

    protected AbstractMavenXmlReader()
    {
        cache = CacheBuilder.newBuilder()
                            .concurrencyLevel( 4 )
                            .expireAfterAccess( 5, TimeUnit.MINUTES )
                            .softValues()
                            .build();
    }

    protected DocRef<T> cache( final DocRef<T> dr )
    {
        final DocRef<T> existing = cache.getIfPresent( dr.getCacheKey() );
        if ( existing == null )
        {
            cache.put( dr.getCacheKey(), dr );
            return dr;
        }
        else
        {
            return existing;
        }
    }

    protected DocRef<T> getFirstCached( final T ref, final List<? extends Location> locations )
    {
        for ( final Location location : locations )
        {
            final DocCacheKey<ProjectRef> key = new DocCacheKey<ProjectRef>( ref, location );
            final DocRef<T> dr = cache.getIfPresent( key );
            if ( dr != null )
            {
                return dr;
            }
        }

        return null;
    }

    protected Map<Location, DocRef<T>> getAllCached( final T ref, final List<? extends Location> locations )
    {
        final Map<Location, DocRef<T>> result = new HashMap<Location, DocRef<T>>();
        for ( final Location location : locations )
        {
            final DocCacheKey<ProjectRef> key = new DocCacheKey<ProjectRef>( ref, location );
            final DocRef<T> dr = cache.getIfPresent( key );
            if ( dr != null )
            {
                result.put( location, dr );
            }
        }

        return result;
    }
}
