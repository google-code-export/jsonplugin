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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.googlecode.jsonplugin.annotations.JSON;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;

/**
 * Populates an action from a JSON string
 *
 */
public class JSONInterceptor implements Interceptor {
    private static final Log log = LogFactory.getLog(JSONInterceptor.class);

    public void destroy() {
    }

    public void init() {
    }

    public String intercept(ActionInvocation invocation) throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        String contentType = request.getHeader("content-type");

        if(contentType.equalsIgnoreCase("application/json")) {
            //load JSON object
            Object obj = JSONUtil.deserialize(request.getReader());

            if(obj instanceof Map) {
                Map json = (Map) obj;

                //populate fields
                populateObject(invocation.getAction(), json);
            } else {
                log.error("Unable to derialize JSON object from request");
                throw new JSONExeption("Unable to derialize JSON object from request");
            }
        } else {
            if(log.isDebugEnabled()) {
                log
                    .debug("Content type must be 'application/json'. Ignoring request with content type "
                        + contentType);
            }
        }

        return invocation.invoke();
    }

    public void populateObject(final Object object, final Map elements)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException,
        IntrospectionException, IllegalArgumentException, JSONExeption,
        InstantiationException {
        Class clazz = object.getClass();

        BeanInfo info = Introspector.getBeanInfo(clazz);
        PropertyDescriptor[] props = info.getPropertyDescriptors();

        //iterate over class fields
        for(int i = 0; i < props.length; ++i) {
            PropertyDescriptor prop = props[i];
            String name = prop.getName();

            if(elements.containsKey(name)) {
                Object value = elements.get(name);
                Method accessor = prop.getWriteMethod();

                JSON json = prop.getWriteMethod().getAnnotation(JSON.class);
                if(json != null && !json.deserialize()) {
                    continue;
                }

                //use only public setters
                if((accessor != null) && Modifier.isPublic(accessor.getModifiers())) {
                    Class[] paramTypes = accessor.getParameterTypes();

                    if(paramTypes.length == 1) {
                        Class paramType = paramTypes[0];

                        if(paramType.isPrimitive() || paramType.equals(String.class)
                            || paramType.equals(Date.class)) {
                            setPrimitive(object, accessor, paramType, value);
                        } else if(List.class.equals(paramType)
                            || Map.class.equals(paramType)) {
                            accessor.invoke(object, new Object[] { value });
                        } else if(paramType.isArray()) {
                            setArray(object, accessor, value);
                        } else if(value instanceof Map) {
                            //nested field
                            Object newInstance = paramType.newInstance();

                            populateObject(newInstance, (Map) value);
                            accessor.invoke(object, new Object[] { newInstance });
                        } else {
                            throw new JSONExeption("Incompatible types for property "
                                + name);
                        }
                    }
                }
            }
        }
    }

    private void setArray(Object target, Method accessor, Object value)
        throws JSONExeption, IllegalArgumentException, IllegalAccessException,
        InvocationTargetException, InstantiationException, NoSuchMethodException,
        IntrospectionException {
        Class arrayType = accessor.getParameterTypes()[0].getComponentType();

        if(value instanceof List) {
            List values = (List) value;
            Object newArray = Array.newInstance(arrayType, values.size());

            //create an object fr each element
            for(int j = 0; j < values.size(); j++) {
                Object listValue = values.get(j);
                Class listValueType = listValue.getClass();

                if(arrayType.equals(Object.class)) {
                    //Object[]
                    Array.set(newArray, j, listValue);
                } else if(arrayType.isPrimitive() || arrayType.equals(String.class)
                    || arrayType.equals(Date.class)) {
                    //primitive array
                    Array.set(newArray, j, convertPrimitive(arrayType, listValue,
                        accessor));
                } else {
                    //array of other class
                    Object newObject = arrayType.newInstance();

                    if(listValue instanceof Map) {
                        populateObject(newObject, (Map) listValue);
                        Array.set(newArray, j, newObject);
                    } else {
                        throw new JSONExeption("Incompatible types for property "
                            + accessor.getName());
                    }
                }
            }

            accessor.invoke(target, new Object[] { newArray });
        } else {
            throw new JSONExeption("Incompatible types for property "
                + accessor.getName());
        }
    }

    private void setPrimitive(Object object, Method accessor, Class clazz, Object value)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException, JSONExeption {
        if(value != null) {
            accessor.invoke(object, new Object[] { convertPrimitive(clazz, value,
                accessor) });
        }
    }

    /**
     * Converts numbers to the desired class, if possible
     * @throws JSONExeption
     */
    private Object convertPrimitive(Class clazz, Object value, Method method)
        throws JSONExeption {
        if(value instanceof Number) {
            Number number = (Number) value;

            if(Short.TYPE.equals(clazz)) {
                return number.shortValue();
            } else if(Byte.TYPE.equals(clazz)) {
                return number.byteValue();
            } else if(Integer.TYPE.equals(clazz)) {
                return number.intValue();
            } else if(Long.TYPE.equals(clazz)) {
                return number.longValue();
            } else if(Float.TYPE.equals(clazz)) {
                return number.floatValue();
            } else if(Double.TYPE.equals(clazz)) {
                return number.doubleValue();
            }
        } else if(clazz.equals(Date.class)) {
            try {
                JSON json = method.getAnnotation(JSON.class);
                DateFormat formatter = json != null && json.format().length() > 0 ? new SimpleDateFormat(
                    json.format())
                    : JSONUtil.RFC3399_FORMAT;
                return formatter.parse((String) value);
            } catch(ParseException e) {
                log.error(e);
                throw new JSONExeption("Unable to parse date from: " + value);
            }
        } else if(value instanceof String) {
            if(Boolean.TYPE.equals(clazz)) {
                return Boolean.valueOf((String) value);
            } else if(Character.TYPE.equals(clazz)) {
                return ((String) value).charAt(0);
            }
        }

        return value;
    }
}
