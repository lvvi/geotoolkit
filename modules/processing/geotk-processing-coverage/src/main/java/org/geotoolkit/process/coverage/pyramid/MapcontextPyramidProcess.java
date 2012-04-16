/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
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

package org.geotoolkit.process.coverage.pyramid;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import org.geotoolkit.coverage.GridMosaic;
import org.geotoolkit.coverage.Pyramid;
import org.geotoolkit.coverage.PyramidalModel;
import org.geotoolkit.display.exception.PortrayalException;
import org.geotoolkit.display2d.service.CanvasDef;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.geotoolkit.display2d.service.SceneDef;
import org.geotoolkit.display2d.service.ViewDef;
import org.geotoolkit.factory.Hints;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.process.AbstractProcess;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.storage.DataStoreException;
import org.geotoolkit.util.ArgumentChecks;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


import static org.geotoolkit.parameter.Parameters.*;
import static org.geotoolkit.process.coverage.pyramid.MapcontextPyramidDescriptor.*;
/**
 * Create a pyramid in the given PyramidalModel.
 * If a pyramid with the given CRS already exist it will be reused.
 * If a mosaic at the given scale exist it will be used.
 * Missing tiles in the mosaic will be generated.
 *
 * @author Johann Sorel (Geomatys)
 * @module pending
 */
public final class MapcontextPyramidProcess extends AbstractProcess {

    MapcontextPyramidProcess(final ParameterValueGroup input) {
        super(INSTANCE,input);
    }

    @Override
    protected void execute() throws ProcessException {
        ArgumentChecks.ensureNonNull("inputParameters", inputParameters);

        final MapContext context = value(IN_MAPCONTEXT, inputParameters);
        final Envelope envelope = value(IN_EXTENT, inputParameters);
        final Dimension tileSize = value(IN_TILE_SIZE, inputParameters);
        final double[] scales = value(IN_SCALES, inputParameters);
        final PyramidalModel container = value(IN_CONTAINER, inputParameters);

        Hints hints = null;
        try{
            hints = value(IN_HINTS, inputParameters);
        } catch (ParameterNotFoundException ex) {
            // Get the hint parameter if exist, otherwise keep it to null.
        }

        final CanvasDef canvasDef = new CanvasDef(new Dimension(1, 1), null);
        final ViewDef viewDef = new ViewDef(envelope);
        final SceneDef sceneDef = new SceneDef(context,hints);

        //find if we already have a pyramid in the given CRS
        Pyramid pyramid = null;
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        try {
            for (Pyramid candidate : container.getPyramidSet().getPyramids()) {
                if (CRS.equalsApproximatively(crs, candidate.getCoordinateReferenceSystem())) {
                    pyramid = candidate;
                    break;
                }
            }

            if (pyramid == null) {
                //we didn't find a pyramid, create one
                pyramid = container.createPyramid(crs);
            }

            //generate each mosaic
            for (final double scale : scales) {
                final double gridWidth  = envelope.getSpan(0) / (scale*tileSize.width);
                final double gridHeight = envelope.getSpan(1) / (scale*tileSize.height);

                //those parameters can change if another mosaic already exist
                Point2D upperleft = new Point2D.Double(envelope.getMinimum(0), envelope.getMaximum(1));
                Dimension tileDim = tileSize;
                Dimension gridSize = new Dimension( (int)(gridWidth+0.5), (int)(gridHeight+0.5));

                //check if we already have a mosaic at this scale
                GridMosaic mosaic = null;
                int index = 0;
                for (double sc : pyramid.getScales()) {
                    if (sc == scale) {
                        mosaic = pyramid.getMosaic(index);
                        //this mosaic definition replaces the given one
                        upperleft = mosaic.getUpperLeftCorner();
                        tileDim = mosaic.getTileSize();
                        gridSize = mosaic.getGridSize();
                        break;
                    }
                    index++;
                }

                if (mosaic == null) {
                    //create a new mosaic
                    mosaic = container.createMosaic(pyramid.getId(),
                        gridSize, tileDim, upperleft, scale);
                }

                final RenderedImage image = DefaultPortrayalService.prepareImage(
                        canvasDef, sceneDef, viewDef,
                        mosaic.getGridSize(), mosaic.getTileSize(), scale);

                container.writeTiles(pyramid.getId(), mosaic.getId(), image, true);
                
                getOrCreate(OUT_CONTAINER, outputParameters).setValue(container);
            }

        } catch (DataStoreException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        } catch (PortrayalException ex) {
            throw new ProcessException(ex.getMessage(), this, ex);
        }
    }

}
