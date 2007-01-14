package com.googlecode.jsonplugin;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.mock.MockActionInvocation;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;

import org.apache.struts2.StrutsStatics;
import org.apache.struts2.StrutsTestCase;

import java.io.BufferedReader;
import java.io.StringReader;

import java.util.List;
import java.util.Map;

public class JSONInterceptorTest extends StrutsTestCase {
    private MockActionInvocation invocation;

    public void test() throws Exception {
        JSONInterceptor interceptor = new JSONInterceptor();
        TestAction action = new TestAction();

        invocation.setAction(action);

        interceptor.intercept(invocation);

        //serialize and compare
        List list = action.getList();

        assertNotNull(list);
        assertEquals(list.size(), 10);

        list = action.getCollection();
        assertNotNull(list);
        assertEquals(list.size(), 3);
        assertEquals(list.get(0), "b");
        assertEquals(list.get(1), 1L);
        list = (List) list.get(2);
        assertNotNull(list);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0), 10L);
        assertEquals(list.get(1), 12L);

        list = action.getCollection2();
        assertNotNull(list);
        assertEquals(list.size(), 1);

        //inside a map any primitive is either: String, Long, Boolean or Double
        Map bean = (Map) list.get(0);

        assertNotNull(bean);
        assertTrue((Boolean) bean.get("booleanField"));
        assertEquals(bean.get("charField"), "s");
        assertEquals(bean.get("doubleField"), 10.1);
        assertEquals(bean.get("floatField"), 1.5);
        assertEquals(bean.get("intField"), 10L);
        assertEquals(bean.get("longField"), 100L);
        assertEquals(bean.get("stringField"), "str");

        bean = (Map) bean.get("objectField");
        assertNotNull(bean);
        assertFalse((Boolean) bean.get("booleanField"));
        assertEquals(bean.get("charField"), "\u0000");
        assertEquals(bean.get("doubleField"), 2.2);
        assertEquals(bean.get("floatField"), 1.1);
        assertEquals(bean.get("intField"), 0L);
        assertEquals(bean.get("longField"), 0L);
        assertEquals(bean.get("stringField"), "  ");

        assertEquals(action.getFoo(), "foo");

        Map map = action.getMap();

        assertNotNull(map);
        assertEquals(map.size(), 2);
        assertEquals(map.get("a"), 1L);
        list = (List) map.get("c");
        assertNotNull(list);
        assertEquals(list.size(), 2);
        assertEquals(list.get(0), 1.0);
        assertEquals(list.get(1), 2.0);

        assertEquals(action.getResult(), null);

        Bean bean2 = (Bean) action.getBean();

        assertNotNull(bean2);
        assertTrue(bean2.isBooleanField());
        assertEquals(bean2.getStringField(), "test");
        assertEquals(bean2.getIntField(), 10);
        assertEquals(bean2.getCharField(), 's');
        assertEquals(bean2.getDoubleField(), 10.1);
        assertEquals(bean2.getByteField(), 3);

        String[] strArray = action.getArray();

        assertNotNull(strArray);
        assertEquals(strArray.length, 2);
        assertEquals(strArray[0], "str0");
        assertEquals(strArray[1], "str1");

        int[] intArray = action.getIntArray();

        assertNotNull(intArray);
        assertEquals(intArray.length, 2);
        assertEquals(intArray[0], 1);
        assertEquals(intArray[1], 2);

        Bean[] beanArray = action.getBeanArray();

        assertNotNull(beanArray);
        assertNotNull(beanArray[0]);
        assertEquals(beanArray[0].getStringField(), "bean1");
        assertNotNull(beanArray[1]);
        assertEquals(beanArray[1].getStringField(), "bean2");
    }

    protected void setUp() throws Exception {
        super.setUp();

        StringReader stringReader =
            new StringReader(TestUtils.readContent(
                    JSONInterceptorTest.class.getResource("json-1.txt")));
        StrutsMockHttpServletRequest request =
            new StrutsMockHttpServletRequest();

        request.setupAddHeader("content-type", "application/json");
        request.setupGetReader(new BufferedReader(stringReader));

        ValueStack stack = ValueStackFactory.getFactory().createValueStack();
        ActionContext context = new ActionContext(stack.getContext());

        ActionContext.setContext(context);
        context.put(StrutsStatics.HTTP_REQUEST, request);

        StrutsMockServletContext servletContext =
            new StrutsMockServletContext();

        context.put(StrutsStatics.SERVLET_CONTEXT, servletContext);
        invocation = new MockActionInvocation();
        invocation.setInvocationContext(context);
    }
}
