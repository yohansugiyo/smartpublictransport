package travel.kiri.smarttransportapp;

import travel.kiri.smarttransportapp.model.LocationFinder;
import travel.kiri.smarttransportapp.model.LocationUtilities;
import travel.kiri.smarttransportapp.model.protocol.CicaheumLedengProtocol;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class SelectOnMapActivity extends FragmentActivity implements 
OnMapClickListener, OnMarkerClickListener, OnInfoWindowClickListener, LocationListener {

	public static final String EXTRA_ENDPOINT_TYPE = "travel.kiri.smarttravelapp.intent.extra.endpointtype";
	public static final String EXTRA_LOCATION = "travel.kiri.smarttravelapp.intent.extra.location";
	public static final float DEFAULT_ZOOM = 12;
	
	private GoogleMap map;
	private LocationFinder locationFinder;
	private Resources resources;
	/** One of {@link CicaheumLedengProtocol#PROTO_START} or {@link CicaheumLedengProtocol#PROTO_FINISH}. */ 
	private String endPointType;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void setupActionBar() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getActionBar() != null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selectonmap);
	
		locationFinder = LocationFinder.getInstance();
		resources = getResources();
		
		// Action bar setup
		setupActionBar();
		
		Intent intent = getIntent();
		endPointType = intent.getStringExtra(EXTRA_ENDPOINT_TYPE);
		
		// Initialize Google Map
		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		if (map != null) {
			map.setMyLocationEnabled(true);
			map.setLocationSource(locationFinder);
			locationFinder.addLocationListener(this);
			
			map.setOnInfoWindowClickListener(this);
			map.setOnMapClickListener(this);
			map.setOnMarkerClickListener(this);
		}
	}
		
	public void onInfoWindowClick(Marker marker) {
		Intent returnIntent = new Intent();
		LatLng position = marker.getPosition();
		returnIntent.putExtra(EXTRA_ENDPOINT_TYPE, endPointType);
		returnIntent.putExtra(EXTRA_LOCATION, LocationUtilities.createLocation((float)position.latitude, (float)position.longitude));
		setResult(RESULT_OK, returnIntent);     
		finish();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return true;
	}
	
	@Override
	public void onMapClick(LatLng latlng) {
		MarkerOptions marker = new MarkerOptions().position(
				latlng);
		map.clear();
		if (endPointType.equals(CicaheumLedengProtocol.PROTO_START)) {
			marker.title(resources.getString(R.string.from_here));
		} else if (endPointType.equals(CicaheumLedengProtocol.PROTO_FINISH)) {
			marker.title(resources.getString(R.string.to_here));
		}
		map.addMarker(marker).showInfoWindow();
		map.moveCamera(CameraUpdateFactory.newLatLng(latlng));
	}

	@Override
	public void onLocationChanged(Location location) {
		CameraUpdate update = CameraUpdateFactory.newLatLngZoom(LocationUtilities.convertToLatLng(location), DEFAULT_ZOOM);
		map.moveCamera(update);
		locationFinder.removeLocationListener(this);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// void
	}

	@Override
	public void onProviderEnabled(String provider) {
		// void
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// void
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		if (marker.isInfoWindowShown()) {
			this.onInfoWindowClick(marker);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}