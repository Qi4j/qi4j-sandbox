/*  Copyright 2008 Rickard �berg.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.entitystore.s3;

import java.io.IOException;
import java.io.Reader;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.security.AWSCredentials;
import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.service.Activatable;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entity.EntityStoreException;

/**
 * Amazon S3 implementation of SerializationStore.
 * <p/>
 * To use this you must supply your own access key and secret key for your Amazon S3 account.
 */
public class S3SerializationStoreMixin
    implements Activatable, MapEntityStore
{
    private @This Configuration<S3Configuration> configuration;

    private S3Service s3Service;
    private S3Bucket entityBucket;

    // Activatable implementation
    public void activate() throws Exception
    {
        String awsAccessKey = configuration.configuration().accessKey().get();
        String awsSecretKey = configuration.configuration().secretKey().get();

        if( awsAccessKey == null || awsSecretKey == null )
        {
            throw new IllegalStateException( "No S3 keys configured" );
        }

        AWSCredentials awsCredentials =
            new AWSCredentials( awsAccessKey, awsSecretKey );
        s3Service = new RestS3Service( awsCredentials );

        S3Bucket[] s3Buckets = s3Service.listAllBuckets();
        System.out.println( "How many buckets do I have in S3? " + s3Buckets.length );

        if( s3Buckets.length == 0 )
        {
            entityBucket = s3Service.createBucket( "entity-bucket" );
            System.out.println( "Created entity bucket: " + entityBucket.getName() );
        }
        else
        {
            entityBucket = s3Buckets[ 0 ];
        }
    }

    public void passivate() throws Exception
    {
    }

    public Reader get( EntityReference entityReference ) throws EntityStoreException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void visitMap( MapEntityStoreVisitor visitor )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void applyChanges( MapChanges changes ) throws IOException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}