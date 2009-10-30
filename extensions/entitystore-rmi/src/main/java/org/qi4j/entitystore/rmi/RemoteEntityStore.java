package org.qi4j.entitystore.rmi;

import java.io.IOException;
import java.rmi.Remote;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.QualifiedIdentity;
import org.qi4j.spi.entitystore.ConcurrentEntityStateModificationException;

/**
 * Interface for remote EntityStore
 */
public interface RemoteEntityStore
    extends Remote
{
    EntityState getEntityState( QualifiedIdentity identity )
        throws IOException;

    void prepare( Iterable<EntityState> newStates, Iterable<EntityState> loadedStates, Iterable<QualifiedIdentity> removedStates )
        throws IOException, ConcurrentEntityStateModificationException;
}
