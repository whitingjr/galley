package org.commonjava.maven.galley.maven.defaults;

import static org.commonjava.maven.galley.maven.model.view.XPathConstants.A;
import static org.commonjava.maven.galley.maven.model.view.XPathConstants.G;
import static org.commonjava.maven.galley.maven.model.view.XPathConstants.V;
import static org.commonjava.maven.galley.maven.parse.XMLInfrastructure.createElement;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.NodeRef;
import org.commonjava.maven.galley.maven.model.view.PluginDependencyView;
import org.commonjava.maven.galley.maven.model.view.PluginView;

public abstract class AbstractMavenPluginImplications
    implements MavenPluginImplications
{

    // TODO: Streamline this by batching creation of multiple plugin deps.
    protected PluginDependencyView createPluginDependency( final PluginView pv, final ProjectRef ref )
    {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put( G, ref.getGroupId() );
        map.put( A, ref.getArtifactId() );
        map.put( V, pv.getVersion() );

        final NodeRef element = pv.getElement();
        final NodeRef nr = createElement( element, "dependencies/dependency", map );
        return new PluginDependencyView( pv.getPomView(), pv, nr );
    }

    @Override
    public Set<PluginDependencyView> getImpliedPluginDependencies( final PluginView pv )
        throws GalleyMavenException
    {
        final Map<ProjectRef, Set<ProjectRef>> impliedDepMap = getImpliedRefMap();
        final ProjectRef ref = pv.asProjectRef();
        final Set<ProjectRef> implied = impliedDepMap.get( ref );

        if ( implied == null || implied.isEmpty() )
        {
            return null;
        }

        final Set<PluginDependencyView> views = new HashSet<>( implied.size() );
        for ( final ProjectRef impliedRef : implied )
        {
            final PluginDependencyView pd = createPluginDependency( pv, impliedRef );
            pv.getElement()
              .updateNav( pd.getElement() );

            views.add( pd );
        }

        return views;
    }

    protected abstract Map<ProjectRef, Set<ProjectRef>> getImpliedRefMap();

}
