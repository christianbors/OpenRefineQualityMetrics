package com.google.refine.metricsExtension.util;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

public class StatisticsUtils {
	
	public static RowVisitor createAggregateRowVisitor(Project project, int cellIndex, DescriptiveStatistics stats, List<Float> values) throws Exception {
        return new RowVisitor() {
            int cellIndex;
            DescriptiveStatistics stats;
            List<Float> values;
            
            public RowVisitor init(int cellIndex, DescriptiveStatistics stats, List<Float> values) {
                this.cellIndex = cellIndex;
                this.stats = stats;
                this.values = values;
                return this;
            }
            
            @Override
            public void start(Project project) {
            	// nothing to do
            }
            
            @Override
            public void end(Project project) {
            	// nothing to do
            }
            
            public boolean visit(Project project, int rowIndex, Row row) {
                try {
                    Number val = (Number)row.getCellValue(this.cellIndex);
                    this.values.add(val.floatValue());
                    this.stats.addValue(val.floatValue());
                } catch (Exception e) {
                }

                return false;
            }
        }.init(cellIndex, stats, values);
    }
}
