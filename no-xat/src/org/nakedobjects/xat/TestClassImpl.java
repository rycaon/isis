package org.nakedobjects.xat;

import org.nakedobjects.object.Naked;
import org.nakedobjects.object.NakedClass;
import org.nakedobjects.object.NakedCollection;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedObjectContext;
import org.nakedobjects.object.NakedObjectRuntimeException;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.NakedObjectStore;
import org.nakedobjects.object.NotPersistableException;
import org.nakedobjects.object.TypedNakedCollection;
import org.nakedobjects.object.reflect.ActionSpecification;

import java.util.Enumeration;


public class TestClassImpl implements TestClass {

    public static void init(NakedObjectStore objectStore) {}

    private NakedObjectContext context;
    private NakedClass nakedClass;
    private final TestObjectFactory factory;

    public TestClassImpl(NakedObjectContext context, NakedClass cls, TestObjectFactory factory) {
        this.context = context;
        nakedClass = cls;
        this.factory = factory;
    }

    /**
     * Finds the instance whose title matched the one specified. A match is any
     * substring matching the specified text, and the result is the first object
     * found that gives such a match, i.e. only one object is returned even
     * though more than one match might occur.
     */
    public TestObject findInstance(String title) {
        NakedCollection c = instances((NakedClass) getForObject());
        Enumeration e = c.elements();

        while (e.hasMoreElements()) {
            NakedObject object = ((NakedObject) e.nextElement());

            if (object.titleString().toString().indexOf(title) >= 0) {
                return factory.createTestObject(context, object);
            }
        }
        throw new IllegalActionError("No instance found with title " + title);
    }

    /**
     * Returns the NakedClass that this view represents.
     */
    public final Naked getForObject() {
        return nakedClass;
    }

    public String getTitle() {
        return nakedClass.titleString().toString();
    }

    /**
     * Get the instances of this class.
     */
    public TestObject instances() {
        NakedClass nakedClass = (NakedClass) getForObject();
        NakedCollection instances = instances((NakedClass) getForObject());
        if (instances.size() == 0) { throw new IllegalActionError("Find must find at least one object"); }
        return factory.createTestObject(context, instances);
    }

    private TypedNakedCollection instances(NakedClass cls) {
        NakedObjectContext context = NakedObjectContext.getDefaultContext();
        TypedNakedCollection instances = context.getObjectManager().allInstances(cls.forNakedClass());
 //       instances.first();
        return instances;
    }

    public TestObject invokeAction(final String name) {
        ActionSpecification action = nakedClass.forNakedClass().getClassAction(ActionSpecification.USER, name);

        if (action == null) { throw new IllegalActionError("No action " + name + " on the " + nakedClass.getPluralName()
                + " class."); }
        NakedObject result = action.execute(nakedClass);

        if (result == null) {
            return null;
        } else {
            return factory.createTestObject(context, result);
        }
    }

    public TestObject invokeAction(final String name, TestObject parameter) {
        NakedObject dropObject = (NakedObject) parameter.getForObject();
        ActionSpecification action = nakedClass.forNakedClass().getClassAction(ActionSpecification.USER, name, new NakedObjectSpecification[] { dropObject.getSpecification() });

        if (action == null) { throw new IllegalActionError("Can't drop a " + parameter.getForObject().getSpecification().getShortName() + " (for "
                + name + ") on the " + nakedClass.getPluralName() + " class."); }
        NakedObject result = action.execute(nakedClass, dropObject);

        if (result == null) {
            return null;
        } else {
            return factory.createTestObject(context, result);
        }
    }

    /**
     * Creates a new instance of this class.
     */
    public TestObject newInstance() {
        NakedObject object = newInstance(nakedClass);

         return factory.createTestObject(context, object);
    }

    private NakedObject newInstance(NakedClass cls) {
        NakedObject object;

        try {
            object = (NakedObject) cls.forNakedClass().acquireInstance();
            object.setContext(cls.getContext());
            object.getContext().makePersistent(object);

           // NakedObjectManager.getInstance().makePersistent(object); //makePersistent(object);
            object.created();
            object.getContext().getObjectManager().objectChanged(object);
        } catch (NotPersistableException e) {
            object = cls.getContext().getObjectManager().generatorError("Failed to create instance of " + cls.forNakedClass().getFullName(), e);

            System.out.println("Failed to create instance of " + cls.forNakedClass().getFullName());
            e.printStackTrace();
        }

        return object;
    }

    public void setForObject(Naked object) {
        throw new NakedObjectRuntimeException();
    }

}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2003 Naked Objects Group
 * Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address
 * of Naked Objects Group is Kingsway House, 123 Goldworth Road, Woking GU21
 * 1NR, UK).
 */