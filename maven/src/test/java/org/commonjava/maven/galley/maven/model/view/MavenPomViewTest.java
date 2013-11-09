package org.commonjava.maven.galley.maven.model.view;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Set;

import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.junit.Test;

public class MavenPomViewTest
    extends AbstractMavenViewTest
{

    @Override
    protected String getBaseResource()
    {
        return "view/pom/";
    }

    @Test
    public void dependencyLikeStructuresInDirectPluginConfig()
        throws Exception
    {
        final String depArtifactItemsPath =
            "/project/build//plugin[artifactId/text()=\"maven-dependency-plugin\"]//artifactItem|"
                + "/project/profiles/profile/build//plugin[artifactId/text()=\"maven-dependency-plugin\"]//artifactItem";

        final MavenPomView pomView = loadPoms( "pom-with-plugin-deplike.xml" );

        final List<DependencyView> dvs = pomView.getAllDependenciesMatching( depArtifactItemsPath );
        assertThat( dvs, notNullValue() );
        assertThat( dvs.size(), equalTo( 1 ) );

        final DependencyView dv = dvs.get( 0 );

        assertThat( dv.getGroupId(), equalTo( "org.foo" ) );
        assertThat( dv.getArtifactId(), equalTo( "bar" ) );
        assertThat( dv.getVersion(), equalTo( "1" ) );
    }

    @Test
    public void dependencyLikeStructuresInProfileDirectPluginConfig()
        throws Exception
    {
        final String depArtifactItemsPath =
            "/project/build//plugin[artifactId/text()=\"maven-dependency-plugin\"]//artifactItem|"
                + "/project/profiles/profile/build//plugin[artifactId/text()=\"maven-dependency-plugin\"]//artifactItem";

        final MavenPomView pomView = loadPoms( "pom-with-profile-plugin-deplike.xml" );

        final List<DependencyView> dvs = pomView.getAllDependenciesMatching( depArtifactItemsPath );
        assertThat( dvs, notNullValue() );
        assertThat( dvs.size(), equalTo( 1 ) );

        final DependencyView dv = dvs.get( 0 );

        assertThat( dv.getGroupId(), equalTo( "org.foo" ) );
        assertThat( dv.getArtifactId(), equalTo( "bar" ) );
        assertThat( dv.getVersion(), equalTo( "1" ) );
    }

    @Test
    public void dependencyLikeStructuresInManagedPluginConfig()
        throws Exception
    {
        final String depArtifactItemsPath =
            "/project/build//plugin[artifactId/text()=\"maven-dependency-plugin\"]//artifactItem|"
                + "/project/profiles/profile/build//plugin[artifactId/text()=\"maven-dependency-plugin\"]//artifactItem";

        final MavenPomView pomView = loadPoms( "pom-with-pluginMgmt-deplike.xml" );

        final List<DependencyView> dvs = pomView.getAllDependenciesMatching( depArtifactItemsPath );
        assertThat( dvs, notNullValue() );
        assertThat( dvs.size(), equalTo( 1 ) );

        final DependencyView dv = dvs.get( 0 );

        assertThat( dv.getGroupId(), equalTo( "org.foo" ) );
        assertThat( dv.getArtifactId(), equalTo( "bar" ) );
        assertThat( dv.getVersion(), equalTo( "1" ) );
    }

    @Test
    public void dependencyLikeStructuresInProfileManagedPluginConfig()
        throws Exception
    {
        final String depArtifactItemsPath =
            "/project/build//plugin[artifactId/text()=\"maven-dependency-plugin\"]//artifactItem|"
                + "/project/profiles/profile/build//plugin[artifactId/text()=\"maven-dependency-plugin\"]//artifactItem";

        final MavenPomView pomView = loadPoms( "pom-with-profile-pluginMgmt-deplike.xml" );

        final List<DependencyView> dvs = pomView.getAllDependenciesMatching( depArtifactItemsPath );
        assertThat( dvs, notNullValue() );
        assertThat( dvs.size(), equalTo( 1 ) );

        final DependencyView dv = dvs.get( 0 );

        assertThat( dv.getGroupId(), equalTo( "org.foo" ) );
        assertThat( dv.getArtifactId(), equalTo( "bar" ) );
        assertThat( dv.getVersion(), equalTo( "1" ) );
    }

    @Test
    public void dependencyManagedBySingleBOM()
        throws Exception
    {
        final MavenPomView pomView = loadPoms( "pom-with-bom.xml" );
        final MavenPomView bomView = loadPoms( "simple-bom.xml" );

        pomView.addMixin( new MavenXmlMixin<ProjectVersionRef>( bomView, MavenXmlMixin.DEPENDENCY_MIXIN ) );

        final DependencyView dv = pomView.getAllDirectDependencies()
                                         .get( 0 );

        assertThat( dv.getVersion(), equalTo( "1.0" ) );
        assertThat( dv.getScope(), equalTo( DependencyScope.test ) );
    }

    @Test
    public void resolveParentVersionExpressionWithoutProjectPrefix()
        throws Exception
    {
        final MavenPomView pomView = loadPoms( "pom-with-parent-expr.xml"/*, "simple-parent-pom.xml"*/);

        final String value = pomView.resolveExpressions( "${parent.version}" );

        assertThat( value, equalTo( "1.0.0.0" ) );
    }

    @Test
    public void resolveExpressionWithDeprecatedPomDotPrefix()
        throws Exception
    {
        final MavenPomView pomView = loadPoms( "pom-with-parent-expr.xml"/*, "simple-parent-pom.xml"*/);

        final String value = pomView.resolveExpressions( "${pom.parent.version}" );

        assertThat( value, equalTo( "1.0.0.0" ) );
    }

    @Test
    public void parentPathIsLocalOnly()
        throws Exception
    {
        final MavenPomView pomView = loadPoms( "pom-with-broken-parent.xml", "pom-with-parent.xml", "simple-parent-pom.xml" );

        final String value = pomView.resolveExpressions( "${parent.version}" );

        assertThat( pomView.containsExpression( value ), equalTo( true ) );
    }

    @Test
    public void groupIdFailOverToParent()
        throws Exception
    {
        final MavenPomView pomView = loadPoms( "pom-with-parent-groupId.xml", "simple-parent-pom.xml" );

        final String gid = pomView.getGroupId();
        final ProjectVersionRef pvr = pomView.asProjectVersionRef();

        assertThat( gid, equalTo( "org.foo" ) );
        assertThat( pvr.getGroupId(), equalTo( "org.foo" ) );
    }

    @Test( expected = GalleyMavenException.class )
    public void artifactId_DOES_NOT_FailOverToParent()
        throws Exception
    {
        final MavenPomView pomView = loadPoms( "pom-with-broken-artifactId.xml", "pom-with-parent.xml", "simple-parent-pom.xml" );

        final String aid = pomView.getArtifactId();

        assertThat( aid, nullValue() );

        pomView.asProjectVersionRef();
    }

    @Test
    public void retrieveDirectBOMReference()
        throws Exception
    {
        final MavenPomView pomView = loadPoms( "pom-with-bom.xml" );
        final List<DependencyView> boms = pomView.getAllBOMs();

        assertThat( boms, notNullValue() );
        assertThat( boms.size(), equalTo( 1 ) );
    }

    @Test
    public void retrieveBOMReferenceInParent()
        throws Exception
    {
        final MavenPomView pomView = loadPoms( "pom-with-bom-child.xml", "pom-with-bom.xml" );
        final List<DependencyView> boms = pomView.getAllBOMs();

        assertThat( boms, notNullValue() );
        assertThat( boms.size(), equalTo( 1 ) );
    }

    @Test
    public void retrieveImpliedPluginDepsForSurefire()
        throws Exception
    {
        final MavenPomView pomView = loadPoms( "pom-with-surefire.xml" );

        final List<PluginView> plugins = pomView.getAllBuildPlugins();
        assertThat( plugins, notNullValue() );
        assertThat( plugins.size(), equalTo( 1 ) );

        final PluginView pv = plugins.get( 0 );
        assertThat( pv, notNullValue() );

        final Set<PluginDependencyView> ipdvs = pv.getImpliedPluginDependencies();
        assertThat( ipdvs, notNullValue() );
        assertThat( ipdvs.size(), equalTo( 5 ) );

    }

}
