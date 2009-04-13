package org.qi4j.entitystore.s3;

import org.qi4j.api.configuration.ConfigurationComposite;
import org.qi4j.api.property.Property;

/**
 * Configuration for the Amazon S3 EntityStore
 */
public interface S3Configuration
    extends ConfigurationComposite
{
    Property<String> accessKey();

    Property<String> secretKey();
}
