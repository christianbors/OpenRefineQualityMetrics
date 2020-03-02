package com.google.refine.metricsExtension.expr.metrics.singleColumn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;

public abstract class SingleColumnMetricFunction implements Function {

	@JsonCreator
	public SingleColumnMetricFunction() {
		super();
	}

	@JsonProperty("defaultParams")
	public abstract String getDefaultParams();

	public abstract String getDescription();

	@JsonProperty("eval")
	public abstract String getEvaluable(String[] params) throws ParsingException;
	
	public abstract String getParams();

	@Override
	public String getReturns() {
		return "boolean";
	}
}
