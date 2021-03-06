/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotoolkit.gui.javafx.parameter;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class FXBooleanEditor extends FXValueEditor {

    private final CheckBox checkbox = new CheckBox();

    public FXBooleanEditor(Spi originatingSpi) {
        super(originatingSpi);
    }

    @Override
    public Node getComponent() {
        return checkbox;
    }

    @Override
    public BooleanProperty valueProperty() {
        return checkbox.selectedProperty();
    }

    public static final class Spi extends FXValueEditorSpi {

        @Override
        public boolean canHandle(Class binding) {
            return Boolean.class.isAssignableFrom(binding) || boolean.class.isAssignableFrom(binding);
        }

        @Override
        public FXValueEditor createEditor() {
            return new FXBooleanEditor(this);
        }
    }
}
