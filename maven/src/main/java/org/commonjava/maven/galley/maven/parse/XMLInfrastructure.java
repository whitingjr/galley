package org.commonjava.maven.galley.maven.parse;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.GalleyMavenRuntimeException;
import org.commonjava.maven.galley.maven.model.view.NodeRef;
import org.commonjava.maven.galley.model.Transfer;

import com.ximpleware.AutoPilot;
import com.ximpleware.ModifyException;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.TranscodeException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

public final class XMLInfrastructure
{

    //    private static final Logger logger = new Logger( XMLInfrastructure.class );

    private static final String[] LEGAL_ENTITIES = { "amp", "apos", "lt", "gt", "quot", "#x[0-9a-fA-F]+" };

    private XMLInfrastructure()
    {
    }

    public static NodeRef createElement( final NodeRef below, final String relativePath, final Map<String, String> leafElements )
    {
        VTDNav nav = below.getNav()
                          .cloneNav();

        final StringBuilder sb = new StringBuilder();
        int indent = nav.getCurrentDepth();
        final LinkedList<String> elementStack = new LinkedList<>();

        if ( relativePath.length() > 0 && !"/".equals( relativePath ) )
        {
            final String[] intermediates = relativePath.split( "/" );

            // DO NOT traverse last "intermediate"...this will be the new element!
            boolean found = true;
            for ( int i = 0; i < intermediates.length - 1; i++ )
            {
                final String intermediate = intermediates[i];

                try
                {
                    if ( found && nav.toElement( VTDNav.FIRST_CHILD, intermediate ) )
                    {
                        indent++;
                    }
                    else
                    {
                        found = false;
                        indent++;
                        elementStack.addFirst( intermediate );
                        newlineElement( intermediate, indent, null, sb );
                    }
                }
                catch ( final NavException e )
                {
                    throw new GalleyMavenRuntimeException(
                                                           "Failed to navigate to insertion point in XML: %s (relative path segment: %d). Reason: %s",
                                                           e, relativePath, i, e.getMessage() );
                }

                i++;
            }

            final String intermediate = intermediates[intermediates.length - 1];
            indent++;
            elementStack.addFirst( intermediate );
            newlineElement( intermediate, indent, null, sb );
        }

        for ( final Entry<String, String> entry : leafElements.entrySet() )
        {
            final String key = entry.getKey();
            final String value = entry.getValue();

            newlineElement( key, indent + 1, value, sb );
        }

        while ( !elementStack.isEmpty() )
        {
            endElement( elementStack.removeFirst(), indent, sb );
            indent--;
        }

        XMLModifier mod;
        try
        {
            mod = new XMLModifier( nav );

            if ( nav.toElement( VTDNav.FIRST_CHILD ) )
            {
                while ( nav.toElement( VTDNav.NEXT_SIBLING ) )
                {
                    // nop, move the cursor forward.
                }

                mod.insertAfterElement( sb.toString() );
            }
            else
            {
                mod.updateToken( nav.getText(), sb.toString() );
            }

            nav = mod.outputAndReparse();
            nav.recoverNode( below.getIdx() );

            final AutoPilot ap = new AutoPilot( nav );
            ap.selectXPath( relativePath );
            ap.evalXPath();

            while ( nav.toElement( VTDNav.NEXT_SIBLING ) )
            {
                // nop, move cursor.
            }

            return new NodeRef( nav, nav.getCurrentIndex() );
        }
        catch ( ModifyException | NavException | ParseException | TranscodeException | IOException | XPathParseException | XPathEvalException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to modify XML or adjust document cursor after modification: %s", e, e.getMessage() );
        }
    }

    private static void newlineElement( final String name, final int indent, final String value, final StringBuilder sb )
    {
        sb.append( "\n" );
        for ( int j = 0; j < indent; j++ )
        {
            sb.append( "  " );
        }
        sb.append( '<' )
          .append( name )
          .append( '>' );
        if ( value != null )
        {
            sb.append( value )
              .append( "</" )
              .append( name )
              .append( '>' );
        }
    }

    private static void endElement( final String name, final int indent, final StringBuilder sb )
    {
        sb.append( "\n" );
        for ( int j = 0; j < indent; j++ )
        {
            sb.append( "  " );
        }
        sb.append( "</" )
          .append( name )
          .append( '>' );
    }

    public static String toXML( final NodeRef element )
    {
        final VTDNav nav = element.getNav();
        return toXML( nav );
    }

    public static String toXML( final VTDNav nav )
    {
        try
        {
            final long fragment = nav.getElementFragment();
            String indent = "";
            for ( int i = 0; i < nav.getCurrentDepth(); i++ )
            {
                indent += "  ";
            }

            return indent + nav.toString( (int) fragment, (int) ( fragment >> 32 ) );
        }
        catch ( final NavException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to isolate XML content from larger document. Reason: %s", e, e.getMessage() );
        }
    }

    public static VTDNav parse( final Object docSource, final InputStream stream )
        throws GalleyMavenXMLException
    {
        if ( stream == null )
        {
            throw new GalleyMavenXMLException( "Cannot parse null input stream from: %s.", docSource );
        }

        byte[] xml;
        try
        {
            xml = IOUtils.toByteArray( stream );
            xml = fixUglyXML( xml );
        }
        catch ( final IOException e )
        {
            throw new GalleyMavenXMLException( "Failed to read raw data from XML stream: %s", e, e.getMessage() );
        }

        //        logger.info( "Parsing:\n\n%s\n\n", new String( xml ) );

        try
        {
            final VTDGen vtd = new VTDGen();
            vtd.setDoc( xml );
            vtd.parse( false );

            return vtd.getNav();
        }
        catch ( final ParseException e )
        {
            throw new GalleyMavenXMLException( "Cannot parse: %s. Reason: %s", e, docSource, e.getMessage() );
        }
    }

    private static byte[] fixUglyXML( final byte[] xml )
        throws IOException
    {
        byte[] result = xml;

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream pw = new PrintStream( baos );

        final BufferedReader br = new BufferedReader( new InputStreamReader( new ByteArrayInputStream( result ) ) );
        final String firstLine = br.readLine()
                                   .trim();
        if ( !firstLine.startsWith( "<?xml" ) )
        {
            pw.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
            pw.println();
        }

        pw.println( firstLine );

        String line = null;
        while ( ( line = br.readLine() ) != null )
        {
            final Pattern p = Pattern.compile( "&([^;]+);" );
            final Matcher m = p.matcher( line );
            final StringBuffer sb = new StringBuffer();

            boolean foundOne = false;
            while ( m.find() )
            {
                foundOne = true;

                final String entity = m.group( 1 );
                final String match = m.group();

                String replacement = null;
                for ( final String legal : LEGAL_ENTITIES )
                {
                    if ( entity.matches( legal ) )
                    {
                        replacement = match;
                    }
                }

                if ( replacement == null )
                {
                    replacement = "<!-- " + entity + " -->";
                }

                //                logger.info( "Replacing:\n  '%s'\n  '%s'", entity, replacement );
                m.appendReplacement( sb, replacement );
            }

            if ( foundOne )
            {
                m.appendTail( sb );
            }

            if ( sb.length() > 0 )
            {
                pw.println( sb.toString() );
            }
            else
            {
                pw.println( line );
            }
        }

        result = baos.toByteArray();
        return result;
    }

    public static VTDNav parse( final Transfer transfer )
        throws GalleyMavenXMLException
    {
        InputStream stream = null;
        VTDNav doc = null;
        try
        {
            try
            {
                stream = transfer.openInputStream( false );
                doc = parse( transfer.toString(), stream );
            }
            catch ( final GalleyMavenXMLException e )
            {
            }
        }
        catch ( final IOException e )
        {
            throw new GalleyMavenXMLException( "Failed to read: %s. Reason: %s", e, transfer, e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }

        return doc;
    }

    public static ProjectVersionRef getParentRef( final VTDNav doc, final Object location )
        throws GalleyMavenException
    {
        final int originalIdx = doc.getCurrentIndex();

        final AutoPilot ap = new AutoPilot( doc );
        final String aid = evalString( "/project/parent/artifactId", ap );
        if ( aid == null )
        {
            return null;
        }

        try
        {
            doc.recoverNode( originalIdx );
            final String gid = evalString( "/project/parent/groupId", ap );
            doc.recoverNode( originalIdx );
            final String ver = evalString( "/project/parent/version", ap );

            if ( isEmpty( aid ) || isEmpty( gid ) || isEmpty( ver ) )
            {
                return null;
            }

            return new ProjectVersionRef( gid, aid, ver );
        }
        catch ( final NavException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to reorient to original node: ", e, e.getMessage() );
        }
    }

    public static ProjectVersionRef getProjectVersionRef( final VTDNav doc, final Object location )
        throws GalleyMavenException
    {
        final int originalIdx = doc.getCurrentIndex();
        final AutoPilot ap = new AutoPilot( doc );

        try
        {
            doc.recoverNode( originalIdx );
            final String aid = evalString( "/project/artifactId", ap );
            String gid = evalString( "/project/groupId", ap );
            String ver = evalString( "/project/version", ap );

            if ( isEmpty( gid ) )
            {
                gid = evalString( "/project/parent/groupId", ap );
            }

            if ( isEmpty( ver ) )
            {
                ver = evalString( "/project/parent/version", ap );
            }

            if ( isEmpty( aid ) || isEmpty( gid ) || isEmpty( ver ) )
            {
                throw new GalleyMavenException( "Could not resolve coordinate for: %s. (Parts: G=%s, A=%s, V=%s)", location, gid, aid, ver );
            }

            return new ProjectVersionRef( gid, aid, ver );
        }
        catch ( final NavException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to reorient to original node: ", e, e.getMessage() );
        }
    }

    private static String evalString( final String path, final AutoPilot ap )
    {
        try
        {
            ap.resetXPath();
            ap.selectXPath( path );
            return ap.evalXPathToString();
        }
        catch ( final XPathParseException e )
        {
            throw new GalleyMavenRuntimeException( "Failed to compile xpath expression: %s. Reason: %s", e, path, e.getMessage() );
        }
    }

}
