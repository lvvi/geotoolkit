/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
 *    (C) 2012, Johann Sorel
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
package org.geotoolkit.client;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.geotoolkit.factory.Factory;
import org.geotoolkit.feature.FeatureUtilities;
import org.apache.sis.metadata.iso.quality.DefaultConformanceResult;
import org.apache.sis.parameter.ParameterBuilder;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.security.ClientSecurity;
import org.apache.sis.storage.DataStoreException;
import org.opengis.metadata.quality.ConformanceResult;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 * Abstract Server factory.
 *
 * @author Johann Sorel
 * @module pending
 */
public abstract class AbstractClientFactory extends Factory implements ClientFactory {

    /**
     * commonly used translation for version parameters.
     */
    public static final InternationalString I18N_VERSION = Bundle.formatInternational(Bundle.Keys.version);

    /**
     * Identifier, Mandatory.
     * Subclasses should redeclared this parameter with a different default value.
     */
    public static final ParameterDescriptor<String> IDENTIFIER = new ParameterBuilder()
            .addName("identifier")
            .addName(Bundle.formatInternational(Bundle.Keys.identifier))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.identifierRemarks))
            .setRequired(true)
            .create(String.class, null);

    /**
     * Version, Mandatory.
     * Subclasses should redeclared this parameter with a different values.
     */
    public static final ParameterDescriptor<String> VERSION = new ParameterBuilder()
            .addName("version")
            .addName(Bundle.formatInternational(Bundle.Keys.version))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.versionRemarks))
            .setRequired(true)
            .create(String.class, null);

    /**
     * Create the identifier descriptor, and set only one valid value, the one in parameter.
     *
     * @param idValue the value to use for identifier.
     * @return an identifier descriptor.
     */
    public static ParameterDescriptor<String> createFixedIdentifier(String idValue) {
            return new ParameterBuilder()
                    .addName(IDENTIFIER.getName().getCode())
                    .addName(IDENTIFIER.getAlias().iterator().next())
                    .setRemarks(IDENTIFIER.getRemarks())
                    .setRequired(true)
                    .createEnumerated(String.class, new String[]{idValue},idValue);
    }

    /**
     * Create the version descriptor.
     *
     * @param values
     * @param defaultValue
     * @return a version descriptor.
     */
    public static ParameterDescriptor<String> createVersionDescriptor(String[] values, String defaultValue) {
        return new ParameterBuilder()
                    .addName(VERSION.getName().getCode())
                    .addName(VERSION.getAlias().iterator().next())
                    .setRemarks(VERSION.getRemarks())
                    .setRequired(true)
                    .createEnumerated(String.class, values,defaultValue);
    }

    /**
     * Server URL, Mandatory.
     */
    public static final ParameterDescriptor<URL> URL = new ParameterBuilder()
            .addName("url")
            .addName(Bundle.formatInternational(Bundle.Keys.url))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.urlRemarks))
            .setRequired(true)
            .create(URL.class, null);
    /**
     * Security, Optional.
     */
    public static final ParameterDescriptor<ClientSecurity> SECURITY = new ParameterBuilder()
            .addName("security")
            .addName(Bundle.formatInternational(Bundle.Keys.security))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.securityRemarks))
            .setRequired(false)
            .create(ClientSecurity.class, null);

    /**
     * Cache images in memory, Optional.
     */
    public static final ParameterDescriptor<Boolean> IMAGE_CACHE = new ParameterBuilder()
            .addName("imagecache")
            .addName(Bundle.formatInternational(Bundle.Keys.imageCache))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.imageCacheRemarks))
            .setRequired(false)
            .create(Boolean.class, Boolean.FALSE);

    /**
     * Cache images in memory, Optional.
     * Default value is 20.000 millisecond.
     */
    public static final ParameterDescriptor<Integer> TIMEOUT = new ParameterBuilder()
            .addName("timeout")
            .addName(Bundle.formatInternational(Bundle.Keys.timeout))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.timeoutRemarks))
            .setRequired(false)
            .create(Integer.class, 20000);

    /**
     * Use NIO when possible for queries, Optional.
     */
    public static final ParameterDescriptor<Boolean> NIO_QUERIES = new ParameterBuilder()
            .addName("nio")
            .addName(Bundle.formatInternational(Bundle.Keys.nio))
            .setRemarks(Bundle.formatInternational(Bundle.Keys.nioRemarks))
            .setRequired(false)
            .create(Boolean.class, Boolean.FALSE);

    /**
     * Default Implementation abuses the naming convention.
     * <p>
     * Will return <code>Foo</code> for
     * <code>org.geotoolkit.data.foo.FooFactory</code>.
     * </p>
     * @return return display name based on class name
     */
    @Override
    public CharSequence getDisplayName() {
        String name = this.getClass().getName();

        name = name.substring(name.lastIndexOf('.')+1);
        if (name.endsWith("Factory")) {
            name = name.substring(0, name.length() - 7);
        }
        return name;
    }

    /**
     * Default Implementation returns the display name.
     * @return return display name
     */
    @Override
    public CharSequence getDescription() {
        return getDisplayName();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Client open(Map<String, ? extends Serializable> params) throws DataStoreException {
        params = forceIdentifier(params);

        final ParameterValueGroup prm = FeatureUtilities.toParameter(params,getParametersDescriptor());
        if(prm == null){
            return null;
        }
        try{
            return open(prm);
        }catch(InvalidParameterValueException ex){
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean canProcess(Map<String, ? extends Serializable> params) {
        params = forceIdentifier(params);

        final ParameterValueGroup prm = FeatureUtilities.toParameter(params,getParametersDescriptor());
        if(prm == null){
            return false;
        }
        try{
            return canProcess(prm);
        }catch(InvalidParameterValueException ex){
            return false;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean canProcess(final ParameterValueGroup params) {
        if(params == null){
            return false;
        }

        //check identifier value is exist
        final boolean validId = checkIdentifier(params);
        if(!validId){
            return false;
        }

        final ParameterDescriptorGroup desc = getParametersDescriptor();
        if(!desc.getName().getCode().equalsIgnoreCase(params.getDescriptor().getName().getCode())){
            return false;
        }

        final ConformanceResult result = Parameters.isValid(params, getParametersDescriptor());
        return (result != null) && Boolean.TRUE.equals(result.pass());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ConformanceResult availability() {
        DefaultConformanceResult result =  new DefaultConformanceResult();
        result.setPass(Boolean.TRUE);
        return result;
    }

    /**
     * Set the identifier parameter in the map if not present.
     */
    private Map<String,Serializable> forceIdentifier(Map params){

        if(!params.containsKey(IDENTIFIER.getName().getCode())){
            //identifier is not specified, force it
            final ParameterDescriptorGroup desc = getParametersDescriptor();
            params = new HashMap<String, Serializable>(params);
            final Object value = ((ParameterDescriptor)desc.descriptor(IDENTIFIER.getName().getCode())).getDefaultValue();
            params.put(IDENTIFIER.getName().getCode(), (Serializable)value);
        }
        return params;
    }

    /**
     * Check if the Identifier parameter exist.
     * if it exist, it must be set to 'value' otherwise return false.
     * if not present, return true;
     * @return
     */
    protected boolean checkIdentifier(final ParameterValueGroup params){
        final String expectedId = ((ParameterDescriptor<String>)getParametersDescriptor()
                .descriptor(IDENTIFIER.getName().getCode())).getDefaultValue();

        for(GeneralParameterValue val : params.values()){
            if(val.getDescriptor().getName().getCode().equals(IDENTIFIER.getName().getCode())){
                final Object candidate = ((ParameterValue)val).getValue();
                return expectedId.equals(candidate);
            }
        }

        return true;
    }

    /**
     * @see #checkIdentifier(org.opengis.parameter.ParameterValueGroup)
     * @throws DataStoreException if identifier is not valid
     */
    protected void checkCanProcessWithError(final ParameterValueGroup params) throws DataStoreException{
        final boolean valid = canProcess(params);
        if(!valid){
            throw new DataStoreException("Parameter values not supported by this factory.");
        }
    }

}
