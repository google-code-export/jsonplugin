package com.googlecode.jsonplugin;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsConstants;

import com.googlecode.jsonplugin.annotations.SMDMethod;
import com.googlecode.jsonplugin.rpc.RPCError;
import com.googlecode.jsonplugin.rpc.RPCErrorCode;
import com.googlecode.jsonplugin.rpc.RPCResponse;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.interceptor.Interceptor;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * Populates an action from a JSON string
 *
 */
public class JSONInterceptor implements Interceptor {
    private static final Log log = LogFactory.getLog(JSONInterceptor.class);
    private boolean enableSMD = false;
    private boolean wrapWithComments;
    private String defaultEncoding = "ISO-8859-1";
    private boolean ignoreHierarchy = true;
    private String root;
    private List<Pattern> excludeProperties = null;
    private boolean ignoreSMDMethodInterfaces = true;

    public void destroy() {
    }

    public void init() {
    }

    @SuppressWarnings("unchecked")
    public String intercept(ActionInvocation invocation) throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        HttpServletResponse response = ServletActionContext.getResponse();
        String contentType = request.getHeader("content-type");

        Object rootObject = null;
        if (this.root != null) {
            ValueStack stack = invocation.getStack();
            rootObject = stack.findValue(this.root);
        } else {
            rootObject = invocation.getAction();
        }

        if ((contentType != null) && contentType.equalsIgnoreCase("application/json")) {
            //load JSON object
            Object obj = JSONUtil.deserialize(request.getReader());

            if (obj instanceof Map) {
                Map json = (Map) obj;

                //populate fields
                JSONPopulator populator = new JSONPopulator();
                populator.populateObject(rootObject, json);
            } else {
                log.error("Unable to deserialize JSON object from request");
                throw new JSONException("Unable to deserialize JSON object from request");
            }
        } else if ((contentType != null) &&
            contentType.equalsIgnoreCase("application/json-rpc")) {
            Object result = null;
            if (this.enableSMD) {
                //load JSON object
                Object obj = JSONUtil.deserialize(request.getReader());

                if (obj instanceof Map) {
                    Map smd = (Map) obj;

                    //invoke method
                    try {
                        result = this.invoke(rootObject, smd);
                    } catch (Exception e) {
                        Throwable t = e;
                        while (t.getCause() != null) {
                            t = t.getCause();
                        }

                        RPCResponse rpcResponse = new RPCResponse();
                        buildError(rpcResponse, t.getMessage(), RPCErrorCode.EXCEPTION);
                        log.error("stack trace:", e); // buildError doesn't log stack trace

                        rpcResponse.getError().setName(t.getClass().getName());

                        result = rpcResponse;
                    }
                } else {
                    String message = "SMD request was not in the right format. See http://json-rpc.org";

                    RPCResponse rpcResponse = new RPCResponse();
                    buildError(rpcResponse, message, RPCErrorCode.INVALID_PROCEDURE_CALL);
                    result = rpcResponse;
                }

                String json = JSONUtil.serialize(result, excludeProperties,
                    ignoreHierarchy);
                JSONUtil.writeJSONToResponse(response, this.defaultEncoding,
                    this.wrapWithComments, json, true);

                return Action.NONE;
            } else {
                String message = "Request with content type of 'application/json-rpc' was received but SMD is "
                    + "not enabled for this interceptor. Set 'enableSMD' to true to enable it";

                RPCResponse rpcResponse = new RPCResponse();
                buildError(rpcResponse, message, RPCErrorCode.SMD_DISABLED);
                result = rpcResponse;
            }

            String json = JSONUtil.serialize(result, excludeProperties, ignoreHierarchy);
            JSONUtil.writeJSONToResponse(response, this.defaultEncoding,
                this.wrapWithComments, json, true);

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
            return buildError(response, message, RPCErrorCode.METHOD_NOT_FOUND);
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
            return buildError(response, message, RPCErrorCode.MISSING_METHOD);
        }

        Method method = this.getMethod(clazz, methodName, parameterCount);
        if (method == null) {
            String message = "Method " + methodName +
                " could not be found in action class.";
            return buildError(response, message, RPCErrorCode.METHOD_NOT_FOUND);
        }

        //parameters
        if (parameterCount > 0) {
            Class[] parameterTypes = method.getParameterTypes();
            List invocationParameters = new ArrayList();

            //validate size
            if (parameterTypes.length != parameterCount) {
                //size mismatch
                String message = "Parameter count in request, " + parameterCount +
                    " do not match expected parameter count for " + methodName + ", " +
                    parameterTypes.length;

                return buildError(response, message, RPCErrorCode.PARAMETERS_MISMATCH);
            }

            //convert parameters
            JSONPopulator populator = new JSONPopulator();
            for (int i = 0; i < parameters.size(); i++) {
                Object parameter = parameters.get(i);
                Class paramType = parameterTypes[i];

                Object converted = populator.convert(paramType, parameter, method);
                invocationParameters.add(converted);
            }

            response.setResult(method.invoke(object, invocationParameters.toArray()));
        } else {
            response.setResult(method.invoke(object, new Object[0]));
        }

        return response;
    }

    private RPCResponse buildError(RPCResponse response, String message, RPCErrorCode code) {
        RPCError error = new RPCError();
        error.setCode(code.code());
        error.setMessage(message);

        log.error(message);
        response.setError(error);
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
     * @param ignoreHierarchy
     */
    public void setIgnoreHierarchy(boolean ignoreHierarchy) {
        this.ignoreHierarchy = ignoreHierarchy;
    }

    /**
     * Sets the root object to be deserialized, defaults to the Action
     * @param root OGNL expression of root object to be serialized
     */
    public void setRoot(String root) {
        this.root = root;
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
}
