/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013, Geomatys
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

package org.geotoolkit.wcs.xml.v200;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;


/**
 * <p>Java class for CoverageSubtypeParentType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CoverageSubtypeParentType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/wcs/2.0}CoverageSubtype"/>
 *         &lt;element ref="{http://www.opengis.net/wcs/2.0}CoverageSubtypeParent" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CoverageSubtypeParentType", propOrder = {
    "coverageSubtype",
    "coverageSubtypeParent"
})
public class CoverageSubtypeParentType {

    @XmlElement(name = "CoverageSubtype", required = true)
    private QName coverageSubtype;
    @XmlElement(name = "CoverageSubtypeParent")
    private CoverageSubtypeParentType coverageSubtypeParent;

    /**
     * Gets the value of the coverageSubtype property.
     * 
     * @return
     *     possible object is
     *     {@link QName }
     *     
     */
    public QName getCoverageSubtype() {
        return coverageSubtype;
    }

    /**
     * Sets the value of the coverageSubtype property.
     * 
     * @param value
     *     allowed object is
     *     {@link QName }
     *     
     */
    public void setCoverageSubtype(QName value) {
        this.coverageSubtype = value;
    }

    /**
     * Gets the value of the coverageSubtypeParent property.
     * 
     * @return
     *     possible object is
     *     {@link CoverageSubtypeParentType }
     *     
     */
    public CoverageSubtypeParentType getCoverageSubtypeParent() {
        return coverageSubtypeParent;
    }

    /**
     * Sets the value of the coverageSubtypeParent property.
     * 
     * @param value
     *     allowed object is
     *     {@link CoverageSubtypeParentType }
     *     
     */
    public void setCoverageSubtypeParent(CoverageSubtypeParentType value) {
        this.coverageSubtypeParent = value;
    }

}
