package com.google.refine.metricsExtension.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.grel.Function;

public class MetricRecommendation {

    @JsonProperty("name")
    private String name;
    @JsonProperty("parameters")
    private String parameters;
    @JsonProperty("function")
    private Function function;

    public MetricRecommendation(String name, String parameters, Function function) {
        this.setName(name);
        this.setParameters(parameters);
        this.setFunction(function);
    }

//    @Override
//    public void write(JSONWriter writer, Properties options) throws JSONException {
//        writer.object().key("metric");
//        this.getFunction().write(writer, options);
//        writer.key("name").value(getName());
//        writer.key("parameters").value(getParameters());
//        writer.endObject();
//    }

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
