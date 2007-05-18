/*
 + * $Id$
 + *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.googlecode.jsonplugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.struts2.StrutsStatics;
import org.apache.struts2.StrutsTestCase;
import org.jmock.Mock;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.mock.MockActionInvocation;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;

/**
 * JSONResultTest
 *
 */
public class JSONResultTest extends StrutsTestCase {
    MockActionInvocation invocation;
    StrutsMockHttpServletResponse response;
    Mock responseMock;
    StringWriter stringWriter;
    PrintWriter writer;
    StrutsMockServletContext servletContext;
    ActionContext context;
    ValueStack stack;
    private StrutsMockHttpServletRequest request;

    public void testSMDDisabledSMD() throws Exception {
        JSONResult result = new JSONResult();
        SMDActionTest1 action = new SMDActionTest1();

        this.invocation.setAction(action);
        result.execute(this.invocation);

        String smd = this.stringWriter.toString();

        String normalizedActual = TestUtils.normalize(smd, true);
        String normalizedExpected = TestUtils.normalize(JSONResultTest.class
            .getResource("smd-8.txt"));
        assertEquals(normalizedExpected, normalizedActual);
    }

    public void testSMDDefault() throws Exception {
        JSONResult result = new JSONResult();
        result.setEnableSMD(true);
        SMDActionTest1 action = new SMDActionTest1();

        this.invocation.setAction(action);
        result.execute(this.invocation);

        String smd = this.stringWriter.toString();

        String normalizedActual = TestUtils.normalize(smd, true);
        String normalizedExpected = TestUtils.normalize(JSONResultTest.class
            .getResource("smd-1.txt"));
        assertEquals(normalizedExpected, normalizedActual);
    }

    public void testSMDDefaultAnnotations() throws Exception {
        JSONResult result = new JSONResult();
        result.setEnableSMD(true);
        SMDActionTest2 action = new SMDActionTest2();

        this.invocation.setAction(action);
        result.execute(this.invocation);

        String smd = this.stringWriter.toString();

        String normalizedActual = TestUtils.normalize(smd, true);
        String normalizedExpected = TestUtils.normalize(JSONResultTest.class
            .getResource("smd-2.txt"));
        assertEquals(normalizedExpected, normalizedActual);
    }

    @SuppressWarnings("unchecked")
    public void test() throws Exception {
        JSONResult result = new JSONResult();

        TestAction action = new TestAction();

        //test scape characters
        action.setArray(new String[] { "a", "a", "\"", "\\", "/", "\b", "\f", "\n", "\r",
                "\t" });

        List list = new ArrayList();

        list.add("b");
        list.add(1);
        list.add(new int[] { 10, 12 });
        action.setCollection(list);

        //beans
        List collection2 = new ArrayList();
        Bean bean1 = new Bean();

        bean1.setStringField("str");
        bean1.setBooleanField(true);
        bean1.setCharField('s');
        bean1.setDoubleField(10.1);
        bean1.setFloatField(1.5f);
        bean1.setIntField(10);
        bean1.setLongField(100);

        Bean bean2 = new Bean();

        bean2.setStringField("  ");
        bean2.setBooleanField(false);
        bean2.setFloatField(1.1f);
        bean2.setDoubleField(2.2);

        //circular reference
        bean1.setObjectField(bean2);
        bean2.setObjectField(bean1);

        collection2.add(bean1);
        action.setCollection2(collection2);

        //keep order in map
        Map map = new LinkedHashMap();

        map.put("a", 1);
        map.put("c", new float[] { 1.0f, 2.0f });
        action.setMap(map);

        action.setFoo("foo");
        //should be ignored, marked 'transient'
        action.setBar("bar");

        //date
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1999);
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        action.setDate(calendar.getTime());
        action.setDate2(calendar.getTime());

        this.invocation.setAction(action);
        result.execute(this.invocation);

        String json = this.stringWriter.toString();

        String normalizedActual = TestUtils.normalize(json, true);
        String normalizedExpected = TestUtils.normalize(JSONResultTest.class
            .getResource("json.txt"));
        assertEquals(normalizedExpected, normalizedActual);
    }

    @SuppressWarnings("unchecked")
    public void testCommentWrap() throws Exception {
        JSONResult result = new JSONResult();

        TestAction action = new TestAction();

        //test scape characters
        action.setArray(new String[] { "a", "a", "\"", "\\", "/", "\b", "\f", "\n", "\r",
                "\t" });

        List list = new ArrayList();

        list.add("b");
        list.add(1);
        list.add(new int[] { 10, 12 });
        action.setCollection(list);

        //beans
        List collection2 = new ArrayList();
        Bean bean1 = new Bean();

        bean1.setStringField("str");
        bean1.setBooleanField(true);
        bean1.setCharField('s');
        bean1.setDoubleField(10.1);
        bean1.setFloatField(1.5f);
        bean1.setIntField(10);
        bean1.setLongField(100);

        Bean bean2 = new Bean();

        bean2.setStringField("  ");
        bean2.setBooleanField(false);
        bean2.setFloatField(1.1f);
        bean2.setDoubleField(2.2);

        //circular reference
        bean1.setObjectField(bean2);
        bean2.setObjectField(bean1);

        collection2.add(bean1);
        action.setCollection2(collection2);

        //keep order in map
        Map map = new LinkedHashMap();

        map.put("a", 1);
        map.put("c", new float[] { 1.0f, 2.0f });
        action.setMap(map);

        action.setFoo("foo");
        //should be ignored, marked 'transient'
        action.setBar("bar");

        //date
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 1999);
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        action.setDate(calendar.getTime());
        action.setDate2(calendar.getTime());

        this.invocation.setAction(action);
        result.setWrapWithComments(true);
        result.execute(this.invocation);

        String json = this.stringWriter.toString();

        String normalizedActual = TestUtils.normalize(json, true);
        String normalizedExpected = TestUtils.normalize(JSONResultTest.class
            .getResource("json-3.txt"));
        assertEquals(normalizedExpected, normalizedActual);
    }

    public void test2() throws Exception {
        JSONResult result = new JSONResult();

        TestAction action = new TestAction();

        //beans
        Bean bean1 = new Bean();

        bean1.setStringField("str");
        bean1.setBooleanField(true);
        bean1.setCharField('s');
        bean1.setDoubleField(10.1);
        bean1.setFloatField(1.5f);
        bean1.setIntField(10);
        bean1.setLongField(100);

        //set root
        action.setBean(bean1);
        result.setRoot("bean");

        ValueStack stack = ValueStackFactory.getFactory().createValueStack();
        stack.push(action);
        this.invocation.setStack(stack);
        this.invocation.setAction(action);

        result.execute(this.invocation);

        String json = this.stringWriter.toString();

        String normalizedActual = TestUtils.normalize(json, true);
        String normalizedExpected = TestUtils.normalize(JSONResultTest.class
            .getResource("json-2.txt"));
        assertEquals(normalizedExpected, normalizedActual);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(this.stringWriter);
        this.response = new StrutsMockHttpServletResponse();
        this.response.setWriter(this.writer);
        this.request = new StrutsMockHttpServletRequest();
        this.request.setRequestURI("http://sumeruri");
        this.stack = ValueStackFactory.getFactory().createValueStack();
        this.context = new ActionContext(this.stack.getContext());
        this.context.put(StrutsStatics.HTTP_RESPONSE, this.response);
        this.context.put(StrutsStatics.HTTP_REQUEST, this.request);
        this.servletContext = new StrutsMockServletContext();
        this.context.put(StrutsStatics.SERVLET_CONTEXT, this.servletContext);
        this.invocation = new MockActionInvocation();
        this.invocation.setInvocationContext(this.context);
    }
}
