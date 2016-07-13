package com.google.refine.metricsExtension.expr;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;

public interface MetricFunction extends Function {
	
	public String getDescription();
	
	public Evaluable getEvaluable(String[] params) throws ParsingException;
	
	public String getParams();
	
}
