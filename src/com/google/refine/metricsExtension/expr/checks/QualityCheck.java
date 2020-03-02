package com.google.refine.metricsExtension.expr.checks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;

public interface QualityCheck extends Function {

	@JsonProperty("description")
	public String getDescription();

	@JsonProperty("evaluable")
	public Evaluable getEvaluable(String[] params) throws ParsingException;

	@JsonProperty("params")
	public String getParams();
}
