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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.jsonplugin.annotations.SMDMethod;

/**
 *  Wrapper for JSONWriter with some utility methods.
 */
public class JSONUtil {
    final static String RFC3339_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final Log log = LogFactory.getLog(JSONUtil.class);

    /**
     * Serializes an object into JSON.
     * @param object to be serialized
     * @return JSON string
     * @throws JSONException
     */
    public static String serialize(Object object) throws JSONException {
        JSONWriter writer = new JSONWriter();

        return writer.write(object);
    }

    /**
     * Serializes an object into JSON, excluding any properties matching
     * any of the regular expressions in the given collection.
     * @param object to be serialized
     * @param excludeProperties Patterns matching properties to exclude
     * @param ignoreHierarchy whether to ignore properties defined on base classes of the root object
     * @return JSON string
     * @throws JSONException
     */
    public static String serialize(Object object, Collection<Pattern> excludeProperties, Collection<Pattern> includeProperties,
        boolean ignoreHierarchy) throws JSONException {
        JSONWriter writer = new JSONWriter();
        writer.setIgnoreHierarchy(ignoreHierarchy);
        return writer.write(object, excludeProperties, includeProperties);
    }

    /**
     * Serializes an object into JSON, excluding any properties matching
     * any of the regular expressions in the given collection.
     * @param object to be serialized
     * @param excludeProperties Patterns matching properties to exclude
     * @param ignoreHierarchy whether to ignore properties defined on base classes of the root object
     * @param enumAsBean whether to serialized enums a Bean or name=value pair
     * @return JSON string
     * @throws JSONException
     */
    public static String serialize(Object object, Collection<Pattern> excludeProperties,
    		Collection<Pattern> includeProperties, boolean ignoreHierarchy, boolean enumAsBean) throws JSONException {
        JSONWriter writer = new JSONWriter();
        writer.setIgnoreHierarchy(ignoreHierarchy);
        writer.setEnumAsBean(enumAsBean);
        return writer.write(object, excludeProperties, includeProperties);
    }

    /**
     * Serializes an object into JSON to the given writer.
     * @param writer Writer to serialize the object to
     * @param object object to be serialized
     * @throws IOException
     * @throws JSONException
     */
    public static void serialize(Writer writer, Object object) throws IOException,
        JSONException {
        writer.write(serialize(object));
    }

    /**
     * Serializes an object into JSON to the given writer, excluding any properties matching
     * any of the regular expressions in the given collection.
     * @param writer Writer to serialize the object to
     * @param object object to be serialized
     * @param excludeProperties Patterns matching properties to ignore
     * @throws IOException
     * @throws JSONException
     */
    public static void serialize(Writer writer, Object object,
    		 Collection<Pattern> excludeProperties, Collection<Pattern> includeProperties) throws IOException, JSONException {
    	 writer.write(serialize(object, excludeProperties, includeProperties, true));
    }

    /**
     * Deserializes a object from JSON
     * @param json string in JSON
     * @return desrialized object
     * @throws JSONException
     */
    public static Object deserialize(String json) throws JSONException {
        JSONReader reader = new JSONReader();
        return reader.read(json);
    }

    /**
     * Deserializes a object from JSON
     * @param reader Reader to read a JSON string from
     * @return deserialized object
     * @throws JSONException when IOException happens
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

    public static void writeJSONToResponse(HttpServletResponse response, String encoding,
                                           boolean wrapWithComments, String serializedJSON,
                                           boolean smd, boolean gzip, boolean noCache) throws IOException {
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

        response.setContentType((smd ? "application/json-rpc;charset="
            : "application/json;charset=") +
            encoding);

        if (noCache) {
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Expires", "0");
            response.setHeader("Pragma", "No-cache");
        }

        if (gzip) {
			response.addHeader("Content-Encoding", "gzip");
			GZIPOutputStream out = null;
			InputStream in = null;
			try {
				out = new GZIPOutputStream(response.getOutputStream());
				in = new ByteArrayInputStream(json.getBytes());
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			} finally {
				if (in != null)
					in.close();
				if (out != null) {
					out.finish();
					out.close();
				}
			}

		} else {
			response.setContentLength(json.getBytes(encoding).length);
			PrintWriter out = response.getWriter();
			out.print(json);
		}
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

    /**
     *  List visible methods carrying the @SMDMethod annotation
     *
     * @param ignoreInterfaces if true, only the methods of the class are examined.  If false, annotations on
     *  every interfaces' methods are examined.
     **/
    @SuppressWarnings("unchecked")
    public static Method[] listSMDMethods(Class clazz, boolean ignoreInterfaces) {
        final List<Method> methods = new LinkedList<Method>();
        if (ignoreInterfaces) {
            for (Method method : clazz.getMethods()) {
                SMDMethod smdMethodAnnotation = method.getAnnotation(SMDMethod.class);
                if (smdMethodAnnotation != null) {
                    methods.add(method);
                }
            }
        } else {
            // recurse the entire superclass/interface hierarchy and add in order encountered
            JSONUtil.visitInterfaces(clazz, new JSONUtil.ClassVisitor() {
                public boolean visit(Class aClass) {
                    for (Method method : aClass.getMethods()) {
                        SMDMethod smdMethodAnnotation = method
                            .getAnnotation(SMDMethod.class);
                        if (smdMethodAnnotation != null &&
                            !methods.contains(method)) {
                            methods.add(method);
                        }
                    }
                    return true;
                }
            });
        }

        Method[] methodResult = new Method[methods.size()];
        return methods.toArray(methodResult);
    }

    /**
     * Realizes the visit(Class) method called by vistInterfaces for all encountered classes/interfaces
     */
    public static interface ClassVisitor {

        /**
        * Called when a new interface/class is encountered
        *
        * @param       aClass the encountered class/interface
        * @return true if the recursion should continue, false to stop recursion immediately
        * */
        @SuppressWarnings("unchecked")
		boolean visit(Class aClass);
    }

    /**
     * Visit all the interfaces realized by the specified object, its superclasses and its interfaces
     *
     * Visitation is performed in the following order:
     *   aClass
     *   aClass' interfaces
     *   the interface's superclasses (interfaces)
     *   aClass' superclass
     *   superclass' interfaces
     *   superclass' interface's superclasses (interfaces)
     *   super-superclass
     *   and so on
     *
     * The Object base class is base excluded.
     * Classes/interfaces are only visited once each
     *
     * @param aClass    the class to start recursing upwards from
     * @param visitor   this vistor is called for each class/interface encountered
     * @return true if all classes/interfaces were visited, false if it was exited early as specified by a ClassVisitor result
     */
    @SuppressWarnings("unchecked")
	public static boolean visitInterfaces(Class aClass, ClassVisitor visitor) {
        List<Class> classesVisited = new LinkedList<Class>();
        return visitUniqueInterfaces(aClass, visitor, classesVisited);
    }

    /**
     * Recursive method to visit all the interfaces of a class (and its superclasses and super-interfaces) if they
     * haven't already been visited.
     *
     * Always visits itself if it hasn't already been visited
     *
     * @param thisClass         the current class to visit (if not already done so)
     * @param classesVisited    classes already visited
     * @param visitor           this vistor is called for each class/interface encountered
     * @return true if recursion can continue, false if it should be aborted
     */
    private static boolean visitUniqueInterfaces(Class thisClass, ClassVisitor visitor,
        List<Class> classesVisited) {
        boolean okayToContinue = true;

        if (!classesVisited.contains(thisClass)) {
            classesVisited.add(thisClass);
            okayToContinue = visitor.visit(thisClass);

            if (okayToContinue) {
                Class[] interfaces = thisClass.getInterfaces();
                int index = 0;
                while ((index < interfaces.length) && (okayToContinue)) {
                    okayToContinue = visitUniqueInterfaces(interfaces[index++], visitor,
                        classesVisited);
                }

                if (okayToContinue) {
                    Class superClass = thisClass.getSuperclass();
                    if ((superClass != null) && (!Object.class.equals(superClass))) {
                        okayToContinue = visitUniqueInterfaces(superClass, visitor,
                            classesVisited);
                    }
                }
            }
        }
        return okayToContinue;
    }

    public static boolean isGzipInRequest(HttpServletRequest request) {
        String header = request.getHeader("Accept-Encoding");
        return header != null && header.indexOf("gzip") >= 0;
    }
}
