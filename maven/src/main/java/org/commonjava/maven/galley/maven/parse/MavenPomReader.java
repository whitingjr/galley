package org.commonjava.maven.galley.maven.parse;

import static org.commonjava.maven.galley.maven.parse.XMLInfrastructure.getParentRef;
import static org.commonjava.maven.galley.maven.parse.XMLInfrastructure.parse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.MavenXmlMixin;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

import com.ximpleware.VTDNav;

@ApplicationScoped
public class MavenPomReader
    extends AbstractMavenXmlReader<ProjectVersionRef>
{

    @Inject
    private ArtifactManager artifacts;

    private final Logger logger = new Logger( getClass() );

    @Inject
    private MavenPluginDefaults pluginDefaults;

    @Inject
    private MavenPluginImplications pluginImplications;

    public MavenPomReader( final ArtifactManager artifactManager, final MavenPluginDefaults pluginDefaults,
                           final MavenPluginImplications pluginImplications )
    {
        this.artifacts = artifactManager;
        this.pluginDefaults = pluginDefaults;
        this.pluginImplications = pluginImplications;
    }

    protected MavenPomReader()
    {
    }

    public void logStructure( final MavenPomView view )
    {
        logger.info( printStructure( view ) );
    }

    public MavenPomView read( final ProjectVersionRef ref, final List<? extends Location> locations )
        throws GalleyMavenException
    {
        return read( ref, locations, false );
    }

    public MavenPomView read( final ProjectVersionRef ref, final List<? extends Location> locations, final boolean cache )
        throws GalleyMavenException
    {
        final List<DocRef<ProjectVersionRef>> stack = new ArrayList<>();

        ProjectVersionRef next = ref;
        do
        {
            DocRef<ProjectVersionRef> dr;
            try
            {
                // always cache parents.
                dr = getDocRef( next, locations, cache || next != ref );
            }
            catch ( final TransferException e )
            {
                throw new GalleyMavenException( "Failed to retrieve POM for: %s, %d levels deep in ancestry stack of: %s. Reason: %s", e, next,
                                                stack.size(), ref, e.getMessage() );
            }

            if ( dr == null )
            {
                throw new GalleyMavenException( "Cannot resolve %s, %d levels dep in the ancestry stack of: %s", next, stack.size(), ref );
            }

            stack.add( dr );

            next = getParentRef( dr.getDoc(), dr );
        }
        while ( next != null );

        final MavenPomView view = new MavenPomView( ref, stack, pluginDefaults, pluginImplications );
        assembleImportedInformation( view, locations );

        logStructure( view );

        return view;
    }

    public MavenPomView read( final ProjectVersionRef ref, final Transfer pom, final List<? extends Location> locations )
        throws GalleyMavenException
    {
        final List<DocRef<ProjectVersionRef>> stack = new ArrayList<>();

        DocRef<ProjectVersionRef> dr = getDocRef( ref, pom, locations, false );
        stack.add( dr );

        ProjectVersionRef next = getParentRef( dr.getDoc(), pom );

        while ( next != null && dr != null )
        {
            try
            {
                dr = getDocRef( next, locations, true );
            }
            catch ( final TransferException e )
            {
                throw new GalleyMavenException( "Failed to retrieve POM for: %s, %d levels deep in ancestry stack of: %s. Reason: %s", e, next,
                                                stack.size(), ref, e.getMessage() );
            }

            if ( dr == null )
            {
                throw new GalleyMavenException( "Cannot resolve %s, %d levels dep in the ancestry stack of: %s", next, stack.size(), ref );
            }

            stack.add( dr );

            next = getParentRef( dr.getDoc(), dr );
        }

        final MavenPomView view = new MavenPomView( ref, stack, pluginDefaults, pluginImplications );
        assembleImportedInformation( view, locations );

        //        logStructure( view );

        return view;
    }

    private void assembleImportedInformation( final MavenPomView view, final List<? extends Location> locations )
        throws GalleyMavenException
    {
        final List<DependencyView> md = view.getAllManagedDependencies();
        for ( final DependencyView dv : md )
        {
            if ( DependencyScope._import == dv.getScope() && "pom".equals( dv.getType() ) )
            {
                final ProjectVersionRef ref = dv.asProjectVersionRef();
                logger.info( "Found BOM: %s for: %s", ref, view.getRef() );

                // This is a BOM, it's likely to be used in multiple locations...cache this.
                final MavenPomView imp = read( ref, locations, true );

                view.addMixin( new MavenXmlMixin<ProjectVersionRef>( imp, MavenXmlMixin.DEPENDENCY_MIXIN ) );
            }
        }
    }

    private DocRef<ProjectVersionRef> getDocRef( final ProjectVersionRef ref, final List<? extends Location> locations, final boolean cache )
        throws TransferException, GalleyMavenException
    {
        DocRef<ProjectVersionRef> dr = getFirstCached( ref, locations );
        if ( dr == null )
        {
            Transfer transfer = null;
            transfer = artifacts.retrieveFirst( locations, ref.asPomArtifact() );

            if ( transfer == null )
            {
                return null;
            }

            final VTDNav doc = parse( transfer );
            dr = new DocRef<ProjectVersionRef>( ref, transfer.getLocation(), doc );

            if ( cache )
            {
                dr = cache( dr );
            }
        }

        return dr;
    }

    private DocRef<ProjectVersionRef> getDocRef( final ProjectVersionRef ref, final Transfer pom, final List<? extends Location> locations,
                                                 final boolean cache )
        throws GalleyMavenException
    {
        final Transfer transfer = pom;

        DocRef<ProjectVersionRef> dr = getFirstCached( ref, Arrays.asList( pom.getLocation() ) );

        if ( dr == null )
        {
            final VTDNav doc = parse( transfer );
            dr = new DocRef<ProjectVersionRef>( ref, transfer.getLocation(), doc );

            if ( cache )
            {
                dr = cache( dr );
            }
        }

        return dr;
    }

    private String printStructure( final MavenPomView view )
    {
        final StringBuilder sb = new StringBuilder();

        final List<DocRef<ProjectVersionRef>> stack = view.getDocRefStack();
        final List<MavenXmlMixin<ProjectVersionRef>> mixins = view.getMixins();

        sb.append( "\n\n" )
          .append( view.getRef() )
          .append( " consists of:\n  " );

        int i = 0;
        for ( final DocRef<ProjectVersionRef> docref : stack )
        {
            sb.append( "\n  D" )
              .append( i++ )
              .append( docref );
        }

        sb.append( "\n\n" );

        if ( mixins != null && !mixins.isEmpty() )
        {
            sb.append( mixins.size() )
              .append( " Mix-ins for " )
              .append( view.getRef() )
              .append( ":\n\n" );

            i = 0;
            for ( final MavenXmlMixin<ProjectVersionRef> mixin : mixins )
            {
                sb.append( 'M' )
                  .append( i++ )
                  .append( mixin )
                  .append( "\n    " );
                sb.append( printStructure( (MavenPomView) mixin.getMixin() ) );
            }

            sb.append( "\n\n" );
        }

        return sb.toString();
    }

}
