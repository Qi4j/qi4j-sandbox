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

import org.qi4j.api.common.Visibility;
import org.qi4j.bootstrap.Assembler;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.entitystore.memory.MemoryEntityStoreService;
import org.qi4j.spi.uuid.UuidIdentityGeneratorService;

public class SwiftEntityStoreAssembler
    implements Assembler
{
    private String configurationModuleName;

    public SwiftEntityStoreAssembler( String configurationModule )
    {
        this.configurationModuleName = configurationModule;
    }

    public void assemble( ModuleAssembly module ) throws AssemblyException
    {
        module.addServices( SwiftEntityStoreService.class, UuidIdentityGeneratorService.class );
        ModuleAssembly config = module.layerAssembly().moduleAssembly( configurationModuleName );
        config.addEntities( SwiftConfiguration.class ).visibleIn( Visibility.layer );
        config.addServices( MemoryEntityStoreService.class );
    }
}
