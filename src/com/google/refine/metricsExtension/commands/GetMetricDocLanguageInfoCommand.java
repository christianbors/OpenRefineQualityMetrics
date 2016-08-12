package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.Properties;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.expr.checks.QualityCheck;
import com.google.refine.metricsExtension.expr.metrics.singleColumn.SingleColumnMetricFunction;
import com.google.refine.metricsExtension.expr.metrics.spanningColumn.SpanningColumnMetricFunction;

public class GetMetricDocLanguageInfoCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
	
	@Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            Properties options = new Properties();
            
            writer.object();
            
            writer.key("qualityCheckFunctions");
            writer.object();
            {
                for (Entry<String, Function> entry : ControlFunctionRegistry.getFunctionMapping()) {
                	if (entry.getValue() instanceof QualityCheck) {
	                    writer.key(entry.getKey());
	                    entry.getValue().write(writer, options);
                	}
                }
            }
            writer.endObject();
            
            writer.key("singleColumnFunctions");
            writer.object();
            {
                for (Entry<String, Function> entry : ControlFunctionRegistry.getFunctionMapping()) {
                	if (entry.getValue() instanceof SingleColumnMetricFunction) {
	                    writer.key(entry.getKey());
	                    entry.getValue().write(writer, options);
                	}
                }
            }
            writer.endObject();
            
            writer.key("spanningColumnFunctions");
            writer.object();
            {
                for (Entry<String, Function> entry : ControlFunctionRegistry.getFunctionMapping()) {
                	if (entry.getValue() instanceof SpanningColumnMetricFunction) {
	                    writer.key(entry.getKey());
	                    entry.getValue().write(writer, options);
                	}
                }
            }
            writer.endObject();
            
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
	
}
