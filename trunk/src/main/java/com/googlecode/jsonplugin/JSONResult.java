package com.googlecode.jsonplugin;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.StrutsConstants;
import org.apache.struts2.StrutsStatics;

import com.googlecode.jsonplugin.annotations.SMD;
import com.googlecode.jsonplugin.annotations.SMDMethod;
import com.googlecode.jsonplugin.annotations.SMDMethodParameter;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * <!-- START SNIPPET: description -->
 *
 * This result serializes an action into JSON. 
 *
 * <!-- END SNIPPET: description -->
 *
 * <p/> <u>Result parameters:</u>
 *
 * <!-- START SNIPPET: parameters -->
 *
 * <ul>
 *
 * <li>excludeProperties - list of regular expressions matching the properties to be excluded.
 * The regular expressions are evaluated against the OGNL expression representation of the properties. </li>
 *
 * </ul>
 *
 * <!-- END SNIPPET: parameters -->
 *
 * <b>Example:</b>
 *
 * <pre><!-- START SNIPPET: example -->
 * &lt;result name="success" type="json" /&gt;
 * <!-- END SNIPPET: example --></pre>
 *
 */
public class JSONResult implements Result {
    private static final Log log = LogFactory.getLog(JSONResult.class);
    private String defaultEncoding = "ISO-8859-1";
    private List<Pattern> excludeProperties = null;
    private String root;
    private boolean wrapWithComments;
    private boolean enableSMD = false;
    private boolean ignoreHierarchy = true;
    private List<Pattern> smdMethodsHack = null;

    @Inject(StrutsConstants.STRUTS_I18N_ENCODING)
    public void setDefaultEncoding(String val) {
        this.defaultEncoding = val;
    }

    /**
     * Gets a list of regular expressions of properties to exclude
     * from the JSON output.
     * 
     * @return A list of compiled regular expression patterns
     */
    public List<Pattern> getExcludePropertiesList() {
        return this.excludeProperties;
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
     * methods that should be included irrespective of the @SMDMethod annotation.
     *
     * @param commaDelim A comma-delimited list of regular expressions
     */
    public void setSMDMethodsHack(String commaDelim) {
        List<String> includePatterns = JSONUtil.asList(commaDelim);
        if (includePatterns != null) {
            this.smdMethodsHack = new ArrayList<Pattern>(includePatterns.size());
            for (String pattern : includePatterns) {
                this.smdMethodsHack.add(Pattern.compile(pattern));
            }
        }
    }

    public void execute(ActionInvocation invocation) throws Exception {
        ActionContext actionContext = invocation.getInvocationContext();
        HttpServletResponse response = (HttpServletResponse) actionContext
            .get(StrutsStatics.HTTP_RESPONSE);

        try {
            String json;
            Object rootObject;
            if (this.enableSMD) {
                //generate SMD
                rootObject = this.writeSMD(invocation);
            } else {
                // generate JSON
                if (this.root != null) {
                    ValueStack stack = invocation.getStack();
                    rootObject = stack.findValue(this.root);
                } else {
                    rootObject = invocation.getAction();
                }
            }
            json = JSONUtil.serialize(rootObject, this.excludeProperties, ignoreHierarchy);

            JSONUtil.writeJSONToResponse(response, this.defaultEncoding,
                isWrapWithComments(), json, false);

        } catch (IOException exception) {
            log.error(exception);
            throw exception;
        }
    }

    @SuppressWarnings("unchecked")
    private com.googlecode.jsonplugin.smd.SMD writeSMD(ActionInvocation invocation) {
        ActionContext actionContext = invocation.getInvocationContext();
        HttpServletRequest request = (HttpServletRequest) actionContext
            .get(StrutsStatics.HTTP_REQUEST);

        //root is based on OGNL expression (action by default)
        Object rootObject = null;
        if (this.root != null) {
            ValueStack stack = invocation.getStack();
            rootObject = stack.findValue(this.root);
        } else {
            rootObject = invocation.getAction();
        }

        Class clazz = rootObject.getClass();
        com.googlecode.jsonplugin.smd.SMD smd = new com.googlecode.jsonplugin.smd.SMD();
        //URL
        smd.setServiceUrl(request.getRequestURI());

        //customize SMD
        SMD smdAnnotation = (SMD) clazz.getAnnotation(SMD.class);
        if (smdAnnotation != null) {
            smd.setObjectName(smdAnnotation.objectName());
            smd.setServiceType(smdAnnotation.serviceType());
            smd.setVersion(smdAnnotation.version());
        }

        //get public methods
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            SMDMethod smdMethodAnnotation = method.getAnnotation(SMDMethod.class);

            //SMDMethod annotation is required
            if (((smdMethodAnnotation != null)
                && !this.shouldExcludeProperty(method.getName())) ||
                 (this.shouldIncludeMethod(method.getName()))) {
                String methodName;
                if (smdMethodAnnotation != null) {
                    methodName = smdMethodAnnotation.name().length() == 0 ? method.getName() : smdMethodAnnotation.name();
                } else {
                    methodName = method.getName();
                }
                com.googlecode.jsonplugin.smd.SMDMethod smdMethod = new com.googlecode.jsonplugin.smd.SMDMethod(
                    methodName);
                smd.addSMDMethod(smdMethod);

                //find params for this method
                int parametersCount = method.getParameterTypes().length;
                if (parametersCount > 0) {
                    Annotation[][] parameterAnnotations = method
                        .getParameterAnnotations();

                    for (int i = 0; i < parametersCount; i++) {
                        //are you ever going to pick shorter names? nope                    
                        SMDMethodParameter smdMethodParameterAnnotation = this
                            .getSMDMethodParameterAnnotation(parameterAnnotations[i]);

                        String paramName = smdMethodParameterAnnotation != null ? smdMethodParameterAnnotation
                            .name()
                            : "p" + i;

                        //goog thing this is the end of the hierarchy, oitherwise I would need that 21'' LCD ;)    
                        smdMethod
                            .addSMDMethodParameter(new com.googlecode.jsonplugin.smd.SMDMethodParameter(
                                paramName));
                    }
                }

            } else {
                if (log.isDebugEnabled())
                    log.debug("Ignoring property " + method.getName());
            }
        }
        return smd;
    }

    /**
     * Find an SMDethodParameter annotation on this array
     */
    private com.googlecode.jsonplugin.annotations.SMDMethodParameter getSMDMethodParameterAnnotation(
        Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof com.googlecode.jsonplugin.annotations.SMDMethodParameter)
                return (com.googlecode.jsonplugin.annotations.SMDMethodParameter) annotation;
        }

        return null;
    }

    private boolean shouldExcludeProperty(String expr) {
        if (this.excludeProperties != null) {
            for (Pattern pattern : this.excludeProperties) {
                if (pattern.matcher(expr).matches())
                    return true;
            }
        }
        return false;
    }

    private boolean shouldIncludeMethod(String expr) {
        if (this.smdMethodsHack != null) {
            for (Pattern pattern : this.smdMethodsHack) {
                if (pattern.matcher(expr).matches())
                    return true;
            }
        }
        return false;
    }

    /**
     * Retrieve the encoding
     * <p/>
     *
     * @return The encoding associated with this template (defaults to the value of 'struts.i18n.encoding' property)
     */
    protected String getEncoding() {
        String encoding = this.defaultEncoding;

        if (encoding == null) {
            encoding = System.getProperty("file.encoding");
        }

        if (encoding == null) {
            encoding = "UTF-8";
        }

        return encoding;
    }

    /**
     * @return  OGNL expression of root object to be serialized
     */
    public String getRoot() {
        return this.root;
    }

    /**
     * Sets the root object to be serialized, defaults to the Action
     * 
     * @param root OGNL expression of root object to be serialized
     */
    public void setRoot(String root) {
        this.root = root;
    }

    /**
     * @return Generated JSON must be enclosed in comments
     */
    public boolean isWrapWithComments() {
        return this.wrapWithComments;
    }

    /**
     * Wrap generated JSON with comments
     * @param wrapWithComments
     */
    public void setWrapWithComments(boolean wrapWithComments) {
        this.wrapWithComments = wrapWithComments;
    }

    /**
     * @return Result has SMD generation enabled
     */
    public boolean isEnableSMD() {
        return this.enableSMD;
    }

    /**
     * Enable SMD generation for action, which can be used for JSON-RPC
     * @param enableSMD
     */
    public void setEnableSMD(boolean enableSMD) {
        this.enableSMD = enableSMD;
    }

    public void setIgnoreHierarchy(boolean ignoreHierarchy) {
        this.ignoreHierarchy = ignoreHierarchy;
    }
}
