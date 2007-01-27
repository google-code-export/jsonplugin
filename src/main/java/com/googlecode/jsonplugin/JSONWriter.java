/*
 * $Id$
 *
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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.jsonplugin.annotations.JSON;

/**
 * <p>Serializes an object into JavaScript Object Notation (JSON). If cyclic references are detected
 * they will be nulled out. </p>
 */
class JSONWriter {
    private static final Log log = LogFactory.getLog(JSONWriter.class);
    static char[] hex = "0123456789ABCDEF".toCharArray();
    private StringBuilder buf = new StringBuilder();
    private Stack stack = new Stack();
    private boolean ignoreRootParents = true;
    private Object root;

    /**
     * @param object Object to be serialized into JSON
     * @return JSON string for object
     * @throws JSONExeption
     */
    public String write(Object object) throws JSONExeption {
        buf.setLength(0);
        this.root = object;
        value(object, null);

        return buf.toString();
    }

    /**
     * Detect yclic references
     */
    private void value(Object object, Method method) throws JSONExeption {
        if(object == null) {
            add("null");

            return;
        }

        if(stack.contains(object)) {
            Class clazz = object.getClass();

            //cyclic reference
            if(clazz.isPrimitive() || clazz.equals(String.class)) {
                process(object, method);
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("Cyclic reference detected on " + object);
                }

                add("null");
            }

            return;
        }

        process(object, method);
    }

    /**
     * Serialize object into json
     */
    private void process(Object object, Method method) throws JSONExeption {
        stack.push(object);

        if(object instanceof Class) {
            string(object);
        } else if(object instanceof Boolean) {
            bool(((Boolean) object).booleanValue());
        } else if(object instanceof Number) {
            add(object);
        } else if(object instanceof String) {
            string(object);
        } else if(object instanceof Character) {
            string(object);
        } else if(object instanceof Map) {
            map((Map) object, method);
        } else if(object.getClass().isArray()) {
            array(object, method);
        } else if(object instanceof Iterable) {
            array(((Iterable) object).iterator(), method);
        } else if(object instanceof Date) {
            date((Date) object, method);
        } else if(object instanceof Calendar) {
            date(((Calendar) object).getTime(), method);
        } else {
            bean(object);
        }

        stack.pop();
    }

    /**
     * Instrospect bean and serialize its properties, ignore the ones
     * who wrap a field marked as transient
     */
    private void bean(Object object) throws JSONExeption {
        add("{");

        BeanInfo info;

        try {
            Class clazz = object.getClass();

            info = ((object == root) && ignoreRootParents) ? Introspector
                .getBeanInfo(clazz, clazz.getSuperclass()) : Introspector
                .getBeanInfo(clazz);

            PropertyDescriptor[] props = info.getPropertyDescriptors();

            for(int i = 0; i < props.length; ++i) {
                PropertyDescriptor prop = props[i];
                String name = prop.getName();
                Method accessor = prop.getReadMethod();

                if(accessor != null) {
                    Object value = accessor.invoke(object, new Object[0]);

                    JSON json = prop.getReadMethod().getAnnotation(JSON.class);
                    if(json != null) {
                        if(!json.serialize())
                            continue;
                        else if(json.name().length() > 0)
                            name = json.name();
                    }

                    //ignore "class" and others
                    if(shouldIgnoreProperty(clazz, prop)) {
                        continue;
                    }

                    add(name, value, accessor);

                    if(i < (props.length - 1)) {
                        add(',');
                    }
                }
            }
        } catch(Exception e) {
            throw new JSONExeption(e);
        }

        add("}");
    }

    /**
     * Ignore "class" field
     */
    private boolean shouldIgnoreProperty(Class clazz, PropertyDescriptor prop)
        throws SecurityException, NoSuchFieldException {
        if(prop.getName().equals("class")) {
            return true;
        }

        return false;
    }

    /**
     * Add name/value pair to buffer
     */
    private void add(String name, Object value, Method method) throws JSONExeption {
        add('"');
        add(name);
        add("\":");
        value(value, method);
    }

    /**
     * Add map to buffer
     */
    private void map(Map map, Method method) throws JSONExeption {
        add("{");

        Iterator it = map.keySet().iterator();

        while(it.hasNext()) {
            Object key = it.next();

            value(key, method);
            add(":");
            value(map.get(key), method);

            if(it.hasNext()) {
                add(",");
            }
        }

        add("}");
    }

    /**
     * Add date to buffer
     */
    private void date(Date date, Method method) {
        JSON json = method.getAnnotation(JSON.class);
        DateFormat formatter = json != null && json.format().length() > 0 ? new SimpleDateFormat(json.format()) : JSONUtil.RFC3399_FORMAT;
        string(formatter.format(date));
    }

    /**
     * Add array to buffer
     */
    private void array(Iterator it, Method method) throws JSONExeption {
        add("[");

        while(it.hasNext()) {
            value(it.next(), method);

            if(it.hasNext()) {
                add(",");
            }
        }

        add("]");
    }

    /**
     * Add array to buffer
     */
    private void array(Object object, Method method) throws JSONExeption {
        add("[");

        int length = Array.getLength(object);

        for(int i = 0; i < length; ++i) {
            value(Array.get(object, i), method);

            if(i < (length - 1)) {
                add(',');
            }
        }

        add("]");
    }

    /**
     * Add boolean to buffer
     */
    private void bool(boolean b) {
        add(b ? "true" : "false");
    }

    /**
     * escape characters
     */
    private void string(Object obj) {
        add('"');

        CharacterIterator it = new StringCharacterIterator(obj.toString());

        for(char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            if(c == '"') {
                add("\\\"");
            } else if(c == '\\') {
                add("\\\\");
            } else if(c == '/') {
                add("\\/");
            } else if(c == '\b') {
                add("\\b");
            } else if(c == '\f') {
                add("\\f");
            } else if(c == '\n') {
                add("\\n");
            } else if(c == '\r') {
                add("\\r");
            } else if(c == '\t') {
                add("\\t");
            } else if(Character.isISOControl(c)) {
                unicode(c);
            } else {
                add(c);
            }
        }

        add('"');
    }

    /**
     * Add object to buffer
     */
    private void add(Object obj) {
        buf.append(obj);
    }

    /**
     * Add char to buffer
     */
    private void add(char c) {
        buf.append(c);
    }

    /**
     * Represent as unicode
     * @param c character to be encoded
     */
    private void unicode(char c) {
        add("\\u");

        int n = c;

        for(int i = 0; i < 4; ++i) {
            int digit = (n & 0xf000) >> 12;

            add(hex[digit]);
            n <<= 4;
        }
    }
}
