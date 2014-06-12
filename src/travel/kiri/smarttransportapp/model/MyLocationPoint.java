package travel.kiri.smarttransportapp.model;

import travel.kiri.smarttransportapp.R;
import android.content.Context;
import android.location.Location;

public class MyLocationPoint extends Point {

	final LocationFinder locationFinder;
	final Context context;
	
	public MyLocationPoint(Context context) {
		locationFinder = LocationFinder.getInstance();
		locationFinder.startLocationDetection();
		this.context = context;
	}
	
	@Override
	public String getEditTextRepresentation() {
		return context.getResources().getString(R.string.my_location);
	}

	@Override
	public Location getLocation() throws NullPointerException {
		Location location = locationFinder.getCurrentLocation();
		if (location == null) {
			throw new NullPointerException(context.getResources().getString(R.string.gps_not_ready));
		}
		return location;
	}

	@Override
	public boolean isEditable() {
		return false;
	} 
}
