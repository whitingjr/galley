package org.commonjava.maven.galley.io;

import org.commonjava.maven.galley.GalleyException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.FilePatternMatcher;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.PathPatternMatcher;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.SpecialPathMatcher;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jdcasey on 1/27/16.
 */
@ApplicationScoped
public class SpecialPathManagerImpl
        implements SpecialPathManager
{
    private ConcurrentHashMap<SpecialPathMatcher, SpecialPathInfo> specialPaths;

    public SpecialPathManagerImpl()
    {
        specialPaths = new ConcurrentHashMap<SpecialPathMatcher, SpecialPathInfo>();
        specialPaths.putAll( SpecialPathConstants.STANDARD_SPECIAL_PATHS );
    }

    @Override
    public void registerSpecialPathInfo( SpecialPathInfo pathInfo )
    {
        specialPaths.put( pathInfo.getMatcher(), pathInfo );
    }

    @Override
    public void deregisterSpecialPathInfo( SpecialPathInfo pathInfo )
    {
        specialPaths.remove( pathInfo.getMatcher() );
    }

    @Override
    public void deregisterSpecialPathInfo( SpecialPathMatcher pathMatcher )
    {
        specialPaths.remove( pathMatcher );
    }

    @Override
    public SpecialPathInfo getSpecialPathInfo( ConcreteResource resource )
    {
        if ( resource != null )
        {
            return getSpecialPathInfo( resource.getLocation(), resource.getPath() );
        }

        return null;
    }

    @Override
    public SpecialPathInfo getSpecialPathInfo( Transfer transfer )
    {
        if ( transfer != null )
        {
            return getSpecialPathInfo( transfer.getLocation(), transfer.getPath() );
        }

        return null;
    }

    @Override
    public SpecialPathInfo getSpecialPathInfo( Location location, String path )
    {
        if ( location != null && path != null )
        {
            for ( Map.Entry<SpecialPathMatcher, SpecialPathInfo> entry : specialPaths.entrySet() )
            {
                if ( entry.getKey().matches( location, path ) )
                {
                    return entry.getValue();
                }
            }
        }

        return null;
    }
}
