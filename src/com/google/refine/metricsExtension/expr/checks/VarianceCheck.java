package com.google.refine.metricsExtension.expr.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.util.StatisticsUtils;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

public class VarianceCheck implements QualityCheck {

	@Override
	public Object call(Properties bindings, Object[] args) {
		Project project = (Project) bindings.get("project");
		Engine engine = new Engine(project);
		FilteredRows filteredRows = engine.getAllFilteredRows();
		List<Float> values = new ArrayList<Float>();
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		try {
			filteredRows.accept(project, StatisticsUtils.createAggregateRowVisitor(project, project.columnModel.getColumnIndexByName((String) args[0]), stats, values));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stats.getVariance();
	}

	@Override
	public String getDescription() {
		return "Returns the variance estimation for the provided column";
	}

	@Override
	public Evaluable getEvaluable(String[] params) throws ParsingException {
		String eval = "variance(";
		Iterator<String> paramIt;
		if (params != null) {
			paramIt = Arrays.asList(params).iterator();
		} else {
			throw new ParsingException("no column parameter provided");
		}
		while(paramIt.hasNext()) {
			eval += ", \"" + paramIt.next() + "\"";
		}
		eval += ")";
		return MetaParser.parse(eval);
	}

	@Override
	public String getParams() {
		return("columnname");
	}

	@Override
	public String getReturns() {
		return "variance in column type format (float, long, double)";
	}

}
