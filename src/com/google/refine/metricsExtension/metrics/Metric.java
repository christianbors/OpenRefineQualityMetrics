package com.google.refine.metricsExtension.metrics;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;


public interface Metric {
    public void calculate(Project project);
    public void recalculate(Project project, HistoryEntry newEntry);
}
