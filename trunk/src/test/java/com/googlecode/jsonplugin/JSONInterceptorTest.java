package com.googlecode.jsonplugin;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.struts2.StrutsStatics;
import org.apache.struts2.StrutsTestCase;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.mock.MockActionInvocation;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;

public class JSONInterceptorTest extends StrutsTestCase {
    private MockActionInvocationEx invocation;
    private StrutsMockHttpServletRequest request;

    public void _testSMDDisabledSMD() throws Exception {
        //request
        StringReader stringReader =
            new StringReader(TestUtils.readContent(
                    JSONInterceptorTest.class.getResource("smd-3.txt")));
        request.setupGetReader(new BufferedReader(stringReader));
        request.setupAddHeader("content-type", "application/json-rpc");
        
        JSONInterceptor interceptor = new JSONInterceptor();
        SMDActionTest1 action = new SMDActionTest1();

        invocation.setAction(action);

        //SMD was not enabled so invocation must happen
        interceptor.intercept(invocation);
        assertTrue(invocation.isInvoked());
    }
    
    public void _testSMDNoMethod() throws Exception {
        //request
        StringReader stringReader =
            new StringReader(TestUtils.readContent(
                    JSONInterceptorTest.class.getResource("smd-4.txt")));
        request.setupGetReader(new BufferedReader(stringReader));
        request.setupAddHeader("content-type", "application/json-rpc");
        
        JSONInterceptor interceptor = new JSONInterceptor();
        interceptor.setEnableSMD(true);
        SMDActionTest1 action = new SMDActionTest1();

        invocation.setAction(action);

        //SMD was enabled so invocation must happen
        interceptor.intercept(invocation);
        assertFalse(invocation.isInvoked());
    }
    
    public void testSMD1() throws Exception {
        //request
        StringReader stringReader =
            new StringReader(TestUtils.readContent(
                    JSONInterceptorTest.class.getResource("smd-3.txt")));
        request.setupGetReader(new BufferedReader(stringReader));
        request.setupAddHeader("content-type", "application/json-rpc");
        
        JSONInterceptor interceptor = new JSONInterceptor();
        interceptor.setEnableSMD(true);
        SMDActionTest1 action = new SMDActionTest1();

        invocation.setAction(action);

        //can't be invoked
        interceptor.intercept(invocation);
        assertTrue(invocation.isInvoked());
        
        
    }
    
    public void _test() throws Exception {
        //request
        StringReader stringReader =
            new StringReader(TestUtils.readContent(
                    JSONInterceptorTest.class.getResource("json-1.txt")));
        request.setupGetReader(new BufferedReader(stringReader));
        request.setupAddHeader("content-type", "application/json");
        
        //interceptor
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

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(action.getDate());

        assertEquals(calendar.get(Calendar.YEAR), 1999);
        assertEquals(calendar.get(Calendar.MONTH), Calendar.DECEMBER);
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), 31);
        assertEquals(calendar.get(Calendar.HOUR), 11);
        assertEquals(calendar.get(Calendar.MINUTE), 59);
        assertEquals(calendar.get(Calendar.SECOND), 59);

        calendar.setTime(action.getDate2());
        assertEquals(calendar.get(Calendar.YEAR), 1999);
        assertEquals(calendar.get(Calendar.MONTH), Calendar.DECEMBER);
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), 31);

        //test desrialize=false
        assertNull(action.getFoo2());
    }

    protected void setUp() throws Exception {
        super.setUp();

        request =
            new StrutsMockHttpServletRequest();

        ValueStack stack = ValueStackFactory.getFactory().createValueStack();
        ActionContext context = new ActionContext(stack.getContext());

        ActionContext.setContext(context);
        context.put(StrutsStatics.HTTP_REQUEST, request);

        StrutsMockServletContext servletContext =
            new StrutsMockServletContext();

        context.put(StrutsStatics.SERVLET_CONTEXT, servletContext);
        invocation = new MockActionInvocationEx();
        invocation.setInvocationContext(context);
    }
}

class MockActionInvocationEx extends MockActionInvocation {
    private boolean invoked;
    
    public String invoke() throws Exception {
        this.invoked = true;
        return super.invoke();
    }

    public boolean isInvoked() {
        return invoked;
    }

    public void setInvoked(boolean invoked) {
        this.invoked = invoked;
    }

   
}
