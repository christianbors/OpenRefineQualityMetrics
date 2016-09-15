package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;
import com.google.refine.model.Project;

public class MergeMetricCommand extends Command {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Project project = getProject(request);
		String[] columnNames = request.getParameterValues("columnNames[]");
		String metric1NameString = request.getParameter("metrics[0][name]");
		String metric1Evaluable = request.getParameter("metrics[1][spanningEvaluable][evaluable]");

		String metric2NameString = request.getParameter("metrics[1][name]");
		String metric2Evaluable = request.getParameter("metrics[1][spanningEvaluable][evaluable]");

		String[] metric1SpanningColumns = request.getParameterValues("metrics[0][spanningColumns][]");
		String[] metric2SpanningColumns = request.getParameterValues("metrics[1][spanningColumns][]");

		MetricsOverlayModel metricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
		if(columnNames != null && metric1NameString != null) {
		    Metric m1 = null;
		    Metric m2 = null;
		    if(metric1SpanningColumns == null) {
			m1 = metricsOverlayModel.getMetricsForColumn(columnNames[0]).get(metric1NameString);
		    } else {
		        Iterator<SpanningMetric> spanMetricsIt = metricsOverlayModel.getSpanMetricsList().iterator();
		        while(spanMetricsIt.hasNext()) {
		            SpanningMetric sm = spanMetricsIt.next();
		            if(sm.getName().equals(metric1NameString) && sm.getSpanningColumns().containsAll(Arrays.asList(metric1SpanningColumns))) {
		                m1 = sm;
		            }
		        }
		    }
		    
		    if(metric1SpanningColumns == null) {
		        m2 = metricsOverlayModel.getMetricsForColumn(columnNames[1]).get(metric2NameString);
                    } else {
                        Iterator<SpanningMetric> spanMetricsIt = metricsOverlayModel.getSpanMetricsList().iterator();
                        while(spanMetricsIt.hasNext()) {
                            SpanningMetric sm = spanMetricsIt.next();
                            if(sm.getName().equals(metric2NameString) && sm.getSpanningColumns().containsAll(Arrays.asList(metric2SpanningColumns))) {
                                m2 = sm;
                            }
                        }
                    }
			
			if(columnNames[0].equals(columnNames[1])) {
				Metric mergedMetric = new Metric("MergedMetric", "Metric merged from " + m1.getName() + " and "
						+ m2.getName(), m1.getDataType(), m1.getConcat());
				for(EvalTuple et : m1.getEvalTuples()) {
					mergedMetric.addEvalTuple(et);
				}
				for(EvalTuple et : m2.getEvalTuples()) {
					mergedMetric.addEvalTuple(et);
				}
				metricsOverlayModel.getMetricsForColumn(columnNames[0]).put(mergedMetric.getName(), mergedMetric);
			} else {
				if (m1 != null && m2 != null) {
//					try {
//						Evaluable eval = MetaParser.parse("and(" + lowercaseEvaluableAndAddColumnInfo(
//											m1.getEvalTuples().get(0).eval, columnNames[0])
//										+ ", "
//										+ lowercaseEvaluableAndAddColumnInfo(
//											m1.getEvalTuples().get(0).eval, columnNames[1])
//										+ ")");
						SpanningMetric sm = new SpanningMetric("MergedMetric",
									"Metric merged from " + m1.getName() + " and " + m2.getName(), 
									Arrays.asList(columnNames));
//						sm.addSpanningEvalTuple(eval, "", "", false);
						if(m1.getDataType().equals(m2.getDataType())) {
							sm.setDataType(m1.getDataType());
						}
						List<String> spanningColumns = new ArrayList<String>();
						spanningColumns.addAll(Arrays.asList(columnNames));
						sm.setSpanningColumns(spanningColumns);
						
						for (int i = 0; i < m1.getEvalTuples().size(); ++i) {
							sm.addEvalTuple(m1.getEvalTuples().get(i));
						}
						for (int i = 0; i < m2.getEvalTuples().size(); ++i) {
							sm.addEvalTuple(m2.getEvalTuples().get(i));
						}
						metricsOverlayModel.addSpanningMetric(sm);
//					} catch (ParsingException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
				}
			}
			metricsOverlayModel.deleteMetric(columnNames[0], m1.getName());
			metricsOverlayModel.deleteMetric(columnNames[1], m2.getName());
		}
	}

	private String lowercaseEvaluableAndAddColumnInfo(Evaluable eval, String columnName) {
		char c[] = eval.toString().toCharArray();
    	c[0] = Character.toLowerCase(c[0]);
    	String lower = new String(c);
    	lower = lower.replace("value", "if(get(cells, \""+ columnName + "\") == null, '', get(cells, \""+ columnName + "\"))");
    	return lower;
	}
}
