package com.googlecode.jsonplugin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsConstants;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * <!-- START SNIPPET: description -->
 *
 * This result serializes an action into JSON. Fields to be serialized must have
 * a getter method and <b>must not</b> be 'transient'.
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
    private String defaultEncoding;
    private List<Pattern> excludeProperties = null;
    private String root;
    private boolean wrapWithComments;
    
    @Inject(StrutsConstants.STRUTS_I18N_ENCODING)
    public void setDefaultEncoding(String val) {
        defaultEncoding = val;
    }

    /**
     * Gets a list of regular expressions of properties to exclude
     * from the JSON output.
     * 
     * @return A list of compiled regular expression patterns
     */
    public List getExcludePropertiesList() {
        return excludeProperties;
    }

    /**
     * Sets a comma-delimited list of regular expressions to match 
     * properties that should be excluded from the JSON output.
     * 
     * @param commaDelim A comma-delimited list of regular expressions
     */
    public void setExcludeProperties(String commaDelim) {
        List<String> excludePatterns = asList(commaDelim);
        if(excludePatterns != null) {
            excludeProperties = new ArrayList<Pattern>(excludePatterns.size());
            for(String pattern : excludePatterns) {
                excludeProperties.add(Pattern.compile(pattern));
            }
        }
    }

    private List<String> asList(String commaDelim) {
        if(commaDelim == null || commaDelim.trim().length() == 0) {
            return null;
        }
        List<String> list = new ArrayList<String>();
        String[] split = commaDelim.split(",");
        for(int i = 0; i < split.length; i++) {
            String trimmed = split[i].trim();
            if(trimmed.length() > 0) {
                list.add(trimmed);
            }
        }
        return list;
    }

    public void execute(ActionInvocation invocation) throws Exception {
        ActionContext actionContext = invocation.getInvocationContext();
        HttpServletResponse response = (HttpServletResponse) actionContext
            .get(ServletActionContext.HTTP_RESPONSE);

        // Write JSON to response.
        try {
            Object rootObject = null;
            if(this.root != null) {
                ValueStack stack = invocation.getStack();
                rootObject = stack.findValue(this.root);
            } else {
                rootObject = invocation.getAction();
            }
           
            String json = JSONUtil.serialize(rootObject, excludeProperties);
            if(wrapWithComments) {
                StringBuilder sb = new StringBuilder("/* ");
                sb.append(json);
                sb.append(" */");
                json = sb.toString();
            }
            if(log.isDebugEnabled()) {
                log.debug("[JSON]" + json);
            }

            response.setContentLength(json.getBytes().length);
            response.setContentType("application/json;charset=" + getEncoding());

            PrintWriter out = response.getWriter();

            out.print(json);
        } catch(IOException exception) {
            log.error(exception);
            throw exception;
        }
    }

    /**
     * Retrieve the encoding
     * <p/>
     *
     * @return The encoding associated with this template (defaults to the value of 'struts.i18n.encoding' property)
     */
    protected String getEncoding() {
        String encoding = defaultEncoding;

        if(encoding == null) {
            encoding = System.getProperty("file.encoding");
        }

        if(encoding == null) {
            encoding = "UTF-8";
        }

        return encoding;
    }

    /**
     * @return  OGNL expression of root object to be serialized
     */
    public String getRoot() {
        return root;
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
        return wrapWithComments;
    }

    /**
     * Wrap generated JSON with comments
     * @param wrapWithComments
     */
    public void setWrapWithComments(boolean wrapWithComments) {
        this.wrapWithComments = wrapWithComments;
    }
}
