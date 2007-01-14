/*
 * $Id: TestAction.java 471756 2006-11-06 15:01:43Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.googlecode.jsonplugin;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 */
public class TestAction extends ActionSupport {
    private static final long serialVersionUID = -8891365561914451494L;
    private List collection;
    private List collection2;
    private Map map;
    private String foo;
    private String result;
    private String[] array;
    private transient Bean[] beanArray;
    private transient int[] intArray;
    private List list;
    private transient String bar;
    private String nogetter;
    private Bean bean;

    /**
     * @return the bean
     */
    public Bean getBean() {
        return bean;
    }

    /**
     * @param bean the bean to set
     */
    public void setBean(Bean bean) {
        this.bean = bean;
    }

    /**
     * @return the collection
     */
    public List getCollection() {
        return collection;
    }

    /**
     * @param collection the collection to set
     */
    public void setCollection(List collection) {
        this.collection = collection;
    }

    /**
     * @return the collection2
     */
    public List getCollection2() {
        return collection2;
    }

    /**
     * @param collection2 the collection2 to set
     */
    public void setCollection2(List collection2) {
        this.collection2 = collection2;
    }

    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String[] getArray() {
        return array;
    }

    public void setArray(String[] array) {
        this.array = array;
    }

    /**
     * @return the list
     */
    public List getList() {
        return list;
    }

    /**
     * @param list the list to set
     */
    public void setList(List list) {
        this.list = list;
    }

    public String execute() throws Exception {
        if(result == null) {
            result = Action.SUCCESS;
        }

        return result;
    }

    public String doInput() throws Exception {
        return INPUT;
    }

    public void setBar(String bar) {
        this.bar = bar;
    }

    public String getBar() {
        return bar;
    }

    public void setNogetter(String nogetter) {
        this.nogetter = nogetter;
    }

    /**
     * @return the intArray
     */
    public int[] getIntArray() {
        return intArray;
    }

    /**
     * @param intArray the intArray to set
     */
    public void setIntArray(int[] intArray) {
        this.intArray = intArray;
    }

    /**
     * @return the beanArray
     */
    public Bean[] getBeanArray() {
        return beanArray;
    }

    /**
     * @param beanArray the beanArray to set
     */
    public void setBeanArray(Bean[] beanArray) {
        this.beanArray = beanArray;
    }
}
