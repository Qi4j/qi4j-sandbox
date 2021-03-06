/*
 * Copyright 2007 Alin Dreghiciu. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.qi4j.library.beans.properties;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.qi4j.api.composite.Composite;
import org.qi4j.api.composite.TransientBuilder;
import org.qi4j.api.composite.TransientComposite;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.test.AbstractQi4jTest;

import java.util.Iterator;

public class PropertiesMixinTest extends AbstractQi4jTest
{
    private SampleJavaBean m_proxy;

    public void assemble( ModuleAssembly aModule ) throws AssemblyException
    {
        aModule.addTransients( SampleJavaBeanComposite.class );
    }

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        TransientBuilder<SampleJavaBeanComposite> builder = transientBuilderFactory.newTransientBuilder( SampleJavaBeanComposite.class );
        m_proxy = builder.newInstance();
    }

    @Test
    public void setAndGetFoo()
    {
        m_proxy.setFoo( "aValue" );
        assertEquals( "aValue", m_proxy.getFoo() );
    }

    @Test
    public void setAndGet()
    {
        m_proxy.set( "aValue" );
        assertEquals( "aValue", m_proxy.get() );
    }

    @Test
    public void getFooWithoutSetFoo()
    {
        assertEquals( null, m_proxy.getFoo() );
    }

    @Test
    public void iterateBarAfterAddBar()
    {
        m_proxy.addBar( "aValue" );
        Iterator<String> iterator = m_proxy.barIterator();
        assertNotNull( "iterator should not be null", iterator );
        assertEquals( "iterator has a value", true, iterator.hasNext() );
        assertEquals( "iterator content", "aValue", iterator.next() );
    }

    @Test
    public void iterateAfterAdd()
    {
        m_proxy.add( "aValue" );
        Iterator<String> iterator = m_proxy.iterator();
        assertNotNull( "iterator should not be null", iterator );
        assertEquals( "iterator has a value", true, iterator.hasNext() );
        assertEquals( "iterator content", "aValue", iterator.next() );
    }

    @Test
    public void iterateBarAfterAddAndRemoveBar()
    {
        m_proxy.addBar( "aValue" );
        m_proxy.removeBar( "aValue" );
        Iterator<String> iterator = m_proxy.barIterator();
        assertNotNull( iterator );
        assertFalse( iterator.hasNext() );
    }

    @Test
    public void iterateAfterAddAndRemove()
    {
        m_proxy.add( "aValue" );
        m_proxy.remove( "aValue" );
        Iterator<String> iterator = m_proxy.iterator();
        assertNotNull( iterator );
        assertFalse( iterator.hasNext() );
    }

    @Test
    public void removeBarWithoutAddBar()
    {
        m_proxy.removeBar( "aValue" );
    }

    @Test
    public void removeWithoutAdd()
    {
        m_proxy.remove( "aValue" );
    }

    @Test
    public void iterateBarWithoutAddBar()
    {
        Iterator<String> iterator = m_proxy.barIterator();
        assertNotNull( "iterator not supposed to be null", iterator );
        assertFalse( iterator.hasNext() );
    }

    @Test
    public void iterateWithoutAdd()
    {
        Iterator<String> iterator = m_proxy.barIterator();
        assertNotNull( "iterator not supposed to be null", iterator );
        assertFalse( iterator.hasNext() );
    }

    @Test
    public void addFooAndGetFoo()
    {
        m_proxy.addFoo( "aValue" );
        assertNull( "getter supposed to be null", m_proxy.getFoo() );
    }

    @Test
    public void addFooAndSetFoo()
    {
        m_proxy.addFoo( "addValue" );
        m_proxy.setFoo( "setValue" );
    }

    @Test
    public void setFooAndAddFoo()
    {
        m_proxy.setFoo( "setValue" );
        m_proxy.addFoo( "addValue" );
    }

    @Test
    public void setFooAndRemoveFoo()
    {
        m_proxy.setFoo( "aValue" );
        m_proxy.removeFoo( "aValue" );
        assertEquals( "aValue", m_proxy.getFoo() );
    }

    @Test
    public void setFooAndIterateFoo()
    {
        m_proxy.setFoo( "aValue" );
        Iterator<String> iterator = m_proxy.fooIterator();
        assertNotNull( "iterator not supposed to be null", iterator );
        assertFalse( iterator.hasNext() );
    }

    @Test
    public void setValidAndIsValid()
    {
        m_proxy.setValid( true );
        assertTrue( m_proxy.isValid() );
    }

    @Test
    public void setTestedAndHasTested()
    {
        m_proxy.setTested( true );
        assertTrue( m_proxy.hasTested() );
    }

    @Mixins( PropertiesMixin.class )
    public static interface SampleJavaBeanComposite extends SampleJavaBean, TransientComposite
    {
    }

    public static interface SampleJavaBean
    {
        public String getFoo();

        public void setFoo( String value );

        public String get();

        public void set( String value );

        public void addFoo( String value );

        public void removeFoo( String value );

        public Iterator<String> fooIterator();

        public void addBar( String value );

        public void removeBar( String value );

        public Iterator<String> barIterator();

        public void add( String value );

        public void remove( String value );

        public Iterator<String> iterator();

        public boolean isValid();

        public void setValid( boolean value );

        public boolean hasTested();

        public void setTested( boolean value );
    }

}
