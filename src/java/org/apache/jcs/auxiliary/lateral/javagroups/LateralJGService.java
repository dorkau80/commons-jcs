package org.apache.jcs.auxiliary.lateral.javagroups;


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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jcs.auxiliary.lateral.LateralCacheAttributes;
import org.apache.jcs.auxiliary.lateral.LateralCacheInfo;
import org.apache.jcs.auxiliary.lateral.LateralElementDescriptor;
import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheAttributes;
import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheObserver;
import org.apache.jcs.auxiliary.lateral.behavior.ILateralCacheService;
import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.behavior.ICacheListener;

/**
 * A lateral cache service implementation.
 *
 * @version $Id: LateralJGService.java,v 1.8 2002/02/17 07:16:24 asmuts Exp
 *      $
 */
public class LateralJGService
     implements ILateralCacheService, ILateralCacheObserver
{
    private final static Log log =
        LogFactory.getLog( LateralJGService.class );

    private ILateralCacheAttributes ilca;
    private LateralJGSender sender;

    /**
     * Constructor for the LateralJGService object
     *
     * @param lca
     * @exception IOException
     */
    public LateralJGService( ILateralCacheAttributes lca )
        throws IOException
    {
        this.ilca = lca;
        try
        {
            log.debug( "creating sender" );

            sender = new LateralJGSender( lca );

            log.debug( "created sender" );
        }
        catch ( IOException e )
        {

            log.error( "Could not create sender to [" + lca.getJGChannelProperties() + "] -- " + e.getMessage() );

            throw e;
        }
    }

    // -------------------------------------------------------- Service Methods

    /**
     * @param item
     * @exception IOException
     */
    public void update( ICacheElement item )
        throws IOException
    {
        update( item, LateralCacheInfo.listenerId );
    }

    /**
     * @param item
     * @param requesterId
     * @exception IOException
     */
    public void update( ICacheElement item, long requesterId )
        throws IOException
    {
        LateralElementDescriptor led = new LateralElementDescriptor( item );
        led.requesterId = requesterId;
        led.command = LateralElementDescriptor.UPDATE;
        sender.send( led );
    }

    /**
     * @param cacheName
     * @param key
     * @exception IOException
     */
    public void remove( String cacheName, Serializable key )
        throws IOException
    {
        remove( cacheName, key, LateralCacheInfo.listenerId );
    }

    /**
     * @param cacheName
     * @param key
     * @param requesterId
     * @exception IOException
     */
    public void remove( String cacheName, Serializable key, long requesterId )
        throws IOException
    {
        CacheElement ce = new CacheElement( cacheName, key, null );
        LateralElementDescriptor led = new LateralElementDescriptor( ce );
        led.requesterId = requesterId;
        led.command = LateralElementDescriptor.REMOVE;
        sender.send( led );
    }

    /**
     * @exception IOException
     */
    public void release()
        throws IOException
    {
        // nothing needs to be done
    }

    /**
     * Will close the connection.
     *
     * @param cache
     * @exception IOException
     */
    public void dispose( String cache )
        throws IOException
    {
        sender.dispose( cache );
    }

    /**
     * @return
     * @param cacheName
     * @param key
     * @exception IOException
     */
    public ICacheElement get( String cacheName, Serializable key )
        throws IOException
    {
        //p( "get(cacheName,key,container)" );
        CacheElement ce = new CacheElement( cacheName, key, null );
        LateralElementDescriptor led = new LateralElementDescriptor( ce );
        //led.requesterId = requesterId; // later
        led.command = LateralElementDescriptor.GET;
        return sender.sendAndReceive( led );
        //return null;
        // nothing needs to be done
    }

    /**
     * @param cacheName
     * @exception IOException
     */
    public void removeAll( String cacheName )
        throws IOException
    {
        removeAll( cacheName, LateralCacheInfo.listenerId );
    }

    /**
     * @param cacheName
     * @param requesterId
     * @exception IOException
     */
    public void removeAll( String cacheName, long requesterId )
        throws IOException
    {
        CacheElement ce = new CacheElement( cacheName, "ALL", null );
        LateralElementDescriptor led = new LateralElementDescriptor( ce );
        led.requesterId = requesterId;
        led.command = LateralElementDescriptor.REMOVEALL;
        sender.send( led );
    }

    /**
      * Gets the set of keys of objects currently in the group
      * throws UnsupportedOperationException
      */
     public Set getGroupKeys(String cacheName, String group)
     {
         if (true)
         {
             throw new UnsupportedOperationException("Groups not implemented.");
         }
         return null;
     }


    /**
     * @param args
     */
    public static void main( String args[] )
    {
        try
        {
            LateralJGSender sender =
                new LateralJGSender( new LateralCacheAttributes() );

            // process user input till done
            boolean notDone = true;
            String message = null;
            // wait to dispose
            BufferedReader br =
                new BufferedReader( new InputStreamReader( System.in ) );

            while ( notDone )
            {
                System.out.println( "enter mesage:" );
                message = br.readLine();
                CacheElement ce = new CacheElement( "test", "test", message );
                LateralElementDescriptor led = new LateralElementDescriptor( ce );
                sender.send( led );
            }
        }
        catch ( Exception e )
        {
            System.out.println( e.toString() );
        }
    }

    // ILateralCacheObserver methods, do nothing here since
    // the connection is not registered, the udp service is
    // is not registered.

    /**
     * @param cacheName The feature to be added to the CacheListener attribute
     * @param obj The feature to be added to the CacheListener attribute
     * @exception IOException
     */
    public void addCacheListener( String cacheName, ICacheListener obj )
        throws IOException
    {
        // Empty
    }

    /**
     * @param obj The feature to be added to the CacheListener attribute
     * @exception IOException
     */
    public void addCacheListener( ICacheListener obj )
        throws IOException
    {
        // Empty
    }


    /**
     * @param cacheName
     * @param obj
     * @exception IOException
     */
    public void removeCacheListener( String cacheName, ICacheListener obj )
        throws IOException
    {
        // Empty
    }

    /**
     * @param obj
     * @exception IOException
     */
    public void removeCacheListener( ICacheListener obj )
        throws IOException
    {
        // Empty
    }

}

