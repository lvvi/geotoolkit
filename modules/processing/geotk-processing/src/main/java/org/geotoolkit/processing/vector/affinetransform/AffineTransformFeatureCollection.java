/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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
package org.geotoolkit.processing.vector.affinetransform;

import org.geotoolkit.data.FeatureStoreRuntimeException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.memory.GenericTransformFeatureIterator;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.data.memory.WrapFeatureCollection;

import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.type.FeatureType;

/**
 * FeatureCollection for AffineTransform process
 * @author Quentin Boileau
 * @module pending
 */
public class AffineTransformFeatureCollection extends WrapFeatureCollection {

    private final FeatureType newFeatureType;
    private final java.awt.geom.AffineTransform transform;

    /**
     * Connect to the original FeatureConnection with an intersection filter
     * @param originalFC - FeatureCollection
     * @param transform - AffineTransformation
     */
    public AffineTransformFeatureCollection(final FeatureCollection originalFC, final java.awt.geom.AffineTransform transform ) {
        super(originalFC);
        this.transform = transform;
        this.newFeatureType = super.getFeatureType();

    }

    /**
     * Return the new FeatureType
     * @return FeatureType
     */
    @Override
    public FeatureType getFeatureType() {
        return newFeatureType;
    }

    /**
     * Return a FeatureIterator with an affine transformation
     * @param hints
     * @return the FeatureIterator
     * @throws FeatureStoreRuntimeException
     */
    @Override
    public FeatureIterator iterator(final Hints hints) throws FeatureStoreRuntimeException {
        return (GenericTransformFeatureIterator.wrap(getOriginalFeatureCollection(),
                new AffineTransformGeometryTransformer(transform))).iterator();
    }

    /**
     *  {@inheritDoc }
     */
    @Override
    protected Feature modify(final Feature original) {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder build = new StringBuilder();
        build.append(getFeatureType().toString());
        build.append(this.size());
        return build.toString();
    }


}
