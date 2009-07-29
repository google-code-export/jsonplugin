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

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpSession;

import junit.framework.AssertionFailedError;

import com.mockobjects.servlet.MockHttpServletRequest;

/**
 * StrutsMockHttpServletRequest
 */
public class StrutsMockHttpServletRequest extends MockHttpServletRequest {
    Locale locale = Locale.US;
    @SuppressWarnings("unchecked")
    private Map attributes = new HashMap();
    @SuppressWarnings("unchecked")
    private Map parameterMap = new HashMap();
    private String context = "";
    private String pathInfo = "";
    private String queryString;
    private String requestURI;
    private String scheme;
    private String serverName;
    private int serverPort;
    private String encoding;
    private String requestDispatherString;

    @SuppressWarnings("unchecked")
    @Override
    public void setAttribute(String s, Object o) {
        this.attributes.put(s, o);
    }

    @Override
    public Object getAttribute(String s) {
        return this.attributes.get(s);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getAttributeNames() {
        Vector v = new Vector();

        v.addAll(this.attributes.keySet());

        return v.elements();
    }

    @Override
    public String getContextPath() {
        return this.context;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public void setCharacterEncoding(String s) {
        this.encoding = s;
    }

    @Override
    public String getCharacterEncoding() {
        return this.encoding;
    }

    @SuppressWarnings("unchecked")
    public void setParameterMap(Map parameterMap) {
        this.parameterMap = parameterMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map getParameterMap() {
        return this.parameterMap;
    }

    @Override
    public String getParameter(String string) {
        return (String) this.parameterMap.get(string);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getParameterNames() {
        return Collections.enumeration(this.parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String string) {
        return (String[]) this.parameterMap.get(string);
    }

    @Override
    public String getPathInfo() {
        return this.pathInfo;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    @Override
    public String getQueryString() {
        return this.queryString;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String string) {
        this.requestDispatherString = string;

        return super.getRequestDispatcher(string);
    }

    /**
     * Get's the source string that was used in the last getRequestDispatcher method call.
     */
    public String getRequestDispatherString() {
        return this.requestDispatherString;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    @Override
    public String getRequestURI() {
        return this.requestURI;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public String getScheme() {
        return this.scheme;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public int getServerPort() {
        return this.serverPort;
    }

    @Override
    public HttpSession getSession() {
        HttpSession session = null;

        try {
            session = super.getSession();
        } catch (AssertionFailedError e) {
            //ignore
        }

        return session;
    }

    public void setupGetContext(String context) {
        this.context = context;
    }

    public void setupGetPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public int getRemotePort() {
        return 0;
    }

    public String getLocalName() {
        return null;
    }

    public String getLocalAddr() {
        return null;
    }

    public int getLocalPort() {
        return 0;
    }
}
