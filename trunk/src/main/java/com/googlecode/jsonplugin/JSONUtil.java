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
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;

/**
 *  Wrapper for JSONWriter with some utility methods.
 */
public class JSONUtil {
    final static SimpleDateFormat RFC3399_FORMAT = new SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss");
    /**
     * Serilizes a object into JSON
     * @param object to be serialized
     * @return JSON string
     * @throws JSONExeption
     */
    public static String serialize(Object object)
        throws JSONExeption {
        JSONWriter writer = new JSONWriter();

        return writer.write(object);
    }

    /**
     * Serilizes a object into JSON to the writer
     * @param writer Writer to serialize the object to
     * @param object object to be serialized
     * @throws IOException
     * @throws JSONExeption
     */
    public static void serialize(Writer writer, Object object)
        throws IOException, JSONExeption {
        writer.write(serialize(object));
    }

    /**
     * Deserilizes a object from JSON
     * @param json string in JSON
     * @return desrialized object
     * @throws JSONExeption
     * @throws JSONExeption
     */
    public static Object deserialize(String json)
        throws JSONExeption {
        JSONReader reader = new JSONReader();

        return reader.read(json);
    }

    /**
     * Deserilizes a object from JSON
     * @param json string in JSON
     * @return desrialized object
     * @throws JSONExeption
     * @throws JSONExeption
     */
    public static Object deserialize(Reader reader)
        throws JSONExeption {
        //read content
        BufferedReader bufferReader = new BufferedReader(reader);
        String line = null;
        StringBuilder buffer = new StringBuilder();

        try {
            while((line = bufferReader.readLine()) != null) {
                buffer.append(line);
            }
        } catch(IOException e) {
            throw new JSONExeption(e);
        }

        return deserialize(buffer.toString());
    }
}
