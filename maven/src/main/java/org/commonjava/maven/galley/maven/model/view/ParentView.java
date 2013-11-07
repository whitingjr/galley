package org.commonjava.maven.galley.maven.model.view;

import org.commonjava.maven.galley.maven.GalleyMavenException;

public class ParentView
    extends MavenGAVView
{

    public ParentView( final MavenPomView pomView, final NodeRef element )
    {
        super( pomView, element );
    }

    public String getRelativePath()
        throws GalleyMavenException
    {
        String val = getValue( "relativePath" );
        if ( val == null )
        {
            val = "../pom.xml";
        }

        return val;
    }

}
