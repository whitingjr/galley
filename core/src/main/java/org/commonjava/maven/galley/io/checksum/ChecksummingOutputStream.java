/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.galley.io.checksum;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChecksummingOutputStream
    extends FilterOutputStream
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Set<AbstractChecksumGenerator> checksums;

    private long size = 0;

    private final Transfer transfer;

    private final TransferMetadataConsumer metadataConsumer;

    private final boolean writeChecksumFiles;

    public ChecksummingOutputStream( final Set<AbstractChecksumGeneratorFactory<?>> checksumFactories,
                                     final OutputStream stream, final Transfer transfer,
                                     final TransferMetadataConsumer metadataConsumer, final boolean writeChecksumFiles )
        throws IOException
    {
        super( stream );
        this.transfer = transfer;
        this.metadataConsumer = metadataConsumer;
        this.writeChecksumFiles = writeChecksumFiles;
        checksums = new HashSet<>();
        for ( final AbstractChecksumGeneratorFactory<?> factory : checksumFactories )
        {
            checksums.add( factory.createGenerator( transfer, writeChecksumFiles ) );
        }
    }

    @Override
    public void close()
        throws IOException
    {
        try
        {
            logger.trace( "START CLOSE: {}", transfer );
            super.flush();
            logger.trace( "Wrote: {} (size: {}) in: {}. Now, writing checksums.", transfer.getPath(), size, transfer.getLocation() );
            Map<ContentDigest, String> hexDigests = new HashMap<>();
            for ( final AbstractChecksumGenerator checksum : checksums )
            {
                hexDigests.put( checksum.getDigestType(), checksum.getDigestHex() );
                if ( writeChecksumFiles )
                {
                    checksum.write();
                }
            }

            if ( metadataConsumer != null )
            {
                metadataConsumer.addMetadata( transfer, new TransferMetadata( hexDigests, size ) );
            }
            else
            {
                logger.trace( "No metadata consumer!" );
            }

            // NOTE: We close the main stream LAST, in case it's holding a file (or other) lock. This allows us to
            // finish out tasks BEFORE releasing that lock.
            super.close();
        }
        finally
        {
            logger.trace( "END CLOSE: {}", transfer );
        }
    }

    @Override
    public void write( final int data )
        throws IOException
    {
        logger.trace( "WRITE: {}", transfer );
        super.write( data );

        size++;
        byte b = (byte) ( data & 0xff );

        logger.trace( "Updating with: {} (raw: {})", b, data );

        for ( final AbstractChecksumGenerator checksum : checksums )
        {
            checksum.update( b );
        }
    }

}