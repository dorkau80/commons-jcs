package org.apache.jcs.engine.control;


/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jcs.auxiliary.AuxiliaryCache;
import org.apache.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.jcs.auxiliary.AuxiliaryCacheFactory;
import org.apache.jcs.config.OptionConverter;
import org.apache.jcs.config.PropertySetter;
import org.apache.jcs.engine.behavior.ICache;
import org.apache.jcs.engine.behavior.ICompositeCacheAttributes;
import org.apache.jcs.engine.behavior.IElementAttributes;

/**
 * This class is based on the log4j class org.apache.log4j.PropertyConfigurator
 * which was made by: "Luke Blanshard" <Luke@quiq.com> "Mark DONSZELMANN"
 * <Mark.Donszelmann@cern.ch> "Anders Kristensen" <akristensen@dynamicsoft.com>
 *
 */
public class CompositeCacheConfigurator
{
    private final static Log log =
        LogFactory.getLog( CompositeCacheConfigurator.class );

    final static String DEFAULT_REGION = "jcs.default";
    final static String REGION_PREFIX = "jcs.region.";
    final static String SYSTEM_REGION_PREFIX = "jcs.system.";
    final static String AUXILIARY_PREFIX = "jcs.auxiliary.";
    final static String ATTRIBUTE_PREFIX = ".attributes";
    final static String CACHE_ATTRIBUTE_PREFIX = ".cacheattributes";
    final static String ELEMENT_ATTRIBUTE_PREFIX = ".elementattributes";

    private CompositeCacheManager ccMgr;

    /**
     * Constructor for the CompositeCacheConfigurator object
     *
     * @param ccMgr
     */
    public CompositeCacheConfigurator( CompositeCacheManager ccMgr )
    {
        this.ccMgr = ccMgr;
    }

    /** Configure cached for file name. */
    public void doConfigure( String configFileName )
    {
        Properties props = new Properties();
        try
        {
            FileInputStream istream = new FileInputStream( configFileName );
            props.load( istream );
            istream.close();
        }
        catch ( IOException e )
        {
            log.error( "Could not read configuration file, ignored: " +
                       configFileName, e );
            return;
        }

        // If we reach here, then the config file is alright.
        doConfigure( props );
    }

    /** Configure cache for properties object */
    public void doConfigure( Properties properties )
    {

        // store props for use by non configured caches
        ccMgr.props = properties;
        // set default value list
        setDefaultAuxValues( properties );
        // set default cache attr
        setDefaultCompositeCacheAttributes( properties );
        // set default element attr
        setDefaultElementAttributes( properties );

        // set up ssytem caches to be used by non system caches
        // need to make sure there is no circuarity of reference
        parseSystemRegions( properties );

        // setup preconfigured caches
        parseRegions( properties );

    }

    /** Set the default aux list for new caches. */
    protected void setDefaultAuxValues( Properties props )
    {
        String value = OptionConverter.findAndSubst( DEFAULT_REGION, props );
        ccMgr.defaultAuxValues = value;

        log.info( "setting defaults to " + value );
    }

    /** Set the default CompositeCacheAttributes for new caches. */
    protected void setDefaultCompositeCacheAttributes( Properties props )
    {
        ICompositeCacheAttributes icca =
            parseCompositeCacheAttributes( props, "", CompositeCacheConfigurator.DEFAULT_REGION );
        ccMgr.setDefaultCacheAttributes( icca );

        log.info( "setting defaultCompositeCacheAttributes to " + icca );
    }

    /** Set the default ElementAttributes for new caches. */
    protected void setDefaultElementAttributes( Properties props )
    {
        IElementAttributes iea =
            parseElementAttributes( props, "", CompositeCacheConfigurator.DEFAULT_REGION );
        ccMgr.setDefaultElementAttributes( iea );

        log.info( "setting defaultElementAttributes to " + iea );
    }

    /**
     * Create caches used internally. System status gives them creation
     * priority.
     */
    protected void parseSystemRegions( Properties props )
    {
        Enumeration enum = props.propertyNames();
        while ( enum.hasMoreElements() )
        {
            String key = ( String ) enum.nextElement();
            if ( key.startsWith( SYSTEM_REGION_PREFIX )
                && ( key.indexOf( "attributes" ) == -1 ) )
            {
                String regionName = key.substring( SYSTEM_REGION_PREFIX.length() );
                String value = OptionConverter.findAndSubst( key, props );
                ICache cache;
                synchronized ( regionName )
                {
                    cache = parseRegion( props, regionName, value, null, SYSTEM_REGION_PREFIX );
                }
                ccMgr.systemCaches.put( regionName, cache );
                // to be availiable for remote reference they need to be here as well
                ccMgr.caches.put( regionName, cache );
            }
        }
    }

    /** Parse region elements. */
    protected void parseRegions( Properties props )
    {
        Enumeration enum = props.propertyNames();
        while ( enum.hasMoreElements() )
        {
            String key = ( String ) enum.nextElement();
            if ( key.startsWith( REGION_PREFIX ) && ( key.indexOf( "attributes" ) == -1 ) )
            {
                String regionName = key.substring( REGION_PREFIX.length() );
                String value = OptionConverter.findAndSubst( key, props );
                ICache cache;
                synchronized ( regionName )
                {
                    cache = parseRegion( props, regionName, value );
                }
                ccMgr.caches.put( regionName, cache );
            }
        }
    }

    /** Create cache region. */
    protected CompositeCache parseRegion( Properties props,
                                  String regName,
                                  String value )
    {
        return parseRegion( props, regName, value, null, REGION_PREFIX );
    }

    /** */
    protected CompositeCache parseRegion( Properties props,
                                  String regName,
                                  String value,
                                  ICompositeCacheAttributes cca )
    {
        return parseRegion( props, regName, value, cca, REGION_PREFIX );
    }

    /** */
    protected CompositeCache parseRegion( Properties props,
                                          String regName,
                                          String value,
                                          ICompositeCacheAttributes cca,
                                          String regionPrefix )
    {
        // First, create or get the cache and element attributes, and create
        // the cache.

        if ( cca == null )
        {
            cca = parseCompositeCacheAttributes( props, regName, regionPrefix );
        }

        IElementAttributes ea = parseElementAttributes( props, regName, regionPrefix );

        CompositeCache cache = new CompositeCache( regName, cca, ea );

        // Next, create the auxiliaries for the new cache

        List auxList = new ArrayList();

        log.debug( "Parsing region name '" + regName + "', value '" + value + "'" );

        // We must skip over ',' but not white space
        StringTokenizer st = new StringTokenizer( value, "," );

        // If value is not in the form ", appender.." or "", then we should set
        // the priority of the category.

        if ( !( value.startsWith( "," ) || value.equals( "" ) ) )
        {
            // just to be on the safe side...
            if ( !st.hasMoreTokens() )
            {
                return null;
            }
        }

        AuxiliaryCache auxCache;
        String auxName;
        while ( st.hasMoreTokens() )
        {
            auxName = st.nextToken().trim();
            if ( auxName == null || auxName.equals( "," ) )
            {
                continue;
            }
            log.debug( "Parsing auxiliary named \"" + auxName + "\"." );

            auxCache = parseAuxiliary( cache, props, auxName, regName );

            if ( auxCache != null )
            {
                auxList.add( auxCache );
            }
        }

        // Associate the auxiliaries with the cache

        cache.setAuxCaches(
            ( AuxiliaryCache[] ) auxList.toArray( new AuxiliaryCache[ 0 ] ) );

        // Return the new cache

        return cache;
    }

    /** Get an compositecacheattributes for the listed region. */
    protected ICompositeCacheAttributes
        parseCompositeCacheAttributes( Properties props, String regName )
    {
        return parseCompositeCacheAttributes( props, regName, REGION_PREFIX );
    }

    /** */
    protected ICompositeCacheAttributes
        parseCompositeCacheAttributes( Properties props,
                                       String regName,
                                       String regionPrefix )
    {
        ICompositeCacheAttributes ccAttr;

        String attrName = regionPrefix + regName + CACHE_ATTRIBUTE_PREFIX;

        // auxFactory was not previously initialized.
        //String prefix = regionPrefix + regName + ATTRIBUTE_PREFIX;
        ccAttr = ( ICompositeCacheAttributes ) OptionConverter.instantiateByKey( props, attrName,
                                                                                 org.apache.jcs.engine.behavior.ICompositeCacheAttributes.class,
                                                                                 null );
        if ( ccAttr == null )
        {
            log.warn( "Could not instantiate ccAttr named '" + attrName +
                      "', using defaults." );

            ICompositeCacheAttributes ccAttr2 = ccMgr.getDefaultCacheAttributes();
            ccAttr = ccAttr2.copy();
        }

        log.debug( "Parsing options for '" + attrName + "'" );

        PropertySetter.setProperties( ccAttr, props, attrName + "." );
        ccAttr.setCacheName( regName );

        log.debug( "End of parsing for \"" + attrName + "\"." );

        // GET CACHE FROM FACTORY WITH ATTRIBUTES
        ccAttr.setCacheName( regName );
        return ccAttr;
    }

    /** */
    protected IElementAttributes
        parseElementAttributes( Properties props,
                                String regName,
                                String regionPrefix )
    {
        IElementAttributes eAttr;

        String attrName = regionPrefix + regName + CompositeCacheConfigurator.ELEMENT_ATTRIBUTE_PREFIX;

        // auxFactory was not previously initialized.
        //String prefix = regionPrefix + regName + ATTRIBUTE_PREFIX;
        eAttr = ( IElementAttributes ) OptionConverter.instantiateByKey( props, attrName,
                                                                         org.apache.jcs.engine.behavior.IElementAttributes.class,
                                                                         null );
        if ( eAttr == null )
        {
            log.warn( "Could not instantiate eAttr named '" + attrName +
                      "', using defaults." );

            IElementAttributes eAttr2 = ccMgr.getDefaultElementAttributes();
            eAttr = eAttr2.copy();
        }

        log.debug( "Parsing options for '" + attrName + "'" );

        PropertySetter.setProperties( eAttr, props, attrName + "." );
        //eAttr.setCacheName( regName );

        log.debug( "End of parsing for \"" + attrName + "\"." );

        // GET CACHE FROM FACTORY WITH ATTRIBUTES
        //eAttr.setCacheName( regName );
        return eAttr;
    }

    /** Get an aux cache for the listed aux for a region. */
    protected AuxiliaryCache parseAuxiliary( CompositeCache cache,
                                             Properties props,
                                             String auxName,
                                             String regName )
    {
        AuxiliaryCache auxCache;

        // GET FACTORY
        AuxiliaryCacheFactory auxFac = ccMgr.registryFacGet( auxName );
        if ( auxFac == null )
        {
            // auxFactory was not previously initialized.
            String prefix = AUXILIARY_PREFIX + auxName;
            auxFac = ( AuxiliaryCacheFactory ) OptionConverter.instantiateByKey( props, prefix,
                                                                                 org.apache.jcs.auxiliary.AuxiliaryCacheFactory.class,
                                                                                 null );
            if ( auxFac == null )
            {
                log.error( "Could not instantiate auxFactory named \"" + auxName + "\"." );
                return null;
            }

            auxFac.setName( auxName );

            ccMgr.registryFacPut( auxFac );
        }

        // GET ATTRIBUTES
        AuxiliaryCacheAttributes auxAttr = ccMgr.registryAttrGet( auxName );
        String attrName = AUXILIARY_PREFIX + auxName + ATTRIBUTE_PREFIX;
        if ( auxAttr == null )
        {
            // auxFactory was not previously initialized.
            String prefix = AUXILIARY_PREFIX + auxName + ATTRIBUTE_PREFIX;
            auxAttr = ( AuxiliaryCacheAttributes ) OptionConverter.instantiateByKey( props, prefix,
                                                                                     org.apache.jcs.auxiliary.AuxiliaryCacheAttributes.class,
                                                                                     null );
            if ( auxFac == null )
            {
                log.error( "Could not instantiate auxAttr named '" + attrName + "'" );
                return null;
            }
            auxAttr.setName( auxName );
            ccMgr.registryAttrPut( auxAttr );
        }

        auxAttr = auxAttr.copy();

        log.debug( "Parsing options for '" + attrName + "'" );
        PropertySetter.setProperties( auxAttr, props, attrName + "." );
        auxAttr.setCacheName( regName );

        log.debug( "End of parsing for '" + attrName + "'" );

        // GET CACHE FROM FACTORY WITH ATTRIBUTES
        auxAttr.setCacheName( regName );
        auxCache = auxFac.createCache( auxAttr, cache );
        return auxCache;
    }
}
