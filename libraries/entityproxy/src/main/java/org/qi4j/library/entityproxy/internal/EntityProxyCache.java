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

package org.qi4j.library.entityproxy.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.qi4j.api.service.Activatable;
import org.qi4j.library.constraints.annotation.GreaterThan;
import org.qi4j.library.entityproxy.EntityProxy;

public interface EntityProxyCache
{
    public EntityProxy getFromCache(String entityID);

    public void storeToCache(EntityProxy proxy);

    public void removeFromCache(String entityID);

    public Integer getMaxCacheSize();

    public Integer getCurrentCacheSize();

    public void setMaxCacheSize(@GreaterThan(0) Integer newSize);

   public abstract class EntityProxyUtilsMixin implements EntityProxyCache, Activatable
   {

      private static final Integer DEFAULT_MAX_PROXIES = 1000;

      private Map<String, EntityProxy> _entityProxyMapping;

      private Integer _maxProxies;

      @Override
      public void activate() throws Exception
      {
         this._maxProxies = DEFAULT_MAX_PROXIES;
         this._entityProxyMapping = new LinkedHashMap<String, EntityProxy>(this._maxProxies);
      }

      @Override
      public void passivate() throws Exception
      {
      }

      @Override
      public Integer getCurrentCacheSize()
      {
          return this._entityProxyMapping.size();
      }

      @Override
      public EntityProxy getFromCache( String entityID )
      {
          return this._entityProxyMapping.get( entityID );
      }
      @Override
      public Integer getMaxCacheSize()
      {
          return this._maxProxies;
      }

      @Override
      public void removeFromCache( String entityID )
      {
          this._entityProxyMapping.remove( entityID );
      }

      @Override
      public void setMaxCacheSize( Integer newSize )
      {
          this._maxProxies = newSize;
          while (this._entityProxyMapping.size() > this._maxProxies)
          {
              this.removeEldestProxy();
          }
      }

      @Override
      public void storeToCache( EntityProxy proxy )
      {
          synchronized( this._entityProxyMapping )
          {
              if (this._entityProxyMapping.size() == this._maxProxies)
              {
                  this.removeEldestProxy();
              }
              this._entityProxyMapping.put( proxy.getEntityID(), proxy );
          }
      }

      private void removeEldestProxy()
      {
          this._entityProxyMapping.remove( this._entityProxyMapping.keySet().iterator().next() );
      }
   }
}
