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
				.get(MetricsOverlayModel.OVERLAY_NAME);
		return new PersistMetricsOperation(overlayModel);
	}
	
	public PersistMetricsOperation(MetricsOverlayModel overlayModel) {
		metricsOverlayModel = overlayModel;
	}
	
	@Override
	protected String getBriefDescription(Project project) {
		return "Persist Metrics";
	}
	
	@Override
	protected HistoryEntry createHistoryEntry(Project project,
			long historyEntryID) throws Exception {
		Change metricsProjectChange = new MetricsExtensionOperation.MetricsProjectChange(metricsOverlayModel);

		return new HistoryEntry(historyEntryID, project,
				getBriefDescription(project), this,
				metricsProjectChange);
	}
}
