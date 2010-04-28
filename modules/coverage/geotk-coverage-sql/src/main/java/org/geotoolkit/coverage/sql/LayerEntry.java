/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2005-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2007-2010, Geomatys
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
package org.geotoolkit.coverage.sql;

import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.sql.SQLException;
import java.awt.geom.Dimension2D;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.InvalidObjectException;

import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.geometry.MismatchedReferenceSystemException;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.geotoolkit.util.Utilities;
import org.geotoolkit.util.DateRange;
import org.geotoolkit.util.MeasurementRange;
import org.geotoolkit.util.logging.Logging;
import org.geotoolkit.util.collection.XCollections;
import org.geotoolkit.util.collection.FrequencySortedSet;
import org.geotoolkit.util.collection.UnmodifiableArrayList;
import org.geotoolkit.internal.sql.table.DefaultEntry;
import org.geotoolkit.internal.sql.table.TablePool;
import org.geotoolkit.internal.sql.table.SpatialDatabase;
import org.geotoolkit.internal.sql.table.NoSuchRecordException;
import org.geotoolkit.internal.referencing.CRSUtilities;
import org.geotoolkit.internal.UnmodifiableArraySortedSet;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.grid.GeneralGridEnvelope;
import org.geotoolkit.coverage.grid.GeneralGridGeometry;
import org.geotoolkit.referencing.crs.DefaultTemporalCRS;
import org.geotoolkit.referencing.operation.transform.LinearTransform;
import org.geotoolkit.referencing.operation.transform.ProjectiveTransform;
import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.geotoolkit.resources.Errors;


/**
 * A layer of {@linkplain GridCoverage grid coverages} sharing common properties.
 *
 * @author Martin Desruisseaux (IRD, Geomatys)
 * @version 3.11
 *
 * @since 3.10 (derived from Seagis)
 * @module
 */
final class LayerEntry extends DefaultEntry implements Layer {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5283559646740856038L;

    /**
     * Typical time interval (in days) between images, or {@link Double#NaN} if unknown.
     * For example a layer of weekly <cite>Sea Surface Temperature</cite> (SST) coverages
     * may set this field to 7, while a layer of mounthly SST coverage may set this field
     * to 30. The value is only approximative.
     *
     * @todo We should compute it automatically instead.
     */
    final double timeInterval;

    /**
     * The domain for this layer, or {@code null} if not yet computed. Will be computed
     * only when first needed, and is serialized because its computation recquires a
     * connection to the database.
     *
     * @see #getDomain()
     */
    private volatile DomainOfLayerEntry domain;

    /**
     * The series associated with their identifiers. This map will be created only when
     * first needed. This field is not declared {@code volatile} because the method that
     * compute it needs to be synchronized anyway.
     *
     * @see #getSeriesMap()
     */
    private Map<Integer,SeriesEntry> series;

    /**
     * How many coverages are found in each series, sorted by decreasing frequency.
     * This is computed when first needed. This field is not declared {@code volatile}
     * because the method that compute it needs to be synchronized anyway.
     *
     * @see #getCountBySeries()
     */
    private FrequencySortedSet<SeriesEntry> countBySeries;

    /**
     * How many coverages are found for each grid geometry, sorted by decreasing frequency.
     * This is computed when first needed. This field is not declared {@code volatile}
     * because the method that compute it needs to be synchronized anyway.
     *
     * @see #getCountByExtent()
     */
    private FrequencySortedSet<GridGeometryEntry> countByExtent;

    /**
     * A fallback layer to be used if no image can be found for a given date in this layer.
     * May be {@code null} if there is no fallback.
     * <p>
     * Upon construction, this field contains only the layer name as a {@link String}.
     * This is converted to {@link LayerEntry} only when first needed.
     *
     * @see #getFallback()
     */
    private volatile Object fallback;

    /**
     * The typical resolution along each axis of the database CRS. This is computed only
     * when first needed. This field is serialized because its computation requires an
     * access to the database.
     *
     * @see #getTypicalResolution()
     */
    private double[] resolution;

    /**
     * The set of available dates. Will be computed by when first needed. This field
     * is serialized because its computation requires a connection to the database.
     *
     * @see #getAvailableTimes()
     */
    private SortedSet<Date> availableTimes;

    /**
     * The set of available altitudes. Will be computed when first needed. This field doesn't
     * need to be serialized since it can be recomputed from the {@linkplain #countByExtent},
     * which is serialized.
     *
     * @see #getAvailableElevations()
     */
    private transient SortedSet<Number> availableElevations;

    /**
     * Caches the value returned by {@link #getSampleValueRanges()}. Computed only when first
     * needed. This field doesn't need to be serialized since it can be recomputed from the
     * {@linkplain #series} field, which is serialized.
     *
     * @see #getSampleValueRanges()
     */
    private transient List<MeasurementRange<?>> sampleValueRanges;

    /**
     * Caches the value returned by {@link #getGridGeometries()}. This field doesn't need
     * to be serialized since it can be recomputed from the {@linkplain #countByExtent},
     * which is serialized.
     *
     * @see #getGridGeometries()
     */
    private transient SortedSet<GeneralGridGeometry> gridGeometries;

    /**
     * Whatever this layer has tiles, or {@code null} if not yet determined.
     * This method is for {@link GridCoverageTable#createEntry} usage only;
     * it is not used by this {@code LayerEntry}.
     */
    transient volatile Boolean isTiled;

    /**
     * The envelope for all coverages in this layer. Will be computed when first needed.
     * This field is serialized because its computation requires a connection to the database.
     *
     * @see #getEnvelope(Date, Number)
     */
    private volatile CoverageEnvelope coverageEnvelope;

    /**
     * The geographic bounding box. Will be computed when first needed.
     */
    private volatile GeographicBoundingBox boundingBox;

    /**
     * The factory for fetching table dependencies.
     * This field is not serialized. It will be {@code null} on deserialization, which
     * imply that the various {@code getCoverageReference} methods will not be available.
     */
    private final transient TableFactory tables;

    /**
     * Creates a new layer.
     *
     * @param name         The layer name.
     * @param timeInterval Typical time interval (in days) between images, or {@link Double#NaN} if unknown.
     * @param fallback     The layer on which to fallback, or {@code null} if none.
     * @param remarks      Optional remarks, or {@code null}.
     * @param tables       The table factory.
     */
    LayerEntry(final Comparable<?> name, final double timeInterval, final String fallback,
            final String remarks, final TableFactory tables)
    {
        super(name, remarks);
        this.tables = tables;
        this.timeInterval = timeInterval;
    }

    /**
     * Returns the name of this layer.
     */
    @Override
    public String getName() {
        return identifier.toString();
    }

    /**
     * Returns the ressources bundle for error messages.
     */
    private Errors errors() {
        return Errors.getResources(tables != null ? tables.getLocale() : null);
    }

    /**
     * Returns the table factory, or thrown an exception if there is none.
     * The exception may happen if this entry has been deserialized.
     *
     * @return The table factory.
     * @throws IllegalStateException If this entry is not connected to a database.
     */
    private TableFactory getTableFactory() throws IllegalStateException {
        if (tables == null) {
            throw new IllegalStateException(errors().getString(Errors.Keys.NO_DATA_SOURCE));
        }
        return tables;
    }

    /**
     * Returns the domain of this layer, or {@code null} if none. This is not a big deal if
     * this method is executed twice concurrently, because {@code Table.getEntry(Comparable)}
     * has its own synchronization lock on its shared cache.
     *
     * @throws SQLException If an error occured while fetching the domain.
     */
    private DomainOfLayerEntry getDomain() throws SQLException {
        DomainOfLayerEntry entry = domain;
        if (entry == null) {
            final String name = getName();
            final DomainOfLayerTable domains = getTableFactory().getTable(DomainOfLayerTable.class);
            try {
                entry = domains.getEntry(name);
            } catch (NoSuchRecordException exception) {
                entry = DomainOfLayerEntry.NULL;
                Logging.recoverableException(LayerEntry.class, "getDomain", exception);
            }
            domains.release();
            domain = entry;
        }
        return entry;
    }

    /**
     * Returns all series in this layer.
     *
     * @throws SQLException If an error occured while fetching the series.
     */
    final Collection<SeriesEntry> getSeries() throws SQLException {
        return getSeriesMap().values();
    }

    /**
     * Returns the series for the given identifier, or {@code null} if none.
     *
     * @param  name The series identifier.
     * @return The series in this layer for the given identifier, or {@code null} if none.
     * @throws SQLException If an error occured while fetching the series.
     */
    final SeriesEntry getSeries(int identifier) throws SQLException {
        return getSeriesMap().get(identifier);
    }

    /**
     * Returns all series in this layer as (<var>identifier</var>, <var>series</var>) pairs.
     *
     * @throws SQLException If an error occured while fetching the series.
     */
    private synchronized Map<Integer,SeriesEntry> getSeriesMap() throws SQLException {
        Map<Integer,SeriesEntry> map = series;
        if (map == null) {
            final String name = getName();
            final SeriesTable st = getTableFactory().getTable(SeriesTable.class);
            st.setLayer(name);
            map = Collections.unmodifiableMap(st.getEntriesMap());
            st.release();
            series = map;
        }
        return map;
    }

    /**
     * A layer to use as a fallback if no data is available in this layer for a given position. For
     * example if no data is available in a weekly averaged <cite>Sea Surface Temperature</cite>
     * (SST) coverage because a location is masked by clouds, we may want to look in the mounthly
     * averaged SST coverage as a fallback.
     *
     * {@section Synchronization note}
     * This is not a big deal if this method is executed twice concurrently, because
     * {@code Table.getEntry(Comparable)} has its own synchronization lock on their
     * shared cache so we will get the same {@code LayerEntry} instance anyway.
     *
     * @return The fallback layer, or {@code null} if none.
     * @throws CoverageStoreException If an error occured while fetching the fallback.
     */
    @Override
    public LayerEntry getFallback() throws CoverageStoreException {
        Object fb = fallback;
        if (fb instanceof String) {
            final String name = (String) fb;
            final TablePool<LayerTable> pool = getTableFactory().layers;
            try {
                final LayerTable table = pool.acquire();
                fb = table.getEntry(name);
                pool.release(table);
            } catch (SQLException e) {
                throw new CoverageStoreException(e);
            }
            fallback = fb;
        }
        return (LayerEntry) fb;
    }

    /**
     * Returns the number of coverages in this layer.
     */
    @Override
    public int getCoverageCount() throws CoverageStoreException {
        final int[] count;
        try {
            count = getCountBySeries().frequencies();
        } catch (SQLException e) {
            throw new CoverageStoreException(e);
        }
        int n = 0;
        for (int i=0; i<count.length; i++) {
            n += count[i];
        }
        return n;
    }

    /**
     * Returns the number of coverages in each series. This method returns a direct
     * reference to the internal set - <strong>do not modify!</strong>.
     */
    final synchronized FrequencySortedSet<SeriesEntry> getCountBySeries() throws SQLException {
        FrequencySortedSet<SeriesEntry> count = countBySeries;
        if (count == null) {
            final Map<Integer,SeriesEntry> seriesMap = getSeriesMap();
            final TablePool<GridCoverageTable> pool = getTableFactory().coverages;
            final GridCoverageTable table = pool.acquire();
            table.envelope.clear();
            table.setLayerEntry(this);
            count = new FrequencySortedSet<SeriesEntry>(true);
            for (final SeriesEntry series : seriesMap.values()) {
                final Map<Integer,Integer> countMap = table.count(series, false);
                for (final Map.Entry<Integer,Integer> e : countMap.entrySet()) {
                    count.add(seriesMap.get(e.getKey()), e.getValue());
                }
            }
            pool.release(table);
            countBySeries = count;
        }
        return count;
    }

    /**
     * Returns the number of coverages for each extent. This method returns a direct
     * reference to the internal set - <strong>do not modify!</strong>.
     */
    final synchronized FrequencySortedSet<GridGeometryEntry> getCountByExtent() throws SQLException {
        FrequencySortedSet<GridGeometryEntry> count = countByExtent;
        if (count == null) {
            final Collection<SeriesEntry> allSeries = getSeries();
            final TablePool<GridCoverageTable> pool = getTableFactory().coverages;
            final GridCoverageTable table = pool.acquire();
            table.envelope.clear();
            table.setLayerEntry(this);
            final GridGeometryTable geometries = table.getGridGeometryTable();
            count = new FrequencySortedSet<GridGeometryEntry>(true);
            for (final SeriesEntry series : allSeries) {
                final Map<Integer,Integer> countMap = table.count(series, true);
                for (final Map.Entry<Integer,Integer> e : countMap.entrySet()) {
                    count.add(geometries.getEntry(e.getKey()), e.getValue());
                }
            }
            pool.release(table);
            countByExtent = count;
        }
        return count;
    }

    /**
     * Returns a time range encompassing all coverages in this layer, or {@code null} if none.
     *
     * @return The time range encompassing all coverages, or {@code null}.
     * @throws CoverageStoreException if an error occured while fetching the time range.
     */
    @Override
    public DateRange getTimeRange() throws CoverageStoreException {
        final DomainOfLayerEntry domain;
        try {
            domain = getDomain();
        } catch (SQLException e) {
            throw new CoverageStoreException(e);
        }
        return (domain != null) ? domain.timeRange : null;
    }

    /**
     * Returns the set of dates when a coverage is available.
     */
    @Override
    public synchronized SortedSet<Date> getAvailableTimes() throws CoverageStoreException {
        SortedSet<Date> available = availableTimes;
        if (available == null) try {
            final TablePool<GridCoverageTable> pool = getTableFactory().coverages;
            final GridCoverageTable table = pool.acquire();
            table.envelope.clear();
            table.setLayerEntry(this);
            available = table.getAvailableTimes();
            pool.release(table);
            availableTimes = available;
        } catch (SQLException e) {
            throw new CoverageStoreException(e);
        }
        return available;
    }

    /**
     * Returns the set of altitudes where a coverage is available.
     */
    @Override
    public synchronized SortedSet<Number> getAvailableElevations() throws CoverageStoreException {
        SortedSet<Number> available = availableElevations;
        if (available == null) {
            final Set<GridGeometryEntry> count;
            try {
                count = getCountByExtent();
            } catch (SQLException e) {
                throw new CoverageStoreException(e);
            }
            available = XCollections.emptySortedSet();
            if (count != null) {
                final Set<Double> all = new HashSet<Double>();
                for (final GridGeometryEntry entry : count) {
                    final double[] ordinates = entry.getVerticalOrdinates();
                    if (ordinates != null) {
                        for (final double z : ordinates) {
                            all.add(z);
                        }
                    }
                }
                if (!all.isEmpty()) {
                    available = new UnmodifiableArraySortedSet.Number(all);
                }
            }
            availableElevations = available;
        }
        return available;
    }

    /**
     * Returns the ranges of valid <cite>geophysics</cite> values for each band. If some
     * coverages found in this layer have different range of values, then this method
     * returns the union of their ranges.
     *
     * @return The range of valid sample values.
     * @throws CoverageStoreException If an error occured while computing the ranges.
     */
    @Override
    public synchronized List<MeasurementRange<?>> getSampleValueRanges() throws CoverageStoreException {
        List<MeasurementRange<?>> sampleValueRanges = this.sampleValueRanges;
        if (sampleValueRanges == null) try {
            MeasurementRange<?>[] ranges = null;
            for (final SeriesEntry series : getSeries()) {
                final FormatEntry format = series.format;
                if (format != null) {
                    final MeasurementRange<Double>[] candidates = format.getSampleValueRanges();
                    if (candidates != null) {
                        if (ranges == null) {
                            ranges = candidates;
                        } else if (!Arrays.equals(ranges, candidates)) {
                            final int length;
                            if (candidates.length <= ranges.length) {
                                length = candidates.length;
                            } else {
                                length = ranges.length;
                                ranges = Arrays.copyOf(ranges, candidates.length);
                                System.arraycopy(candidates, length, ranges, length, candidates.length - length);
                            }
                            for (int i=0; i<length; i++) {
                                ranges[i] = ranges[i].union(candidates[i]);
                            }
                        }
                    }
                }
            }
            if (ranges != null) {
                sampleValueRanges = UnmodifiableArrayList.wrap(ranges);
            } else {
                sampleValueRanges = Collections.emptyList();
            }
            this.sampleValueRanges = sampleValueRanges;
        } catch (SQLException e) {
            throw new CoverageStoreException(e);
        }
        return sampleValueRanges;
    }

    /**
     * Returns the typical pixel resolution in this layer. Values are in the unit of the
     * {@linkplain CoverageDatabase#getCoordinateReferenceSystem() main CRS used by the database}
     * (typically degrees of longitude and latitude for the horizontal part, and days for the
     * temporal part). Some elements of the returned array may be {@link Double#NaN NaN} if they
     * are unnkown.
     */
    @Override
    public synchronized double[] getTypicalResolution() throws CoverageStoreException {
        double[] resolution = this.resolution;
        if (resolution == null) {
            final SpatialDatabase database = getTableFactory();
            final CoordinateSystem cs = database.spatioTemporalCRS.getCoordinateSystem();
            final int xPos = CRSUtilities.dimensionColinearWith(cs, database.horizontalCRS.getCoordinateSystem());
            final int tPos = CRSUtilities.dimensionColinearWith(cs, database.temporalCRS  .getCoordinateSystem());
            final int dim  = cs.getDimension();
            resolution = new double[dim];
            Arrays.fill(resolution, Double.NaN);
            if (xPos >= 0) {
                final DomainOfLayerEntry domain;
                try {
                    domain = getDomain();
                } catch (SQLException e) {
                    throw new CoverageStoreException(e);
                }
                if (domain == null) {
                    return null;
                }
                final Dimension2D xyRes = domain.resolution;
                if (xyRes != null) {
                    resolution[xPos]   = xyRes.getWidth();
                    resolution[xPos+1] = xyRes.getHeight();
                }
            }
            if (tPos >= 0) {
                resolution[tPos] = timeInterval;
            }
            this.resolution = resolution;
        }
        return resolution.clone();
    }

    /**
     * Returns the image format used by the coverages in this layer.
     */
    @Override
    public SortedSet<String> getImageFormats() throws CoverageStoreException {
        final FrequencySortedSet<SeriesEntry> series;
        try {
            series = getCountBySeries();
        } catch (SQLException e) {
            throw new CoverageStoreException(e);
        }
        final int[] count = series.frequencies();
        final FrequencySortedSet<String> names = new FrequencySortedSet<String>();
        int i = 0;
        for (final SeriesEntry entry : series) {
            names.add(entry.format.imageFormat, count[i++]);
        }
        return names;
    }

    /**
     * Returns the grid geometries used by the coverages in this layer.
     */
    @Override
    public synchronized SortedSet<GeneralGridGeometry> getGridGeometries() throws CoverageStoreException {
        SortedSet<GeneralGridGeometry> gridGeometries = this.gridGeometries;
        if (gridGeometries == null) {
            boolean hasCheckedTimeRange = false;
            Date startTime = null, endTime = null;
            final FrequencySortedSet<GridGeometryEntry> extents;
            try {
                extents = getCountByExtent();
            } catch (SQLException e) {
                throw new CoverageStoreException(e);
            }
            if (extents == null) {
                return XCollections.emptySortedSet();
            }
            final int[] count = extents.frequencies();
            final FrequencySortedSet<GeneralGridGeometry> geometries = new FrequencySortedSet<GeneralGridGeometry>();
            int i = 0;
            for (final GridGeometryEntry entry : extents) {
                GeneralGridGeometry gg = entry.geometry;
                final DefaultTemporalCRS temporalCRS = entry.getTemporalCRS();
                if (temporalCRS != null) {
                    /*
                     * If the geometry has a temporal component, we need to configure the grid
                     * geometry in the time dimension ourself because the start time and end time
                     * are layer-dependent. Fetch those start time and end time when first needed.
                     */
                    if (!hasCheckedTimeRange) {
                        hasCheckedTimeRange = true;
                        final DateRange timeRange = getTimeRange();
                        if (timeRange != null) {
                            startTime = timeRange.getMinValue();
                            endTime   = timeRange.getMaxValue();
                        }
                    }
                    final double min = (startTime != null) ? temporalCRS.toValue(startTime) : Double.NEGATIVE_INFINITY;
                    final double max = (  endTime != null) ? temporalCRS.toValue(  endTime) : Double.POSITIVE_INFINITY;
                    if (!Double.isInfinite(min) || !Double.isInfinite(max)) {
                        final double interval;
                        if (!Double.isNaN(timeInterval)) {
                            final long dt = Math.round(timeInterval * GridCoverageTable.MILLIS_IN_DAY);
                            final long epoch = temporalCRS.getDatum().getOrigin().getTime();
                            interval = temporalCRS.toValue(new Date(dt + epoch));
                        } else {
                            interval = (max - min) / getCoverageCount();
                        }
                        /*
                         * Creates the new math transform with the same coefficients than the previous
                         * one, except for the time dimension. The temporal "cell size" is the interval
                         * computed above.
                         */
                        final PixelInCell anchor = entry.getPixelInCell();
                        final Matrix gridToCRS = ((LinearTransform) gg.getGridToCRS(anchor)).getMatrix();
                        final CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem();
                        final CoordinateSystem cs = crs.getCoordinateSystem();
                        final int dimension = cs.getDimension();
                        final int timeDimension = dimension - 1;
                        /*
                         * The code below makes the following assumptions,
                         * which are checked by the assert statements below:
                         *
                         *   1) The temporal dimension is the last dimension.
                         *   2) The temporal dimension is at the same index in
                         *      both the grid CRS and the "real world" CRS.
                         */
                        assert CRSUtilities.dimensionColinearWith(cs, temporalCRS.getCoordinateSystem()) == timeDimension : crs;
                        assert gridToCRS.getElement(timeDimension, timeDimension) != 0 : gridToCRS;
                        gridToCRS.setElement(timeDimension, timeDimension, interval);
                        gridToCRS.setElement(timeDimension, dimension, min);
                        GridEnvelope env = gg.getGridRange();
                        final int[] lower = env.getLow ().getCoordinateValues();
                        final int[] upper = env.getHigh().getCoordinateValues();
                        lower[timeDimension] = 0;
                        upper[timeDimension] = Math.max(((int) Math.round((max - min) / interval)) - 1, 0);
                        env = new GeneralGridEnvelope(lower, upper, true);
                        gg = new GeneralGridGeometry(env, anchor, ProjectiveTransform.create(gridToCRS), crs);
                    }
                }
                geometries.add(gg, count[i++]);
            }
            gridGeometries = Collections.unmodifiableSortedSet(geometries);
            this.gridGeometries = gridGeometries;
        }
        return gridGeometries;
    }

    /**
     * Returns the geographic bounding box, or {@code null} if unknown. If the CRS used by
     * the database is not geographic (for example if it is a projected CRS), then this method
     * will transform the layer envelope to a geographic CRS.
     */
    @Override
    public GeographicBoundingBox getGeographicBoundingBox() throws CoverageStoreException {
        GeographicBoundingBox bbox = boundingBox;
        if (bbox == null) { // Not a big deal if computed twice.
            final CoverageEnvelope envelope = getEnvelope(null, null);
            if (envelope != null) {
                try {
                    bbox = new DefaultGeographicBoundingBox(envelope);
                } catch (TransformException e) {
                    throw new CoverageStoreException(e);
                }
                ((DefaultGeographicBoundingBox) bbox).freeze();
                boundingBox = bbox;
            }
        }
        if (Double.isInfinite(bbox.getWestBoundLongitude()) &&
            Double.isInfinite(bbox.getEastBoundLongitude()) &&
            Double.isInfinite(bbox.getSouthBoundLatitude()) &&
            Double.isInfinite(bbox.getNorthBoundLatitude()))
        {
            return null;
        }
        return bbox;
    }

    /**
     * Returns the envelope of this layer, optionnaly centered at the given date and
     * elevation. Callers are free to modify the returned instance before to pass it
     * to the {@code getCoverageReference} methods.
     */
    @Override
    public CoverageEnvelope getEnvelope(final Date time, final Number elevation) throws CoverageStoreException {
        CoverageEnvelope envelope = coverageEnvelope;
        if (envelope == null) {
            synchronized (this) {
                envelope = coverageEnvelope;
                if (envelope == null) try {
                    final TablePool<GridCoverageTable> pool = getTableFactory().coverages;
                    final GridCoverageTable table = pool.acquire();
                    table.envelope.clear();
                    table.setLayerEntry(this);
                    table.trimEnvelope();
                    envelope = table.envelope.clone();
                    pool.release(table);
                    coverageEnvelope = envelope;
                } catch (SQLException e) {
                    throw new CoverageStoreException(e);
                }
            }
        }
        envelope = envelope.clone();
        /*
         * Now apply the optional user parameters.
         */
        if (time != null) {
            long delay = Math.round(timeInterval * (GridCoverageTable.MILLIS_IN_DAY / 2));
            if (delay <= 0) {
                delay = GridCoverageTable.MILLIS_IN_DAY / 2;
            }
            final long t = time.getTime();
            envelope.setTimeRange(new Date(t - delay), new Date(t + delay));
        }
        if (elevation != null) {
            final double zmin = elevation.doubleValue();
            final double zmax = zmin; // TODO: choose a better range.
            envelope.setVerticalRange(zmin, zmax);
        }
        return envelope;
    }

    /**
     * Returns a reference to every coverages available in this layer which intersect the
     * given envelope.
     * <p>
     * <b>Implementation note:</b> this method casts {@code Set<GridCoverageEntry>} to
     * {@code Set<GridCoverageReference>}. This is okay if the {@code Set} is a generic
     * implementation like {@link java.util.LinkedHashSet} and this class does not keep
     * any reference to the returned set (so no {@code Set<GridCoverageEntry>} view
     * exist anymore).
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public Set<GridCoverageReference> getCoverageReferences(final CoverageEnvelope envelope)
            throws CoverageStoreException
    {
        final Set<GridCoverageEntry> entries;
        try {
            final TablePool<GridCoverageTable> pool = getTableFactory().coverages;
            final GridCoverageTable table = pool.acquire();
            table.setLayerEntry(this);
            table.envelope.setAll(envelope);
            entries = table.getEntries();
            pool.release(table);
        } catch (SQLException exception) {
            throw new CoverageStoreException(exception);
        } catch (TransformException exception) {
            throw new MismatchedReferenceSystemException(errors()
                    .getString(Errors.Keys.ILLEGAL_COORDINATE_REFERENCE_SYSTEM, exception));
        }
        return (Set) entries; // See implementation note above.
    }

    /**
     * Returns a reference to a coverage that intersect the given envelope. If more than one
     * coverage intersect the given envelope, then this method will select the one which seem
     * the most repesentative.
     */
    @Override
    public final GridCoverageEntry getCoverageReference(final CoverageEnvelope envelope)
            throws CoverageStoreException
    {
        return getCoverageReference(envelope, null);
    }

    /**
     * Same as {@link #getCoverageReference(CoverageEnvelope)}, but using the given executor
     * for the database queries.
     */
    GridCoverageEntry getCoverageReference(final CoverageEnvelope envelope, final Executor executor)
            throws CoverageStoreException
    {
        final GridCoverageEntry entry;
        try {
            final TablePool<GridCoverageTable> pool = getTableFactory().coverages;
            final GridCoverageTable table = pool.acquire();
            table.setLayerEntry(this);
            table.envelope.setAll(envelope);
            entry = table.select(executor);
            pool.release(table);
        } catch (SQLException exception) {
            throw new CoverageStoreException(exception);
        } catch (TransformException exception) {
            throw new MismatchedReferenceSystemException(errors()
                    .getString(Errors.Keys.ILLEGAL_COORDINATE_REFERENCE_SYSTEM, exception));
        }
        return entry;
    }

    /**
     * Compares this layer with the specified object for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (super.equals(object)) {
            final LayerEntry that = (LayerEntry) object;
            return Utilities.equals(this.timeInterval, that.timeInterval);
            /*
             * Do not test costly fields like 'fallback'.
             */
        }
        return false;
    }

    /**
     * Invoked before serialization in order to ensure that the elements for which
     * the computation was deferred are now computed.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        try {
            getDomain();
            getSeriesMap();
            getFallback();
            getCountBySeries();
            getCountByExtent();
            getAvailableTimes();
            getEnvelope(null, null);
            getTypicalResolution();
        } catch (Exception e) {
            final InvalidObjectException ex = new InvalidObjectException(e.getLocalizedMessage());
            ex.initCause(e);
            throw ex;
        }
        out.defaultWriteObject();
    }
}
