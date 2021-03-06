package dk.dda.ddieditor.spss.view;

import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerItem;

public class StateMarkerField extends MarkerField {
	public static final String DDI_STATE = "ddi_state";

	@Override
	public String getValue(MarkerItem item) {
		return item.getAttributeValue(DDI_STATE, "NA");
	}
}
