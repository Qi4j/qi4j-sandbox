package org.qi4j.entitystore.cassandra;

import me.prettyprint.cassandra.serializers.BytesSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.api.io.Input;
import org.qi4j.api.service.Activatable;
import org.qi4j.entitystore.map.MapEntityStore;
import org.qi4j.spi.entity.EntityType;
import org.qi4j.spi.entitystore.EntityAlreadyExistsException;
import org.qi4j.spi.entitystore.EntityNotFoundException;
import org.qi4j.spi.entitystore.EntityStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static me.prettyprint.hector.api.factory.HFactory.*;

/**
 * // TODO: Document this
 *
 * @author pvdyck
 * @since 4.0
 */
public class CassandraMapEntityStoreMixin implements MapEntityStore, Activatable {
   private static final String COLUMN_NAME = "entry";
   private final Logger logger = LoggerFactory.getLogger(CassandraMapEntityStoreMixin.class);

   private static final String keySpace = "Qi4j";
   static final String cf = "Qi4jEntries";

   static final int BYTE_ARRAY_BUFFER_INITIAL_SIZE = 512;

   Cluster c;
   Keyspace ko;


   private static final StringSerializer se = new StringSerializer();
   private static final BytesSerializer bs = new BytesSerializer();


   @Service
   private
   CassandraConfiguration conf;

   public void activate() throws Exception {
      c = HFactory.getOrCreateCluster("Qi4jCluster", conf.getHost() + ":" + conf.getPort());
      ko = HFactory.createKeyspace(keySpace, c);
      logger.info("started cassandra store");
   }


   public void passivate() throws Exception {
      logger.info("shutting down cassandra");
   }


   public void applyChanges(final MapChanges changes) throws IOException {
      if (conf.readOnly()) {
         throw new EntityStoreException("Read-only Entity Store");
      }

      try {
         final MapUpdater changer = new MapUpdater();
         changes.visitMap(changer);

         MutationResult result = changer.m.execute();

         logger.info("applying changes to cassandra store " + result.getExecutionTimeMicro() + " / " + result.getHostUsed());

      } catch (Throwable e) {
         throw new EntityStoreException("Exception during cassandra batch "
                                              + " - ", e);
      }
   }

   boolean contains(EntityReference ref) throws EntityStoreException {
      try {
         //TODO optimise this... no need to fetch everything
         get(ref);
         return true;
      } catch (final EntityNotFoundException e1) {
         return false;
      } catch (final Exception e1) {
         throw new EntityStoreException(e1);
      }
   }

   public Reader get(final EntityReference ref) {
      //TODO .. should be able to use only one ColumnQuery ... btw .. is it thread-safe ?
      ColumnQuery<String, byte[]> q = createColumnQuery(ko, se, bs);
      q.setName(COLUMN_NAME).setColumnFamily(cf);
      QueryResult<HColumn<String, byte[]>> r;
      try {
         r = q.setKey(ref.toString()).execute();
      } catch (Exception e) {
         throw new EntityStoreException(e);
      }
      if (r.get() == null) throw new EntityNotFoundException(ref);

      try {
         return createReader(new ByteArrayInputStream(r.get().getValue()));
      } catch (IOException e) {
         throw new EntityStoreException(e);
      }
   }


   public Input<Reader, IOException> entityStates() {
      throw new UnsupportedOperationException("Not implemented yet");
   }


   Writer createWriter(OutputStream out) throws IOException {
      if (conf.gzipCompress())
         return new OutputStreamWriter(new GZIPOutputStream(out));
      return new OutputStreamWriter(out);
   }

   private Reader createReader(InputStream in) throws IOException {
      if (conf.gzipCompress())
         return new InputStreamReader(new GZIPInputStream(in));
      return new InputStreamReader(in);
   }

   void checkAbsentBeforeCreate(EntityReference ref) {
      if (!conf.checkAbsentBeforeCreate())
         return;
      if (contains(ref))
         throw new EntityAlreadyExistsException(ref);
   }

   void checkPresentBeforeDelete(EntityReference ref) {
      if (!conf.checkPresentBeforeDelete())
         return;
      if (!contains(ref))
         throw new EntityNotFoundException(ref);
   }

   void checkPresentBeforeUpdate(EntityReference ref) {
      if (!conf.checkPresentBeforeUpdate())
         return;
      if (!contains(ref))
         throw new EntityNotFoundException(ref);
   }

   class MapUpdater implements MapEntityStore.MapChanger {
      me.prettyprint.hector.api.mutation.Mutator m = createMutator(ko);


      public Writer newEntity(final EntityReference ref, EntityType entityType) {
         checkAbsentBeforeCreate(ref);
         return getWriter(ref);
      }

      public Writer updateEntity(final EntityReference ref, EntityType entityType)
            throws IOException {
         checkPresentBeforeUpdate(ref);
         return getWriter(ref);
      }

      public void removeEntity(EntityReference ref, EntityType entityType)
            throws EntityNotFoundException {
         checkPresentBeforeDelete(ref);
         m.addDeletion(ref.identity(), cf, COLUMN_NAME, se);
      }

      private Writer getWriter(final EntityReference ref) {
         try {
            return createWriter(new ByteArrayOutputStream(CassandraMapEntityStoreMixin.BYTE_ARRAY_BUFFER_INITIAL_SIZE) {
               @Override
               public void close() throws IOException {
                  super.close();
                  m.addInsertion(ref.identity(), cf, createColumn(COLUMN_NAME, toByteArray(), se, bs));
               }
            });
         } catch (final Exception e) {
            throw new EntityStoreException(e);
         }
      }
   }
}