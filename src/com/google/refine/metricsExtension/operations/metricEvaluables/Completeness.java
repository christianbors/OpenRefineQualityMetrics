
package com.google.refine.metricsExtension.operations.metricEvaluables;

import java.util.Properties;

import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricEvaluable;


public class Completeness implements MetricEvaluable {

    private boolean acceptZeroes;

	public Completeness() {
    	this(false);
    }
    
    public Completeness(boolean acceptZeroes) {
        this.acceptZeroes = acceptZeroes;
    }

    public boolean evaluateValue(Object val) {
        if (val instanceof Long 
                || val instanceof Double
                || val instanceof Float) {
                return val != null;
        } else if (val instanceof String) {
            return !((String) val).isEmpty();
        }
        //TODO: add function to check date
        return false;
    }

	@Override
	public Object evaluate(Properties bindings) {
		// TODO Auto-generated method stub
		return null;
	}

}
