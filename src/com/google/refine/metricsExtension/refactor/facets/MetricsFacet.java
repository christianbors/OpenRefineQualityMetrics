
package com.google.refine.metricsExtension.refactor.facets;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.facets.Facet;
import com.google.refine.browsing.facets.NominalFacetChoice;
import com.google.refine.browsing.filters.AllRowsRecordFilter;
import com.google.refine.browsing.filters.AnyRowRecordFilter;
import com.google.refine.browsing.filters.ExpressionEqualRowFilter;
import com.google.refine.browsing.util.ExpressionNominalValueGrouper;
import com.google.refine.browsing.util.RowEvaluable;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.metricsExtension.model.Metric;
import com.google.refine.metricsExtension.operations.metricEvaluables.Completeness;
import com.google.refine.metricsExtension.refactor.filters.MetricRowFilter;
import com.google.refine.metricsExtension.refactor.util.MetricFacetChoice;
import com.google.refine.metricsExtension.refactor.util.MetricsEvaluator;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

public class MetricsFacet implements Facet {

    protected String columnName;
    protected boolean invert;

    protected Metric[] metrics;
    protected boolean update = false;

    /*
     * Derived configuration
     */
    protected int cellIndex;
    protected Evaluable eval;
    protected String errorMessage;

    protected List<MetricFacetChoice> selection = new LinkedList<MetricFacetChoice>();
    protected List<MetricFacetChoice> choices = new LinkedList<MetricFacetChoice>();

    protected int spuriousCount;

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("metrics").array();
        for (Metric entry : metrics) {
            entry.write(writer, options);
        }
        writer.endArray();

        writer.key("updateOnChange").value(update);
        writer.key("invert").value(invert);

        writer.endObject();
    }

    @Override
    public void initializeFromJSON(Project project, JSONObject o)
            throws JSONException {
        // TODO I think we don't need to store metrics, for now.
        // we'll have to re-calculate them anyway
//        JSONArray metricsArray = o.getJSONArray("metrics");
//        for (int i = 0; i < metricsArray.length(); ++i) {
//            JSONObject metricObj = metricsArray.getJSONObject(i);
//            switch (metricObj.getString("metric")) {
//            case "completeness":
//                metrics[i] = new Completeness().initializeFromJSON(project, metricObj);
//                break;
//            default:
//                throw new JSONException("Metric not recognized");
//            }
//        }
        metrics = new Metric[1];

        columnName = o.getString("columnName");
        invert = o.has("invert") && o.getBoolean("invert");
        
        if (columnName.length() > 0) {
            Column column = project.columnModel.getColumnByName(columnName);
            if (column != null) {
                cellIndex = column.getCellIndex();
            } else {
                errorMessage = "No column named " + columnName;
            }
        } else {
            cellIndex = -1;
        }

        try {
            eval = MetaParser.parse("value");
        } catch (ParsingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public RowFilter getRowFilter(Project project) {
        return (selection.size() == 0) ? null : new MetricRowFilter(metrics, columnName, cellIndex, true, true,
                false);
    }

    @Override
    public RecordFilter getRecordFilter(Project project) {
        RowFilter rowFilter = getRowFilter(project);
        return rowFilter == null ? null : new AnyRowRecordFilter(rowFilter);
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
        // TODO this needs work
        // TODO currently we are only obtaining the metrics for one column, not
        // the overall table
        Column column = project.columnModel.getColumnByCellIndex(cellIndex);

        MetricsEvaluator evaluator = new MetricsEvaluator(metrics, columnName, cellIndex);
        // TODO insert row/record visitor

        filteredRows.accept(project, evaluator);

        postProcessEvaluator(evaluator);
    }

    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        MetricsEvaluator evaluator = new MetricsEvaluator(metrics, columnName, cellIndex);
        // TODO insert row/record visitor

        filteredRecords.accept(project, evaluator);

        postProcessEvaluator(evaluator);
    }

    protected void postProcessEvaluator(MetricsEvaluator grouper) {
        // choices.clear();
        // choices.addAll(grouper.choices.values());
        //
        // for (MetricFacetChoice metric : selection) {
        // String valueString = choice.decoratedValue.value.toString();
        //
        // if (grouper.choices.containsKey(valueString)) {
        // grouper.choices.get(valueString).selected = true;
        // } else {
        // /*
        // * A selected choice can have zero count if it is selected
        // * together with other choices, and some other facets'
        // * constraints eliminate all rows projected to this choice
        // * altogether. For example, if you select both "car" and
        // * "bicycle" in the "type of vehicle" facet, and then constrain
        // * the "wheels" facet to more than 2, then the "bicycle" choice
        // * now has zero count even if it's still selected. The grouper
        // * won't be able to detect the "bicycle" choice, so we need to
        // * inject that choice into the choice list ourselves.
        // */
        // choice.count = 0;
        // choices.add(choice);
        // }
        // }
        //
        // spuriousCount = grouper.spuriousCount;
    }

    protected Object[] createMatches() {
        // Object[] a = new Object[_selection.size()];
        // for (int i = 0; i < a.length; i++) {
        // a[i] = _selection.get(i).decoratedValue.value;
        // }
        // return a;
        return null;
    }

}
