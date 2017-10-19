package com.google.refine.metricsExtension.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.commands.Command;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.model.Metric.EvalTuple;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.Metric.Concatenation;
import com.google.refine.metricsExtension.model.SpanningMetric;
import com.google.refine.model.Project;

public class UpdateMetricCommand extends Command {

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		internalRespond(request, response);
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		internalRespond(request, response);
	}
	
	protected void internalRespond(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		Project project = getProject(request);
		MetricsOverlayModel model = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");

		Metric toBeEdited = null;
		try {
			String metricJSON = request.getParameter("metric");
			JSONObject metric = new JSONObject(metricJSON);
			String metricName = metric.getString("name");
			String metricDescription = metric.getString("description");
			String metricDatatype = metric.getString("datatype");
			String metricConcat = metric.getString("concat");

			String column = request.getParameter("column");

			JSONArray evalTuplesJSON = metric.getJSONArray("evalTuples");

			if (metric.has("spanningEvaluable")) {
				JSONArray colsJSON = new JSONArray(request.getParameter("columns"));
				ArrayList<String> cols = new ArrayList<String>();
				if (colsJSON != null) {
					int len = colsJSON.length();
					for (int i=0;i<len;i++){
						cols.add(colsJSON.get(i).toString());
					}
				}

				if (metricName.equalsIgnoreCase("uniqueness")) {
					toBeEdited = model.getUniqueness();
				} else {
					for (SpanningMetric spanningMetric : model.getSpanMetricsList()) {
						if (spanningMetric.getName().equals(metricName)
								&& spanningMetric.getSpanningColumns().size() == cols.size()
								&& spanningMetric.getSpanningColumns().containsAll(cols)) {
							toBeEdited = spanningMetric;
							break;
						}
					}
				}

				JSONObject metricSpanningEvaluable = metric.getJSONObject("spanningEvaluable");
				String spanningEvaluable = metricSpanningEvaluable.getString("evaluable");
				String spanningColumn = metricSpanningEvaluable.getString("column");
				String spanningComment = metricSpanningEvaluable.getString("comment");
				boolean spanningDisabled = metricSpanningEvaluable.getBoolean("disabled");

				if (toBeEdited != null) {
					EvalTuple et = ((SpanningMetric) toBeEdited).getSpanningEvaluable();
					et.eval = MetaParser.parse(spanningEvaluable);
					et.column = spanningColumn;
					et.comment = spanningComment;
					et.disabled = spanningDisabled;
				} else {
					throw new JSONException("metric not found");
				}
			} else {
				Map<String, Metric> colMetrics = model.getMetricsForColumn(column);
				toBeEdited = colMetrics.get(metricName);
			}
			toBeEdited.setName(metricName);
			toBeEdited.setConcat(Concatenation.valueOf(metricConcat));
			toBeEdited.setDataType(metricDatatype);
			toBeEdited.setDescription(metricDescription);

			if (evalTuplesJSON.length() > 0) {
				toBeEdited.getEvalTuples().clear();
				for (int i = 0; i < evalTuplesJSON.length(); ++i) {
					JSONObject tupleJSON = evalTuplesJSON.getJSONObject(i);
					toBeEdited.addEvalTuple(MetaParser.parse(tupleJSON.getString("evaluable")),
							tupleJSON.getString("column"),
							tupleJSON.getString("comment"),
							tupleJSON.getBoolean("disabled"));
				}
			}

			toBeEdited.getDirtyIndices().clear();
			toBeEdited.setMeasure(0f);

		} catch (JSONException e) {
			respondException(response, e);
			e.printStackTrace();
		} catch (ParsingException e) {
			respondException(response, e);
			e.printStackTrace();
		}

		try {
			ProjectManager.singleton.ensureProjectSaved(project.id);
			respondJSON(response, toBeEdited);
		} catch (JSONException e) {
			respondException(response, e);
			e.printStackTrace();
		}
	}
}
