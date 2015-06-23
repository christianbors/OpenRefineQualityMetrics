package com.google.refine.metricsExtension.model;

import java.util.List;

import com.google.refine.metricsExtension.metrics.column.ColumnMetric;
import com.google.refine.model.Column;


public class MetricsColumn extends Column {

    List<ColumnMetric> metrics;
    
    public MetricsColumn(int cellIndex, String originalName) {
        super(cellIndex, originalName);
        // TODO Auto-generated constructor stub
    }

}
