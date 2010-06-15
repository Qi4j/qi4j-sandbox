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

import org.qi4j.api.composite.TransientBuilder;
import org.qi4j.api.composite.TransientBuilderFactory;
import org.qi4j.api.entity.EntityComposite;
import org.qi4j.api.entity.Lifecycle;
import org.qi4j.api.entity.LifecycleException;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.api.injection.scope.Structure;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.unitofwork.UnitOfWorkCallback;
import org.qi4j.api.unitofwork.UnitOfWorkCompletionException;
import org.qi4j.api.unitofwork.UnitOfWorkFactory;
import org.qi4j.library.entityproxy.internal.EntityProxyCache;

@Mixins({ProxyableEntity.ProxyableEntityMixin.class})
public interface ProxyableEntity
{
   <ProxyType> ProxyType getProxy(Class<ProxyType> proxyClass);

   public abstract class ProxyableEntityMixin
   implements ProxyableEntity, Lifecycle
{

   @Service
   private EntityProxyCache _proxyUtils;

   @Structure
   private UnitOfWorkFactory _uowf;

   @This
   private EntityComposite _meAsEntityComposite;

   @Structure private TransientBuilderFactory _tbf;

   @Override
   public <ProxyType> ProxyType getProxy( Class<ProxyType> proxyClass )
   {
       String id = this._meAsEntityComposite.identity().get();
       EntityProxy proxy = this._proxyUtils.getFromCache( id );
       if (proxy == null)
       {
           proxy = this.createProxy( proxyClass );
           this._proxyUtils.storeToCache( proxy );
       }
       ProxyType result = proxyClass.cast( proxy );
       return result;
   }

   private <ProxyType> EntityProxy createProxy(Class<ProxyType> proxyClass)
   {
      TransientBuilder<ProxyType> builder = this._tbf.newTransientBuilder(proxyClass);
      EntityProxy.EntityProxyState state = builder.prototypeFor(EntityProxy.EntityProxyState.class);
      Class<?> commonClass = this.doGetCommonType(proxyClass);
      if (commonClass == null)
      {
         throw new NoCommonClassFoundException("Did not find common class for entity of type: " + this._meAsEntityComposite.type() + " [proxyClass: " + proxyClass.getName() + "].");
      }
      state.commonClass().set(commonClass);
      state.entityID().set(this._meAsEntityComposite.identity().get());

      return EntityProxy.class.cast( builder.newInstance() );
   }

   private Class<?> doGetCommonType(Class<?> clazz)
   {
      Class<?> result = null;
      MutualType commonType = clazz.getAnnotation(MutualType.class);
      if (commonType == null)
      {
         for (Class<?> sInterface : clazz.getInterfaces())
         {
            result = this.doGetCommonType(sInterface);
            if (result != null)
            {
               break;
            }
         }

         if (result == null && clazz.getSuperclass() != null)
         {
            result = this.doGetCommonType(clazz.getSuperclass());
         }
      }
      else
      {
          result = clazz;
      }

      return result;
   }

   @Override
   public void create()
       throws LifecycleException
   {

   }

   @Override
   public void remove()
       throws LifecycleException
   {

       final String id = this._meAsEntityComposite.identity().get();
       this._uowf.currentUnitOfWork().addUnitOfWorkCallback( new UnitOfWorkCallback()
       {

           @Override
           public void beforeCompletion()
               throws UnitOfWorkCompletionException
           {
               // Nothing.
           }

           @Override
           public void afterCompletion( UnitOfWorkStatus status )
           {
               if( status == UnitOfWorkStatus.COMPLETED )
               {
                   _proxyUtils.removeFromCache( id );
               }
           }
       } );

   }
}
}
