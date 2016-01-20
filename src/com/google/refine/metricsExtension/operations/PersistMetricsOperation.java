package com.google.refine.metricsExtension.operations;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;

public class PersistMetricsOperation extends AbstractOperation {

	final protected MetricsOverlayModel metricsOverlayModel;
	
	static public AbstractOperation reconstruct(Project project,
			JSONObject object) throws Exception {
		MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels
				.get("metricsOverlayModel");
		return new PersistMetricsOperation(overlayModel);
	}
	
	public PersistMetricsOperation(MetricsOverlayModel overlayModel) {
		metricsOverlayModel = overlayModel;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
		writer.key("op").value(
				OperationRegistry.s_opClassToName.get(this.getClass()));
		writer.key("description").value(getBriefDescription(null));
		if (metricsOverlayModel != null) {
			writer.key("model");
			metricsOverlayModel.write(writer, options);
		}
		writer.endObject();
	}
	
	@Override
	protected String getBriefDescription(Project project) {
		return "Persist Metrics";
	}
	
	@Override
	protected HistoryEntry createHistoryEntry(Project project,
			long historyEntryID) throws Exception {
		Change metricsProjectChange = new MetricsProjectChange(metricsOverlayModel);

		return new HistoryEntry(historyEntryID, project,
				getBriefDescription(project), this,
				metricsProjectChange);
	}
}
