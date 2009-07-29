package com.googlecode.jsonplugin;

import com.googlecode.jsonplugin.annotations.SMDMethod;
import com.googlecode.jsonplugin.rpc.RPCError;
import com.googlecode.jsonplugin.rpc.RPCErrorCode;
import com.googlecode.jsonplugin.rpc.RPCResponse;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.interceptor.Interceptor;
import com.opensymphony.xwork2.util.ValueStack;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Populates an action from a JSON string
 */
public class JSONInterceptor implements Interceptor {
    private static final long serialVersionUID = 4950170304212158803L;
    private static final Log log = LogFactory.getLog(JSONInterceptor.class);
    private boolean enableSMD = false;
    private boolean enableGZIP = false;
    private boolean wrapWithComments;
    private boolean prefix;
    private String defaultEncoding = "ISO-8859-1";
    private boolean ignoreHierarchy = true;
    private String root;
    private List<Pattern> excludeProperties;
    private List<Pattern> includeProperties;
    private boolean ignoreSMDMethodInterfaces = true;
    private JSONPopulator populator = new JSONPopulator();
    private JSONCleaner dataCleaner = null;
    private boolean debug = false;
    private boolean noCache = false;
    private boolean excludeNullProperties;
    private String callbackParameter;
    private String contentType;

    public void destroy() {
    }

    public void init() {
    }

    @SuppressWarnings("unchecked")
    public String intercept(ActionInvocation invocation) throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        HttpServletResponse response = ServletActionContext.getResponse();
        String contentType = request.getHeader("content-type");
        if (contentType != null) {
            int iSemicolonIdx;
            if ((iSemicolonIdx = contentType.indexOf(";")) != -1)
                contentType = contentType.substring(0, iSemicolonIdx);
        }

        Object rootObject;
        if (this.root != null) {
            ValueStack stack = invocation.getStack();
            rootObject = stack.findValue(this.root);

            if (rootObject == null) {
                throw new RuntimeException("Invalid root expression: '" + this.root + "'.");
            }
        } else {
            rootObject = invocation.getAction();
        }

        if ((contentType != null) && contentType.equalsIgnoreCase("application/json")) {
            //load JSON object
            Object obj = JSONUtil.deserialize(request.getReader());

            if (obj instanceof Map) {
                Map json = (Map) obj;

                //clean up the values
                if (dataCleaner != null)
                    dataCleaner.clean("", json);

                //populate fields
                populator.populateObject(rootObject, json);
            } else {
                log.error("Unable to deserialize JSON object from request");
                throw new JSONException("Unable to deserialize JSON object from request");
            }
        } else if ((contentType != null) &&
                contentType.equalsIgnoreCase("application/json-rpc")) {
            Object result;
            if (this.enableSMD) {
                //load JSON object
                Object obj = JSONUtil.deserialize(request.getReader());

                if (obj instanceof Map) {
                    Map smd = (Map) obj;

                    //invoke method
                    try {
                        result = this.invoke(rootObject, smd);
                    } catch (Exception e) {
                        RPCResponse rpcResponse = new RPCResponse();
                        rpcResponse.setId(smd.get("id").toString());
                        rpcResponse.setError(new RPCError(e, RPCErrorCode.EXCEPTION, debug));

                        result = rpcResponse;
                    }
                } else {
                    String message = "SMD request was not in the right format. See http://json-rpc.org";

                    RPCResponse rpcResponse = new RPCResponse();
                    rpcResponse.setError(new RPCError(message, RPCErrorCode.INVALID_PROCEDURE_CALL));
                    result = rpcResponse;
                }

                String json = JSONUtil.serialize(result, excludeProperties, includeProperties,
                        ignoreHierarchy, excludeNullProperties);
                json = addCallbackIfApplicable(request, json);
                JSONUtil.writeJSONToResponse(
                        new SerializationParams(response, this.defaultEncoding, this.wrapWithComments, json, true, false, noCache, -1, -1, prefix, contentType));

                return Action.NONE;
            } else {
                String message = "Request with content type of 'application/json-rpc' was received but SMD is "
                        + "not enabled for this interceptor. Set 'enableSMD' to true to enable it";

                RPCResponse rpcResponse = new RPCResponse();
                rpcResponse.setError(new RPCError(message, RPCErrorCode.SMD_DISABLED));
                result = rpcResponse;
            }

            String json = JSONUtil.serialize(result, excludeProperties, includeProperties, ignoreHierarchy, excludeNullProperties);
            json = addCallbackIfApplicable(request, json);
            boolean writeGzip = enableGZIP && JSONUtil.isGzipInRequest(request);
            JSONUtil.writeJSONToResponse(
                    new SerializationParams(response, this.defaultEncoding, this.wrapWithComments, json, true, writeGzip, noCache, -1, -1, prefix, contentType));

            return Action.NONE;
        } else {
            if (log.isDebugEnabled()) {
                log
                        .debug("Content type must be 'application/json' or 'application/json-rpc'. Ignoring request with content type " +
                                contentType);
            }
        }

        return invocation.invoke();
    }

    @SuppressWarnings("unchecked")
    public RPCResponse invoke(Object object, Map data) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException, JSONException,
            InstantiationException, NoSuchMethodException, IntrospectionException {

        RPCResponse response = new RPCResponse();

        //validate id
        Object id = data.get("id");
        if (id == null) {
            String message = "'id' is required for JSON RPC";
            response.setError(new RPCError(message, RPCErrorCode.METHOD_NOT_FOUND));
            return response;
        }
        //could be a numeric value
        response.setId(id.toString());

        // the map is going to have: 'params', 'method' and 'id' (what is the id for?)
        Class clazz = object.getClass();

        //parameters
        List parameters = (List) data.get("params");
        int parameterCount = parameters != null ? parameters.size() : 0;

        //method
        String methodName = (String) data.get("method");
        if (methodName == null) {
            String message = "'method' is required for JSON RPC";
            response.setError(new RPCError(message, RPCErrorCode.MISSING_METHOD));
            return response;
        }

        Method method = this.getMethod(clazz, methodName, parameterCount);
        if (method == null) {
            String message = "Method " + methodName +
                    " could not be found in action class.";
            response.setError(new RPCError(message, RPCErrorCode.METHOD_NOT_FOUND));
            return response;
        }

        //parameters
        if (parameterCount > 0) {
            Class[] parameterTypes = method.getParameterTypes();
            Type[] genericTypes = method.getGenericParameterTypes();
            List invocationParameters = new ArrayList();

            //validate size
            if (parameterTypes.length != parameterCount) {
                //size mismatch
                String message = "Parameter count in request, " + parameterCount +
                        " do not match expected parameter count for " + methodName + ", " +
                        parameterTypes.length;

                response.setError(new RPCError(message, RPCErrorCode.PARAMETERS_MISMATCH));
                return response;
            }

            //convert parameters
            for (int i = 0; i < parameters.size(); i++) {
                Object parameter = parameters.get(i);
                Class paramType = parameterTypes[i];
                Type genericType = genericTypes[i];

                //clean up the values
                if (dataCleaner != null)
                    parameter = dataCleaner.clean("[" + i + "]", parameter);

                Object converted = populator.convert(paramType, genericType, parameter, method);
                invocationParameters.add(converted);
            }

            response.setResult(method.invoke(object, invocationParameters.toArray()));
        } else {
            response.setResult(method.invoke(object, new Object[0]));
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    private Method getMethod(Class clazz, String name, int parameterCount) {
        Method[] smdMethods = JSONUtil.listSMDMethods(clazz, ignoreSMDMethodInterfaces);

        for (Method method : smdMethods) {
            if (checkSMDMethodSignature(method, name, parameterCount)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Look for a method in clazz carrying the SMDMethod annotation with matching name and parametersCount
     *
     * @return true if matches name and parameterCount
     */
    private boolean checkSMDMethodSignature(Method method, String name, int parameterCount) {

        SMDMethod smdMethodAnntotation = method.getAnnotation(SMDMethod.class);
        if (smdMethodAnntotation != null) {
            String alias = smdMethodAnntotation.name();
            boolean paramsMatch = method.getParameterTypes().length == parameterCount;
            if ((alias.length() == 0 && method.getName().equals(name) && paramsMatch) ||
                    (alias.equals(name) && paramsMatch)) {
                return true;
            }
        }

        return false;
    }

    protected String addCallbackIfApplicable(HttpServletRequest request,
                                             String json) {
        if (callbackParameter != null && callbackParameter.length() > 0) {
            String callbackName = request.getParameter(callbackParameter);
            if (callbackName != null && callbackName.length() > 0)
                json = callbackName + "(" + json + ")";
        }
        return json;
    }

    public boolean isEnableSMD() {
        return this.enableSMD;
    }

    public void setEnableSMD(boolean enableSMD) {
        this.enableSMD = enableSMD;
    }

    /**
     * Ignore annotations on methods in interfaces
     * You may need to set to this true if your action is a proxy/enhanced as annotations are not inherited
     */
    public void setIgnoreSMDMethodInterfaces(boolean ignoreSMDMethodInterfaces) {
        this.ignoreSMDMethodInterfaces = ignoreSMDMethodInterfaces;
    }

    /**
     * Wrap generated JSON with comments. Only used if SMD is enabled.
     *
     * @param wrapWithComments
     */
    public void setWrapWithComments(boolean wrapWithComments) {
        this.wrapWithComments = wrapWithComments;
    }

    @Inject(StrutsConstants.STRUTS_I18N_ENCODING)
    public void setDefaultEncoding(String val) {
        this.defaultEncoding = val;
    }

    /**
     * Ignore properties defined on base classes of the root object.
     *
     * @param ignoreHierarchy
     */
    public void setIgnoreHierarchy(boolean ignoreHierarchy) {
        this.ignoreHierarchy = ignoreHierarchy;
    }

    /**
     * Sets the root object to be deserialized, defaults to the Action
     *
     * @param root OGNL expression of root object to be serialized
     */
    public void setRoot(String root) {
        this.root = root;
    }

    /**
     * Sets the JSONPopulator to be used
     *
     * @param populator JSONPopulator
     */
    public void setJSONPopulator(JSONPopulator populator) {
        this.populator = populator;
    }

    /**
     * Sets the JSONCleaner to be used
     *
     * @param dataCleaner JSONCleaner
     */
    public void setJSONCleaner(JSONCleaner dataCleaner) {
        this.dataCleaner = dataCleaner;
    }

    /**
     * Turns debugging on or off
     *
     * @param debug true or false
     */
    public boolean getDebug() {
        return this.debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Sets a comma-delimited list of regular expressions to match
     * properties that should be excluded from the JSON output.
     *
     * @param commaDelim A comma-delimited list of regular expressions
     */
    public void setExcludeProperties(String commaDelim) {
        List<String> excludePatterns = JSONUtil.asList(commaDelim);
        if (excludePatterns != null) {
            this.excludeProperties = new ArrayList<Pattern>(excludePatterns.size());
            for (String pattern : excludePatterns) {
                this.excludeProperties.add(Pattern.compile(pattern));
            }
        }
    }

    /**
     * Sets a comma-delimited list of regular expressions to match
     * properties that should be included from the JSON output.
     *
     * @param commaDelim A comma-delimited list of regular expressions
     */
    public void setIncludeProperties(String commaDelim) {
        List<String> includePatterns = JSONUtil.asList(commaDelim);
        if (includePatterns != null) {
            this.includeProperties = new ArrayList<Pattern>(includePatterns.size());
            for (String pattern : includePatterns) {
                this.includeProperties.add(Pattern.compile(pattern));
            }
        }
    }

    public boolean isEnableGZIP() {
        return enableGZIP;
    }

    /**
     * Setting this property to "true" will compress the output.
     *
     * @param enableGZIP Enable compressed output
     */
    public void setEnableGZIP(boolean enableGZIP) {
        this.enableGZIP = enableGZIP;
    }

    public boolean isNoCache() {
        return noCache;
    }

    /**
     * Add headers to response to prevent the browser from caching the response
     *
     * @param noCache
     */
    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    public boolean isExcludeNullProperties() {
        return excludeNullProperties;
    }

    /**
     * Do not serialize properties with a null value
     *
     * @param excludeNullProperties
     */
    public void setExcludeNullProperties(boolean excludeNullProperties) {
        this.excludeNullProperties = excludeNullProperties;
    }

    public void setCallbackParameter(String callbackParameter) {
        this.callbackParameter = callbackParameter;
    }

    public String getCallbackParameter() {
        return callbackParameter;
    }

    /**
     * Add "{} && " to generated JSON
     * @param prefix
     */
    public void setPrefix(boolean prefix) {
        this.prefix = prefix;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
