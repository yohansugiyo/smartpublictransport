package travel.kiri.smarttransportapp;

import java.util.ArrayList;
import java.util.List;

import travel.kiri.smarttransportapp.model.LocationFinder;
import travel.kiri.smarttransportapp.model.LocationUtilities;
import travel.kiri.smarttransportapp.model.Route;
import travel.kiri.smarttransportapp.model.StatisticCounter;
import travel.kiri.smarttransportapp.model.protocol.CicaheumLedengProtocol;
import travel.kiri.smarttransportapp.model.protocol.ImageResponseHandler;
import travel.kiri.smarttransportapp.model.protocol.MarkerOptionsResponseHandler;
import travel.kiri.smarttransportapp.view.SlidingUpPanelLayout;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class DirectionActivity extends ActionBarActivity implements
		OnClickListener, OnInfoWindowClickListener, OnItemClickListener,
		OnMarkerClickListener, ErrorReporter, LocationListener {

	public static final String EXTRA_ROUTE = "travel.kiri.smarttransportapp.intent.extra.route";
	public static final String EXTRA_ADKEYWORDS = "travel.kiri.smarttransportapp.intent.extra.adkeywords";
	public static final String EXTRA_DESTINATION = "travel.kiri.smarttransportapp.intent.extra.destination";
	public static final String EXTRA_FROM = "travel.kiri.smarttransportapp.intent.extra.from";

	public static final float DEFAULT_ZOOM = 12;
	public static final float FOCUS_ZOOM = 18;

	private static final int COLOR_VEHICLE = Color.rgb(0, 128, 0);
	private static final int COLOR_WALK = Color.rgb(255, 0, 0);

	private static float DEFAULT_POLYLINE_WIDTH = 3.0f;

	private static int BOUNDS_PADDING = 70;

	SlidingUpPanelLayout slidingUpLayout;

	private GoogleMap map;
	private Route route;
	private List<Marker> markers;
	/** The marker to focus, or null to show everything. */
	private Integer selectedMarker;
	private LatLngBounds allPointsBounds;

	private TextView tvSelectedStep;
	private View stepListView;
	private View mapView;
	private ImageButton previousImageButton;
	private ImageButton nextImageButton;
	private MenuItem toggleMapMenuItem;

	private CicaheumLedengProtocol request;
	private Resources resources;

	private LocationFinder locationFinder;

	private Location lastLocation = null;
	private String from;
	private String destination;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void setupActionBar() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
				&& getActionBar() != null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		List<String> adKeywords = null;
		Location adLocation = null;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_direction);

		setupActionBar();

		tvSelectedStep = (TextView) findViewById(R.id.tv_selected_step);
		stepListView = findViewById(R.id.steplistview);
		mapView = findViewById(R.id.mapview);
		previousImageButton = (ImageButton) findViewById(R.id.imageButtonPrevious);
		nextImageButton = (ImageButton) findViewById(R.id.imageButtonNext);
		slidingUpLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

		request = new CicaheumLedengProtocol(this, this);
		resources = getResources();
		locationFinder = LocationFinder.getInstance();
		locationFinder.addLocationListener(this);
		locationFinder.startLocationDetection();

		Intent intent = getIntent();
		route = intent.getParcelableExtra(EXTRA_ROUTE);
		destination = intent.getStringExtra(EXTRA_DESTINATION);
		from = intent.getStringExtra(EXTRA_FROM);
		adKeywords = intent.getStringArrayListExtra(EXTRA_ADKEYWORDS);

		// Setup list
		ListView stepListView = (ListView) findViewById(R.id.steplistview);
		stepListView.setAdapter(new RouteAdapter(this, route, this));
		stepListView.setOnItemClickListener(this);

		slidingUpLayout.setAnchorPoint(0.6F);

		// Initialize map
		map = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.mapfragment)).getMap();
		if (map != null) {
			map.setMyLocationEnabled(true);
			map.setLocationSource(locationFinder);
			map.setInfoWindowAdapter(new TextWrappedInfoWindowAdapter(
					LayoutInflater.from(this)));
			map.setOnInfoWindowClickListener(this);
			map.setOnMarkerClickListener(this);
			map.getUiSettings().setZoomControlsEnabled(false);

			previousImageButton.setOnClickListener(this);
			nextImageButton.setOnClickListener(this);

			markers = new ArrayList<Marker>();

			List<LatLng> allPoints = new ArrayList<LatLng>();
			if (route.steps.size() == 1
					&& route.steps.get(0).means
							.equals(CicaheumLedengProtocol.PROTO_MEANS_NONE)) {
				Toast toast = Toast.makeText(getApplicationContext(),
						R.string.route_not_found, Toast.LENGTH_LONG);
				toast.show();
			}
			for (int i = 0, iLength = route.steps.size(); i < iLength; i++) {
				final Route.Step step = route.steps.get(i);
				if (!step.means.equals(CicaheumLedengProtocol.PROTO_MEANS_NONE)) {
					PolylineOptions polyline = new PolylineOptions();
					if (step.means
							.equals(CicaheumLedengProtocol.PROTO_MEANS_WALK)) {
						polyline.color(COLOR_WALK);
					} else {
						polyline.color(COLOR_VEHICLE);
					}
					polyline.width(DEFAULT_POLYLINE_WIDTH);
					polyline.addAll(step.path);
					map.addPolyline(polyline);
				}
				allPoints.addAll(step.path);
				// Request for marker, starts with default marker.
				MarkerOptions initialMarker = new MarkerOptions()
						.position(step.path.get(0))
						.title(String.format(
								resources.getString(R.string.step), i + 1))
						.snippet(step.description);
				markers.add(map.addMarker(initialMarker));
				final int markerIndex = i;
				MarkerOptionsResponseHandler markerResponseHandler = new MarkerOptionsResponseHandler() {
					@Override
					public void markerOptionsReady(MarkerOptions markerOptions) {
						// Replace marker with the one just downloaded.
						markers.get(markerIndex).remove();
						markers.set(markerIndex, map.addMarker(markerOptions));
					}
				};
				// Create markers.
				if (i == 0) {
					request.getStartMarker(initialMarker, markerResponseHandler);
				} else {
					request.getStepMarker(step, initialMarker,
							markerResponseHandler);
				}
				if (i == iLength - 1) {
					initialMarker = new MarkerOptions().position(
							step.path.get(step.path.size() - 1)).title(
							destination);
					markers.add(map.addMarker(initialMarker));
					request.getFinishMarker(initialMarker,
							new MarkerOptionsResponseHandler() {
								@Override
								public void markerOptionsReady(
										MarkerOptions markerOptions) {
									markers.get(markerIndex + 1).remove();
									map.addMarker(markerOptions);
								}
							});
					// Set last point for ad location
					LatLng lastPoint = step.path.get(step.path.size() - 1);
					adLocation = LocationUtilities.createLocation(
							(float) lastPoint.latitude,
							(float) lastPoint.longitude);
				}
			}
			// Set initial location: current location
			Location location = locationFinder.getCurrentLocation();
			if (location != null) {
				map.moveCamera(CameraUpdateFactory.newLatLngZoom(
						LocationUtilities.convertToLatLng(location),
						DEFAULT_ZOOM));
			}

			allPointsBounds = LocationUtilities.detectBounds(allPoints);

			map.setOnCameraChangeListener(new OnCameraChangeListener() {
				@Override
				public void onCameraChange(CameraPosition arg0) {
					selectedMarker = null;
					focusOnSelectedMarker(null);
					map.setOnCameraChangeListener(null);
				}
			});
		}

		// Lastly, setup the admob
		// AdView adView = (AdView) findViewById(R.id.admob);
		// AdRequest.Builder builder = new AdRequest.Builder();
		// for (String keyword: adKeywords) {
		// builder.addKeyword(keyword);
		// }
		// if (adLocation != null) {
		// builder.setLocation(adLocation);
		// }
		// adView.loadAd(builder.build());
	}

	@Override
	public void onDestroy() {
		// When the activity is finished, restore the interval value to normal
		// and stop locating.
		locationFinder.removeLocationListener(this);
		locationFinder.stopLocationDetection();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_direction, menu);
		// toggleMapMenuItem = menu.findItem(R.id.menu_togglemap);
		return true;
	}

	@Override
	public void onClick(View sender) {
		if (sender == previousImageButton) {
			Integer lastSelectedMarker = selectedMarker;
			if (selectedMarker == null) {
				selectedMarker = markers.size() - 1;
			} else if (selectedMarker == 0) {
				selectedMarker = null;
			} else {
				selectedMarker--;
			}
			focusOnSelectedMarker(lastSelectedMarker);
		} else if (sender == nextImageButton) {
			Integer lastSelectedMarker = selectedMarker;
			if (selectedMarker == null) {
				selectedMarker = 0;
			} else if (selectedMarker == markers.size() - 1) {
				selectedMarker = null;
			} else {
				selectedMarker++;
			}
			focusOnSelectedMarker(lastSelectedMarker);
		}
	}

	/**
	 * Zooms in to the {@link #selectedMarker}, or show all if null. Requires
	 * map to be fully drawn.
	 */
	private void focusOnSelectedMarker(Integer lastSelectedMarker) {
		if (map != null) {
			if (lastSelectedMarker != null) {
				markers.get(lastSelectedMarker).hideInfoWindow();
			}
			CameraUpdate cameraUpdate;
			if (selectedMarker == null) {
				cameraUpdate = CameraUpdateFactory.newLatLngBounds(
						allPointsBounds, BOUNDS_PADDING);
				initSelectedStep(null, null);
			} else {
				cameraUpdate = CameraUpdateFactory.newLatLngZoom(
						markers.get(selectedMarker).getPosition(), FOCUS_ZOOM);
				markers.get(selectedMarker).showInfoWindow();
				initSelectedStep(markers.get(selectedMarker).getTitle(),
						markers.get(selectedMarker).getSnippet());
			}
			map.animateCamera(cameraUpdate);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_previous:
			return true;
		case R.id.menu_next:
			return true;
			// case R.id.menu_togglemap:
			// toggleMapAndList();
			// return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void toggleMapAndList() {
		if (stepListView.getVisibility() == View.VISIBLE) {
			mapView.setVisibility(View.VISIBLE);
			stepListView.setVisibility(View.GONE);
			toggleMapMenuItem.setIcon(R.drawable.ic_action_view_as_list);
			if (map == null) {
				Toast toast = Toast.makeText(getApplicationContext(),
						R.string.map_not_available, Toast.LENGTH_LONG);
				toast.show();
			}
		} else {
			mapView.setVisibility(View.GONE);
			stepListView.setVisibility(View.VISIBLE);
			toggleMapMenuItem.setIcon(R.drawable.ic_action_map);
		}
	}

	@Override
	public void reportError(Object source, Throwable tr) {
		Toast toast = Toast.makeText(getApplicationContext(),
				R.string.connection_error, Toast.LENGTH_LONG);
		toast.show();
		Log.e(source.getClass().toString(), tr.getMessage(), tr);
	}

	/**
	 * Custom info window adapter to show all snippet text.
	 * 
	 * @author nathan
	 * 
	 */
	private static class TextWrappedInfoWindowAdapter implements
			InfoWindowAdapter {
		private LayoutInflater inflater;

		TextWrappedInfoWindowAdapter(LayoutInflater inflater) {
			this.inflater = inflater;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			return null;
		}

		@Override
		public View getInfoContents(Marker marker) {
			View popup = inflater.inflate(R.layout.view_infowindow, null);
			TextView textViewTitle = (TextView) popup.findViewById(R.id.title);
			TextView textViewSnippet = (TextView) popup
					.findViewById(R.id.snippet);
			if (marker.getTitle() == null)
				textViewTitle.setVisibility(View.GONE);
			else
				textViewTitle.setText(marker.getTitle());
			if (marker.getSnippet() == null)
				textViewSnippet.setVisibility(View.GONE);
			else
				textViewSnippet.setText(marker.getSnippet());
			return (popup);
		}
	}

	/**
	 * The adapter for this class route steps list.
	 * 
	 * @author pascal
	 * 
	 */
	private static class RouteAdapter extends BaseAdapter {

		private final Route route;
		private final LayoutInflater inflater;
		private final CicaheumLedengProtocol request;

		public RouteAdapter(Context context, Route route,
				ErrorReporter errorReporter) {
			this.inflater = LayoutInflater.from(context);
			this.route = route;
			this.request = new CicaheumLedengProtocol(context, errorReporter);
		}

		@Override
		public int getCount() {
			return route.steps.size();
		}

		@Override
		public Object getItem(int position) {
			return route.steps.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Route.Step step = route.steps.get(position);
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.view_route_step_item,
						null);
			}
			final ImageView stepIcon = (ImageView) convertView
					.findViewById(R.id.stepIcon);
			request.getStepImage(step, CicaheumLedengProtocol.MODIFIER_ICON,
					new ImageResponseHandler() {
						@Override
						public void imageReceived(Bitmap bitmap) {
							stepIcon.setImageBitmap(bitmap);
						}
					});
			TextView stepDescription = (TextView) convertView
					.findViewById(R.id.stepDescription);
			stepDescription.setText(step.description);
			return convertView;
		}

	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		// Switch to list if any of info window is clicked.
		// toggleMapAndList();
		// TODO set text
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int position,
			long id) {
		// Switch to map if any of the item list is clicked.
		// toggleMapAndList();
		// TODO set marker popup
		markers.get(position).showInfoWindow();
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		int index = markers.indexOf(marker);
		if (index != -1) {
			selectedMarker = index;
		}
		initSelectedStep(marker.getTitle(), marker.getSnippet());
		return false;
	}

	private void initSelectedStep(String txt1, String txt2) {
		if (txt1 == null && txt2 == null) {
			tvSelectedStep.setText(String.format(
					getString(R.string.route_from_x_to_y), from, destination));
		} else {
			tvSelectedStep.setText(txt1 + ": " + txt2);
		}

	}

	@Override
	public void onLocationChanged(Location currentLocation) {
		if (lastLocation != null) {
			float distance = lastLocation.distanceTo(currentLocation);
			StatisticCounter.getInstance().addTotalDistance(distance);
			// TODO just a debug
			Toast toast = Toast.makeText(this, "distance=" + distance,
					Toast.LENGTH_SHORT);
			toast.show();
		}
		lastLocation = currentLocation;
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// void
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// void
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// void
	}

	public static void startThisActivity(Context ctx, String from,
			String destination, ArrayList<String> adKeyword, Route route) {
		Intent intent = new Intent(ctx, DirectionActivity.class);
		intent.putExtra(EXTRA_FROM, from);
		intent.putExtra(EXTRA_DESTINATION, destination);
		intent.putExtra(EXTRA_ADKEYWORDS, adKeyword);
		intent.putExtra(EXTRA_ROUTE, route);
		ctx.startActivity(intent);
	}
}
