/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008 - 2009, Geomatys
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
package org.geotoolkit.observation;

import java.io.IOException;
import org.geotoolkit.observation.xml.v100.ObservationType;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import org.geotoolkit.gml.xml.v311.DirectPositionType;
import org.geotoolkit.gml.xml.v311.FeaturePropertyType;
import org.geotoolkit.gml.xml.v311.PointType;
import org.geotoolkit.gml.xml.v311.TimePeriodType;
import org.geotoolkit.gml.xml.v311.UnitOfMeasureEntry;
import org.geotoolkit.observation.xml.v100.MeasureType;
import org.geotoolkit.observation.xml.v100.MeasurementType;
import org.geotoolkit.observation.xml.v100.ObservationCollectionType;
import org.geotoolkit.sampling.xml.v100.SamplingPointType;

//Junit dependencies
import org.geotoolkit.swe.xml.v101.AnyScalarPropertyType;
import org.geotoolkit.swe.xml.v101.DataArrayType;
import org.geotoolkit.swe.xml.v101.DataArrayPropertyType;
import org.geotoolkit.swe.xml.v101.PhenomenonType;
import org.geotoolkit.swe.xml.v101.SimpleDataRecordType;
import org.geotoolkit.swe.xml.v101.Text;
import org.geotoolkit.swe.xml.v101.TextBlockType;
import javax.xml.bind.JAXBContext;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.xml.MarshallerPool;
import org.junit.*;
import org.xml.sax.SAXException;

import static org.apache.sis.test.Assert.*;
import org.apache.sis.xml.XML;


/**
 *
 * @author Guilhem Legal (Geomatys)
 * @module pending
 */
public class ObservationXMLBindingTest extends org.geotoolkit.test.TestBase {

    private MarshallerPool pool;
    private Unmarshaller unmarshaller;
    private Marshaller   marshaller;

    @Before
    public void setUp() throws JAXBException {
        pool = new MarshallerPool(JAXBContext.newInstance(
                "org.geotoolkit.sampling.xml.v100:" +
                "org.geotoolkit.swe.xml.v101:" +
                "org.geotoolkit.observation.xml.v100:" +
                "org.geotoolkit.gml.xml.v311:" +
                "org.apache.sis.internal.jaxb.geometry"), null);
        unmarshaller = pool.acquireUnmarshaller();
        marshaller   = pool.acquireMarshaller();
    }

    @After
    public void tearDown() {
        if (unmarshaller != null) {
            pool.recycle(unmarshaller);
        }
        if (marshaller != null) {
            pool.recycle(marshaller);
        }
    }

    /**
     * Test simple Record Marshalling.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void marshallingTest() throws JAXBException, IOException, ParserConfigurationException, SAXException {

        DirectPositionType pos = new DirectPositionType("urn:ogc:crs:espg:4326", 2, Arrays.asList(3.2, 6.5));
        PointType location     = new PointType("point-ID", pos);
        SamplingPointType sp  = new SamplingPointType("samplingID-007", "urn:sampling:test:007", "a sampling Test", new FeaturePropertyType(""), location);

        PhenomenonType observedProperty = new PhenomenonType("phenomenon-007", "urn:OGC:phenomenon-007");
        TimePeriodType samplingTime      = new TimePeriodType("t1", "2007-01-01", "2008-09-09");

        TextBlockType encoding            = new TextBlockType("encoding-001", ",", "@@", ".");
        List<AnyScalarPropertyType> fields = new ArrayList<AnyScalarPropertyType>();
        AnyScalarPropertyType field        = new AnyScalarPropertyType("text-field-001", new Text("urn:something", "some value"));
        fields.add(field);
        SimpleDataRecordType record       = new SimpleDataRecordType(fields);
        DataArrayType array               = new DataArrayType("array-001", 1, "array-001", record, encoding, "somevalue");
        DataArrayPropertyType arrayProp    = new DataArrayPropertyType(array);
        ObservationType obs = new ObservationType("urn:Observation-007", "observation definition", sp, observedProperty, "urn:sensor:007", arrayProp, samplingTime);

        StringWriter sw = new StringWriter();
        marshaller.marshal(obs, sw);

        String result = sw.toString();
        //we remove the first line
        result = result.substring(result.indexOf("?>") + 2);
        if (result.startsWith("/n")) {
            result = result.substring(1);
        }

        String expResult = "<om:Observation xmlns:sampling=\"http://www.opengis.net/sampling/1.0\"" +
                                          " xmlns:om=\"http://www.opengis.net/om/1.0\"" +
                                          " xmlns:xlink=\"http://www.w3.org/1999/xlink\"" +
                                          " xmlns:gml=\"http://www.opengis.net/gml\"" +
                                          " xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"" +
                                          " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + '\n' +
                           "    <gml:name>urn:Observation-007</gml:name>" + '\n' +
                           "    <om:samplingTime>" + '\n' +
                           "        <gml:TimePeriod gml:id=\"t1\">" + '\n' +
                           "            <gml:beginPosition>2007-01-01</gml:beginPosition>" + '\n' +
                           "            <gml:endPosition>2008-09-09</gml:endPosition>" + '\n' +
                           "        </gml:TimePeriod>" + '\n' +
                           "    </om:samplingTime>" + '\n' +
                           "    <om:procedure xlink:href=\"urn:sensor:007\"/>" + '\n' +
                           "    <om:observedProperty>" + '\n' +
                           "        <swe:Phenomenon gml:id=\"phenomenon-007\">" + '\n' +
                           "            <gml:name>urn:OGC:phenomenon-007</gml:name>" + '\n' +
                           "        </swe:Phenomenon>" + '\n' +
                           "    </om:observedProperty>" + '\n' +
                           "    <om:featureOfInterest>" + '\n' +
                           "        <sampling:SamplingPoint gml:id=\"samplingID-007\">" + '\n' +
                           "            <gml:description>a sampling Test</gml:description>" + '\n' +
                           "            <gml:name>urn:sampling:test:007</gml:name>" + '\n' +
                           "            <gml:boundedBy>" + '\n' +
                           "                <gml:Null>not_bounded</gml:Null>" + '\n' +
                           "            </gml:boundedBy>" + '\n' +
                           "            <sampling:sampledFeature xlink:href=\"\"/>" + '\n' +
                           "            <sampling:position>" + '\n' +
                           "                <gml:Point gml:id=\"point-ID\">" + '\n' +
                           "                    <gml:pos srsName=\"urn:ogc:crs:espg:4326\" srsDimension=\"2\">3.2 6.5</gml:pos>" + '\n' +
                           "                </gml:Point>" + '\n' +
                           "            </sampling:position>" + '\n' +
                           "        </sampling:SamplingPoint>" + '\n' +
                           "    </om:featureOfInterest>" + '\n' +
                           "    <om:result xsi:type=\"swe:DataArrayPropertyType\" >" + '\n' +
                           "        <swe:DataArray gml:id=\"array-001\">" + '\n' +
                           "            <swe:elementCount>" + '\n' +
                           "                <swe:Count>" + '\n' +
                           "                    <swe:value>1</swe:value>" + '\n' +
                           "                </swe:Count>" + '\n' +
                           "            </swe:elementCount>" + '\n' +
                           "            <swe:elementType name=\"array-001\">" + '\n' +
                           "                <swe:SimpleDataRecord>" + '\n' +
                           "                    <swe:field name=\"text-field-001\">" + '\n' +
                           "                        <swe:Text definition=\"urn:something\">" + '\n' +
                           "                            <swe:value>some value</swe:value>" + '\n' +
                           "                        </swe:Text>" + '\n' +
                           "                    </swe:field>" + '\n' +
                           "                </swe:SimpleDataRecord>" + '\n' +
                           "            </swe:elementType>" + '\n' +
                           "            <swe:encoding>" + '\n' +
                           "                <swe:TextBlock blockSeparator=\"@@\" decimalSeparator=\".\" tokenSeparator=\",\" id=\"encoding-001\"/>" + '\n' +
                           "            </swe:encoding>" + '\n' +
                           "            <swe:values>somevalue</swe:values>" + '\n' +
                           "        </swe:DataArray>" + '\n' +
                           "    </om:result>" + '\n' +
                           "</om:Observation>\n";
        assertXmlEquals(expResult, result, "xmlns:*");


        UnitOfMeasureEntry uom  = new UnitOfMeasureEntry("m", "meters", "distance", "meters");
        MeasureType meas       = new MeasureType(uom, 7);
        MeasurementType measmt = new MeasurementType("urn:Observation-007", "observation definition", sp, observedProperty, "urn:sensor:007", meas, samplingTime);

        sw = new StringWriter();
        marshaller.marshal(measmt, sw);

        result = sw.toString();
        //we remove the first line
       result = result.substring(result.indexOf("?>") + 2);
        if (result.startsWith("/n")) {
            result = result.substring(1);
        }

        expResult =        "<om:Measurement xmlns:sampling=\"http://www.opengis.net/sampling/1.0\"" +
                                          " xmlns:om=\"http://www.opengis.net/om/1.0\"" +
                                          " xmlns:xlink=\"http://www.w3.org/1999/xlink\"" +
                                          " xmlns:gml=\"http://www.opengis.net/gml\"" +
                                          " xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"" +
                                          " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + '\n' +
                           "    <gml:name>urn:Observation-007</gml:name>" + '\n' +
                           "    <om:samplingTime>" + '\n' +
                           "        <gml:TimePeriod gml:id=\"t1\">" + '\n' +
                           "            <gml:beginPosition>2007-01-01</gml:beginPosition>" + '\n' +
                           "            <gml:endPosition>2008-09-09</gml:endPosition>" + '\n' +
                           "        </gml:TimePeriod>" + '\n' +
                           "    </om:samplingTime>" + '\n' +
                           "    <om:procedure xlink:href=\"urn:sensor:007\"/>" + '\n' +
                           "    <om:observedProperty>" + '\n' +
                           "        <swe:Phenomenon gml:id=\"phenomenon-007\">" + '\n' +
                           "            <gml:name>urn:OGC:phenomenon-007</gml:name>" + '\n' +
                           "        </swe:Phenomenon>" + '\n' +
                           "    </om:observedProperty>" + '\n' +
                           "    <om:featureOfInterest>" + '\n' +
                           "        <sampling:SamplingPoint gml:id=\"samplingID-007\">" + '\n' +
                           "            <gml:description>a sampling Test</gml:description>" + '\n' +
                           "            <gml:name>urn:sampling:test:007</gml:name>" + '\n' +
                           "            <gml:boundedBy>" + '\n' +
                           "                <gml:Null>not_bounded</gml:Null>" + '\n' +
                           "            </gml:boundedBy>" + '\n' +
                           "            <sampling:sampledFeature xlink:href=\"\"/>" + '\n' +
                           "            <sampling:position>" + '\n' +
                           "                <gml:Point gml:id=\"point-ID\">" + '\n' +
                           "                    <gml:pos srsName=\"urn:ogc:crs:espg:4326\" srsDimension=\"2\">3.2 6.5</gml:pos>" + '\n' +
                           "                </gml:Point>" + '\n' +
                           "            </sampling:position>" + '\n' +
                           "        </sampling:SamplingPoint>" + '\n' +
                           "    </om:featureOfInterest>" + '\n' +
                           "    <om:result xsi:type=\"om:MeasureType\" uom=\"meters\">7.0</om:result>" + '\n' +
                           "</om:Measurement>\n";
        assertXmlEquals(expResult, result, "xmlns:*");


        ObservationCollectionType collection = new ObservationCollectionType();
        collection.add(measmt);

        sw = new StringWriter();
        marshaller.marshal(collection, sw);

        result = sw.toString();
        //System.out.println(result);

        collection = new ObservationCollectionType();
        collection.add(obs.getTemporaryTemplate("temporaryName", samplingTime));

        sw = new StringWriter();
        marshaller.marshal(collection, sw);

        result = sw.toString();

    }

    /**
     * Test simple Record Marshalling.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void UnmarshalingTest() throws JAXBException {

        /*
         * Test Unmarshalling observation
         */

        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + '\n' +
                "<om:Observation xmlns:om=\"http://www.opengis.net/om/1.0\" xmlns:sampling=\"http://www.opengis.net/sampling/1.0\" " +
                " xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:swe=\"http://www.opengis.net/swe/1.0.1\">" + '\n' +
                "    <gml:name>urn:Observation-007</gml:name>" + '\n' +
                "    <om:samplingTime>" + '\n' +
                "        <gml:TimePeriod>" + '\n' +
                "            <gml:beginPosition>2007-01-01</gml:beginPosition>" + '\n' +
                "            <gml:endPosition>2008-09-09</gml:endPosition>" + '\n' +
                "        </gml:TimePeriod>" + '\n' +
                "    </om:samplingTime>" + '\n' +
                "    <om:procedure xlink:href=\"urn:sensor:007\"/>" + '\n' +
                "    <om:observedProperty>" + '\n' +
                "        <swe:Phenomenon gml:id=\"phenomenon-007\">" + '\n' +
                "            <gml:name>urn:OGC:phenomenon-007</gml:name>" + '\n' +
                "        </swe:Phenomenon>" + '\n' +
                "    </om:observedProperty>" + '\n' +
                "    <om:featureOfInterest>" + '\n' +
                "        <sampling:SamplingPoint gml:id=\"samplingID-007\">" + '\n' +
                "            <gml:description>a sampling Test</gml:description>" + '\n' +
                "            <gml:name>urn:sampling:test:007</gml:name>" + '\n' +
                "            <gml:boundedBy>" + '\n' +
                "                <gml:Null>not_bounded</gml:Null>" + '\n' +
                "            </gml:boundedBy>" + '\n' +
                "            <sampling:sampledFeature xlink:href=\"urn:sampling:sampledFeature\"/>" + '\n' +
                "            <sampling:position gml:id=\"point-ID\">" + '\n' +
                "                <gml:Point gml:id=\"point-ID\">" + '\n' +
                "                   <gml:pos srsName=\"urn:ogc:crs:espg:4326\" srsDimension=\"2\">3.2 6.5</gml:pos>" + '\n' +
                "                </gml:Point>" + '\n' +
                "            </sampling:position>" + '\n' +
                "        </sampling:SamplingPoint>" + '\n' +
                "    </om:featureOfInterest>" + '\n' +
                "    <om:result xsi:type=\"swe:DataArrayPropertyType\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + '\n' +
               "        <swe:DataArray gml:id=\"array-001\">" + '\n' +
               "            <swe:elementCount>" + '\n' +
               "                <swe:Count>" + '\n' +
               "                    <swe:value>1</swe:value>" + '\n' +
               "                </swe:Count>" + '\n' +
               "            </swe:elementCount>" + '\n' +
               "            <swe:elementType name=\"array-001\">" + '\n' +
               "                <swe:SimpleDataRecord>" + '\n' +
               "                    <swe:field name=\"text-field-001\">" + '\n' +
               "                        <swe:Text definition=\"urn:something\">" + '\n' +
               "                            <swe:value>some value</swe:value>" + '\n' +
               "                        </swe:Text>" + '\n' +
               "                    </swe:field>" + '\n' +
               "                </swe:SimpleDataRecord>" + '\n' +
               "            </swe:elementType>" + '\n' +
               "            <swe:encoding>" + '\n' +
               "                <swe:TextBlock blockSeparator=\"@@\" decimalSeparator=\".\" tokenSeparator=\",\" id=\"encoding-001\"/>" + '\n' +
               "            </swe:encoding>" + '\n' +
               "            <swe:values>somevalue</swe:values>" + '\n' +
               "        </swe:DataArray>" + '\n' +
               "    </om:result>" + '\n' +
                "</om:Observation>\n";

        StringReader sr = new StringReader(xml);

        JAXBElement jb =  (JAXBElement) unmarshaller.unmarshal(sr);
        ObservationType result =  (ObservationType) jb.getValue();

        DirectPositionType pos = new DirectPositionType("urn:ogc:crs:espg:4326", 2, Arrays.asList(3.2, 6.5));
        PointType location = new PointType("point-ID", pos);
        SamplingPointType sp = new SamplingPointType("samplingID-007", "urn:sampling:test:007", "a sampling Test", new FeaturePropertyType("urn:sampling:sampledFeature"), location);

        PhenomenonType observedProperty = new PhenomenonType("phenomenon-007", "urn:OGC:phenomenon-007");
        TimePeriodType samplingTime = new TimePeriodType(null, "2007-01-01", "2008-09-09");

        TextBlockType encoding            = new TextBlockType("encoding-001", ",", "@@", ".");
        List<AnyScalarPropertyType> fields = new ArrayList<AnyScalarPropertyType>();
        AnyScalarPropertyType field        = new AnyScalarPropertyType("text-field-001", new Text("urn:something", "some value"));
        fields.add(field);
        SimpleDataRecordType record       = new SimpleDataRecordType(fields);
        DataArrayType array               = new DataArrayType("array-001", 1, "array-001", record, encoding, "somevalue");
        DataArrayPropertyType arrayProp    = new DataArrayPropertyType(array);

        ObservationType expResult = new ObservationType("urn:Observation-007", null, sp, observedProperty, "urn:sensor:007", arrayProp, samplingTime);

         assertEquals(expResult.getFeatureOfInterest(), result.getFeatureOfInterest());
        assertEquals(expResult.getDefinition(), result.getDefinition());
        assertEquals(expResult.getName(), result.getName());
        assertEquals(expResult.getObservationMetadata(), result.getObservationMetadata());
        assertEquals(expResult.getObservedProperty(), result.getObservedProperty());
        assertEquals(expResult.getProcedure(), result.getProcedure());
        assertEquals(expResult.getProcedureParameter(), result.getProcedureParameter());
        assertEquals(expResult.getProcedureTime(), result.getProcedureTime());
        assertEquals(expResult.getPropertyFeatureOfInterest(), result.getPropertyFeatureOfInterest());
        assertEquals(expResult.getPropertyObservedProperty(), result.getPropertyObservedProperty());
        assertEquals(expResult.getQuality(), result.getQuality());
        assertEquals(expResult.getResult(), result.getResult());
        assertEquals(expResult.getSamplingTime(), result.getSamplingTime());
        assertEquals(expResult, result);



        /*
         * Test Unmarshalling measurement
         */

        xml =  "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + '\n' +
               "<om:Measurement xmlns:om=\"http://www.opengis.net/om/1.0\" xmlns:sampling=\"http://www.opengis.net/sampling/1.0\" " +
               " xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:swe=\"http://www.opengis.net/swe/1.0.1\">" + '\n' +
               "    <gml:name>urn:Observation-007</gml:name>" + '\n' +
               "    <om:samplingTime>" + '\n' +
               "        <gml:TimePeriod gml:id=\"t1\">" + '\n' +
               "            <gml:beginPosition>2007-01-01</gml:beginPosition>" + '\n' +
               "            <gml:endPosition>2008-09-09</gml:endPosition>" + '\n' +
               "        </gml:TimePeriod>" + '\n' +
               "    </om:samplingTime>" + '\n' +
               "    <om:procedure xlink:href=\"urn:sensor:007\"/>" + '\n' +
               "    <om:observedProperty>" + '\n' +
               "        <swe:Phenomenon gml:id=\"phenomenon-007\">" + '\n' +
               "            <gml:name>urn:OGC:phenomenon-007</gml:name>" + '\n' +
               "        </swe:Phenomenon>" + '\n' +
               "    </om:observedProperty>" + '\n' +
               "    <om:featureOfInterest>" + '\n' +
               "        <sampling:SamplingPoint gml:id=\"samplingID-007\">" + '\n' +
               "            <gml:description>a sampling Test</gml:description>" + '\n' +
               "            <gml:name>urn:sampling:test:007</gml:name>" + '\n' +
               "            <gml:boundedBy>" + '\n' +
               "                <gml:Null>not_bounded</gml:Null>" + '\n' +
               "            </gml:boundedBy>" + '\n' +
               "            <sampling:sampledFeature xlink:href=\"urn:sampling:sampledFeature\"/>" + '\n' +
               "            <sampling:position gml:id=\"point-ID\">" + '\n' +
               "                <gml:Point gml:id=\"point-ID\">" + '\n' +
               "                    <gml:pos srsName=\"urn:ogc:crs:espg:4326\" srsDimension=\"2\">3.2 6.5</gml:pos>" + '\n' +
               "                </gml:Point>" + '\n' +
               "            </sampling:position>" + '\n' +
               "        </sampling:SamplingPoint>" + '\n' +
               "    </om:featureOfInterest>" + '\n' +
               "    <om:result xsi:type=\"om:MeasureType\" uom=\"meters\">7.0</om:result>" + '\n' +
               "</om:Measurement>\n";

        sr = new StringReader(xml);

        jb =  (JAXBElement) unmarshaller.unmarshal(sr);
        MeasurementType result2 =  (MeasurementType) jb.getValue();

        UnitOfMeasureEntry uom  = new UnitOfMeasureEntry("m", null, null, "meters");
        MeasureType meas       = new MeasureType(uom, 7);

        MeasurementType expResult2 = new MeasurementType("urn:Observation-007", null, sp, observedProperty, "urn:sensor:007", meas, samplingTime);

        assertEquals(expResult2.getFeatureOfInterest(), result2.getFeatureOfInterest());
        assertEquals(expResult2.getDefinition(), result2.getDefinition());
        assertEquals(expResult2.getName(), result2.getName());
        assertEquals(expResult2.getObservationMetadata(), result2.getObservationMetadata());
        assertEquals(expResult2.getObservedProperty(), result2.getObservedProperty());
        assertEquals(expResult2.getProcedure(), result2.getProcedure());
        assertEquals(expResult2.getProcedureParameter(), result2.getProcedureParameter());
        assertEquals(expResult2.getProcedureTime(), result2.getProcedureTime());
        assertEquals(expResult2.getPropertyFeatureOfInterest(), result2.getPropertyFeatureOfInterest());
        assertEquals(expResult2.getPropertyObservedProperty(), result2.getPropertyObservedProperty());
        assertEquals(expResult2.getQuality(), result2.getQuality());
        assertEquals(expResult2.getResult(), result2.getResult());
        assertEquals(expResult2.getSamplingTime(), result2.getSamplingTime());
        assertEquals(expResult2, result2);


        xml =  "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + '\n' +
               "<om:ObservationCollection xmlns:swe=\"http://www.opengis.net/swe/1.0.1\" xmlns:sampling=\"http://www.opengis.net/sampling/1.0\" xmlns:om=\"http://www.opengis.net/om/1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:gml=\"http://www.opengis.net/gml\">" + '\n' +
               "    <gml:boundedBy>" + '\n' +
               "        <gml:Envelope srsName=\"urn:ogc:crs:espg:4326\">" + '\n' +
               "            <gml:lowerCorner>-180.0 -90.0</gml:lowerCorner>" + '\n' +
               "            <gml:upperCorner>180.0 90.0</gml:upperCorner>" + '\n' +
               "        </gml:Envelope>" + '\n' +
               "    </gml:boundedBy>" + '\n' +
               "    <om:member>" + '\n' +
               "        <om:Measurement>" + '\n' +
               "            <gml:name>urn:ogc:object:observationTemplate:SunSpot:0014.4F01.0000.2626-12</gml:name>" + '\n' +
               "            <om:samplingTime>" + '\n' +
               "                <gml:TimePeriod>" + '\n' +
               "                    <gml:beginPosition>2009-08-03 11:18:06</gml:beginPosition>" + '\n' +
               "                    <gml:endPosition indeterminatePosition=\"now\"></gml:endPosition>" + '\n' +
               "                </gml:TimePeriod>" + '\n' +
               "            </om:samplingTime>" + '\n' +
               "            <om:procedure xlink:href=\"urn:ogc:object:sensor:SunSpot:0014.4F01.0000.2626\"/>" + '\n' +
               "            <om:observedProperty>" + '\n' +
               "                <swe:Phenomenon gml:id=\"temperature\">" + '\n' +
               "                    <gml:name>urn:phenomenon:temperature</gml:name>" + '\n' +
               "                </swe:Phenomenon>" + '\n' +
               "            </om:observedProperty>" + '\n' +
               "            <om:featureOfInterest>" + '\n' +
               "                <sampling:SamplingPoint gml:id=\"sampling-point-001\">" + '\n' +
               "                    <gml:name>sampling-point-001</gml:name>" + '\n' +
               "                    <gml:boundedBy>" + '\n' +
               "                        <gml:Null>not_bounded</gml:Null>" + '\n' +
               "                    </gml:boundedBy>" + '\n' +
               "                <sampling:sampledFeature>sampling-point-001</sampling:sampledFeature>" + '\n' +
               "                    <sampling:position>" + '\n' +
               "                        <gml:Point gml:id=\"point-ID\">" + '\n' +
               "                            <gml:pos srsDimension=\"0\">0.0 0.0</gml:pos>" + '\n' +
               "                        </gml:Point>" + '\n' +
               "                    </sampling:position>" + '\n' +
               "                </sampling:SamplingPoint>" + '\n' +
               "            </om:featureOfInterest>" + '\n' +
               "            <om:result xsi:type=\"om:Measure\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + '\n' +
               "                <om:name>mesure-027</om:name>" + '\n' +
               "                <om:value>0.0</om:value>" + '\n' +
               "            </om:result>" + '\n' +
               "        </om:Measurement>" + '\n' +
               "    </om:member>" + '\n' +
               "</om:ObservationCollection>" + '\n';

        sr = new StringReader(xml);

        ObservationCollectionType result3 =  (ObservationCollectionType) unmarshaller.unmarshal(sr);

    }
}
