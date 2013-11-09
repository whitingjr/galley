package org.commonjava.maven.galley.maven.model.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.GalleyMavenRuntimeException;
import org.commonjava.maven.galley.maven.parse.DocRef;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.util.logging.Logger;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public class MavenXmlView<T extends ProjectRef>
{

    private static final String EXPRESSION_PATTERN = ".*\\$\\{.+\\}.*";

    private static final String AUTOPILOT = "autopilot";

    private final Logger logger = new Logger( getClass() );

    private final List<DocRef<T>> stack;

    private StringSearchInterpolator ssi;

    private final List<MavenXmlMixin<T>> mixins = new ArrayList<>();

    private final Set<String> localOnlyPaths;

    public MavenXmlView( final List<DocRef<T>> stack, final String... localOnlyPaths )
    {
        this.stack = stack;
        this.localOnlyPaths = new HashSet<>( Arrays.asList( localOnlyPaths ) );
    }

    public MavenXmlView( final List<DocRef<T>> stack, final Set<String> localOnlyPaths )
    {
        this.stack = stack;
        this.localOnlyPaths = localOnlyPaths;
    }

    public T getRef()
    {
        return stack.get( 0 )
                    .getRef();
    }

    public List<DocRef<T>> getDocRefStack()
    {
        return stack;
    }

    public String resolveMavenExpression( final String expression, final String... activeProfileIds )
        throws GalleyMavenException
    {
        String expr = expression.replace( '.', '/' );
        if ( !expr.startsWith( "/" ) )
        {
            expr = "/" + expr;
        }

        if ( !expr.startsWith( "/project" ) )
        {
            expr = "/project" + expr;
        }

        String value = resolveXPathExpression( expr, true, -1 );
        if ( value == null )
        {
            value = resolveXPathExpression( "//properties/" + expression, true, -1 );
        }

        for ( int i = 0; value == null && activeProfileIds != null && i < activeProfileIds.length; i++ )
        {
            final String profileId = activeProfileIds[i];
            value = resolveXPathExpression( "//profile[id/text()=\"" + profileId + "\"]/properties/" + expression, true, -1, activeProfileIds );
        }

        return value;
    }

    public String resolveXPathExpression( String path, final boolean cachePath, final int maxAncestry, final String... activeProfileIds )
    {
        if ( !path.endsWith( "/text()" ) )
        {
            path += "/text()";
        }

        NodeRef result = null;
        try
        {
            result = resolveXPathToNode( path, maxAncestry );
        }
        catch ( final GalleyMavenRuntimeException e )
        {
            // TODO: We don't want to spit this out, but is there another more appropriate action than ignoring it?
        }

        return resolveExpressions( result );
    }

    public List<String> resolveXPathExpressionToAggregatedList( String path, final int maxAncestry )
    {
        if ( !path.endsWith( "/text()" ) )
        {
            path += "/text()";
        }

        final List<NodeRef> nodes = resolveXPathToAggregatedNodeList( path, maxAncestry );
        final List<String> result = new ArrayList<>( nodes.size() );
        for ( final NodeRef node : nodes )
        {
            final String value = resolveExpressions( node );
            if ( value != null )
            {
                result.add( value );
            }
        }

        return result;
    }

    public NodeRef resolveXPathToNode( final String path, final int maxDepth )
        throws GalleyMavenRuntimeException
    {
        NodeRef result = null;
        try
        {
            int maxAncestry = maxDepth;
            for ( final String pathPrefix : localOnlyPaths )
            {
                if ( path.startsWith( pathPrefix ) )
                {
                    maxAncestry = 0;
                    break;
                }
            }

            int ancestryDepth = 0;
            for ( final DocRef<T> dr : stack )
            {
                if ( maxAncestry > -1 && ancestryDepth > maxAncestry )
                {
                    break;
                }

                final VTDNav doc = dr.getDoc()
                                     .cloneNav();

                final AutoPilot ap = getAutoPilot( doc );

                //                if ( path.startsWith( "/" ) )
                //                {
                //                    doc.toElement( VTDNav.ROOT );
                //                }

                ap.selectXPath( path );
                final int idx = ap.evalXPath();
                if ( idx > -1 )
                {
                    result = new NodeRef( doc, idx );
                }

                if ( result != null )
                {
                    break;
                }

                ancestryDepth++;
            }

            if ( result == null )
            {
                for ( final MavenXmlMixin<T> mixin : mixins )
                {
                    if ( mixin.matches( path ) )
                    {
                        result = mixin.getMixin()
                                      .resolveXPathToNode( path, maxAncestry );
                        //                        logger.info( "Value of '%s' in mixin: %s is: '%s'", path, mixin );
                    }

                    if ( result != null )
                    {
                        break;
                    }
                }
            }
        }
        catch ( final XPathParseException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to compile xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }
        catch ( XPathEvalException | NavException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to resolve contents of xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }

        return result;
    }

    public AutoPilot getAutoPilot( final VTDNav doc )
    {
        final DocRef<T> dr = getDocRefStack().get( 0 );

        AutoPilot ap;
        synchronized ( dr )
        {
            ap = dr.getAttribute( AUTOPILOT, AutoPilot.class );
            if ( ap == null )
            {
                ap = new AutoPilot();
                dr.setAttribute( AUTOPILOT, ap );
            }
        }

        ap.bind( doc );
        return ap;
    }

    public synchronized List<NodeRef> resolveXPathToAggregatedNodeList( final String path, final int maxDepth )
        throws GalleyMavenRuntimeException
    {
        try
        {
            int maxAncestry = maxDepth;
            for ( final String pathPrefix : localOnlyPaths )
            {
                if ( path.startsWith( pathPrefix ) )
                {
                    maxAncestry = 0;
                    break;
                }
            }

            int ancestryDepth = 0;
            final List<NodeRef> result = new ArrayList<>();
            for ( final DocRef<T> dr : stack )
            {
                if ( maxAncestry > -1 && ancestryDepth > maxAncestry )
                {
                    break;
                }

                final List<NodeRef> nodes = getLocalNodeList( path, dr );
                if ( nodes != null )
                {
                    for ( final NodeRef node : nodes )
                    {
                        result.add( node );
                    }
                }

                ancestryDepth++;
            }

            for ( final MavenXmlMixin<T> mixin : mixins )
            {
                if ( !mixin.matches( path ) )
                {
                    continue;
                }

                final List<NodeRef> nodes = mixin.getMixin()
                                                 .resolveXPathToAggregatedNodeList( path, maxAncestry );
                if ( nodes != null )
                {
                    for ( final NodeRef node : nodes )
                    {
                        result.add( node );
                    }
                }
            }

            return result;
        }
        //        catch ( final XPathExpressionException e )
        //        {
        //            throw new GalleyMavenRuntimeException( "Failed to retrieve content for xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        //        }
        finally
        {
        }
    }

    public synchronized List<NodeRef> resolveXPathToFirstNodeList( final String path, final int maxDepth )
        throws GalleyMavenRuntimeException
    {
        try
        {
            int maxAncestry = maxDepth;
            for ( final String pathPrefix : localOnlyPaths )
            {
                if ( path.startsWith( pathPrefix ) )
                {
                    maxAncestry = 0;
                    break;
                }
            }

            int ancestryDepth = 0;
            for ( final DocRef<T> dr : stack )
            {
                if ( maxAncestry > -1 && ancestryDepth > maxAncestry )
                {
                    break;
                }

                final List<NodeRef> result = getLocalNodeList( path, dr );
                if ( result != null )
                {
                    return result;
                }

                ancestryDepth++;
            }

            for ( final MavenXmlMixin<T> mixin : mixins )
            {
                if ( !mixin.matches( path ) )
                {
                    continue;
                }

                final List<NodeRef> result = mixin.getMixin()
                                                  .resolveXPathToFirstNodeList( path, maxAncestry );
                if ( result != null )
                {
                    return result;
                }
            }

            return null;
        }
        //        catch ( final XPathExpressionException e )
        //        {
        //            throw new GalleyMavenRuntimeException( "Failed to retrieve content for xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        //        }
        finally
        {
        }
    }

    private List<NodeRef> getLocalNodeList( final String path, final DocRef<T> dr )
        throws GalleyMavenRuntimeException
    {
        final List<NodeRef> result = new ArrayList<>();

        final VTDNav doc = dr.getDoc()
                             .cloneNav();

        try
        {
            //            if ( path.startsWith( "/" ) )
            //            {
            //                doc.toElement( VTDNav.ROOT );
            //            }

            final AutoPilot ap = getAutoPilot( doc );
            ap.selectXPath( path );

            int idx = -1;
            while ( ( idx = ap.evalXPath() ) > -1 )
            {
                result.add( new NodeRef( doc, idx ) );
            }
        }
        catch ( final XPathParseException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to parse xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }
        catch ( XPathEvalException | NavException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to retrieve content for xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }

        //        NodeList nl;
        //        try
        //        {
        //            nl = (NodeList) expression.evaluate( doc, XPathConstants.NODESET );
        //        }
        //        catch ( final XPathExpressionException e )
        //        {
        //            throw new GalleyMavenRuntimeException( "Failed to retrieve content for xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        //        }
        //
        //        if ( nl != null && nl.getLength() > 0 )
        //        {
        //            final List<Node> result = new ArrayList<>();
        //            for ( int i = 0; i < nl.getLength(); i++ )
        //            {
        //                result.add( nl.item( i ) );
        //            }
        //
        //            // we're not aggregating, so return the result.
        //            return result;
        //        }

        return result.isEmpty() ? null : result;
    }

    public String resolveXPathExpressionFrom( final NodeRef root, String path )
    {
        if ( !path.endsWith( "/text()" ) )
        {
            path += "/text()";
        }

        final NodeRef result = resolveXPathToNodeFrom( root, path );
        return resolveExpressions( result );
    }

    public List<String> resolveXPathExpressionToListFrom( final NodeRef root, String path )
        throws GalleyMavenException
    {
        if ( !path.endsWith( "/text()" ) )
        {
            path += "/text()";
        }

        final List<NodeRef> nodes = resolveXPathToNodeListFrom( root, path );
        final List<String> result = new ArrayList<>( nodes.size() );
        for ( final NodeRef node : nodes )
        {
            final String value = resolveExpressions( node );
            if ( value != null )
            {
                result.add( value );
            }
        }

        return result;
    }

    public synchronized NodeRef resolveXPathToNodeFrom( final NodeRef root, final String path )
    {
        try
        {
            final VTDNav nav = root.getNav()
                                   .cloneNav();
            nav.recoverNode( root.getIdx() );

            final AutoPilot ap = getAutoPilot( nav );

            ap.selectXPath( path );
            final int idx = ap.evalXPath();
            if ( idx > -1 )
            {
                return new NodeRef( nav, idx );
            }
        }
        catch ( final XPathParseException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to parse xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }
        catch ( XPathEvalException | NavException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to retrieve content for xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }

        return null;
    }

    public synchronized List<NodeRef> resolveXPathToNodeListFrom( final NodeRef root, final String path )
        throws GalleyMavenRuntimeException
    {
        try
        {
            final List<NodeRef> result = new ArrayList<>();
            final VTDNav nav = root.getNav()
                                   .cloneNav();
            nav.recoverNode( root.getIdx() );

            final AutoPilot ap = getAutoPilot( nav );
            ap.selectXPath( path );
            int idx = -1;
            while ( ( idx = ap.evalXPath() ) > -1 )
            {
                result.add( new NodeRef( nav, idx ) );
            }

            return result;
        }
        catch ( final XPathParseException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to compile xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }
        catch ( XPathEvalException | NavException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to retrieve content for xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }
    }

    public boolean containsExpression( final String value )
    {
        return value != null && value.matches( EXPRESSION_PATTERN );
    }

    public String resolveExpressions( final NodeRef node, final String... activeProfileIds )
    {
        if ( node == null )
        {
            return null;
        }

        String value;
        try
        {
            value = node.getNav()
                        .toNormalizedString( node.getNav()
                                                 .getText() );
        }
        catch ( final NavException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to navigate to text content in node: %s. Reason: %s", e, node.getIdx(), e.getMessage() );
        }

        return resolveExpressions( value, activeProfileIds );
    }

    public String resolveExpressions( final String value, final String... activeProfileIds )
    {
        if ( !containsExpression( value ) )
        {
            //            logger.info( "No expressions in: '%s'", value );
            return value;
        }

        synchronized ( this )
        {
            if ( ssi == null )
            {
                ssi = new StringSearchInterpolator();
                ssi.addValueSource( new MavenPomViewVS<T>( this, activeProfileIds ) );
            }
        }

        try
        {
            String result = ssi.interpolate( value );
            //            logger.info( "Resolved '%s' to '%s'", value, result );

            if ( result == null || result.trim()
                                         .length() < 1 )
            {
                result = value;
            }

            return result;
        }
        catch ( final InterpolationException e )
        {
            logger.error( "Failed to resolve expressions in: '%s'. Reason: %s", e, value, e.getMessage() );
            return value;
        }
    }

    private static final class MavenPomViewVS<T extends ProjectRef>
        implements ValueSource
    {

        //        private final Logger logger = new Logger( getClass() );

        private final MavenXmlView<T> view;

        private final List<Object> feedback = new ArrayList<>();

        private final String[] activeProfileIds;

        public MavenPomViewVS( final MavenXmlView<T> view, final String[] activeProfileIds )
        {
            this.view = view;
            this.activeProfileIds = activeProfileIds;
        }

        @Override
        public void clearFeedback()
        {
            feedback.clear();
        }

        @SuppressWarnings( "rawtypes" )
        @Override
        public List getFeedback()
        {
            return feedback;
        }

        @Override
        public Object getValue( final String expr )
        {
            try
            {
                final String value = view.resolveMavenExpression( expr, activeProfileIds );
                //                logger.info( "Value of: '%s' is: '%s'", expr, value );
                return value;
            }
            catch ( final GalleyMavenException e )
            {
                feedback.add( String.format( "Error resolving maven expression: '%s'", expr ) );
                feedback.add( e );
            }

            return null;
        }

    }

    public List<MavenXmlMixin<T>> getMixins()
    {
        return mixins;
    }

    public void addMixin( final MavenXmlMixin<T> mixin )
    {
        mixins.add( mixin );
    }

    public void removeMixin( final MavenXmlMixin<T> mixin )
    {
        mixins.remove( mixin );
    }

    public String toXML( final NodeRef element )
    {
        return XMLInfrastructure.toXML( element );
    }

}
