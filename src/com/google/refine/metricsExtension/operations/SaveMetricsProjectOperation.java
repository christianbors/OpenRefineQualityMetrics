package com.google.refine.metricsExtension.operations;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.history.HistoryEntry;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;

public class SaveMetricsProjectOperation extends AbstractOperation{

	public SaveMetricsProjectOperation(MetricsOverlayModel model) {
		
	}
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected HistoryEntry createHistoryEntry(Project project,
			long historyEntryID) throws Exception {
		// TODO Auto-generated method stub
		return super.createHistoryEntry(project, historyEntryID);
	}

	@Override
	protected String getBriefDescription(Project project) {
		// TODO Auto-generated method stub
		return super.getBriefDescription(project);
	}

}
