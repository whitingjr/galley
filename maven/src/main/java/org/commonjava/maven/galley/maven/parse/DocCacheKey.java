package org.commonjava.maven.galley.maven.parse;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.galley.model.Location;

public final class DocCacheKey<T extends ProjectRef>
{
    private final T ref;

    private final Location location;

    DocCacheKey( final T ref, final Location location )
    {
        this.ref = ref;
        this.location = location;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( location == null ) ? 0 : location.hashCode() );
        result = prime * result + ( ( ref == null ) ? 0 : ref.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        @SuppressWarnings( "unchecked" )
        final DocCacheKey<T> other = (DocCacheKey<T>) obj;
        if ( location == null )
        {
            if ( other.location != null )
            {
                return false;
            }
        }
        else if ( !location.equals( other.location ) )
        {
            return false;
        }
        if ( ref == null )
        {
            if ( other.ref != null )
            {
                return false;
            }
        }
        else if ( !ref.equals( other.ref ) )
        {
            return false;
        }
        return true;
    }
}