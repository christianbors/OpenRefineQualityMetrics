package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.metricsExtension.util.MetricUtils;
import com.google.refine.metricsExtension.util.MetricUtils.RegisteredMetrics;
import com.google.refine.metricsExtension.util.MetricUtils.RegisteredSpanningMetrics;
import com.google.refine.model.Project;

public class CreateMetricCommand extends Command {

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String[] columnNames = request.getParameterValues("columns[]");
		String metricName = request.getParameter("metric");
		String description = request.getParameter("description");
		String[] parameters = request.getParameterValues("parameters[]");
		String dataType = request.getParameter("dataType");
		Project project = getProject(request);
		
		MetricsOverlayModel metricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		try {
			if (columnNames.length == 1) {
				Metric m = new Metric(metricName, description, dataType);
//				for (Entry<String, Function> entry : ControlFunctionRegistry.getFunctionMapping()) {
//					if(entry.getKey().equals(metricName)) {
						String evalString = metricName + "(value";
						if(parameters != null) {
							for(int i = 0; i < parameters.length; ++i) {
								evalString += parameters[i];
								if(i+1 < parameters.length)
									evalString += ", ";
							}
						}
						evalString += ")";
						m.addEvalTuple(MetaParser.parse(evalString), "", false);
//					}
//				}
//				m.addEvalTuple(RegisteredMetrics.valueOf(metricName).evaluable(), "", false);
				metricsOverlayModel.addMetric(columnNames[0], m);
			} else if (columnNames.length == 2) {
					SpanningMetric newSpanningMetric = new SpanningMetric(
							metricName,
							description, 
							Arrays.asList(columnNames));
					String evalString = metricName + "(";
					if(columnNames.length > 1) {
						for(int i = 0; i < columnNames.length; ++i) {
							evalString += "columnName";
							if(i+1 < columnNames.length)
								evalString += ", ";
						}
						evalString += ")";
					} else {
						evalString += "value)";
					}
					newSpanningMetric.addSpanningEvalTuple(MetaParser.parse(evalString), "", false);
					metricsOverlayModel.addSpanningMetric(newSpanningMetric);
			}
		} catch (ParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ProjectManager.singleton.ensureProjectSaved(project.id);
	}

}
