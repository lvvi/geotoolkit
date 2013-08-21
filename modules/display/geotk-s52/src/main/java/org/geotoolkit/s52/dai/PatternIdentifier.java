/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013, Geomatys
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
package org.geotoolkit.s52.dai;

/**
 * Pattern Identifier.
 * Identifies a pattern‑module.
 *
 * @author Johann Sorel (Geomatys)
 */
public class PatternIdentifier extends DAIField{

    /** A(2) : Module Identifier (Module Name):
     * presently a constant string = 'PT';
     * labels a module of the 'pattern'‑type. */
    public String MODN;
    /** I(5) : Record Identifier: continuous numbering where x is 00000 < x < 32768;
     * uniquely identifies a pattern‑module within the data‑transfer‑set. */
    public int RCID;
    /** A(3) : status of the module contents:
     * 'NIL' no change, used for new editions and editions */
    public String STAT;

    public PatternIdentifier() {
        super("PATT");
    }

    @Override
    protected void readSubFields(String str) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
