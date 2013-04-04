package org.geotoolkit.data.mapinfo.mif.style;

import org.opengis.style.Symbolizer;

import java.io.Serializable;

/**
 * Base class to represent MIF styles.
 *
 * @author Alexis Manin (Geomatys)
 *         Date : 01/03/13
 */
public interface MIFSymbolizer extends Serializable, Symbolizer {

    public String toMIFText();
}
