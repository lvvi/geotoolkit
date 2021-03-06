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
package org.geotoolkit.storage;

import org.apache.sis.storage.DataStoreException;

import java.nio.file.Path;


/**
 * Files-related {@linkplain org.apache.sis.storage.DataStore data stores}.
 *
 * @author Cédric Briançon (Geomatys)
 */
public interface DataFileStore {
    /**
     * Get all files pointed by this {@linkplain org.apache.sis.storage.DataStore data store}.
     *
     * @return Files used by this store. Should never be {@code null}.
     * @throws org.apache.sis.storage.DataStoreException
     */
    Path[] getDataFiles() throws DataStoreException;
}
