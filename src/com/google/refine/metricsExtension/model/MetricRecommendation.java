package com.google.refine.metricsExtension.model;

import com.google.refine.Jsonizable;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.util.Properties;

public class MetricRecommendation implements Jsonizable {

    private String name;
    private String parameters;
    private Function function;

    public MetricRecommendation(String name, String parameters, Function function) {
        this.setName(name);
        this.setParameters(parameters);
        this.setFunction(function);
    }

    @Override
    public void write(JSONWriter writer, Properties options) throws JSONException {
        writer.object().key("metric");
        this.getFunction().write(writer, options);
        writer.key("name").value(getName());
        writer.key("parameters").value(getParameters());
        writer.endObject();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }
}
