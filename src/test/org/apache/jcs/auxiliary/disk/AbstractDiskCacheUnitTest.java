package org.apache.jcs.auxiliary.disk;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.jcs.TestLogConfigurationUtil;
import org.apache.jcs.auxiliary.AuxiliaryCacheAttributes;
import org.apache.jcs.auxiliary.disk.behavior.IDiskCacheAttributes;
import org.apache.jcs.auxiliary.disk.indexed.IndexedDiskCacheAttributes;
import org.apache.jcs.engine.CacheConstants;
import org.apache.jcs.engine.CacheElement;
import org.apache.jcs.engine.ElementAttributes;
import org.apache.jcs.engine.behavior.ICacheElement;
import org.apache.jcs.engine.behavior.IElementAttributes;

/** Tests for the abstract disk cache. It's largely tested by actual instances. */
public class AbstractDiskCacheUnitTest
    extends TestCase
{
    /**
     * Verify that update and get work.
     * <p>
     * @throws IOException
     */
    public void testUpdateGet_allowed()
        throws IOException
    {
        // SETUP
        String cacheName = "testUpdateGet_allowed";
        IDiskCacheAttributes diskCacheAttributes = new IndexedDiskCacheAttributes();
        diskCacheAttributes.setCacheName( cacheName );

        AbstractDiskCacheTestInstance diskCache = new AbstractDiskCacheTestInstance( diskCacheAttributes );

        String key = "myKey";
        String value = "myValue";
        IElementAttributes elementAttributes = new ElementAttributes();
        ICacheElement cacheElement = new CacheElement( cacheName, key, value, elementAttributes );

        diskCache.update( cacheElement );

        // DO WORK
        ICacheElement result = diskCache.get( key );

        // VERIFY
        //System.out.println( diskCache.getStats() );
        assertNotNull( "Item should be in the map.", result );
    }

    /**
     * Verify that alive is set to false..
     * <p>
     * @throws IOException
     */
    public void testDispose()
        throws IOException
    {
        // SETUP
        String cacheName = "testDispose";
        IDiskCacheAttributes diskCacheAttributes = new IndexedDiskCacheAttributes();
        diskCacheAttributes.setCacheName( cacheName );

        AbstractDiskCacheTestInstance diskCache = new AbstractDiskCacheTestInstance( diskCacheAttributes );

        String key = "myKey";
        String value = "myValue";
        IElementAttributes elementAttributes = new ElementAttributes();
        ICacheElement cacheElement = new CacheElement( cacheName, key, value, elementAttributes );

        diskCache.update( cacheElement );

        // DO WORK
        diskCache.dispose();

        // VERIFY
        assertFalse( "disk cache should not be alive.", diskCache.alive );
        assertEquals( "Status should be disposed", CacheConstants.STATUS_DISPOSED, diskCache.getStatus() );
    }

    /**
     * Verify that removeAll is prohibited.
     * <p>
     * @throws IOException
     */
    public void testRemoveAll_notAllowed()
        throws IOException
    {
        // SETUP
        StringWriter stringWriter = new StringWriter();
        TestLogConfigurationUtil.configureLogger( stringWriter, AbstractDiskCache.class.getName() );

        IDiskCacheAttributes diskCacheAttributes = new IndexedDiskCacheAttributes();
        diskCacheAttributes.setAllowRemoveAll( false );

        AbstractDiskCacheTestInstance diskCache = new AbstractDiskCacheTestInstance( diskCacheAttributes );

        String cacheName = "testRemoveAll_notAllowed";
        String key = "myKey";
        String value = "myValue";
        IElementAttributes elementAttributes = new ElementAttributes();
        ICacheElement cacheElement = new CacheElement( cacheName, key, value, elementAttributes );

        diskCache.update( cacheElement );

        // DO WORK
        diskCache.removeAll();
        String result = stringWriter.toString();

        // VERIFY
        assertTrue( "Should say not allowed.", result.indexOf( "set to false" ) != -1 );
        assertNotNull( "Item should be in the map.", diskCache.get( key ) );
    }

    /**
     * Verify that removeAll is allowed.
     * <p>
     * @throws IOException
     */
    public void testRemoveAll_allowed()
        throws IOException
    {
        // SETUP
        IDiskCacheAttributes diskCacheAttributes = new IndexedDiskCacheAttributes();
        diskCacheAttributes.setAllowRemoveAll( true );

        AbstractDiskCacheTestInstance diskCache = new AbstractDiskCacheTestInstance( diskCacheAttributes );

        String cacheName = "testRemoveAll_allowed";
        String key = "myKey";
        String value = "myValue";
        IElementAttributes elementAttributes = new ElementAttributes();
        ICacheElement cacheElement = new CacheElement( cacheName, key, value, elementAttributes );

        diskCache.update( cacheElement );

        // DO WORK
        diskCache.removeAll();

        // VERIFY
        assertNull( "Item should not be in the map.", diskCache.get( key ) );
    }

    /** Concrete, testable instance. */
    protected static class AbstractDiskCacheTestInstance
        extends AbstractDiskCache
    {
        /** Internal map */
        protected Map<Serializable, ICacheElement> map = new HashMap<Serializable, ICacheElement>();

        /** used by the abstract aux class */
        protected IDiskCacheAttributes diskCacheAttributes;

        /**
         * Creates the disk cache.
         * <p>
         * @param attr
         */
        public AbstractDiskCacheTestInstance( IDiskCacheAttributes attr )
        {
            super( attr );
            diskCacheAttributes = attr;
            super.alive = true;
        }

        /** Nothing. */
        private static final long serialVersionUID = 1L;

        /**
         * The location on disk
         * <p>
         * @return "memory"
         */
        @Override
        protected String getDiskLocation()
        {
            return "memory";
        }

        /**
         * @param groupName
         * @return Collections.EMPTY_SET
         */
        @Override
        public Set<Serializable> getGroupKeys(String groupName)
        {
            return Collections.emptySet();
        }

        /**
         * @return map.size()
         */
        @Override
        public int getSize()
        {
            return map.size();
        }

        /**
         * @throws IOException
         */
        @Override
        protected void processDispose()
            throws IOException
        {
            //System.out.println( "processDispose" );
        }

        /**
         * @param key
         * @return ICacheElement
         * @throws IOException
         */
        @Override
        protected ICacheElement processGet( Serializable key )
            throws IOException
        {
            //System.out.println( "processGet: " + key );
            return map.get( key );
        }

        /**
         * @param pattern
         * @return Collections.EMPTY_MAP
         * @throws IOException
         */
        @Override
        protected Map<Serializable, ICacheElement> processGetMatching( String pattern )
            throws IOException
        {
            return Collections.emptyMap();
        }

        /**
         * @param key
         * @return false
         * @throws IOException
         */
        @Override
        protected boolean processRemove( Serializable key )
            throws IOException
        {
            return map.remove( key ) != null;
        }

        /**
         * @throws IOException
         */
        @Override
        protected void processRemoveAll()
            throws IOException
        {
            //System.out.println( "processRemoveAll" );
            map.clear();
        }

        /**
         * @param cacheElement
         * @throws IOException
         */
        @Override
        protected void processUpdate( ICacheElement cacheElement )
            throws IOException
        {
            //System.out.println( "processUpdate: " + cacheElement );
            map.put( cacheElement.getKey(), cacheElement );
        }

        /**
         * @return null
         */
        public AuxiliaryCacheAttributes getAuxiliaryCacheAttributes()
        {
            return diskCacheAttributes;
        }
    }
}
