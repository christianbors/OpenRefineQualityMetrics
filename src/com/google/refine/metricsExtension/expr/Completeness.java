
package com.google.refine.metricsExtension.expr;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricEvaluable;


public class Completeness implements Function {

    private boolean acceptZeroes;

    public Completeness() {
    	this(false);
    }
    
    public Completeness(boolean acceptZeroes) {
        this.acceptZeroes = acceptZeroes;
    }

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object call(Properties bindings, Object[] args) {
		Object value = bindings.get("value");
//		acceptZeroes = (boolean) bindings.get("acceptZeroes");
		
		if (value instanceof Long 
                || value instanceof Double
                || value instanceof Float) {
                return value != null;
        } else if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        //TODO: add function to check date
        return false;
	}

}
