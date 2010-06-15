/*
 * Copyright (c) 2010, Stanislav Muhametsin. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.qi4j.library.entityproxy;

import junit.framework.Assert;

import org.junit.Test;
import org.qi4j.api.unitofwork.ConcurrentEntityModificationException;
import org.qi4j.api.unitofwork.UnitOfWork;
import org.qi4j.api.unitofwork.UnitOfWorkCompletionException;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.entitystore.memory.MemoryEntityStoreService;
import org.qi4j.library.entityproxy.EntityWithMutualType.EntityCompositeWithMutualType;
import org.qi4j.library.entityproxy.EntityWithMutualType.EntityProxyWithMutualType;
import org.qi4j.library.entityproxy.MissingMutualTypeEntity.MissingMutualTypeEntityComposite;
import org.qi4j.library.entityproxy.MissingMutualTypeEntity.MissingMutualTypeEntityProxy;
import org.qi4j.library.entityproxy.assembling.EntityProxyAssembler;
import org.qi4j.spi.uuid.UuidIdentityGeneratorService;
import org.qi4j.test.AbstractQi4jTest;

/**
 *
 * @author Stanislav Muhametsin
 */
public class EntityProxyTest extends AbstractQi4jTest
{

    @Override
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        module.addEntities( EntityCompositeWithMutualType.class, MissingMutualTypeEntityComposite.class );
        module.addTransients( EntityProxyWithMutualType.class, MissingMutualTypeEntityProxy.class );

        EntityProxyAssembler eAss = new EntityProxyAssembler();
        eAss.assemble( module );

        module.addServices( UuidIdentityGeneratorService.class, MemoryEntityStoreService.class );
    }

    private <EntityType> EntityType createEntity(Class<EntityType> entityClass) throws ConcurrentEntityModificationException, UnitOfWorkCompletionException
    {
        UnitOfWork uow = this.unitOfWorkFactory.newUnitOfWork();
        EntityType result = uow.newEntity( entityClass );
        uow.complete();
        return result;
    }

    @Test
    public void getEntityProxyTest() throws Exception
    {
        EntityWithMutualType entity = this.createEntity( EntityCompositeWithMutualType.class );

        UnitOfWork uow = this.unitOfWorkFactory.newUnitOfWork();
        EntityWithMutualType proxy = ((ProxyableEntity)uow.get( entity )).getProxy( EntityWithMutualType.class );
        uow.complete();

        Assert.assertNotNull( "The proxy must be non-null.", proxy );
    }

    @Test
    public void getProxyableEntity() throws Exception
    {
        EntityWithMutualType entity = this.createEntity( EntityCompositeWithMutualType.class );

        UnitOfWork uow = this.unitOfWorkFactory.newUnitOfWork();
        EntityWithMutualType proxy = ((ProxyableEntity)uow.get( entity )).getProxy( EntityWithMutualType.class );
        uow.complete();

        entity = ((EntityProxy)proxy).getEntity( EntityWithMutualType.class );

        uow = this.unitOfWorkFactory.newUnitOfWork();
        try
        {
            Assert.assertNotNull( "The proxy must point to existing entity.", uow.get(entity) );
        } finally
        {
            uow.discard();
        }
    }

    @Test(expected = NoCommonClassFoundException.class)
    public void testMissingMutualType() throws Exception
    {
        MissingMutualTypeEntity entity = this.createEntity( MissingMutualTypeEntity.class );

        UnitOfWork uow = this.unitOfWorkFactory.newUnitOfWork();
        try
        {
            MissingMutualTypeEntity proxy = ((ProxyableEntity)uow.get( entity )).getProxy( MissingMutualTypeEntity.class );
        } finally
        {
            uow.complete();
        }
    }

    @Test
    public void changesMustPropagateToEntityTest() throws Exception
    {
        EntityWithMutualType entity = this.createEntity( EntityCompositeWithMutualType.class );

        UnitOfWork uow = this.unitOfWorkFactory.newUnitOfWork();
        EntityWithMutualType proxy = ((ProxyableEntity)uow.get( entity )).getProxy( EntityWithMutualType.class );
        uow.complete();

        proxy.setMyString( "TestString" );

        uow = this.unitOfWorkFactory.newUnitOfWork();
        try
        {
            Assert.assertEquals( "The change must go down to entity.", "TestString", uow.get( entity ).getMyString() );
        } finally
        {
            uow.discard();
        }
    }
}
