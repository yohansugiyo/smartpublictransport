package travel.kiri.smarttransportapp.model;

import travel.kiri.smarttransportapp.R;
import android.content.Context;
import android.location.Location;

public class SelectFromMapPoint extends Point {
	final Context context;
	final Location location;
	
	public SelectFromMapPoint(Context context, Location location) {
		this.context = context;
		this.location = location;
	}
	
	@Override
	public String getEditTextRepresentation() {
		return context.getResources().getString(R.string.selected_on_map);
	}

	@Override
	public Location getLocation() throws NullPointerException {
		return location;
	}

	@Override
	public boolean isEditable() {
		return false;
	}
}
