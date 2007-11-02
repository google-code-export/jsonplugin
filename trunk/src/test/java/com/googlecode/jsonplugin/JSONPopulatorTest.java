package com.googlecode.jsonplugin;

import java.io.StringReader;
import java.util.Map;

import junit.framework.TestCase;

public class JSONPopulatorTest extends TestCase {

    public void testPrimitiveBean() throws Exception {
        StringReader stringReader = new StringReader(TestUtils
            .readContent(JSONInterceptorTest.class.getResource("json-7.txt")));
        Object json = JSONUtil.deserialize(stringReader);
        assertNotNull(json);
        assertTrue(json instanceof Map);
        Map jsonMap = (Map) json;
        JSONPopulator populator = new JSONPopulator();
        Bean bean = new Bean();
        populator.populateObject(bean, jsonMap);
        assertTrue(bean.isBooleanField());
        assertEquals("test", bean.getStringField());
        assertEquals(10, bean.getIntField());
        assertEquals('s', bean.getCharField());
        assertEquals(10.1d, bean.getDoubleField(), 0d);
        assertEquals(3, bean.getByteField());
    }

    public void testObjectBean() throws Exception {
        StringReader stringReader = new StringReader(TestUtils
            .readContent(JSONInterceptorTest.class.getResource("json-7.txt")));
        Object json = JSONUtil.deserialize(stringReader);
        assertNotNull(json);
        assertTrue(json instanceof Map);
        Map jsonMap = (Map) json;
        JSONPopulator populator = new JSONPopulator();
        WrapperClassBean bean = new WrapperClassBean();
        populator.populateObject(bean, jsonMap);
        assertEquals(Boolean.TRUE, bean.getBooleanField());
        assertEquals("test", bean.getStringField());
        assertEquals(new Integer(10), bean.getIntField());
        assertEquals(new Character('s'), bean.getCharField());
        assertEquals(new Double(10.1d), bean.getDoubleField());
        assertEquals(new Byte((byte) 3), bean.getByteField());

    }
}
