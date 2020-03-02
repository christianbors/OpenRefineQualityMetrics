package com.google.refine.metricsExtension.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class Recommendation {

    @JsonProperty("recommendedMetrics")
    private Map<String, List<MetricRecommendation>> recommendedMetrics;

    public Recommendation(Map<String, List<MetricRecommendation>> recommendedMetrics) {
        this.recommendedMetrics = recommendedMetrics;
    }

}
