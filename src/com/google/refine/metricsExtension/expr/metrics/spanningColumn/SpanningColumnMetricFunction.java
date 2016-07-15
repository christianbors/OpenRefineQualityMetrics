package com.google.refine.metricsExtension.expr.metrics.spanningColumn;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;

public interface SpanningColumnMetricFunction extends Function {
	
	public String getDescription();
	
	public Evaluable getEvaluable(String[] columns, String[] params) throws ParsingException;
	
	public String getParams();
	
}
