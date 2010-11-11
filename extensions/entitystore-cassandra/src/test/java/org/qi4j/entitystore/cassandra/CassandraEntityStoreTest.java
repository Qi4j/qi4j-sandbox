package org.qi4j.entitystore.cassandra;

import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.test.entity.AbstractEntityStoreTest;

public class CassandraEntityStoreTest extends AbstractEntityStoreTest {

    @Override
    public void assemble(ModuleAssembly module) throws AssemblyException {
        super.assemble(module);
        module.addServices(CassandraEntityStoreService.class,
                CassandraConfigurationService.class);
    }
}
