package com.google.refine.metricsExtension.expr;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;

public interface MetricFunction extends Function {
	public String getDescription();
	public Evaluable getEvaluable() throws ParsingException;
	public String getParams();
}
