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

    public void test() throws Exception {
        JSONResult result = new JSONResult();

        TestAction action = new TestAction();

        //test scape characters
        action.setArray(new String[] {
                "a", "a", "\"", "\\", "/", "\b", "\f", "\n", "\r", "\t"
            });

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
        calendar.set(Calendar.HOUR, 11);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        action.setDate(calendar.getTime());
        action.setDate2(calendar.getTime());

        invocation.setAction(action);
        result.execute(invocation);

        String json = stringWriter.toString();

        String normalizedActual = TestUtils.normalize(json, true);
        String normalizedExpected = TestUtils.normalize(JSONResultTest.class.getResource(
        "json.txt"));
        assertEquals(normalizedExpected, normalizedActual);
    }

    protected void setUp() throws Exception {
        super.setUp();
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        response = new StrutsMockHttpServletResponse();
        response.setWriter(writer);
        stack = ValueStackFactory.getFactory().createValueStack();
        context = new ActionContext(stack.getContext());
        context.put(StrutsStatics.HTTP_RESPONSE, response);
        servletContext = new StrutsMockServletContext();
        context.put(StrutsStatics.SERVLET_CONTEXT, servletContext);
        invocation = new MockActionInvocation();
        invocation.setInvocationContext(context);
    }
}
