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
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class MetricsExtensionOperation extends AbstractOperation {

	final protected MetricsOverlayModel metricsOverlayModel;

	static public AbstractOperation reconstruct(Project project,
			JSONObject object) throws Exception {
		MetricsOverlayModel overlayModel = (MetricsOverlayModel) project.overlayModels
				.get(MetricsOverlayModel.OVERLAY_NAME);
		return new MetricsExtensionOperation(overlayModel);
	}

	public MetricsExtensionOperation(MetricsOverlayModel model) {
		metricsOverlayModel = model;
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
		return "Add Metrics Overlay Model";
	}

	@Override
	protected HistoryEntry createHistoryEntry(Project project,
			long historyEntryID) throws Exception {
        Change metricsProjectChange = new MetricsProjectChange(metricsOverlayModel);

		return new HistoryEntry(historyEntryID, project,
				getBriefDescription(project), this,
				metricsProjectChange);
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
				_oldMetricsOverlayModel = (MetricsOverlayModel) project.overlayModels.get(MetricsOverlayModel.OVERLAY_NAME);
                
                project.overlayModels.put(MetricsOverlayModel.OVERLAY_NAME, _newMetricsOverlayModel);
                project.getMetadata().setCustomMetadata("metricsProject", true);
            }
		}

		@Override
		public void revert(Project project) {
			synchronized (project) {
				if (_oldMetricsOverlayModel == null) {
					project.overlayModels.remove(MetricsOverlayModel.OVERLAY_NAME);
					project.getMetadata().setCustomMetadata("metricsProject", false);
				} else {
					project.overlayModels.put(MetricsOverlayModel.OVERLAY_NAME,
							_oldMetricsOverlayModel);
					project.getMetadata().setCustomMetadata("metricsProject", false);
				}
			}
		}

		@Override
		public void save(Writer writer, Properties options) throws IOException {
			writer.write("newMetricsOverlayModel=");
			writeMetricsOverlay(_newMetricsOverlayModel, writer);
			writer.write('\n');
			writer.write("oldMetricsOverlayModel=");
			writeMetricsOverlay(_oldMetricsOverlayModel, writer);
			writer.write('\n');
			writer.write("/ec/\n"); // end of change marker
		}

		static public Change load(LineNumberReader reader, Pool pool)
				throws Exception {
			MetricsOverlayModel oldMetricsOverlayModel = null;
			MetricsOverlayModel newMetricsOverlayModel = null;

			String line;
			while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
				int equal = line.indexOf('=');
				CharSequence field = line.subSequence(0, equal);
				String value = line.substring(equal + 1);

				if ("oldMetricsOverlayModel".equals(field)
						&& value.length() > 0) {
					oldMetricsOverlayModel = MetricsOverlayModel
							.reconstruct(ParsingUtilities
									.evaluateJsonStringToObject(value));
				} else if ("newMetricsOverlayModel".equals(field)
						&& value.length() > 0) {
					newMetricsOverlayModel = MetricsOverlayModel
							.reconstruct(ParsingUtilities
									.evaluateJsonStringToObject(value));
				}
			}

			MetricsProjectChange change = new MetricsProjectChange(
					newMetricsOverlayModel);
			change._oldMetricsOverlayModel = oldMetricsOverlayModel;

			return change;
		}

		static protected void writeMetricsOverlay(
				MetricsOverlayModel metricsOverlayModel, Writer writer) {
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
