
package com.google.refine.metricsExtension.model;

import java.util.Properties;

public class Completeness extends Metric<Object> {

    public Completeness() {
        this("Completeness", "Todo");
    }
    
    public Completeness(String name, String description) {
        super(name, description);
        // TODO Auto-generated constructor stub
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

    @Override
    public boolean evaluateValue(Properties bindings) {
        return evaluateValue(bindings.get("value"));
    }

}
