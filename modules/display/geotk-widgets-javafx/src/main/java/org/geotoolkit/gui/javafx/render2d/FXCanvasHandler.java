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
package org.geotoolkit.gui.javafx.render2d;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public interface FXCanvasHandler {

    /**
     *
     * @return current installed FXMap
     */
    FXMap getMap();
    
    void install(FXMap component);
    
    /**
     * 
     * @param component
     * @return true if the handler can be removed, false if the handle has raised a veto
     */
    boolean uninstall(FXMap component);
        
}
