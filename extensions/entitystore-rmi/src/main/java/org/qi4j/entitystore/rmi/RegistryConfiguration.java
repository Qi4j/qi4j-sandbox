package org.qi4j.entitystore.rmi;

import org.qi4j.api.configuration.ConfigurationComposite;
import org.qi4j.api.property.Property;

/**
 * JAVADOC
 */
public interface RegistryConfiguration
    extends ConfigurationComposite
{
    Property<Integer> port();
}
