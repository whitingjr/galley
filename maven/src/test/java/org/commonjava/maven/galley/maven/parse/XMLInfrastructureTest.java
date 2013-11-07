package org.commonjava.maven.galley.maven.parse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ximpleware.VTDNav;

public class XMLInfrastructureTest
{

    @BeforeClass
    public static void startLogging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    protected String getBaseResource()
    {
        return "xml/";
    }

    @Test
    public void extractSimpleProjectVersionRef()
        throws Exception
    {
        final String xml =
            "<project><modelVersion>4.0.0</modelVersion><groupId>org.test</groupId><artifactId>test</artifactId><version>1</version></project>";
        final VTDNav doc = XMLInfrastructure.parse( xml, new ByteArrayInputStream( xml.getBytes() ) );

        final ProjectVersionRef ref = XMLInfrastructure.getProjectVersionRef( doc, xml );

        assertThat( ref, notNullValue() );
        assertThat( ref.getGroupId(), equalTo( "org.test" ) );
        assertThat( ref.getArtifactId(), equalTo( "test" ) );
        assertThat( ref.getVersionString(), equalTo( "1" ) );
    }

    @Test
    public void extractSimpleParentRef()
        throws Exception
    {
        final String xml =
            "<project><modelVersion>4.0.0</modelVersion><parent><groupId>org.test</groupId><artifactId>test</artifactId><version>1</version></parent><artifactId>child</artifactId></project>";
        final VTDNav doc = XMLInfrastructure.parse( xml, new ByteArrayInputStream( xml.getBytes() ) );

        final ProjectVersionRef ref = XMLInfrastructure.getParentRef( doc, xml );

        assertThat( ref, notNullValue() );
        assertThat( ref.getGroupId(), equalTo( "org.test" ) );
        assertThat( ref.getArtifactId(), equalTo( "test" ) );
        assertThat( ref.getVersionString(), equalTo( "1" ) );
    }

    @Test
    public void projectVersionRefFailsOverToParent()
        throws Exception
    {
        final String xml =
            "<project><modelVersion>4.0.0</modelVersion><parent><groupId>org.test</groupId><artifactId>test</artifactId><version>1</version></parent><artifactId>child</artifactId></project>";
        final VTDNav doc = XMLInfrastructure.parse( xml, new ByteArrayInputStream( xml.getBytes() ) );

        final ProjectVersionRef ref = XMLInfrastructure.getProjectVersionRef( doc, xml );

        assertThat( ref, notNullValue() );
        assertThat( ref.getGroupId(), equalTo( "org.test" ) );
        assertThat( ref.getArtifactId(), equalTo( "child" ) );
        assertThat( ref.getVersionString(), equalTo( "1" ) );
    }

    @Test
    public void parsePOMWithUndeclaredEntity()
        throws Exception
    {
        // This is to handle the plexus POMs that have &oslash; in them.
        final VTDNav doc = loadDocument( "pom-with-undeclared-entity.xml" );

        assertThat( doc, notNullValue() );
    }

    private VTDNav loadDocument( final String resource )
        throws Exception
    {
        final InputStream stream = Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream( getBaseResource() + resource );

        return XMLInfrastructure.parse( resource, stream );
    }

}
