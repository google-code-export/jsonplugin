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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.jsonplugin.annotations.JSON;

/**
 * <p>Serializes an object into JavaScript Object Notation (JSON). If cyclic references are detected
 * they will be nulled out. </p>
 */
@SuppressWarnings("unchecked")
class JSONWriter {
    private static final Log log = LogFactory.getLog(JSONWriter.class);
    static char[] hex = "0123456789ABCDEF".toCharArray();
    private StringBuilder buf = new StringBuilder();
    private Stack stack = new Stack();
    private boolean ignoreHierarchy = true;
    private Object root;
    private boolean buildExpr = true;
    private String exprStack = "";
    private Collection<Pattern> excludeProperties;
    private DateFormat formatter;

    /**
     * @param object Object to be serialized into JSON
     * @return JSON string for object
     * @throws JSONException
     */
    public String write(Object object) throws JSONException {
        return this.write(object, null);
    }

    /**
     * @param object Object to be serialized into JSON
     * @return JSON string for object
     * @throws JSONException
     */
    public String write(Object object, Collection<Pattern> excludeProperties)
        throws JSONException {
        this.buf.setLength(0);
        this.root = object;
        this.exprStack = "";
        this.buildExpr = (excludeProperties != null) && !excludeProperties.isEmpty();
        this.excludeProperties = excludeProperties;
        this.value(object, null);

        return this.buf.toString();
    }

    /**
     * Detect yclic references
     */
    private void value(Object object, Method method) throws JSONException {
        if (object == null) {
            this.add("null");

            return;
        }

        if (this.stack.contains(object)) {
            Class clazz = object.getClass();

            //cyclic reference
            if (clazz.isPrimitive() || clazz.equals(String.class)) {
                this.process(object, method);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Cyclic reference detected on " + object);
                }

                this.add("null");
            }

            return;
        }

        this.process(object, method);
    }

    /**
     * Serialize object into json
     */
    private void process(Object object, Method method) throws JSONException {
        this.stack.push(object);

        if (object instanceof Class) {
            this.string(object);
        } else if (object instanceof Boolean) {
            this.bool(((Boolean) object).booleanValue());
        } else if (object instanceof Number) {
            this.add(object);
        } else if (object instanceof String) {
            this.string(object);
        } else if (object instanceof Character) {
            this.string(object);
        } else if (object instanceof Map) {
            this.map((Map) object, method);
        } else if (object.getClass().isArray()) {
            this.array(object, method);
        } else if (object instanceof Iterable) {
            this.array(((Iterable) object).iterator(), method);
        } else if (object instanceof Date) {
            this.date((Date) object, method);
        } else if (object instanceof Calendar) {
            this.date(((Calendar) object).getTime(), method);
        } else {
            this.bean(object);
        }

        this.stack.pop();
    }

    /**
     * Instrospect bean and serialize its properties
     */
    private void bean(Object object) throws JSONException {
        this.add("{");

        BeanInfo info;

        try {
            Class clazz = object.getClass();

            info = ((object == this.root) && this.ignoreHierarchy) ? Introspector
                .getBeanInfo(clazz, clazz.getSuperclass()) : Introspector
                .getBeanInfo(clazz);

            PropertyDescriptor[] props = info.getPropertyDescriptors();

            boolean hasData = false;
            for (int i = 0; i < props.length; ++i) {
                PropertyDescriptor prop = props[i];
                String name = prop.getName();
                Method accessor = prop.getReadMethod();

                if (accessor != null) {
                    JSON json = prop.getReadMethod().getAnnotation(JSON.class);
                    if (json != null) {
                        if (!json.serialize())
                            continue;
                        else if (json.name().length() > 0)
                            name = json.name();
                    }

                    //ignore "class" and others
                    if (this.shouldExcludeProperty(clazz, prop)) {
                        continue;
                    }
                    String expr = null;
                    if (this.buildExpr) {
                        expr = this.expandExpr(name);
                        if (this.shouldExcludeProperty(expr)) {
                            continue;
                        }
                        expr = this.setExprStack(expr);
                    }
                    if (hasData) {
                        this.add(',');
                    }
                    hasData = true;
                    
                    Object value = accessor.invoke(object, new Object[0]);
                    this.add(name, value, accessor);
                    if (this.buildExpr) {
                        this.setExprStack(expr);
                    }
                }
            }
        } catch (Exception e) {
            throw new JSONException(e);
        }

        this.add("}");
    }

    /**
     * Ignore "class" field
     */
    private boolean shouldExcludeProperty(Class clazz, PropertyDescriptor prop)
        throws SecurityException, NoSuchFieldException {
        if (prop.getName().equals("class"))
            return true;

        return false;
    }

    private String expandExpr(int i) {
        return this.exprStack + "[" + i + "]";
    }

    private String expandExpr(String property) {
        if (this.exprStack.length() == 0)
            return property;
        return this.exprStack + "." + property;
    }

    private String setExprStack(String expr) {
        String s = this.exprStack;
        this.exprStack = expr;
        return s;
    }

    private boolean shouldExcludeProperty(String expr) {
        for (Pattern pattern : this.excludeProperties) {
            if (pattern.matcher(expr).matches()) {
                if (log.isDebugEnabled())
                    log.debug("Ignoring property " + expr);
                return true;
            }
        }
        return false;
    }

    /**
     * Add name/value pair to buffer
     */
    private void add(String name, Object value, Method method) throws JSONException {
        this.add('"');
        this.add(name);
        this.add("\":");
        this.value(value, method);
    }

    /**
     * Add map to buffer
     */
    private void map(Map map, Method method) throws JSONException {
        this.add("{");

        Iterator it = map.entrySet().iterator();

        boolean hasData = false;
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Object key = entry.getKey();
            String expr = null;
            if (this.buildExpr) {
                if (key == null) {
                    log
                        .error("Cannot build expression for null key in "
                            + this.exprStack);
                    continue;
                } else {
                    expr = this.expandExpr(key.toString());
                    if (this.shouldExcludeProperty(expr)) {
                        continue;
                    }
                    expr = this.setExprStack(expr);
                }
            }
            if (hasData) {
                this.add(',');
            }
            hasData = true;
            this.value(key, method);
            this.add(":");
            this.value(entry.getValue(), method);
            if (this.buildExpr) {
                this.setExprStack(expr);
            }
        }

        this.add("}");
    }

    /**
     * Add date to buffer
     */
    private void date(Date date, Method method) {
        JSON json = method.getAnnotation(JSON.class);
        if (this.formatter == null)
            this.formatter = new SimpleDateFormat(JSONUtil.RFC3339_FORMAT);

        DateFormat formatter = (json != null) && (json.format().length() > 0) ? new SimpleDateFormat(
            json.format())
            : this.formatter;
        this.string(formatter.format(date));
    }

    /**
     * Add array to buffer
     */
    private void array(Iterator it, Method method) throws JSONException {
        this.add("[");

        boolean hasData = false;
        for (int i = 0; it.hasNext(); i++) {
            String expr = null;
            if (this.buildExpr) {
                expr = this.expandExpr(i);
                if (this.shouldExcludeProperty(expr)) {
                    continue;
                }
                expr = this.setExprStack(expr);
            }
            if (hasData) {
                this.add(',');
            }
            hasData = true;
            this.value(it.next(), method);
            if (this.buildExpr) {
                this.setExprStack(expr);
            }
        }

        this.add("]");
    }

    /**
     * Add array to buffer
     */
    private void array(Object object, Method method) throws JSONException {
        this.add("[");

        int length = Array.getLength(object);

        boolean hasData = false;
        for (int i = 0; i < length; ++i) {
            String expr = null;
            if (this.buildExpr) {
                expr = this.expandExpr(i);
                if (this.shouldExcludeProperty(expr)) {
                    continue;
                }
                expr = this.setExprStack(expr);
            }
            if (hasData) {
                this.add(',');
            }
            hasData = true;
            this.value(Array.get(object, i), method);
            if (this.buildExpr) {
                this.setExprStack(expr);
            }
        }

        this.add("]");
    }

    /**
     * Add boolean to buffer
     */
    private void bool(boolean b) {
        this.add(b ? "true" : "false");
    }

    /**
     * escape characters
     */
    private void string(Object obj) {
        this.add('"');

        CharacterIterator it = new StringCharacterIterator(obj.toString());

        for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            if (c == '"') {
                this.add("\\\"");
            } else if (c == '\\') {
                this.add("\\\\");
            } else if (c == '/') {
                this.add("\\/");
            } else if (c == '\b') {
                this.add("\\b");
            } else if (c == '\f') {
                this.add("\\f");
            } else if (c == '\n') {
                this.add("\\n");
            } else if (c == '\r') {
                this.add("\\r");
            } else if (c == '\t') {
                this.add("\\t");
            } else if (Character.isISOControl(c)) {
                this.unicode(c);
            } else {
                this.add(c);
            }
        }

        this.add('"');
    }

    /**
     * Add object to buffer
     */
    private void add(Object obj) {
        this.buf.append(obj);
    }

    /**
     * Add char to buffer
     */
    private void add(char c) {
        this.buf.append(c);
    }

    /**
     * Represent as unicode
     * @param c character to be encoded
     */
    private void unicode(char c) {
        this.add("\\u");

        int n = c;

        for (int i = 0; i < 4; ++i) {
            int digit = (n & 0xf000) >> 12;

            this.add(hex[digit]);
            n <<= 4;
        }
    }

    public void setIgnoreHierarchy(boolean ignoreHierarchy) {
        this.ignoreHierarchy = ignoreHierarchy;
    }
}
