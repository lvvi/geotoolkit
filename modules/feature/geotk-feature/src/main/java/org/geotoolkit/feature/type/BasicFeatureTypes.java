/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.feature.type;

import java.util.Collections;
import java.util.logging.Level;

import org.geotoolkit.feature.DefaultName;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.simple.DefaultSimpleFeatureType;

import org.opengis.feature.simple.SimpleFeatureType;


/**
 * Defines required attributes for Annotations.
 *
 * <p>
 * Annotations represent a text based geographic feature.
 * The geometry stored in the feature indicates where the
 * text should be drawn and the attribute indicated by
 * the {@link #GEOMETRY_ATTRIBUTE_NAME} attribute holds
 * the text to be displayed for the feature.
 * </p>
 *
 * <p>Example:
 * <pre>
 *   if ( feature.getFeatureType().isDescendedFrom( AnnotationFeatureType.ANNOTATION ) )
 *   {
 *     String attributeName = (String)feature.getAttribute( AnnotationFeatureType.ANNOTATION_ATTRIBUTE_NAME );
 *     String annotationText = (String)feature.getAttribute( attributeName );
 *     ... // Do something with the annotation text and feature
 *   }
 * </pre>
 * </p>
 *
 * @author John Meagher
 * @module pending
 */
public class BasicFeatureTypes {
    /**
     * The base type for all features
     */
    public static final SimpleFeatureType FEATURE;
    /**
     * The FeatureType reference that should be used for Polygons
     */
    public static final SimpleFeatureType POLYGON;
    /**
     * The FeatureType reference that should be used for Points
     */
    public static final SimpleFeatureType POINT;
    /**
     * The FeatureType reference that should be used for Lines
     */
    public static final SimpleFeatureType LINE;
    /**
     * The attribute name used to store the geometry
     */
    public static final String GEOMETRY_ATTRIBUTE_NAME = "the_geom";
    /**
     * Default namespace used for our POINT, LINE, POLYGON types.
     */
    public static final String DEFAULT_NAMESPACE = "http://www.opengis.net/gml";
    // Static initializer for the tyoe variables

    static {
        SimpleFeatureType tmpPoint = null;
        SimpleFeatureType tmpPolygon = null;
        SimpleFeatureType tmpLine = null;

        // Feature is the base of everything else, must be created directly instead
        // of going thru the builder because the builder assumes it as the default base type
        FEATURE = new DefaultSimpleFeatureType(new DefaultName("Feature"),
                Collections.EMPTY_LIST, null, true,
                Collections.EMPTY_LIST, null, null);

        try {
            FeatureTypeBuilder build = new FeatureTypeBuilder();

            //AttributeDescriptor[] types =  new AttributeDescriptor[] {};

            build.setName(DEFAULT_NAMESPACE,"pointFeature");
            tmpPoint = build.buildSimpleFeatureType();

            build.setName(DEFAULT_NAMESPACE,"lineFeature");
            tmpLine = build.buildSimpleFeatureType();

            build.setName(DEFAULT_NAMESPACE,"polygonFeature");
            tmpPolygon = build.buildSimpleFeatureType();
        } catch (Exception ex) {
            org.apache.sis.util.logging.Logging.getLogger("org.geotoolkit.feature.type.BasicFeatureTypes").log(
                    Level.WARNING, "Error creating basic feature types", ex);
        }
        POINT = tmpPoint;
        LINE = tmpLine;
        POLYGON = tmpPolygon;
    }

    /**
     * Noone else should be able to build me.
     */
    private BasicFeatureTypes() {
    }
}