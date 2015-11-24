
package com.google.refine.metricsExtension.operations.evaluate;


public class Completeness implements EvaluateCell {

    private boolean acceptZeroes;

	public Completeness() {
    	this(false);
    }
    
    public Completeness(boolean acceptZeroes) {
        this.acceptZeroes = acceptZeroes;
    }

    @Override
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

}
