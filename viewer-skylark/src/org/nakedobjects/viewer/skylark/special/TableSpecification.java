package org.nakedobjects.viewer.skylark.special;

import org.nakedobjects.object.Naked;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.defaults.collection.AbstractTypedNakedCollectionVector;
import org.nakedobjects.object.reflect.FieldSpecification;
import org.nakedobjects.object.security.Session;
import org.nakedobjects.viewer.skylark.Canvas;
import org.nakedobjects.viewer.skylark.CompositeViewBuilder;
import org.nakedobjects.viewer.skylark.CompositeViewSpecification;
import org.nakedobjects.viewer.skylark.Content;
import org.nakedobjects.viewer.skylark.ObjectContent;
import org.nakedobjects.viewer.skylark.Style;
import org.nakedobjects.viewer.skylark.View;
import org.nakedobjects.viewer.skylark.ViewAxis;
import org.nakedobjects.viewer.skylark.ViewSpecification;
import org.nakedobjects.viewer.skylark.basic.WindowDecorator;
import org.nakedobjects.viewer.skylark.core.AbstractBorder;
import org.nakedobjects.viewer.skylark.core.AbstractBuilderDecorator;
import org.nakedobjects.viewer.skylark.core.AbstractCompositeViewSpecification;

class TableHeader extends AbstractBorder {
    private final static int padding = 2;

    public TableHeader(View view) {
        super(view);
        top = padding + Style.LABEL.getHeight() + padding;
    }

    public void draw(Canvas canvas) {
        int y = padding + Style.LABEL.getAscent();

        TableColumnAxis axis = ((TableColumnAxis) getViewAxis());
        FieldSpecification[] fields = axis.getFields();
        int[] widths = axis.getWidths();
        int x = axis.getOffset();
        for (int i = 0; i < fields.length; i++) {
            FieldSpecification field = fields[i];
            
            String label = field.getLabel();
            canvas.drawText(label, x, y, Style.SECONDARY1, Style.LABEL);
            x += widths[i];
        }

        super.draw(canvas);
    }

    public String toString() {
        return wrappedView.toString() + "/TableHeader";
    }
}

class TableHeaderBuilder extends AbstractBuilderDecorator {
    // could this be the axis?
    public TableHeaderBuilder(CompositeViewBuilder design) {
        super(design);
    }

    public View createCompositeView(Content content, CompositeViewSpecification specification, ViewAxis axis) {
        AbstractTypedNakedCollectionVector coll = (AbstractTypedNakedCollectionVector) ((ObjectContent) content).getObject();
        FieldSpecification[] viewFields = NakedObjectSpecification.getSpecification(coll.getType().getFullName()).getVisibleFields(
                null, Session.getSession().getContext());
        TableColumnAxis tableAxis = new TableColumnAxis(viewFields, 100);

        View view = wrappedBuilder.createCompositeView(content, specification, tableAxis);

        tableAxis.setRoot(view);

        return new TableHeader(view);
    }
}

public class TableSpecification extends AbstractCompositeViewSpecification implements SubviewSpec {
    private ViewSpecification rowSpecification = new TableRowSpecification();

    public TableSpecification() {
        builder = new WindowDecorator(new TableHeaderBuilder(new StackLayout(new CollectionElementBuilder(this))));
    }

    public boolean canDisplay(Naked object) {
        return object instanceof AbstractTypedNakedCollectionVector;
    }

    public View createSubview(Content content, ViewAxis axis) {
        return rowSpecification.createView(content, axis);
    }

    public String getName() {
        return "Standard Table";
    }

    public boolean isReplaceable() {
        return false;
    }
}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2004 Naked Objects Group
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