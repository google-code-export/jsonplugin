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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.googlecode.jsonplugin.annotations.JSON;

/**
 * Isolate the process of populating JSON objects from the Interceptor class itself.
 */
public class JSONPopulator {

    private static final Log log = LogFactory.getLog(JSONPopulator.class);

    private String dateFormat = JSONUtil.RFC3339_FORMAT;

    public JSONPopulator() {
    }

    public JSONPopulator(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @SuppressWarnings("unchecked")
    public void populateObject(final Object object, final Map elements)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException,
        IntrospectionException, IllegalArgumentException, JSONException,
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
    public Object convert(Class clazz, Object value, Method method)
        throws IllegalArgumentException, JSONException, IllegalAccessException,
        InvocationTargetException, InstantiationException, NoSuchMethodException,
        IntrospectionException {
        if (isJSONPrimitive(clazz))
            return convertPrimitive(clazz, value, method);
        else if (List.class.equals(clazz) || Map.class.equals(clazz))
            return value;
        else if (clazz.isArray())
            return convertToArray(value, method, value);
        else if (value instanceof Map) {
            //nested field
            Object convertedValue = clazz.newInstance();

            this.populateObject(convertedValue, (Map) value);
            return convertedValue;
        } else
            throw new JSONException("Incompatible types for property " + method.getName());
    }

    private static boolean isJSONPrimitive(Class clazz) {
        return clazz.isPrimitive() || clazz.equals(String.class) ||
            clazz.equals(Date.class) || clazz.equals(Boolean.class) ||
            clazz.equals(Byte.class) || clazz.equals(Character.class) ||
            clazz.equals(Double.class) || clazz.equals(Float.class) ||
            clazz.equals(Integer.class) || clazz.equals(Long.class) ||
            clazz.equals(Short.class) || clazz.isEnum();
    }

    @SuppressWarnings("unchecked")
    private Object convertToArray(Object target, Method accessor, Object value)
        throws JSONException, IllegalArgumentException, IllegalAccessException,
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
                } else if (isJSONPrimitive(arrayType)) {
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
                        throw new JSONException("Incompatible types for property " +
                            accessor.getName());
                }
            }

            return newArray;
        } else
            throw new JSONException("Incompatible types for property " +
                accessor.getName());
    }

    /**
     * Converts numbers to the desired class, if possible
     * @throws JSONException
     */
    @SuppressWarnings("unchecked")
    private Object convertPrimitive(Class clazz, Object value, Method method)
        throws JSONException {
        if (value instanceof Number) {
            Number number = (Number) value;

            if (Short.TYPE.equals(clazz))
                return number.shortValue();
            else if (Short.class.equals(clazz))
                return new Short(number.shortValue());
            else if (Byte.TYPE.equals(clazz))
                return number.byteValue();
            else if (Byte.class.equals(clazz))
                return new Byte(number.byteValue());
            else if (Integer.TYPE.equals(clazz))
                return number.intValue();
            else if (Integer.class.equals(clazz))
                return new Integer(number.intValue());
            else if (Long.TYPE.equals(clazz))
                return number.longValue();
            else if (Long.class.equals(clazz))
                return new Long(number.longValue());
            else if (Float.TYPE.equals(clazz))
                return number.floatValue();
            else if (Float.class.equals(clazz))
                return new Float(number.floatValue());
            else if (Double.TYPE.equals(clazz))
                return number.doubleValue();
            else if (Double.class.equals(clazz))
                return new Double(number.doubleValue());
        } else if (clazz.equals(Date.class)) {
            try {
                JSON json = method.getAnnotation(JSON.class);

                DateFormat formatter = (json != null) && (json.format().length() > 0) ? new SimpleDateFormat(
                    json.format())
                    : new SimpleDateFormat(this.dateFormat);
                return formatter.parse((String) value);
            } catch (ParseException e) {
                log.error(e);
                throw new JSONException("Unable to parse date from: " + value);
            }
        } else if (clazz.isEnum()) {
            String sValue = (String) value;
			return Enum.valueOf(clazz, sValue);
		} else if (value instanceof String) {
            String sValue = (String) value;
            if (Boolean.TYPE.equals(clazz))
                return Boolean.valueOf(sValue);
            else if (Boolean.class.equals(clazz))
                return new Boolean(sValue);
            else if (Character.TYPE.equals(clazz) || Character.class.equals(clazz)) {
                char charValue = 0;
                if (sValue.length() > 0) {
                    charValue = sValue.charAt(0);
                }
                if (Character.TYPE.equals(clazz))
                    return charValue;
                else
                    return new Character(charValue);
            }
        }

        return value;
    }

}
