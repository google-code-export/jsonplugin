package com.googlecode.jsonplugin;

import java.io.StringReader;
import java.util.Map;

import com.opensymphony.xwork2.validator.annotations.ExpressionValidator;

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

        assertEquals(2, bean.getListField().size());
        assertEquals("1", bean.getListField().get(0).getValue());
        assertEquals("2", bean.getListField().get(1).getValue());

        assertEquals(1, bean.getListMapField().size());
        assertEquals(2, bean.getListMapField().get(0).size());
        assertEquals(new Long(2073501), bean.getListMapField().get(0).get("id1"));
        assertEquals(new Long(3), bean.getListMapField().get(0).get("id2"));

        assertEquals(2, bean.getMapListField().size());
        assertEquals(3, bean.getMapListField().get("id1").size());
        assertEquals(new Long(2), bean.getMapListField().get("id1").get(1));
        assertEquals(4, bean.getMapListField().get("id2").size());
        assertEquals(new Long(3), bean.getMapListField().get("id2").get(1));

        assertEquals(1, bean.getArrayMapField().length);
        assertEquals(2, bean.getArrayMapField()[0].size());
        assertEquals(new Long(2073501), bean.getArrayMapField()[0].get("id1"));
        assertEquals(new Long(3), bean.getArrayMapField()[0].get("id2"));
    }

    public void testObjectBeanWithStrings() throws Exception {
        StringReader stringReader = new StringReader(TestUtils
            .readContent(JSONInterceptorTest.class.getResource("json-8.txt")));
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

        assertEquals(null, bean.getListField());
        assertEquals(null, bean.getListMapField());
        assertEquals(null, bean.getMapListField());
        assertEquals(null, bean.getArrayMapField());
    }

    public void testInfiniteLoop() throws JSONException {
        try {
            JSONReader reader = new JSONReader();
            reader.read("[1,\"a]");
            fail("Should have thrown an exception");
        } catch (JSONException e) {
            //I can't get JUnit to ignore the exception
            // @Test(expected = JSONException.class)
        }
    }

    public void testParseBadInput() throws JSONException {
        try {
            JSONReader reader = new JSONReader();
            reader.read("[1,\"a\"1]");
            fail("Should have thrown an exception");
        } catch (JSONException e) {
            //I can't get JUnit to ignore the exception
            // @Test(expected = JSONException.class)
        }
    }
}
