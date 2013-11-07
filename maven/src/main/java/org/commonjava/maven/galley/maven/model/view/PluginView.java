package org.commonjava.maven.galley.maven.model.view;

import static org.commonjava.maven.galley.maven.model.view.XPathConstants.A;
import static org.commonjava.maven.galley.maven.model.view.XPathConstants.AND;
import static org.commonjava.maven.galley.maven.model.view.XPathConstants.G;
import static org.commonjava.maven.galley.maven.model.view.XPathConstants.QUOTE;
import static org.commonjava.maven.galley.maven.model.view.XPathConstants.TEXTEQ;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.defaults.MavenPluginImplications;

public class PluginView
    extends MavenGAVView
{

    private final MavenPluginDefaults pluginDefaults;

    private List<PluginDependencyView> pluginDependencies;

    private final MavenPluginImplications pluginImplications;

    protected PluginView( final MavenPomView pomView, final NodeRef element, final MavenPluginDefaults pluginDefaults,
                          final MavenPluginImplications pluginImplications )
    {
        super( pomView, element, "build/pluginManagement/plugins/plugin" );
        this.pluginDefaults = pluginDefaults;
        this.pluginImplications = pluginImplications;
    }

    public boolean isManaged()
        throws GalleyMavenException
    {
        return pomView.resolveXPathToNodeFrom( element, "ancestor::pluginManagement" ) != null;
    }

    public synchronized List<PluginDependencyView> getLocalPluginDependencies()
        throws GalleyMavenException
    {
        if ( pluginDependencies == null )
        {
            final List<PluginDependencyView> result = new ArrayList<>();

            final List<NodeRef> nodes = getFirstNodesWithManagement( "dependencies/dependency" );
            if ( nodes != null )
            {
                for ( final NodeRef node : nodes )
                {
                    result.add( new PluginDependencyView( pomView, this, node ) );
                }

                this.pluginDependencies = result;
            }
        }

        return pluginDependencies;
    }

    public Set<PluginDependencyView> getImpliedPluginDependencies()
        throws GalleyMavenException
    {
        return pluginImplications.getImpliedPluginDependencies( this );
    }

    @Override
    public synchronized String getVersion()
    {
        if ( super.getVersion() == null )
        {
            setVersion( pluginDefaults.getDefaultVersion( getGroupId(), getArtifactId() ) );
        }

        return super.getVersion();
    }

    @Override
    public synchronized String getGroupId()
    {
        final String gid = super.getGroupId();
        if ( gid == null )
        {
            setGroupId( pluginDefaults.getDefaultGroupId( getArtifactId() ) );
        }

        return super.getGroupId();
    }

    @Override
    protected String getManagedViewQualifierFragment()
    {
        final StringBuilder sb = new StringBuilder();

        final String aid = getArtifactId();
        final String gid = getGroupId();
        final String dgid = pluginDefaults.getDefaultGroupId( aid );
        if ( !gid.equals( dgid ) )
        {
            sb.append( G )
              .append( TEXTEQ )
              .append( gid )
              .append( QUOTE )
              .append( AND );
        }

        sb.append( A )
          .append( TEXTEQ )
          .append( aid )
          .append( QUOTE );

        return sb.toString();
    }

}
