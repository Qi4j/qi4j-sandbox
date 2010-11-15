package org.qi4j.entitystore.cassandra;

import org.apache.cassandra.thrift.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * // TODO: Document this
 *
 * @author pvdyck
 * @since 4.0
 */
public class CassandraMapEntityStoreMixin implements MapEntityStore, Activatable {
    private final Logger logger = LoggerFactory.getLogger(CassandraMapEntityStoreMixin.class);

    private static final String keySpace = "Qi4j";
    static final String entryColumnFamily = "Qi4jEntries";

    private ColumnPath entryColumnPath;


    static final int BYTE_ARRAY_BUFFER_INITIAL_SIZE = 512;

    private TTransport tr = new TSocket("localhost", 9160);
    private Cassandra.Client client = new Cassandra.Client(new TBinaryProtocol(tr));

    @Service
    private
    CassandraConfiguration conf;

    public void activate() throws Exception {
        logger.info("starting cassandra store");
        tr = new TSocket(conf.getHost(), 9160);
        if(conf.getLogin()!=null){

        }
        client = new Cassandra.Client(new TBinaryProtocol(tr));
        tr.open();
        entryColumnPath = new ColumnPath(entryColumnFamily).setColumn("entry".getBytes("UTF-8"));
        logger.info("started cassandra store");
    }


    public void passivate() throws Exception {
        logger.info("shutting down cassandra");
        synchronized (client) {
            tr.close();
        }
    }


    public void applyChanges(final MapChanges changes) throws IOException {
        if (conf.readOnly()) {
            throw new EntityStoreException("Read-only Entity Store");
        }

        try {
            final MapUpdater changer = new MapUpdater();
            changes.visitMap(changer);
            synchronized (client) {
                client.batch_mutate(keySpace, changer.mutationMap, ConsistencyLevel.ONE);
            }
        } catch (Throwable e) {
            throw new EntityStoreException("Exception during cassandra batch "
                    + " - ", e);
        }
    }

    boolean contains(EntityReference ref) throws EntityStoreException {
        try {
            return get(ref) != null;
        } catch (final EntityNotFoundException e1) {
            return false;
        } catch (final Exception e1) {
            throw new EntityStoreException(e1);
        }
    }

    public Reader get(final EntityReference ref) {
        String hashKey = ref.toString();

        synchronized (client) {
            try {
                ColumnOrSuperColumn column = client.get(keySpace, hashKey,
                        entryColumnPath, ConsistencyLevel.ONE);

                return createReader(new ByteArrayInputStream(column.getColumn().getValue()));
            } catch (NotFoundException nfe) {
                throw new EntityNotFoundException(ref);
            } catch (Exception e) {
                throw new EntityStoreException(e);
            }
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

    public ColumnPath getEntryColumnPath() {
        return entryColumnPath;
    }

    class MapUpdater implements MapEntityStore.MapChanger {

        private final Map<String, Map<String, List<Mutation>>> mutationMap = new HashMap<String, Map<String, List<Mutation>>>();

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
            createMutationHolder(ref.identity()).add(new Mutation().setDeletion(new Deletion(System.currentTimeMillis())));
        }

        private Writer getWriter(final EntityReference ref) {
            try {
                return createWriter(new ByteArrayOutputStream(CassandraMapEntityStoreMixin.BYTE_ARRAY_BUFFER_INITIAL_SIZE) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                        ColumnOrSuperColumn cosc = new ColumnOrSuperColumn();
                        cosc.setColumn(new Column(getEntryColumnPath().getColumn(), toByteArray(), System.currentTimeMillis()));
                        createMutationHolder(ref.identity()).add(new Mutation().setColumn_or_supercolumn(cosc));
                    }
                });
            } catch (final Exception e) {
                throw new EntityStoreException(e);
            }
        }

        private List<Mutation> createMutationHolder(String key) {
            Map<String, List<Mutation>> keyMutations = mutationMap.get(key);

            if (keyMutations == null) {
                keyMutations = new HashMap<String, List<Mutation>>();
                mutationMap.put(key, keyMutations);
            }

            List<Mutation> columnFamilyMutations = keyMutations.get(CassandraMapEntityStoreMixin.entryColumnFamily);
            if (columnFamilyMutations == null) {
                columnFamilyMutations = new ArrayList<Mutation>();
                keyMutations.put(CassandraMapEntityStoreMixin.entryColumnFamily, columnFamilyMutations);
            }
            return columnFamilyMutations;
        }
    }
}