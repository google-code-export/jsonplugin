package com.googlecode.jsonplugin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsConstants;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.inject.Inject;

/**
 * <!-- START SNIPPET: description -->
 *
 * This result serializes an action into JSON. Fields to be serialized must have
 * a getter method and <b>must not</b> be 'transient'.
 *
 * <!-- END SNIPPET: description -->
 * <p/>

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

    @Inject(StrutsConstants.STRUTS_I18N_ENCODING)
    public void setDefaultEncoding(String val) {
        defaultEncoding = val;
    }

    public void execute(ActionInvocation invocation)
        throws Exception {
        ActionContext actionContext = invocation.getInvocationContext();
        HttpServletResponse response =
            (HttpServletResponse) actionContext.get(ServletActionContext.HTTP_RESPONSE);

        // Write JSON to response.
        try {
            String json = JSONUtil.serialize((invocation.getAction()));

            if(log.isDebugEnabled()) {
                log.debug("[JSON]" + json);
            }

            response.setContentLength(json.length());
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
}
