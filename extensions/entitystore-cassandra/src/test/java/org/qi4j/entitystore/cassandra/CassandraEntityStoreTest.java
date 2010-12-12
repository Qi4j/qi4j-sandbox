package org.qi4j.entitystore.cassandra;

import org.apache.cassandra.contrib.utils.service.CassandraServiceDataCleaner;
import org.apache.cassandra.service.EmbeddedCassandraService;
import org.apache.thrift.transport.TTransportException;
import org.junit.BeforeClass;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.test.entity.AbstractEntityStoreTest;

import java.io.IOException;


//need to launch a cassandra instance b4 running this test...
//waiting for Hector version of the ES
public class CassandraEntityStoreTest extends AbstractEntityStoreTest {

   private static EmbeddedCassandraService cassandra;
   private static Thread t;

   @Override
   public void assemble(ModuleAssembly module) throws AssemblyException {
      super.assemble(module);
      module.addServices(CassandraEntityStoreService.class,
                         CassandraConfigurationService.class);
   }

   @BeforeClass
   public static void setup() throws TTransportException, IOException,
                                     InterruptedException {
      // Tell cassandra where the configuration files are.
      // Use the test configuration file.

      System.setProperty("storage-config", "src/test/resources");

      new CassandraServiceDataCleaner().prepare();
      cassandra = new EmbeddedCassandraService();
      cassandra.init();
      t = new Thread(cassandra);
      t.setDaemon(true);
      t.start();
   }
}
