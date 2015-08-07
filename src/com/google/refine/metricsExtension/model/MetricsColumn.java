package com.google.refine.metricsExtension.model;

import java.util.List;

import com.google.refine.metricsExtension.commands.ColumnMetricEvaluation;
import com.google.refine.model.Column;


public class MetricsColumn extends Column {

    List<Metric<?>> metrics;
    
    public MetricsColumn(int cellIndex, String originalName) {
        super(cellIndex, originalName);
        // TODO Auto-generated constructor stub
    }

}
