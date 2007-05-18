package com.googlecode.jsonplugin;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.googlecode.jsonplugin.annotations.JSON;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;

/**
 * Populates an action from a JSON string
 *
 */
public class JSONInterceptor implements Interceptor {
    private static final Log log = LogFactory.getLog(JSONInterceptor.class);
    private boolean enableSMD = false;
    private DateFormat formatter;

    public void destroy() {
    }

    public void init() {
    }

    @SuppressWarnings("unchecked")
    public String intercept(ActionInvocation invocation) throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        String contentType = request.getHeader("content-type");

        if ((contentType != null) && contentType.equalsIgnoreCase("application/json")) {
            //load JSON object
            Object obj = JSONUtil.deserialize(request.getReader());

            if (obj instanceof Map) {
                Map json = (Map) obj;

                //populate fields
                this.populateObject(invocation.getAction(), json);
            } else {
                log.error("Unable to deserialize JSON object from request");
                throw new JSONExeption("Unable to deserialize JSON object from request");
            }
        } else if ((contentType != null)
            && contentType.equalsIgnoreCase("application/json-rpc")) {
            if (this.enableSMD) {
                //load JSON object
                Object obj = JSONUtil.deserialize(request.getReader());

                if (obj instanceof Map) {
                    Map smd = (Map) obj;
                    //invoke method
                    this.invoke(invocation.getAction(), smd);
                    return Action.NONE;
                } else {
                    log.error("SMD request was not on the right format.");
                    throw new JSONExeption("SMD request was not on the right format.");
                }
            } else {
                if (log.isDebugEnabled())
                    log
                        .debug("Request with content type of 'application/json-rpc' was received but SMD is "
                            + "not enabled for this interceptor. Set 'enableSMD' to true to enable it");
            }
        } else {
            if (log.isDebugEnabled()) {
                log
                    .debug("Content type must be 'application/json' or 'application/json-rpc'. Ignoring request with content type "
                        + contentType);
            }
        }

        return invocation.invoke();
    }

    @SuppressWarnings("unchecked")
    public void invoke(Object object, Map data) throws IllegalArgumentException,
        IllegalAccessException, InvocationTargetException, JSONExeption,
        InstantiationException, NoSuchMethodException, IntrospectionException {
        // the map is going to have: 'params', 'method' and 'id' (what is the id for?)
        Class clazz = object.getClass();

        //parameters
        List parameters = (List) data.get("params");
        int parameterCount = parameters != null ? parameters.size() : 0;

        //method
        String methodName = (String) data.get("method");
        if (methodName == null) {
            String message = "'method' is required for JSON RPC";
            log.error(message);
            throw new JSONExeption(message);
        }
        Method method = this.getMethod(clazz, methodName, parameterCount);
        if (method == null) {
            String message = "Method " + methodName
                + " could not be found in action class.";
            log.error(message);
            throw new JSONExeption(message);
        }

        if (parameterCount > 0) {
            Class[] parameterTypes = method.getParameterTypes();
            List invocationParameters = new ArrayList();

            if (parameterTypes.length != parameterCount) {
                //size mismatch
                String message = "Parameter count in request, " + parameterCount
                    + " do not match expected parameter count for " + methodName + ", "
                    + parameterTypes.length;
                log.error(message);
                throw new JSONExeption(message);
            }

            //convert parameters
            for (int i = 0; i < parameters.size(); i++) {
                Object parameter = parameters.get(i);
                Class paramType = parameterTypes[i];

                Object converted = this.convert(paramType, parameter, method);
                invocationParameters.add(converted);
            }

            method.invoke(object, invocationParameters.toArray());
        } else {
            method.invoke(object, new Object[0]);
        }
    }

    @SuppressWarnings("unchecked")
    private Method getMethod(Class clazz, String name, int parameterCount) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(name)
                && (method.getParameterTypes().length == parameterCount))
                return method;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void populateObject(final Object object, final Map elements)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException,
        IntrospectionException, IllegalArgumentException, JSONExeption,
        InstantiationException {
        Class clazz = object.getClass();

        BeanInfo info = Introspector.getBeanInfo(clazz);
        PropertyDescriptor[] props = info.getPropertyDescriptors();

        //iterate over class fields
        for (int i = 0; i < props.length; ++i) {
            PropertyDescriptor prop = props[i];
            String name = prop.getName();

            if (elements.containsKey(name)) {
                Object value = elements.get(name);
                Method method = prop.getWriteMethod();

                JSON json = prop.getWriteMethod().getAnnotation(JSON.class);
                if ((json != null) && !json.deserialize()) {
                    continue;
                }

                //use only public setters
                if ((method != null) && Modifier.isPublic(method.getModifiers())) {
                    Class[] paramTypes = method.getParameterTypes();

                    if (paramTypes.length == 1) {
                        Class paramType = paramTypes[0];
                        Object convertedValue = this.convert(paramType, value, method);
                        method.invoke(object, new Object[] { convertedValue });
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object convert(Class clazz, Object value, Method method)
        throws IllegalArgumentException, JSONExeption, IllegalAccessException,
        InvocationTargetException, InstantiationException, NoSuchMethodException,
        IntrospectionException {
        if (clazz.isPrimitive() || clazz.equals(String.class) || clazz.equals(Date.class))
            return this.convertPrimitive(clazz, value, method);
        else if (List.class.equals(clazz) || Map.class.equals(clazz))
            return value;
        else if (clazz.isArray())
            return this.convertToArray(value, method, value);
        else if (value instanceof Map) {
            //nested field
            Object convertedValue = clazz.newInstance();

            this.populateObject(convertedValue, (Map) value);
            return convertedValue;
        } else
            throw new JSONExeption("Incompatible types for property " + method.getName());
    }

    @SuppressWarnings("unchecked")
    private Object convertToArray(Object target, Method accessor, Object value)
        throws JSONExeption, IllegalArgumentException, IllegalAccessException,
        InvocationTargetException, InstantiationException, NoSuchMethodException,
        IntrospectionException {
        Class arrayType = accessor.getParameterTypes()[0].getComponentType();

        if (value instanceof List) {
            List values = (List) value;
            Object newArray = Array.newInstance(arrayType, values.size());

            //create an object fr each element
            for (int j = 0; j < values.size(); j++) {
                Object listValue = values.get(j);
                
                if (arrayType.equals(Object.class)) {
                    //Object[]
                    Array.set(newArray, j, listValue);
                } else if (arrayType.isPrimitive() || arrayType.equals(String.class)
                    || arrayType.equals(Date.class)) {
                    //primitive array
                    Array.set(newArray, j, this.convertPrimitive(arrayType, listValue,
                        accessor));
                } else {
                    //array of other class
                    Object newObject = arrayType.newInstance();

                    if (listValue instanceof Map) {
                        this.populateObject(newObject, (Map) listValue);
                        Array.set(newArray, j, newObject);
                    } else
                        throw new JSONExeption("Incompatible types for property "
                            + accessor.getName());
                }
            }

            return newArray;
        } else
            throw new JSONExeption("Incompatible types for property "
                + accessor.getName());
    }

    /**
     * Converts numbers to the desired class, if possible
     * @throws JSONExeption
     */
    @SuppressWarnings("unchecked")
    private Object convertPrimitive(Class clazz, Object value, Method method)
        throws JSONExeption {
        if (value instanceof Number) {
            Number number = (Number) value;

            if (Short.TYPE.equals(clazz))
                return number.shortValue();
            else if (Byte.TYPE.equals(clazz))
                return number.byteValue();
            else if (Integer.TYPE.equals(clazz))
                return number.intValue();
            else if (Long.TYPE.equals(clazz))
                return number.longValue();
            else if (Float.TYPE.equals(clazz))
                return number.floatValue();
            else if (Double.TYPE.equals(clazz))
                return number.doubleValue();
        } else if (clazz.equals(Date.class)) {
            try {
                JSON json = method.getAnnotation(JSON.class);

                if (this.formatter == null)
                    this.formatter = new SimpleDateFormat(JSONUtil.RFC3339_FORMAT);

                DateFormat formatter = (json != null) && (json.format().length() > 0) ? new SimpleDateFormat(
                    json.format())
                    : this.formatter;
                return formatter.parse((String) value);
            } catch (ParseException e) {
                log.error(e);
                throw new JSONExeption("Unable to parse date from: " + value);
            }
        } else if (value instanceof String) {
            if (Boolean.TYPE.equals(clazz))
                return Boolean.valueOf((String) value);
            else if (Character.TYPE.equals(clazz))
                return ((String) value).charAt(0);
        }

        return value;
    }

    public boolean isEnableSMD() {
        return this.enableSMD;
    }

    public void setEnableSMD(boolean enableSMD) {
        this.enableSMD = enableSMD;
    }
}
