package com.google.refine.metricsExtension.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.metricsExtension.model.MetricsOverlayModel;
import com.google.refine.metricsExtension.model.changes.MetricsExtensionChange;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.process.QuickHistoryEntryProcess;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class MetricsProjectOperation extends AbstractOperation {

	final protected MetricsOverlayModel metricsOverlayModel;
	
	static public AbstractOperation reconstruct(Project project, JSONObject object) throws Exception {
		return new MetricsProjectOperation(
	            MetricsOverlayModel.reconstruct(object.getJSONObject("metricsOverlayModel"))
	        );
	}

	public MetricsProjectOperation(MetricsOverlayModel model) {
		metricsOverlayModel = model;
	}
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected String getBriefDescription(Project project) {
		// TODO Auto-generated method stub
		return super.getBriefDescription(project);
	}

	@Override
	public Process createProcess(Project project, Properties options)
			throws Exception {
		// TODO Auto-generated method stub
		return super.createProcess(project, options);
	}

	static public class MetricsProjectChange implements Change {

		final protected MetricsOverlayModel _newMetricsOverlayModel;
        protected MetricsOverlayModel _oldMetricsOverlayModel;
		
		public MetricsProjectChange(MetricsOverlayModel newMetricsOverlayModel) {
			_newMetricsOverlayModel = newMetricsOverlayModel;
		}

		@Override
		public void apply(Project project) {
			synchronized (project) {
				_oldMetricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get("metricsOverlayModel");
                
                project.overlayModels.put("metricsOverlayModel", _newMetricsOverlayModel);
            }
		}

		@Override
		public void revert(Project project) {
			synchronized (project) {
                if (_oldMetricsOverlayModel == null) {
                    project.overlayModels.remove("metricsOverlayModel");
                } else {
                    project.overlayModels.put("metricsOverlayModel", _oldMetricsOverlayModel);
                }
            }
		}

		@Override
		public void save(Writer writer, Properties options) throws IOException {
			writer.write("newMetricsOverlayModel="); writeMetricsOverlay(_newMetricsOverlayModel, writer); writer.write('\n');
            writer.write("oldMetricsOverlayModel="); writeMetricsOverlay(_oldMetricsOverlayModel, writer); writer.write('\n');
            writer.write("/ec/\n"); // end of change marker
		}

		static public Change load(LineNumberReader reader, Pool pool) throws Exception {
            MetricsOverlayModel oldMetricsOverlayModel = null;
            MetricsOverlayModel newMetricsOverlayModel = null;
            
            String line;
            while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
                int equal = line.indexOf('=');
                CharSequence field = line.subSequence(0, equal);
                String value = line.substring(equal + 1);
                
                if ("oldMetricsOverlayModel".equals(field) && value.length() > 0) {
                	oldMetricsOverlayModel = MetricsOverlayModel.reconstruct(ParsingUtilities.evaluateJsonStringToObject(value));
                } else if ("newMetricsOverlayModel".equals(field) && value.length() > 0) {
                	newMetricsOverlayModel = MetricsOverlayModel.reconstruct(ParsingUtilities.evaluateJsonStringToObject(value));
                }
            }
            
            MetricsProjectChange change = new MetricsProjectChange(newMetricsOverlayModel);
            change._oldMetricsOverlayModel = oldMetricsOverlayModel;
            
            return change;
        }
		
		static protected void writeMetricsOverlay(MetricsOverlayModel metricsOverlayModel, Writer writer) {
			if (metricsOverlayModel != null) {
                JSONWriter jsonWriter = new JSONWriter(writer);
                try {
                	metricsOverlayModel.write(jsonWriter, new Properties());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
		}
		
	}
}
