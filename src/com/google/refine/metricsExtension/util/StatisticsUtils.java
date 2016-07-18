package com.google.refine.metricsExtension.util;

import java.util.Date;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.EvalError;
import com.google.refine.expr.WrappedCell;
import com.google.refine.model.Cell;
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
	
	public static RowVisitor createAggregateRowVisitor(Project project, int cellIndex, DescriptiveStatistics stats, List<Float> values, int startIndex, int endIndex) throws Exception {
        return new RowVisitor() {
            int cellIndex;
            int startIndex;
            int endIndex;
            DescriptiveStatistics stats;
            List<Float> values;
            
            public RowVisitor init(int cellIndex, DescriptiveStatistics stats, List<Float> values, int startIndex, int endIndex) {
                this.cellIndex = cellIndex;
                this.startIndex = startIndex;
                this.endIndex = endIndex;
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
            	if(rowIndex >= startIndex && rowIndex <= endIndex) {
					try {
						Number val = (Number) row.getCellValue(this.cellIndex);
						this.values.add(val.floatValue());
						this.stats.addValue(val.floatValue());
					} catch (Exception e) {
						return false;
					}
            	}
            	if(rowIndex > endIndex) {
            		return true;
            	}
            	return false;
            }
        }.init(cellIndex, stats, values, startIndex, endIndex);
    }
	
	public static RowVisitor createAggregateSpanningRowVisitor(Project project, DescriptiveStatistics stats, List<Long> values, int colIdxFrom, int colIdxTo, String unit) throws Exception {
        return new RowVisitor() {
            int colFrom;
            int colTo;
            String unit;
            DescriptiveStatistics stats;
            List<Long> values;
            
            public RowVisitor init(DescriptiveStatistics stats, List<Long> values, int colFrom, int colTo, String unit) {
                this.colFrom = colFrom;
                this.colTo = colTo;
                this.unit = unit;
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
					long from = ((Date) row.cells.get(colFrom).value).getTime();
					long to = ((Date) row.cells.get(colTo).value).getTime();
					
					long delta = getIntervalValue(from, to, unit);
					this.values.add(delta);
					this.stats.addValue(delta);
				} catch (Exception e) {
					return false;
				}
            	return false;
            }
        }.init(stats, values, colIdxFrom, colIdxTo, unit);
    }
	
	public static long getIntervalValue(long valFrom, long valTo, String unit) throws Exception {
		long delta = (valTo - valFrom) / 1000;
		
		if ("seconds".equals(unit)) {
			return delta;
        } else if ("minutes".equals(unit)) {
        	return delta / 60;
        } else if ("hours".equals(unit)) {
        	return (delta / 60)/60;
        } else if ("days".equals(unit)) {
        	return ((delta / 60) / 60) / 24;
        } else if ("weeks".equals(unit)) {
        	return (((delta / 60) / 60) / 24) / 7;
        } else if ("months".equals(unit)) {
        	return (((delta / 60) / 60) / 24) / 30;
        } else if ("years".equals(unit)) {
        	return (((delta / 60) / 60) / 24) / 365;
        }
		throw new Exception("unknown time unit");
	}
}
