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

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.googlecode.jsonplugin.annotations.JSON;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;

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
    private Bean[] beanArray;
    private int[] intArray;
    private List list;
    private String bar;
    private String nogetter;
    private Date date2;
    private Bean bean;
    private Date date;

    public Bean getBean() {
        return bean;
    }

    public void setBean(Bean bean) {
        this.bean = bean;
    }

    public List getCollection() {
        return collection;
    }

    public void setCollection(List collection) {
        this.collection = collection;
    }

    public List getCollection2() {
        return collection2;
    }

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

    public List getList() {
        return list;
    }

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

    @JSON(serialize=false)
    public String getBar() {
        return bar;
    }

    public void setNogetter(String nogetter) {
        this.nogetter = nogetter;
    }

    @JSON(serialize=false)
    public int[] getIntArray() {
        return intArray;
    }

    public void setIntArray(int[] intArray) {
        this.intArray = intArray;
    }

    @JSON(serialize=false)
    public Bean[] getBeanArray() {
        return beanArray;
    }

    public void setBeanArray(Bean[] beanArray) {
        this.beanArray = beanArray;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @JSON(format="dd/MM/yy")
    public Date getDate2() {
        return date2;
    }

    @JSON(format="dd/MM/yy")
    public void setDate2(Date date2) {
        this.date2 = date2;
    }
}
