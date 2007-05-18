package com.googlecode.jsonplugin;

import java.util.List;
import java.util.Map;

import com.googlecode.jsonplugin.annotations.SMDMethod;

public class SMDActionTest1 {
    private boolean addWasCalled;
    private int intParameter;
    private boolean cooleanParameter;
    private char charParameter;
    private String stringParameter;
    private List listParameter;
    private Map mapParameter;
    private int[] arrayParameter;
    private Bean beanParameter;

    @SMDMethod
    public void add(int a, int b) {
        addWasCalled = true;
    }

    public void testPrimitiveParameters(int intParameter, char charParameter,
        boolean booleanParameter, String stringParameter) {

    }

    @SMDMethod
    public void doSomething() {

    }

    public boolean isAddWasCalled() {
        return addWasCalled;
    }

    public void setAddWasCalled(boolean addWasCalled) {
        this.addWasCalled = addWasCalled;
    }
}
