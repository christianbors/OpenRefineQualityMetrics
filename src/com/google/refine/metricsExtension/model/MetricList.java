package com.google.refine.metricsExtension.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MetricList {

    @JsonProperty("metricList")
    private List<Metric> metricList;

    public MetricList(@JsonProperty("metricList") List<Metric> metricList) {
        this.metricList = metricList;
    }

}
