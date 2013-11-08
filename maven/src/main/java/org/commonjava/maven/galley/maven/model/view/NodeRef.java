package org.commonjava.maven.galley.maven.model.view;

import org.commonjava.maven.galley.maven.GalleyMavenRuntimeException;

import com.ximpleware.NavException;
import com.ximpleware.VTDNav;

public class NodeRef
{
    private VTDNav nav;

    private final int idx;

    public NodeRef( final VTDNav nav, final int idx )
    {
        this.nav = nav;
        this.idx = idx;
    }

    public VTDNav getNav()
    {
        return nav;
    }

    public int getIdx()
    {
        return idx;
    }

    public void updateNav( final NodeRef nodeRef )
    {
        try
        {
            this.nav = nodeRef.getNav()
                              .cloneNav();
            this.nav.recoverNode( idx );
        }
        catch ( final NavException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to recover to node index after navigation update: %s", e, e.getMessage() );
        }
    }

}
