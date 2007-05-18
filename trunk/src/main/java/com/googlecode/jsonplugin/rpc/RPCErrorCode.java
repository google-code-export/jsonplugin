package com.googlecode.jsonplugin.rpc;

public enum RPCErrorCode {
    MISSING_METHOD(100, "'method' parameter is missing in request"),
    MISSING_ID(100, "'id' parameter is missing in request"),
    INVALID_PROCEDURE_CALL(0, "Invalid procedure call"),
    METHOD_NOT_FOUND(101, "Procedure not found"),
    PARAMETERS_MISMATCH(102,
        "Parameters count in request does not patch parameters count on method");

    private int code;
    private String message;

    RPCErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return this.message;
    }
}
