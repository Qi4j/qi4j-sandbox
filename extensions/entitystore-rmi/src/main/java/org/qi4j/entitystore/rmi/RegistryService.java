package org.qi4j.entitystore.rmi;

import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.service.Activatable;
import org.qi4j.api.service.ServiceComposite;

import java.rmi.registry.Registry;

/**
 * RMI Registry service
 */
@Mixins( RegistryMixin.class )
public interface RegistryService
    extends ServiceComposite, Registry, Activatable
{
}
