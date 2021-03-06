/*
 *    GeotoolKit - An Open Source Java GIS Toolkit
 *    http://geotoolkit.org
 *
 *    (C) 2009, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotoolkit.resources.jaxb.feature.catalog;

import com.sun.xml.internal.bind.IDResolver;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import org.geotoolkit.feature.catalog.Referenceable;
import org.apache.sis.util.logging.Logging;

/**
 *
 * @author guilhem
 * @since 3.03
 * @module pending
 */
public class DocumentIDResolver extends IDResolver {
  Map<String, Referenceable> referenceables = new HashMap<String, Referenceable>();


  private Logger logger = Logging.getLogger("DocumentIdResolver");


  void startDocument() {
    referenceables.clear();
  }

  public void bind(final String id, final Object obj) {
    if (obj instanceof Referenceable)
      referenceables.put(id, (Referenceable)obj);
    else
       logger.info("not refereanceable type: " + obj.getClass().getSimpleName());
  }

  public Callable resolve(final String id, final Class targetType) {
    return new Callable() {
      public Object call() throws Exception {
        logger.finer("searching id="  + id + " of type " + targetType.getSimpleName());
        Object result;
        boolean referenceable = false;
        for (Class c:targetType.getInterfaces()) {
            if (c.equals(Referenceable.class)) {
                referenceable = true;
            }
        }

        if (referenceable)
            result = referenceables.get(id);
        else {
            logger.severe("not a referenceable targetType: " + targetType.getSimpleName());
            result = null;
        }
        if (result == null) {
            logger.severe("unable to find an object for this id=" + id);
        }
        return result;
      }
    };
  }
}
