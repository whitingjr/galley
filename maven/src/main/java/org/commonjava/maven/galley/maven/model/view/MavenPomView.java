package org.commonjava.maven.galley.maven.model.view;

import static org.commonjava.maven.galley.maven.model.view.XPathConstants.V;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.maven.parse.DocRef;

public class MavenPomView
    extends MavenXmlView<ProjectVersionRef>
{

    private final ProjectVersionRef versionedRef;

    private final MavenPluginDefaults pluginDefaults;

    private final MavenPluginImplications pluginImplications;

    public MavenPomView( final ProjectVersionRef ref, final List<DocRef<ProjectVersionRef>> stack, final MavenPluginDefaults pluginDefaults,
                         final MavenPluginImplications pluginImplications )
    {
        // define what xpaths are not inheritable...
        super( stack, "/project/parent", "/project/artifactId" );
        this.pluginImplications = pluginImplications;

        if ( stack.isEmpty() )
        {
            throw new IllegalArgumentException( "Cannot create a POM view with no POMs!" );
        }

        this.versionedRef = ref;
        this.pluginDefaults = pluginDefaults;
    }

    @Override
    public String resolveMavenExpression( final String expression, final String... activeProfileIds )
        throws GalleyMavenException
    {
        String expr = expression;
        if ( expr.startsWith( "pom." ) )
        {
            expr = "project." + expr.substring( 4 );
        }

        return super.resolveMavenExpression( expr, activeProfileIds );
    }

    public synchronized ProjectVersionRef asProjectVersionRef()
    {
        return versionedRef;
    }

    public String getGroupId()
        throws GalleyMavenException
    {
        return asProjectVersionRef().getGroupId();
    }

    public String getArtifactId()
        throws GalleyMavenException
    {
        return asProjectVersionRef().getArtifactId();
    }

    public String getVersion()
        throws GalleyMavenException
    {
        return asProjectVersionRef().getVersionString();
    }

    public String getProfileIdFor( final NodeRef element )
    {
        return resolveXPathExpressionFrom( element, "ancestor::profile/id/text()" );
    }

    public List<DependencyView> getAllDirectDependencies()
        throws GalleyMavenException
    {
        final List<NodeRef> depNodes =
            resolveXPathToAggregatedNodeList( "/project/dependencies/dependency|/project/profiles/profile/dependencies/dependency", -1 );
        final List<DependencyView> depViews = new ArrayList<>( depNodes.size() );
        for ( final NodeRef node : depNodes )
        {
            depViews.add( new DependencyView( this, node ) );
        }

        return depViews;
    }

    public List<DependencyView> getAllManagedDependencies()
        throws GalleyMavenException
    {
        final List<NodeRef> depNodes =
            resolveXPathToAggregatedNodeList( "/project/dependencyManagement/dependencies/dependency|"
                + "/project/profiles/profile/dependencyManagement/dependencies/dependency", -1 );
        final List<DependencyView> depViews = new ArrayList<>( depNodes.size() );
        for ( final NodeRef node : depNodes )
        {
            depViews.add( new DependencyView( this, node ) );
        }

        return depViews;
    }

    public List<DependencyView> getAllBOMs()
        throws GalleyMavenException
    {
        final List<NodeRef> depNodes =
            resolveXPathToAggregatedNodeList( "/project/dependencyManagement/dependencies/dependency[type/text()=\"pom\" and scope/text()=\"import\"]|"
                                                  + "/project/profiles/profile/dependencyManagement/dependencies/dependency[type/text()=\"pom\" and scope/text()=\"import\"]",
                                              -1 );

        final List<DependencyView> depViews = new ArrayList<>( depNodes.size() );
        for ( final NodeRef node : depNodes )
        {
            depViews.add( new DependencyView( this, node ) );
        }

        return depViews;
    }

    // TODO: Do these methods need to be here??

    public String resolveXPathExpression( final String path, final boolean localOnly )
        throws GalleyMavenException
    {
        final String value = resolveXPathExpression( path, true, localOnly ? 0 : -1 );
        return value;
    }

    @Deprecated
    public NodeRef resolveXPathToElement( final String path, final boolean localOnly )
        throws GalleyMavenException
    {
        return resolveXPathToNode( path, localOnly );
    }

    public List<NodeRef> resolveXPathToElements( final String path, final boolean localOnly )
        throws GalleyMavenException
    {
        return resolveXPathToAggregatedNodeList( path, localOnly ? 0 : -1 );
    }

    public synchronized NodeRef resolveXPathToNode( final String path, final boolean localOnly )
        throws GalleyMavenException
    {
        final NodeRef node = resolveXPathToNode( path, localOnly ? 0 : -1 );
        return node;
    }

    public DependencyView asDependency( final NodeRef depElement )
    {
        return new DependencyView( this, depElement );
    }

    public PluginView asPlugin( final NodeRef element )
    {
        return new PluginView( this, element, pluginDefaults, pluginImplications );
    }

    public ParentView getParent()
        throws GalleyMavenException
    {
        final NodeRef parentEl = resolveXPathToNode( "/project/parent", true );

        if ( parentEl != null )
        {
            return new ParentView( this, parentEl );
        }

        return null;
    }

    public List<ExtensionView> getBuildExtensions()
        throws GalleyMavenException
    {
        final List<NodeRef> list =
            resolveXPathToAggregatedNodeList( "/project/build/extensions/extension|" + "/project/profiles/profile/build/extensions/extension", -1 );
        final List<ExtensionView> result = new ArrayList<>( list.size() );
        for ( final NodeRef node : list )
        {
            if ( node == null )
            {
                continue;
            }

            result.add( new ExtensionView( this, node ) );
        }

        return result;
    }

    public List<PluginView> getAllPluginsMatching( final String path )
        throws GalleyMavenException
    {
        final List<NodeRef> list = resolveXPathToAggregatedNodeList( path, -1 );
        final List<PluginView> result = new ArrayList<>( list.size() );
        for ( final NodeRef node : list )
        {
            if ( node == null )
            {
                continue;
            }

            result.add( new PluginView( this, node, pluginDefaults, pluginImplications ) );
        }

        return result;
    }

    public List<DependencyView> getAllDependenciesMatching( final String path )
        throws GalleyMavenException
    {
        final List<NodeRef> list = resolveXPathToAggregatedNodeList( path, -1 );
        final List<DependencyView> result = new ArrayList<>( list.size() );
        for ( final NodeRef node : list )
        {
            if ( node == null )
            {
                continue;
            }

            result.add( new DependencyView( this, node ) );
        }

        return result;
    }

    public List<PluginView> getAllBuildPlugins()
        throws GalleyMavenException
    {
        final List<NodeRef> list =
            resolveXPathToAggregatedNodeList( "/project/build/plugins/plugin|" + "/project/profiles/profile/build/plugins/plugin", -1 );
        final List<PluginView> result = new ArrayList<>( list.size() );
        for ( final NodeRef node : list )
        {
            if ( node == null )
            {
                continue;
            }

            result.add( new PluginView( this, node, pluginDefaults, pluginImplications ) );
        }

        return result;
    }

    public List<PluginView> getAllManagedBuildPlugins()
        throws GalleyMavenException
    {
        final List<NodeRef> list =
            resolveXPathToAggregatedNodeList( "/project/build/pluginManagement/plugins/plugin|"
                + "/project/profiles/profile/build/pluginManagement/plugins/plugin", -1 );
        final List<PluginView> result = new ArrayList<>( list.size() );
        for ( final NodeRef node : list )
        {
            if ( node == null )
            {
                continue;
            }

            result.add( new PluginView( this, node, pluginDefaults, pluginImplications ) );
        }

        return result;
    }

    public List<ProjectVersionRefView> getProjectVersionRefs( final String path )
        throws GalleyMavenException
    {
        final List<NodeRef> list = resolveXPathToAggregatedNodeList( path, -1 );
        final List<ProjectVersionRefView> result = new ArrayList<>( list.size() );
        for ( final NodeRef node : list )
        {
            if ( node == null )
            {
                continue;
            }

            result.add( new MavenGAVView( this, node ) );
        }

        return result;
    }

    public List<ProjectRefView> getProjectRefs( final String path )
        throws GalleyMavenException
    {
        final List<NodeRef> list = resolveXPathToAggregatedNodeList( path, -1 );
        final List<ProjectRefView> result = new ArrayList<>( list.size() );
        for ( final NodeRef node : list )
        {
            if ( node == null )
            {
                continue;
            }

            final NodeRef vNode = resolveXPathToNodeFrom( node, V );
            if ( vNode != null )
            {
                result.add( new MavenGAVView( this, node ) );
            }
            else
            {
                result.add( new MavenGAView( this, node ) );
            }
        }

        return result;
    }

}
