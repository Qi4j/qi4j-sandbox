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

/**
 *
 * @author Stanislav Muhametsin
 */
public class EntityProxyHelper
{
    public static <EType, ReturnType> ReturnType getEntity(Class<ReturnType> entityClass, EType proxyOrEntity)
    {
        return proxyOrEntity instanceof EntityProxy ? ((EntityProxy)proxyOrEntity).getEntity( entityClass ) : entityClass.cast( proxyOrEntity );
    }

    public static <PType, ReturnType> ReturnType getProxy(Class<ReturnType> proxyClass, PType proxyOrEntity)
    {
        return proxyOrEntity instanceof ProxyableEntity ? ((ProxyableEntity)proxyOrEntity).getProxy( proxyClass ) : proxyClass.cast( proxyOrEntity );
    }
}
