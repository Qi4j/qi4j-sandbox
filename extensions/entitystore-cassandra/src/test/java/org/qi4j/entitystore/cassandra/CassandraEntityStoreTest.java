package org.qi4j.entitystore.cassandra;

import org.junit.Ignore;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.test.entity.AbstractEntityStoreTest;


//need to launch a cassandra instance b4 running this test...
//waiting for Hector version of the ES
@Ignore
public class CassandraEntityStoreTest extends AbstractEntityStoreTest {

    @Override
    public void assemble(ModuleAssembly module) throws AssemblyException {
        super.assemble(module);
        module.addServices(CassandraEntityStoreService.class,
                CassandraConfigurationService.class);
    }
}
