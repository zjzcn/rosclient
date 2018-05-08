package com.github.rosclient.messages;

import org.json.JSONObject;

public class Response {

    private boolean result;

    private JSONObject values;

    private String valuesString;

    public Response() {

    }

    public Response(boolean result, JSONObject values) {
        this.result = result;
        this.values = values;
    }

    public Response(boolean result, String valuesString) {
        this.result = result;
        this.valuesString = valuesString;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public JSONObject getValues() {
        return values;
    }

    public void setValues(JSONObject values) {
        this.values = values;
    }

    public String getValuesString() {
        return valuesString;
    }

    public void setValuesString(String valuesString) {
        this.valuesString = valuesString;
    }
}
