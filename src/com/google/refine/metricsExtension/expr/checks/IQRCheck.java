package com.google.refine.metricsExtension.expr.checks;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.util.StatisticsUtils;
import com.google.refine.model.Project;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;

public class IQRCheck implements QualityCheck {

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
		return stats.getPercentile(75) - stats.getPercentile(25);
	}

	@Override
	public String getDescription() {
		return "Returns the inter-quartile range for the provided column";
	}

	@Override
	public Evaluable getEvaluable(String[] params) throws ParsingException {
		String eval = "iqr(";
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
		return "Inter-quartile Range (float, long, double)";
	}

}
