/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2010, Geomatys
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
package org.geotoolkit.image.io.plugin;

import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.metadata.IIOMetadata;
import javax.media.jai.JAI;
import javax.xml.parsers.ParserConfigurationException;

import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.image.io.ImageReaderAdapter;
import org.geotoolkit.image.io.metadata.SpatialMetadata;
import org.geotoolkit.image.io.metadata.SpatialMetadataFormat;
import org.geotoolkit.image.io.metadata.ReferencingBuilder;
import org.geotoolkit.internal.image.io.DimensionAccessor;
import org.geotoolkit.internal.image.io.GridDomainAccessor;
import org.geotoolkit.internal.image.io.Formats;
import org.geotoolkit.internal.io.IOUtilities;
import org.geotoolkit.lang.Configuration;
import org.geotoolkit.metadata.dimap.DimapMetadata;
import org.geotoolkit.metadata.dimap.DimapParser;
import org.geotoolkit.util.Version;
import org.geotoolkit.util.logging.Logging;

/**
 * Reader for the <cite>Dimap</cite> format. This reader wraps an other image reader
 * for an "ordinary" image format, like TIFF, PNG or JPEG. This {@code DimapImageReader}
 * delegates the reading of pixel values to the wrapped reader, and additionally looks for
 * a xml file in the same directory than the image file, with the same filename or constant name
 * metadata and extension .dim :
 *
 * <ul>
 *   <li><p>The dim file contain a complete metadata description of the image.
 *      This file may contain source, aquisition, referencing and color informations.
 *      So other informations may be found on different dimap profiles. Check the dimap
 *      description for the complete list of all metadatas available.
 *      </p>
 *   </li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 *
 * @see <a href="http://www.spotimage.com/web/154-le-format-dimap.php">DIMAP Description</a> *
 * @module pending
 */
public class DimapImageReader extends ImageReaderAdapter {

    public DimapImageReader(final Spi provider) throws IOException {
        super(provider);
    }

    public DimapImageReader(final Spi provider, final ImageReader main) {
        super(provider, main);
    }

    @Override
    protected Object createInput(final String readerID) throws IOException {
        if ("main".equalsIgnoreCase(readerID)) {
            return super.createInput(readerID);
        }else if("dim".equalsIgnoreCase(readerID)){
            File metadata = DimapImageReader.Spi.searchMetadataFile(input);
            return metadata;
        }
        throw new IOException("Unexpected reader id : " + readerID +" allowed ids are 'main' and 'dim'.");
    }

    private RenderedImage changeColorModel(RenderedImage image, boolean bufferedImage) throws IOException{
        if(image == null) return image;

        final DimapMetadata metadata = (DimapMetadata) getImageMetadata(0);
        final int[] colorMapping = DimapParser.readColorBandMapping((Element)metadata.getAsTree("dimap"));

        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.add(colorMapping);
        RenderedImage img = JAI.create("bandSelect",pb);

        if(bufferedImage){
            BufferedImage buffer = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            buffer.createGraphics().drawRenderedImage(img, new AffineTransform());
            return buffer;
        }else{
            return img;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BufferedImage read(final int imageIndex) throws IOException {
        return (BufferedImage) changeColorModel(super.read(imageIndex),true);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BufferedImage read(final int imageIndex, final ImageReadParam param) throws IOException {
        return (BufferedImage) changeColorModel(super.read(imageIndex,param),true);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public RenderedImage readAsRenderedImage(int imageIndex, ImageReadParam param) throws IOException {
        return changeColorModel(super.readAsRenderedImage(imageIndex, param),false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BufferedImage readTile(final int imageIndex, int tileX, int tileY) throws IOException {
        return (BufferedImage) changeColorModel(super.readTile(imageIndex, tileX, tileY),true);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Raster readRaster(final int imageIndex, final ImageReadParam param) throws IOException {
        return super.readRaster(imageIndex, param);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Raster readTileRaster(final int imageIndex, int tileX, int tileY) throws IOException {
        return super.readTileRaster(imageIndex, tileX, tileY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        return super.readThumbnail(imageIndex,thumbnailIndex);
    }


    @Override
    protected SpatialMetadata createMetadata(final int imageIndex) throws IOException {

        //parse the dimap metadata file
        final Object metaFile = createInput("dim");
        final Document doc;
        try {
            doc = DimapParser.read(metaFile);
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        } catch (SAXException ex) {
            throw new IOException(ex);
        }
        final Element dimapNode = doc.getDocumentElement();


        SpatialMetadata metadata = null;
        if (imageIndex >= 0) {
            final IIOMetadata iometa = main.getImageMetadata(imageIndex);
            if (iometa != null) {
                metadata = new DimapMetadata(SpatialMetadataFormat.IMAGE, this, iometa, dimapNode);
            }
        } else {
            final IIOMetadata iometa = main.getStreamMetadata();
            if (iometa != null) {
                metadata = new DimapMetadata(SpatialMetadataFormat.STREAM, this, iometa, dimapNode);
            }
        }

        if (imageIndex >= 0) {
            AffineTransform gridToCRS = null;
            CoordinateReferenceSystem crs = null;
            GridSampleDimension[] dims = null;

            try {
                crs = DimapParser.readCRS(dimapNode);
                final int[] dim = DimapParser.readRasterDimension(dimapNode);
                gridToCRS = DimapParser.readGridToCRS(dimapNode);
                dims = DimapParser.readSampleDimensions(dimapNode, "cn", dim[2]);

            } catch (FactoryException ex) {
                Logger.getLogger(DimapImageReader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformException ex) {
                Logger.getLogger(DimapImageReader.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            /*
             * If we have found metadata information in dimap file, complete metadata.
             */
            if (gridToCRS != null || crs != null) {
                if (metadata == null) {
                    metadata = new DimapMetadata(SpatialMetadataFormat.IMAGE, this, null,dimapNode);
                }
                if (gridToCRS != null) {
                    final int width  = getWidth (imageIndex);
                    final int height = getHeight(imageIndex);
                    new GridDomainAccessor(metadata).setAll(gridToCRS, new Rectangle(width, height),
                            null, PixelOrientation.UPPER_LEFT);
                }
                if (crs != null) {
                    new ReferencingBuilder(metadata).setCoordinateReferenceSystem(crs);
                }
            }

            if(dims != null){
                final DimensionAccessor accesor = new DimensionAccessor(metadata);
                for(GridSampleDimension dim : dims){
                    accesor.selectChild(accesor.appendChild());
                    accesor.setAttribute("descriptor", dim.getDescription().toString());
                }
            }

        }
        return metadata;
    }

    public static class Spi extends ImageReaderAdapter.Spi {
        public Spi(final ImageReaderSpi main) {
            super(main);
            pluginClassName = "org.geotoolkit.image.io.plugin.DimapImageReader";
            vendorName      = "Geotoolkit.org";
            version         = Version.GEOTOOLKIT.toString();
        }

        public Spi(final String format) throws IllegalArgumentException {
            this(Formats.getReaderByFormatName(format, Spi.class));
        }

        @Override
        public String getDescription(final Locale locale) {
            return "Dimap format.";
        }

        private static File searchMetadataFile(Object input) throws IOException{
            if(input instanceof File){
                final File file = (File) input;
                final File parent = file.getParentFile();
                final File candidate = new File(parent, "metadata.dim");
                if (candidate.isFile()) {
                    return candidate;
                }else{
                    throw new IOException("Could not find metadata file");
                }
            }else{
                throw new IOException("Input must be of type file, found : " + input.getClass());
            }
        }

        @Override
        public boolean canDecodeInput(Object source) throws IOException {
            if (IOUtilities.canProcessAsPath(source)) {
                source = IOUtilities.tryToFile(source);
                searchMetadataFile(source);
            }
            return super.canDecodeInput(source);
        }

        @Override
        public ImageReader createReaderInstance(final Object extension) throws IOException {
            return new DimapImageReader(this, main.createReaderInstance(extension));
        }

        @Configuration
        public static void registerDefaults(ServiceRegistry registry) {
            if (registry == null) {
                registry = IIORegistry.getDefaultInstance();
            }
            for (int index=0; ;index++) {
                final Spi provider;
                try {
                    switch (index) {
                        case 0: provider = new TIFF(); break;
                        case 1: provider = new JPEG(); break;
                        case 2: provider = new PNG (); break;
                        case 3: provider = new GIF (); break;
                        case 4: provider = new BMP (); break;
                        case 5: provider = new TXT (); break;
                        default: return;
                    }
                } catch (RuntimeException e) {
                    /*
                     * If we failed to register a plugin, this is not really a big deal.
                     * This format will not be available, but it will not prevent the
                     * rest of the application to work.
                     */
                    Logging.recoverableException(Logging.getLogger("org.geotoolkit.image.io"),
                            Spi.class, "registerDefaults", e);
                    continue;
                }
                registry.registerServiceProvider(provider, ImageReaderSpi.class);
                registry.setOrdering(ImageReaderSpi.class, provider, provider.main);
            }
        }

        @Configuration
        public static void unregisterDefaults(ServiceRegistry registry) {
            if (registry == null) {
                registry = IIORegistry.getDefaultInstance();
            }
            for (int index=0; ;index++) {
                final Class<? extends Spi> type;
                switch (index) {
                    case 0: type = TIFF.class; break;
                    case 1: type = JPEG.class; break;
                    case 2: type = PNG .class; break;
                    case 3: type = GIF .class; break;
                    case 4: type = BMP .class; break;
                    case 5: type = TXT .class; break;
                    default: return;
                }
                final Spi provider = registry.getServiceProviderByClass(type);
                if (provider != null) {
                    registry.deregisterServiceProvider(provider, ImageReaderSpi.class);
                }
            }
        }
    }

    private static final class TIFF extends Spi {TIFF() {super("TIFF"  );}}
    private static final class JPEG extends Spi {JPEG() {super("JPEG"  );}}
    private static final class PNG  extends Spi { PNG() {super("PNG"   );}}
    private static final class GIF  extends Spi { GIF() {super("GIF"   );}}
    private static final class BMP  extends Spi { BMP() {super("BMP"   );}}
    private static final class TXT  extends Spi { TXT() {super("matrix");}}
}
