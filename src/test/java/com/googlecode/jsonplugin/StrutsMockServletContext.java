/*
 * $Id: StrutsMockServletContext.java 471756 2006-11-06 15:01:43Z husted $
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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * StrutsMockServletContext
 */
public class StrutsMockServletContext implements ServletContext {
    String realPath;
    String servletInfo;
    @SuppressWarnings("unchecked")
    Map initParams = new HashMap();
    @SuppressWarnings("unchecked")
    Map attributes = new HashMap();
    InputStream resourceAsStream;

    @SuppressWarnings("unchecked")
    public void setInitParameter(String name, String value) {
        this.initParams.put(name, value);
    }

    public void setRealPath(String value) {
        this.realPath = value;
    }

    public String getRealPath(String string) {
        return this.realPath;
    }

    public ServletContext getContext(String s) {
        return null;
    }

    public int getMajorVersion() {
        return 0;
    }

    public int getMinorVersion() {
        return 0;
    }

    public String getMimeType(String s) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Set getResourcePaths(String s) {
        return null;
    }

    public URL getResource(String s) throws MalformedURLException {
        return null;
    }

    public InputStream getResourceAsStream(String s) {
        if (this.resourceAsStream != null)
            return this.resourceAsStream;

        return null;
    }

    public void setResourceAsStream(InputStream is) {
        this.resourceAsStream = is;
    }

    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    public RequestDispatcher getNamedDispatcher(String s) {
        return null;
    }

    public Servlet getServlet(String s) throws ServletException {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Enumeration getServlets() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Enumeration getServletNames() {
        return null;
    }

    public void log(String s) {
    }

    public void log(Exception e, String s) {
    }

    public void log(String s, Throwable throwable) {
    }

    public String getServerInfo() {
        return this.servletInfo;
    }

    public String getInitParameter(String s) {
        return (String) this.initParams.get(s);
    }

    @SuppressWarnings("unchecked")
    public Enumeration getInitParameterNames() {
        return Collections.enumeration(this.initParams.keySet());
    }

    public Object getAttribute(String s) {
        return this.attributes.get(s);
    }

    @SuppressWarnings("unchecked")
    public Enumeration getAttributeNames() {
        return Collections.enumeration(this.attributes.keySet());
    }

    @SuppressWarnings("unchecked")
    public void setAttribute(String s, Object o) {
        this.attributes.put(s, o);
    }

    public void removeAttribute(String s) {
        this.attributes.remove(s);
    }

    public String getServletContextName() {
        return null;
    }

    public void setServletInfo(String servletInfo) {
        this.servletInfo = servletInfo;
    }
}
