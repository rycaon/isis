package org.nakedobjects.persistence.sql.auto;

import org.nakedobjects.object.InternalCollection;
import org.nakedobjects.object.LoadedObjects;
import org.nakedobjects.object.Naked;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedValue;
import org.nakedobjects.object.ObjectNotFoundException;
import org.nakedobjects.object.Oid;
import org.nakedobjects.object.ResolveException;
import org.nakedobjects.object.UnsupportedFindException;
import org.nakedobjects.object.defaults.SerialOid;
import org.nakedobjects.object.reflect.FieldSpecification;
import org.nakedobjects.object.reflect.OneToManyAssociationSpecification;
import org.nakedobjects.object.reflect.OneToOneAssociationSpecification;
import org.nakedobjects.object.reflect.ValueFieldSpecification;
import org.nakedobjects.persistence.sql.AbstractObjectMapper;
import org.nakedobjects.persistence.sql.DatabaseConnector;
import org.nakedobjects.persistence.sql.ObjectMapper;
import org.nakedobjects.persistence.sql.Results;
import org.nakedobjects.persistence.sql.SqlObjectStoreException;
import org.nakedobjects.persistence.sql.ValueMapper;
import org.nakedobjects.persistence.sql.ValueMapperLookup;

import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * @deprecated
 */
public class NameBasedMapper extends AbstractObjectMapper implements ObjectMapper {
    private static final Logger LOG = Logger.getLogger(NameBasedMapper.class);
	private ValueMapperLookup typeMapper;

	public NameBasedMapper() {
		typeMapper = ValueMapperLookup.getInstance();
	}
	

	private String table(NakedObjectSpecification cls) {
		String name = cls.getFullName();
		return "no_" + name.substring(name.lastIndexOf('.') + 1).toLowerCase();
	}

	private String columns(NakedObjectSpecification cls) {
        StringBuffer sb = new StringBuffer();
        FieldSpecification[] fields = cls.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].isDerived() || fields[i] instanceof OneToManyAssociationSpecification) {
                continue;
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(columnName(fields[i]));
        }
        return sb.toString();
    }

   
    private String columnName(FieldSpecification field) {
        return field.getName().replace(' ', '_').toLowerCase();
    }


    public void createObject(DatabaseConnector connector, NakedObject object) throws SqlObjectStoreException {
        NakedObjectSpecification cls = object.getSpecification();

        String table = table(cls);
        String columns = columns(cls);
        String values = values(cls, object);
        long id = ((SerialOid) object.getOid()).getSerialNo();
        String statement = "insert into " + table + " (ID, " + columns + ") values (" + id + values + ")";

        // one-to-many assoc are not persisted - see save
        connector.update(statement);
    }

    public void destroyObject(DatabaseConnector connector, NakedObject object) throws SqlObjectStoreException {
        NakedObjectSpecification cls = object.getSpecification();
        String table = table(cls);
        long id = ((SerialOid) object.getOid()).getSerialNo();
        String statement = "delete from " + table + " where id = " + id;
        connector.update(statement);
    }

    public NakedObject[] getInstances(DatabaseConnector connector, NakedObjectSpecification cls) throws SqlObjectStoreException {
        Vector instances = new Vector();

        String table = table(cls);
        LOG.debug("loading instances from SQL " + table);
        String statement = "select id from " + table + " order by id";
        Results rs = connector.select(statement);
        while (rs.next()) {
            int id = rs.getInt("id"); 
            NakedObject instance = setupReference(loadedObjects, cls, id);
            LOG.debug("  instance  " + instance);
            instances.addElement(instance);
        }
        rs.close();
        return toInstancesArray(instances);
    }

    public NakedObject[] getInstances(DatabaseConnector connector, NakedObjectSpecification cls, String pattern) throws SqlObjectStoreException, UnsupportedFindException {
        Vector instances = new Vector();

       String table = table(cls);
        LOG.debug("loading instances from SQL " + table);
        String statement = "select id from " + table + " order by id";
        Results rs = connector.select(statement);
        while (rs.next()) {
            int id = rs.getInt("id"); 
            NakedObject instance = setupReference(loadedObjects, cls, id);
            LOG.debug("  instance  " + instance);
            instances.addElement(instance);
        }
        rs.close();

        return toInstancesArray(instances);
    }
    
    protected NakedObject[] toInstancesArray(Vector instances) {
        NakedObject[] array = new NakedObject[instances.size()];
        instances.copyInto(array);
        return array;
    }

    public NakedObject[] getInstances(DatabaseConnector connector, NakedObject pattern) throws SqlObjectStoreException, UnsupportedFindException {
        throw new UnsupportedFindException();
    }

    public NakedObject getObject(DatabaseConnector connector, Oid oid, NakedObjectSpecification hint) throws ObjectNotFoundException, SqlObjectStoreException {
        NakedObject object = (NakedObject) hint.acquireInstance();
        object.setOid(oid);
        loadedObjects.loaded(object);
        return object;
    }

    public boolean hasInstances(DatabaseConnector connector, NakedObjectSpecification cls) throws SqlObjectStoreException {
        return numberOfInstances(connector, cls) > 0;
    }

    private void loadInternalCollection(DatabaseConnector connector, String id, FieldSpecification field, InternalCollection collection) throws ResolveException, SqlObjectStoreException {
        NakedObjectSpecification cls = collection.forParent().getSpecification();
        NakedObjectSpecification elementCls = NakedObjectSpecification.getSpecification(collection.getType().getFullName());

        String table = table(cls) + "_" + field.getName().toLowerCase();
        LOG.debug("loading internal collection data from SQL " + table);
        String a = field.getName().toLowerCase() + "_id";
        String b = table(cls) + "_id";
        String statement = "select " + a + " from " + table + " where " + b + " = " + id;
        Results rs = connector.select(statement);
        while(rs.next()) {
            int ref = rs.getInt(a);
            
            NakedObject element = setupReference(loadedObjects, elementCls, ref);
            LOG.debug("  element  " + element);
            collection.added(element);
        }
        rs.close();
        collection.setResolved();
    }

    private NakedObject setupReference(LoadedObjects manager, NakedObjectSpecification elementCls, int id) {
        NakedObject element;
        SerialOid oid = new SerialOid(id);
        if (manager.isLoaded(oid)) {
            element = manager.getLoadedObject(oid);
        } else {
            element = (NakedObject) elementCls.acquireInstance();
            element.setOid(oid);
            manager.loaded(element);
        }
        return element;
    }

    public int numberOfInstances(DatabaseConnector connector, NakedObjectSpecification cls) throws SqlObjectStoreException {
        String table = table(cls);
        LOG.debug("counting instances in SQL " + table);
        String statement = "select count(*) from " + table;
        return connector.count(statement);
    }

    public void resolve(DatabaseConnector connector, NakedObject object) throws SqlObjectStoreException {
        NakedObjectSpecification cls = object.getSpecification();
        String table = table(cls);
        String columns = columns(cls);
        String id = primaryKey(object.getOid());
        
        LOG.debug("loading data from SQL " + table + " for " + object);
        String statement = "select " + columns + " from " + table + " where id = " + id;
        Results rs = connector.select(statement);
        if (rs.next()) {
            FieldSpecification[] fields = cls.getFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].isDerived()) {
                    continue;
                } else if (fields[i] instanceof OneToManyAssociationSpecification) {
                   DatabaseConnector connection = connector.getConnectionPool().acquire();
                    loadInternalCollection(connection, id, fields[i], (InternalCollection) fields[i].get(object));
                    connector.getConnectionPool().release(connection);
                } else if (fields[i] instanceof ValueFieldSpecification) {
                    ValueMapper mapper = ValueMapperLookup.getInstance().mapperFor(fields[i].getType());
                    mapper.setFromDBColumn(columnName(fields[i]), fields[i], object, rs);
               } else if (fields[i] instanceof OneToOneAssociationSpecification) {
                    NakedObjectSpecification associatedCls = fields[i].getType();
                    NakedObject reference = setupReference(loadedObjects, associatedCls, rs.getInt(columnName(fields[i])));
                    ((OneToOneAssociationSpecification) fields[i]).setAssociation(object, reference);
                }
            }
            object.setResolved();
	        rs.close();
        } else {
	        rs.close();
            throw new SqlObjectStoreException("Unable to load data for " + id + " from " + table);
        }
    }

    public void save(DatabaseConnector connector, NakedObject object) throws SqlObjectStoreException {
        NakedObjectSpecification cls = object.getSpecification();

        String table = table(cls);

        StringBuffer sb = new StringBuffer();
        FieldSpecification[] fields = cls.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].isDerived()) {
                continue;
            }
            Naked fieldValue = fields[i].get(object);
            if (fields[i] instanceof OneToManyAssociationSpecification) {
                saveInternalCollection(connector, fields[i], (InternalCollection) fieldValue);
            } else {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(columnName(fields[i]));
                sb.append('=');
                if (fieldValue instanceof NakedObject) {
                    if (fieldValue == null) {
                        sb.append("NULL");
                    } else {
                        sb.append(primaryKey((NakedObject) fieldValue));
                    }
                } else if (fieldValue instanceof NakedValue) {
                    ValueMapper mapper = typeMapper.mapperFor(fields[i].getType());
                    sb.append(mapper.valueAsDBString((NakedValue) fieldValue));
                } else {
                    sb.append("NULL");
                }
            }
        }
        String assignments = sb.toString();

        long id = ((SerialOid) object.getOid()).getSerialNo();
        String statement = "update " + table + " set " + assignments + " where ID = " + id;
        connector.update(statement);
    }

    private void saveInternalCollection(DatabaseConnector connector, FieldSpecification field, InternalCollection collection) throws SqlObjectStoreException {
        NakedObjectSpecification cls = collection.forParent().getSpecification();

        String table = table(cls) + "_" + field.getName().toLowerCase();

        long parentId = ((SerialOid) collection.forParent().getOid()).getSerialNo();
        int size = collection.size();
        for (int i = 0; i < size; i++) {
            NakedObject element = collection.elementAt(i);
            String columns = table(cls) + "_id, " + field.getName().toLowerCase() + "_id";
            long elementId = ((SerialOid) element.getOid()).getSerialNo();
            String values = parentId + ", " + elementId;
            String statement = "insert into " + table + " (" + columns + ") values (" + values + ")";
            connector.update(statement);
        }
    }

    private String values(NakedObjectSpecification cls, NakedObject object) {
        StringBuffer sb = new StringBuffer();
        FieldSpecification[] fields = cls.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].isDerived() || fields[i] instanceof OneToManyAssociationSpecification) {
                continue;
            }
            sb.append(", ");
            Naked fieldValue = fields[i].get(object);
            if (fieldValue == null) {
                sb.append("NULL");
            } else {
                sb.append("'" + fieldValue.titleString() + "'");
            }
        }
        return sb.toString();
    }

}

/*
 * Naked Objects - a framework that exposes behaviourally complete business objects directly to the
 * user. Copyright (C) 2000 - 2004 Naked Objects Group Ltd This program is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA The authors can be
 * contacted via www.nakedobjects.org (the registered address of Naked Objects Group is Kingsway
 * House, 123 Goldworth Road, Woking GU21 1NR, UK).
 */