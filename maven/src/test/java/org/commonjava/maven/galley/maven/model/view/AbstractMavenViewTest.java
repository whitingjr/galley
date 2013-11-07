package org.commonjava.maven.galley.maven.model.view;

import static org.commonjava.maven.galley.maven.parse.XMLInfrastructure.parse;
import static org.commonjava.maven.galley.maven.parse.XMLInfrastructure.toXML;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.defaults.StandardMaven304PluginDefaults;
import org.commonjava.maven.galley.maven.defaults.StandardMavenPluginImplications;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.util.logging.Log4jUtil;
import org.commonjava.util.logging.Logger;
import org.junit.BeforeClass;

import com.ximpleware.VTDNav;

public abstract class AbstractMavenViewTest
{

    protected final Logger logger = new Logger( getClass() );

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    protected DependencyView loadFirstDirectDependency( final String... pomNames )
        throws Exception
    {
        final MavenPomView mpv = loadPoms( pomNames );

        return mpv.getAllDirectDependencies()
                  .get( 0 );
    }

    protected DependencyView loadFirstManagedDependency( final String... pomNames )
        throws Exception
    {
        final MavenPomView mpv = loadPoms( pomNames );

        return mpv.getAllManagedDependencies()
                  .get( 0 );
    }

    protected MavenPomView loadPoms( final String... pomNames )
        throws Exception
    {
        final List<DocRef<ProjectVersionRef>> stack = new ArrayList<>();
        final ProjectVersionRef pvr = new ProjectVersionRef( "not.used", "project-ref", "1.0" );
        for ( final String pomName : pomNames )
        {
            final InputStream is = Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream( getBaseResource() + pomName );

            final VTDNav document = parse( getBaseResource() + pomName, is );

            final DocRef<ProjectVersionRef> dr = new DocRef<ProjectVersionRef>( pvr, new SimpleLocation( "http://localhost:8080/" ), document );

            stack.add( dr );
        }

        // FIXME: The use of pvr here is probably going to lead to problems.
        return new MavenPomView( pvr, stack, new StandardMaven304PluginDefaults(), new StandardMavenPluginImplications() );
    }

    protected MavenXmlView<ProjectRef> loadDocs( final Set<String> localOnlyPaths, final String... docNames )
        throws Exception
    {
        final List<DocRef<ProjectRef>> stack = new ArrayList<>();
        final ProjectRef pr = new ProjectRef( "not.used", "project-ref" );
        for ( final String pomName : docNames )
        {
            final InputStream is = Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream( getBaseResource() + pomName );

            final VTDNav document = parse( getBaseResource() + pomName, is );

            final DocRef<ProjectRef> dr = new DocRef<ProjectRef>( pr, new SimpleLocation( "http://localhost:8080/" ), document );

            stack.add( dr );
        }

        return new MavenXmlView<ProjectRef>( stack, localOnlyPaths );
    }

    protected void dump( final NodeRef node )
        throws Exception
    {
        if ( node == null )
        {
            logger.error( "Cannot dump null node." );
            return;
        }

        logger.info( toXML( node ) );
    }

    protected abstract String getBaseResource();

}
