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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  Wrapper for JSONWriter with some utility methods.
 */
public class JSONUtil {
    final static String RFC3339_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final Log log = LogFactory.getLog(JSONUtil.class);

    /**
     * Serilizes an object into JSON.
     * @param object to be serialized
     * @return JSON string
     * @throws JSONException
     */
    public static String serialize(Object object) throws JSONException {
        JSONWriter writer = new JSONWriter();

        return writer.write(object);
    }

    /**
     * Serilizes an object into JSON, excluding any properties matching
     * any of the regular expressions in the given collection.
     * @param object to be serialized
     * @param Patterns matching properties to ignore
     * @return JSON string
     * @throws JSONException
     */
    public static String serialize(Object object, Collection<Pattern> ignoreProperties,
        boolean ignoreHierarchy) throws JSONException {
        JSONWriter writer = new JSONWriter();
        writer.setIgnoreHierarchy(ignoreHierarchy);
        return writer.write(object, ignoreProperties);
    }

    /**
     * Serilizes an object into JSON to the given writer.
     * @param writer Writer to serialize the object to
     * @param object object to be serialized
     * @param Patterns matching properties to ignore
     * @throws IOException
     * @throws JSONException
     */
    public static void serialize(Writer writer, Object object) throws IOException,
        JSONException {
        writer.write(serialize(object));
    }

    /**
     * Serilizes an object into JSON to the given writer, excluding any properties matching
     * any of the regular expressions in the given collection.
     * @param writer Writer to serialize the object to
     * @param object object to be serialized
     * @param Patterns matching properties to ignore
     * @throws IOException
     * @throws JSONException
     */
    public static void serialize(Writer writer, Object object,
        Collection<Pattern> ignoreProperties) throws IOException, JSONException {
        writer.write(serialize(object, ignoreProperties, true));
    }

    /**
     * Deserilizes a object from JSON
     * @param json string in JSON
     * @return desrialized object
     * @throws JSONException
     * @throws JSONException
     */
    public static Object deserialize(String json) throws JSONException {
        JSONReader reader = new JSONReader();

        return reader.read(json);
    }

    /**
     * Deserilizes a object from JSON
     * @param json string in JSON
     * @return desrialized object
     * @throws JSONException
     * @throws JSONException
     */
    public static Object deserialize(Reader reader) throws JSONException {
        //read content
        BufferedReader bufferReader = new BufferedReader(reader);
        String line = null;
        StringBuilder buffer = new StringBuilder();

        try {
            while ((line = bufferReader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            throw new JSONException(e);
        }

        return deserialize(buffer.toString());
    }

    static void writeJSONToResponse(HttpServletResponse response, String encoding,
        boolean wrapWithComments, String serializedJSON, boolean smd) throws IOException {
        String json = serializedJSON == null ? "" : serializedJSON;
        if (wrapWithComments) {
            StringBuilder sb = new StringBuilder("/* ");
            sb.append(json);
            sb.append(" */");
            json = sb.toString();
        }
        if (log.isDebugEnabled()) {
            log.debug("[JSON]" + json);
        }

        response.setContentLength(json.getBytes(encoding).length);
        response.setContentType((smd ? "application/json-rpc;charset="
            : "application/json;charset=") +
            encoding);

        PrintWriter out = response.getWriter();
        out.print(json);
    }

    public static List<String> asList(String commaDelim) {
        if ((commaDelim == null) || (commaDelim.trim().length() == 0))
            return null;
        List<String> list = new ArrayList<String>();
        String[] split = commaDelim.split(",");
        for (int i = 0; i < split.length; i++) {
            String trimmed = split[i].trim();
            if (trimmed.length() > 0) {
                list.add(trimmed);
            }
        }
        return list;
    }
}
