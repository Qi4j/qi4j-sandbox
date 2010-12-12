package org.qi4j.entitystore.cassandra;

import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.service.ServiceComposite;

@Mixins(CassandraConfigurationService.CassandraConfigurationMixin.class)
public interface CassandraConfigurationService extends ServiceComposite,
                                                       CassandraConfiguration {
   public class CassandraConfigurationMixin implements CassandraConfiguration {
      private final boolean gzipCompress = true;
      private final boolean checkAbsentBeforeCreate = false;
      private final boolean checkPresentBeforeDelete = false;
      private final boolean checkPresentBeforeUpdate = false;

      private final String NULL = null;
      private final String LOCALHOST = "localhost";

      public boolean checkAbsentBeforeCreate() {
         return checkAbsentBeforeCreate;
      }

      public boolean checkPresentBeforeDelete() {
         return checkPresentBeforeDelete;
      }

      public boolean checkPresentBeforeUpdate() {
         return checkPresentBeforeUpdate;
      }

      public boolean gzipCompress() {
         return gzipCompress;
      }


      public boolean readOnly() {
         return false;
      }

      public String getHost() {
         return LOCALHOST;
      }

      public String getLogin() {
         return NULL;
      }

      public String getPassword() {
         return NULL;
      }

      public int getPort() {
         return 9160;
      }


   }
}
