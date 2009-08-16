/*
 * Copyright 2008 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.entitystore.swift;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.locks.ReadWriteLock;
import org.qi4j.api.configuration.Configuration;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.injection.scope.Uses;
import org.qi4j.api.service.Activatable;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entity.EntityStoreException;
import org.qi4j.spi.service.ServiceDescriptor;

public class SwiftEntityStoreMixin
    implements Activatable, MapEntityStore
{
    private @This ReadWriteLock lock;
    @Uses private ServiceDescriptor descriptor;
    @This private Configuration<SwiftConfiguration> configuration;
    private RecordManager recordManager;

    public SwiftEntityStoreMixin()
    {
    }

    public void activate()
        throws Exception
    {
        SwiftConfiguration conf = configuration.configuration();
        String storage = conf.storageDirectory().get();
        File storageDir;
        storageDir = new File( storage );
        Boolean recover = conf.recover().get();
        if( recover == null )
        {
            recover = Boolean.TRUE;
        }
        recordManager = new RecordManager( storageDir, recover );
    }

    public void passivate()
        throws Exception
    {
        recordManager.close();
    }

//    public EntityState newEntityState( QualifiedIdentity identity ) throws EntityStoreException
//    {
//        EntityType entityType = getEntityType( identity.type() );
//        return new DefaultEntityState( identity, entityType );
//        return null;
//    }

//    public EntityState getEntityState( QualifiedIdentity identity ) throws EntityStoreException
//    {
//        EntityType entityType = getEntityType( identity.type() );
//
//        try
//        {
//
//            try
//            {
//                SerializableState serializableState = loadSerializableState( identity );
//                if( serializableState == null )
//                {
//                    throw new EntityNotFoundException( descriptor.identity(), identity );
//                }
//
//                return new DefaultEntityState( serializableState.version(),
//                                               serializableState.lastModified(),
//                                               identity,
//                                               EntityStatus.LOADED,
//                                               entityType,
//                                               serializableState.properties(),
//                                               serializableState.associations(),
//                                               serializableState.manyAssociations() );
//            }
//            catch( ClassNotFoundException e )
//            {
//                throw new EntityStoreException( e );
//            }
//        }
//        catch( IOException e )
//        {
//            throw new EntityStoreException( e );
//        }
//    }
//
//    public StateCommitter prepare( Iterable<EntityState> newStates, Iterable<EntityState> updatedStates, Iterable<QualifiedIdentity> removedStates )
//        throws EntityStoreException
//    {
//        boolean turbo = configuration.configuration().turboMode().get();
//        lock.writeLock().lock();
//
//        long lastModified = System.currentTimeMillis();
//        try
//        {
//            ByteArrayOutputStream bout = new ByteArrayOutputStream();
//            storeNewStates( newStates, turbo, lastModified, bout );
//            storeLoadedStates( updatedStates, turbo, lastModified, bout );
//            removeStates( removedStates );
//        }
//        catch( Throwable e )
//        {
//            try
//            {
//                recordManager.discard();
//            }
//            catch( IOException e1 )
//            {
//                throw new EntityStoreException( "Problem with underlying storage system." );
//            }
//            lock.writeLock().unlock();
//            if( e instanceof EntityStoreException )
//            {
//                throw (EntityStoreException) e;
//            }
//            else
//            {
//                throw new EntityStoreException( e );
//            }
//        }
//
//        return new StateCommitter()
//        {
//            public void commit()
//            {
//                try
//                {
//                    recordManager.commit();
//                }
//                catch( IOException e1 )
//                {
//                    throw new EntityStoreException( "Problem with underlying storage system." );
//                }
//                finally
//                {
//                    lock.writeLock().unlock();
//                }
//            }
//
//            public void cancel()
//            {
//                try
//                {
//                    recordManager.discard();
//                }
//                catch( IOException e1 )
//                {
//                    throw new EntityStoreException( "Problem with underlying storage system." );
//                }
//                finally
//                {
//                    lock.writeLock().unlock();
//                }
//            }
//        };
//    }
//
//    public Iterator<EntityState> iterator()
//    {
//        final Iterator<QualifiedIdentity> iterator = recordManager.iterator();
//
//        return new Iterator<EntityState>()
//        {
//            public boolean hasNext()
//            {
//                return iterator.hasNext();
//            }
//
//            public EntityState next()
//            {
//                try
//                {
//                    QualifiedIdentity identity = iterator.next();
//                    SerializableState serializableState = loadSerializableState( identity );
//                    if( serializableState == null )
//                    {
//                        throw new EntityNotFoundException( descriptor.identity(), identity );
//                    }
//
//                    return new DefaultEntityState( serializableState.version(),
//                                                   serializableState.lastModified(),
//                                                   serializableState.qualifiedIdentity(),
//                                                   EntityStatus.LOADED,
//                                                   getEntityType( serializableState.qualifiedIdentity().type() ),
//                                                   serializableState.properties(),
//                                                   serializableState.associations(),
//                                                   serializableState.manyAssociations() );
//                }
//                catch( Exception e )
//                {
//                    throw new EntityStoreException( e );
//                }
//            }
//
//            public void remove()
//            {
//            }
//        };
//    }
//
//
//    private SerializableState loadSerializableState( QualifiedIdentity identity )
//        throws IOException, ClassNotFoundException
//    {
//        DataBlock data = recordManager.readData( identity );
//
//        if( data == null )
//        {
//            return null;
//        }
//        byte[] serializedState = data.data;
//
//        ByteArrayInputStream bin = new ByteArrayInputStream( serializedState );
//        ObjectInputStream oin = new FastObjectInputStream( bin, configuration.configuration().turboMode().get() );
//        return (SerializableState) oin.readObject();
//    }
//
//    private void storeNewStates( Iterable<EntityState> newStates, boolean turbo, long lastModified, ByteArrayOutputStream bout )
//        throws IOException
//    {
//        for( EntityState entityState : newStates )
//        {
//            DefaultEntityState entityStateInstance = (DefaultEntityState) entityState;
//            SerializableState state = new SerializableState( entityState.qualifiedIdentity(),
//                                                             entityState.version(),
//                                                             lastModified,
//                                                             entityStateInstance.getProperties(),
//                                                             entityStateInstance.getAssociations(),
//                                                             entityStateInstance.getManyAssociations() );
//            ObjectOutputStream out = new FastObjectOutputStream( bout, turbo );
//            out.writeObject( state );
//            out.close();
//            byte[] stateArray = bout.toByteArray();
//            DataBlock data = new DataBlock( entityState.qualifiedIdentity(), stateArray, entityState.version(), 1 );
//            recordManager.putData( data );
//            bout.reset();
//        }
//    }
//
//    private void storeLoadedStates( Iterable<EntityState> loadedStates, boolean turbo, long lastModified, ByteArrayOutputStream bout )
//        throws IOException
//    {
//        for( EntityState entityState : loadedStates )
//        {
//            DefaultEntityState entityStateInstance = (DefaultEntityState) entityState;
//
//            if( entityStateInstance.isModified() )
//            {
//                long newVersion = entityState.version() + 1;
//                SerializableState state = new SerializableState( entityState.qualifiedIdentity(),
//                                                                 newVersion,
//                                                                 lastModified,
//                                                                 entityStateInstance.getProperties(),
//                                                                 entityStateInstance.getAssociations(),
//                                                                 entityStateInstance.getManyAssociations() );
//                ObjectOutputStream out = new FastObjectOutputStream( bout, turbo );
//                out.writeObject( state );
//                out.close();
//                byte[] stateArray = bout.toByteArray();
//                bout.reset();
//                DataBlock data = new DataBlock( entityState.qualifiedIdentity(), stateArray, newVersion, 1 );
//                recordManager.putData( data );
//            }
//        }
//    }
//
//    private void removeStates( Iterable<QualifiedIdentity> removedStates )
//        throws IOException
//    {
//        for( QualifiedIdentity removedState : removedStates )
//        {
//            recordManager.deleteData( removedState );
//        }
//    }

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
