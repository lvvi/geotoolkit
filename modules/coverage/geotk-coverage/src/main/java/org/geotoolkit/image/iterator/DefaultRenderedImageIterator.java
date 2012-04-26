/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2012, Geomatys
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
package org.geotoolkit.image.iterator;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import org.geotoolkit.util.ArgumentChecks;

/**
 * An Iterator for traversing anyone rendered Image.
 * <p>
 * Iteration transverse each tiles(raster) from rendered image source one by one in order.
 * Iteration to follow tiles(raster) begin by raster bands, next, raster x coordinates,
 * and to finish raster y coordinates.
 * <p>
 * Iteration follow this scheme :
 * tiles band --&lt; tiles x coordinates --&lt; tiles y coordinates --&lt; next rendered image tiles.
 *
 * Moreover iterator traversing a read-only each rendered image tiles(raster) in top-to-bottom, left-to-right order.
 *
 * Code example :
 * {@code
 *                  final DefaultRenderedImageIterator dRII = new DefaultRenderedImageIterator(renderedImage);
 *                  while (dRII.next()) {
 *                      dRii.getSample();
 *                  }
 * }
 *
 * @author Rémi Marechal       (Geomatys).
 * @author Martin Desruisseaux (Geomatys).
 */
public class DefaultRenderedImageIterator extends RasterBasedIterator {

//    /**
//     * Current raster which is followed by Iterator.
//     */
//    private Raster currentRaster;

    /**
     * true if raster constructor is used else false.
     */
    private final boolean raster;
    /**
     * RenderedImage which is followed by Iterator.
     */
    private RenderedImage renderedImage;
//
//    /**
//     * Number of raster bands .
//     */
//    private int numBand;
//
//    /**
//     * The X coordinate of the upper-left pixel of current Raster.
//     */
//    private int minX;
//
//    /**
//     * The Y coordinate of the upper-left pixel of current Raster.
//     */
//    private int minY;
//
//    /**
//     * The X coordinate of the bottom-right pixel of current Raster.
//     */
//    private int maxX;
//
//    /**
//     * The Y coordinate of the bottom-right pixel of current Raster.
//     */
//    private int maxY;

    /**
     * The X index coordinate of the upper-left tile of this rendered image.
     */
    private int tMinX;

    /**
     * The Y index coordinate of the upper-left tile of this rendered image.
     */
    private int tMinY;

    /**
     * The X index coordinate of the bottom-right tile of this rendered image.
     */
    private int tMaxX;

    /**
     * The Y index coordinate of the bottom-right tile of this rendered image.
     */
    private int tMaxY;

    /**
     * The X coordinate of the sub-Area upper-left corner.
     */
    private int subAreaMinX;

    /**
     * The Y coordinate of the sub-Area upper-left corner.
     */
    private int subAreaMinY;

    /**
     * The X index coordinate of the sub-Area bottom-right corner.
     */
    private int subAreaMaxX;

    /**
     * The Y index coordinate of the sub-Area bottom-right corner.
     */
    private int subAreaMaxY;

//    /**
//     * Current X pixel coordinate in current rendered image raster.
//     */
//    private int x;
//
//    /**
//     * Current Y pixel coordinate in current rendered image raster.
//     */
//    private int y;
//
//    /**
//     * Current band position in current rendered image raster.
//     */
//    private int band;

    /**
     * Current x tile position in rendered image tile array.
     */
    private int tX;
    /**
     * Current y tile position in rendered image tile array.
     */
    private int tY;

    /**
     * Create default rendered image iterator.
     *
     * @param renderedImage : image which will be follow by iterator.
     */
    public DefaultRenderedImageIterator(final RenderedImage renderedImage) {
        super();
        this.raster = false;
        this.renderedImage = renderedImage;
        //rect attributs
        this.subAreaMinX = renderedImage.getMinX();
        this.subAreaMinY = renderedImage.getMinY();
        this.subAreaMaxX = this.subAreaMinX + renderedImage.getWidth();
        this.subAreaMaxY = this.subAreaMinY + renderedImage.getHeight();
        //tiles attributs
        this.tMinX = renderedImage.getMinTileX();
        this.tMinY = renderedImage.getMinTileY();
        this.tMaxX = tMinX + renderedImage.getNumXTiles();
        this.tMaxY = tMinY + renderedImage.getNumYTiles();
        //initialize attributs to first iteration
        this.numBand = this.maxX = this.maxY = 1;
        this.tY = tMinY;
        this.tX = tMinX - 1;
    }

    /**
     * Create default rendered image iterator.
     *
     * @param renderedImage : image which will be follow by iterator.
     * @param subArea : Rectangle which represent image sub area iteration.
     * @throws IllegalArgumentException if subArea don't intersect image.
     */
    public DefaultRenderedImageIterator(final RenderedImage renderedImage, final Rectangle subArea) {
        super();
        this.raster = false;
        this.renderedImage = renderedImage;
        //rect attributs
        this.subAreaMinX = subArea.x;
        this.subAreaMinY = subArea.y;
        this.subAreaMaxX = this.subAreaMinX + subArea.width;
        this.subAreaMaxY = this.subAreaMinY + subArea.height;
        //define min max intervals
        final int minIAX = Math.max(renderedImage.getMinX(), subAreaMinX);
        final int minIAY = Math.max(renderedImage.getMinY(), subAreaMinY);
        final int maxIAX = Math.min(renderedImage.getMinX() + renderedImage.getWidth(), subAreaMaxX);
        final int maxIAY = Math.min(renderedImage.getMinY() + renderedImage.getHeight(), subAreaMaxY);
        //intersection test
        if (minIAX > maxIAX || minIAY > maxIAY)
        throw new IllegalArgumentException("invalid subArea coordinate no intersection between it and RenderedImage"+renderedImage+subArea);
        //tiles attributs
        final int rITWidth   = renderedImage.getTileWidth();
        final int rITHeight  = renderedImage.getTileHeight();
        final int rIMinTileX = renderedImage.getMinTileX();
        final int rIMinTileY = renderedImage.getMinTileY();
        this.tMinX = minIAX / rITWidth  + rIMinTileX;
        this.tMinY = minIAY / rITHeight + rIMinTileY;
        this.tMaxX = maxIAX / rITWidth  + rIMinTileX;
        this.tMaxY = maxIAY / rITHeight + rIMinTileY;
        this.tY = tMinY;
        //initialize attributs to first iteration
        this.numBand = this.maxX = this.maxY = 1;
        this.tX = tMinX - 1;
    }

    /**
     * Create raster iterator to follow from its minX and minY coordinates.
     *
     * @param raster will be followed by this iterator.
     */
    public DefaultRenderedImageIterator(final Raster raster) {
        super(raster);
        this.raster = true;
//        ArgumentChecks.ensureNonNull("Raster : ", raster);
//        this.raster = true;
//        this.currentRaster   = raster;
//        this.renderedImage = null;
//        this.minX     = raster.getMinX();
//        this.minY     = raster.getMinY();
//        x = minX;
//        y = minY;
//        this.numBand  = raster.getNumBands();
//        this.maxY     = minY + raster.getHeight();
//        this.maxX     = minX + raster.getWidth();
//        //rect attributs
//        this.subAreaMinX = minX;
//        this.subAreaMinY = minY;
//        this.subAreaMaxX = this.maxX;
//        this.subAreaMaxY = this.maxY;
//        //initialize attributs to first iteration
//        tMaxX = tMaxY = 1;
//        tMinX = tX = 0;
//        tMinY = tY = 0;//band a -1
//        this.band = -1;
    }

    /**
     * Create raster iterator to follow from minX, minY raster and rectangle2D intersection coordinate.
     *
     * @param raster will be followed by this iterator.
     */
    public DefaultRenderedImageIterator(final Raster raster, final Rectangle subArea) {
        super(raster, subArea);
        this.raster = true;
//        ArgumentChecks.ensureNonNull("Raster : ", raster);
//        ArgumentChecks.ensureNonNull("sub Area iteration : ", subArea);
//        this.currentRaster = raster;
//        this.renderedImage = null;
//        //rect attributs
//        this.subAreaMinX = subArea.x;
//        this.subAreaMinY = subArea.y;
//        this.subAreaMaxX = subAreaMinX + subArea.width;
//        this.subAreaMaxY = subAreaMinY + subArea.height;
//        final int minx   = raster.getMinX();
//        final int miny   = raster.getMinY();
//        final int maxx   = minx + raster.getWidth();
//        final int maxy   = miny + raster.getHeight();
//        this.numBand     = raster.getNumBands();
//        this.minX        =  Math.max(subAreaMinX, minx);
//        this.minY        =  Math.max(subAreaMinY, miny);
//        this.maxX        =  Math.min(subAreaMaxX, maxx);
//        this.maxY        =  Math.min(subAreaMaxY, maxy);
//        if(minX > maxX || this.y > maxY)
//        throw new IllegalArgumentException("invalid subArea coordinate no intersection between it and raster"+raster+subArea);
//        //initialize attributs to first iteration
//        tMaxX = tMaxY = 1;
//        tMinX = tX    = 0;
//        tMinY = tY    = 0;
//        this.band = -1;
//        x  = this.minX;
//        y  = this.minY;
    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public boolean next() {
        if (raster) return super.next();
        if (++band == numBand) {
            band = 0;
            if (++x == maxX) {
                x = minX;
                if (++y == maxY) {
                    if (++tX == tMaxX) {
                        tX = tMinX;
                        if(++tY == tMaxY) return false;
                    }
                    //initialize from new tile(raster).
                    currentRaster = renderedImage.getTile(tX, tY);
                    final int cRMinX = currentRaster.getMinX();
                    final int cRMinY = currentRaster.getMinY();
                    this.minX = this.x = Math.max(subAreaMinX, cRMinX);
                    this.y = Math.max(subAreaMinY, cRMinY);
                    this.maxX = Math.min(subAreaMaxX, cRMinX + currentRaster.getWidth());
                    this.maxY = Math.min(subAreaMaxY, cRMinY + currentRaster.getHeight());
                    this.numBand = currentRaster.getNumBands();
                }
            }
        }
        return true;
    }
//
//    /**
//     * {@inheritDoc }.
//     */
//    @Override
//    public int getX() {
//        return x;
//    }
//
//    /**
//     * {@inheritDoc }.
//     */
//    @Override
//    public int getY() {
//        return y;
//    }
//
//    /**
//     * {@inheritDoc }.
//     */
//    @Override
//    public int getSample() {
//        return currentRaster.getSample(x, y, band);
//    }
//
//    /**
//     * {@inheritDoc }.
//     */
//    @Override
//    public float getSampleFloat() {
//        return currentRaster.getSampleFloat(x, y, band);
//    }
//
//    /**
//     * {@inheritDoc }.
//     */
//    @Override
//    public double getSampleDouble() {
//        return currentRaster.getSampleDouble(x, y, band);
//    }

    /**
     * {@inheritDoc }.
     */
    @Override
    public void rewind() {
        if (this.raster) {
            super.rewind();
        } else {
            this.x    = this.y    = this.band    = 0;
            this.maxX = this.maxY = this.numBand = 1;
            this.tX   = tMinX - 1;
            this.tY   = tMinY;
        }
    }
}
