package com.googlecode.jsonplugin;

import com.googlecode.jsonplugin.annotations.SMD;
import com.googlecode.jsonplugin.annotations.SMDMethod;
import com.googlecode.jsonplugin.annotations.SMDMethodParameter;

@SMD(objectName = "testaction", serviceType = "service", version = "10.0")
public class SMDActionTest2 {
    @SMDMethod
    public void add(@SMDMethodParameter(name = "a")
    int a, @SMDMethodParameter(name = "b")
    int b) {

    }

    @SMDMethod(name = "doSomethingElse")
    public void doSomething() {

    }
}
