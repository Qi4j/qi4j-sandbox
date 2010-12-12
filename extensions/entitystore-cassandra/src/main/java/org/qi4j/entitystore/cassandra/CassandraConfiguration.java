package org.qi4j.entitystore.cassandra;


public interface CassandraConfiguration {
   boolean gzipCompress();

   boolean checkAbsentBeforeCreate();

   boolean checkPresentBeforeDelete();

   boolean checkPresentBeforeUpdate();

   boolean readOnly();


   String getHost();

   String getLogin();

   String getPassword();

   int getPort();
}
