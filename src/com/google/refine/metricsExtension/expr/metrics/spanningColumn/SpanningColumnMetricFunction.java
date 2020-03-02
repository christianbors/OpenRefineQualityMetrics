package com.google.refine.metricsExtension.expr.metrics.spanningColumn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;

import java.util.List;

public abstract class SpanningColumnMetricFunction implements Function {

	@JsonCreator
	public SpanningColumnMetricFunction() {
		super();
	}

	@JsonProperty("defaultParams")
	public abstract String getDefaultParams();

	public abstract String getDescription();

	public abstract String getEvaluable(String[] columns, String[] params) throws ParsingException;

	public abstract String getParams();

	@Override
	public String getReturns() {
		return "boolean";
	}
}
