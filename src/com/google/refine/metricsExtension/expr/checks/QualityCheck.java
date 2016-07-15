package com.google.refine.metricsExtension.expr.checks;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.Function;

public interface QualityCheck extends Function {
	
	public String getDescription();
	
	public Evaluable getEvaluable(String[] params) throws ParsingException;
	
	public String getParams();
}
