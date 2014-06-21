package travel.kiri.smarttransportapp;

import java.util.ArrayList;
import java.util.List;

import travel.kiri.smarttransportapp.model.City;
import travel.kiri.smarttransportapp.model.LocationFinder;
import travel.kiri.smarttransportapp.model.MyLocationPoint;
import travel.kiri.smarttransportapp.model.Place;
import travel.kiri.smarttransportapp.model.Point;
import travel.kiri.smarttransportapp.model.Route;
import travel.kiri.smarttransportapp.model.SelectFromMapPoint;
import travel.kiri.smarttransportapp.model.StatisticCounter;
import travel.kiri.smarttransportapp.model.TextQueryPoint;
import travel.kiri.smarttransportapp.model.protocol.CicaheumLedengProtocol;
import travel.kiri.smarttransportapp.model.protocol.FindRouteResponseHandler;
import travel.kiri.smarttransportapp.model.protocol.SearchPlaceResponseHandler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements OnClickListener,
		ErrorReporter, OnCancelListener, DialogInterface.OnClickListener,
		LocationListener {

	private static final String PREF_REGION = "region";

	private CicaheumLedengProtocol request;

	private static final int ENDPOINT_START = 0;
	private static final int ENDPOINT_FINISH = 1;
	private ImageButton[] endpointMyLocationButton = new ImageButton[2];
	private ImageButton[] endpointSelectOnMapButton = new ImageButton[2];
	private EditText[] endpointEditText = new EditText[2];
	private DialogInterface[] endpointDialog = new DialogInterface[2];
	private Point[] endpointPoint = new Point[2];
	private TextQueryPoint[] textQueryPoint = new TextQueryPoint[2];
	/** Text watcher that resets the text view to text query point. */
	private TextWatcher[] textQueryReverter = new TextWatcher[2];

	/** The single loading dialog instance for this activity. */
	private LoadingDialog loadingDialog;
	private TextView regionTextView;
	private Button findButton;

	/** Keeps track of place search request not completed. */
	private int pendingPlaceSearch;

	/** Stores history of textual searches, for ad customization. */
	private ArrayList<String> adKeywords;

	/**
	 * Determines whether a dialog box has been cancelled or not. In such case
	 * pending operations should not continue.
	 */
	private boolean cancelled;

	/** City as detected by the GPS. Null means undetected yet */
	private City cityDetected;
	/** City selected manually, null means not selected yet (from detection). */
	private City citySelected;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().hide();
		setContentView(R.layout.activity_main);

		LocationFinder.createInstance(this);
		StatisticCounter.createInstance(this);
		request = new CicaheumLedengProtocol(this, this);
		adKeywords = new ArrayList<String>();

		// Set up display metrics.
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		CicaheumLedengProtocol.setDisplayMetrics(metrics);

		loadingDialog = new LoadingDialog(this);
		loadingDialog.setOnCancelListener(this);
		findViewById(R.id.btn_settings).setOnClickListener(this);
		endpointMyLocationButton[ENDPOINT_START] = (ImageButton) findViewById(R.id.imageButtonGpsFrom);
		endpointMyLocationButton[ENDPOINT_FINISH] = (ImageButton) findViewById(R.id.imageButtonGpsTo);
		endpointSelectOnMapButton[ENDPOINT_START] = (ImageButton) findViewById(R.id.imageButtonMapPointFrom);
		endpointSelectOnMapButton[ENDPOINT_FINISH] = (ImageButton) findViewById(R.id.imageButtonMapPointTo);
		endpointEditText[ENDPOINT_START] = (EditText) findViewById(R.id.fromEditText);
		endpointEditText[ENDPOINT_FINISH] = (EditText) findViewById(R.id.toEditText);
		regionTextView = (TextView) findViewById(R.id.regionTextView);
		findButton = (Button) findViewById(R.id.findButton);

		for (int i = 0; i < endpointEditText.length; i++) {
			endpointMyLocationButton[i].setOnClickListener(this);
			endpointSelectOnMapButton[i].setOnClickListener(this);
			endpointPoint[i] = textQueryPoint[i] = new TextQueryPoint(
					endpointEditText[i]);
			final int index = i;
			textQueryReverter[i] = new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// void
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// void
				}

				@Override
				public void afterTextChanged(Editable s) {
					endpointEditText[index].removeTextChangedListener(this);
					endpointEditText[index].setSelectAllOnFocus(false);
					endpointPoint[index] = textQueryPoint[index];
					endpointPoint[index].reset();
				}
			};
		}
		LocationFinder locationFinder = LocationFinder.getInstance();
		locationFinder.startLocationDetection();
		locationFinder.addLocationListener(this);
		cityDetected = null;
		citySelected = City.getCityFromCode(getStringPreference(PREF_REGION));
		updateRegionTextView(citySelected);
		regionTextView.setOnClickListener(this);
		findButton.setOnClickListener(this);
		
		// Quick find
		this.onClick(endpointMyLocationButton[ENDPOINT_START]);
		endpointEditText[ENDPOINT_FINISH].requestFocus();
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// // Inflate the menu; this adds items to the action bar if it is present.
	// getMenuInflater().inflate(R.menu.menu_main, menu);
	// return true;
	// }

	@Override
	public void onClick(View sender) {
		if (sender.getId() == R.id.btn_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
		} else {
			// Check if sender is one of endpoint buttons.
			for (int i = 0; i < endpointEditText.length; i++) {
				// Check for my location button
				if (sender == endpointMyLocationButton[i]) {
					endpointEditText[i]
							.removeTextChangedListener(textQueryReverter[i]);
					endpointPoint[i] = new MyLocationPoint(this);
					updateEditTextBehavior(i, endpointPoint[i]);
				}
				// Check for select on map button
				if (sender == endpointSelectOnMapButton[i]) {
					endpointEditText[i]
							.removeTextChangedListener(textQueryReverter[i]);
					Intent intent = new Intent(this, SelectOnMapActivity.class);
					intent.putExtra(
							SelectOnMapActivity.EXTRA_ENDPOINT_TYPE,
							i == ENDPOINT_START ? CicaheumLedengProtocol.PROTO_START
									: CicaheumLedengProtocol.PROTO_FINISH);
					startActivityForResult(intent, i);
				}
			}

			if (sender == findButton) {
				if (validateEmptyEditText(endpointEditText) == null) {
					cancelled = false;
					pendingPlaceSearch = 0;
					for (Point endpoint : endpointPoint) {
						if (endpoint instanceof TextQueryPoint) {
							final TextQueryPoint endpointCopy = (TextQueryPoint) endpoint;
							if (!adKeywords.contains(endpoint
									.getEditTextRepresentation())) {
								adKeywords.add(endpoint
										.getEditTextRepresentation());
							}
							String regionCode;
							if (citySelected != null) {
								regionCode = citySelected.code;
							} else {
								if (cityDetected != null) {
									regionCode = cityDetected.code;
								} else {
									Location lastLocation = LocationFinder
											.getInstance()
											.getLastKnownLocation();
									if (lastLocation == null) {
										regionCode = City.CITIES[0].code;
									} else {
										regionCode = City
												.findNearestCity(lastLocation).code;
									}
								}
								pendingPlaceSearch++;
								final String textQuery = endpoint
										.getEditTextRepresentation();
								request.searchPlace(
										endpoint.getEditTextRepresentation(),
										regionCode,
										new SearchPlaceResponseHandler() {
											@Override
											public void searchPlaceResponseReceived(
													List<Place> places,
													List<String> attributions) {
												if (places.size() == 0) {
													cancelled = true;
													loadingDialog.dismiss();
													Toast toast = Toast
															.makeText(
																	getApplicationContext(),
																	String.format(
																			getString(R.string._not_found),
																			textQuery),
																	Toast.LENGTH_LONG);
													toast.show();
												} else {
													// One place search has been
													// completed
													if (!cancelled) {
														pendingPlaceSearch--;
														endpointCopy
																.setPlaces(places);
														if (pendingPlaceSearch == 0) {
															loadingDialog
																	.dismiss();
															showPlaceOptionsPickDialog();
														}
													} else {
														loadingDialog.dismiss();
													}
												}
											}
										});
							}
						}
					}
					if (pendingPlaceSearch == 0) {
						getDirectionAndShowResult();
					} else {
						loadingDialog.show();
					}
				} else {
					Toast toast = Toast.makeText(getApplicationContext(),
							getString(R.string.fill_both_textboxes),
							Toast.LENGTH_SHORT);
					toast.show();
				}
			} else if (sender == regionTextView) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.city);
				final CitiesAdapter adapter = new CitiesAdapter(this);
				builder.setAdapter(adapter, new AlertDialog.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						Object selected = adapter.getItem(which);
						if (selected == null) {
							cityDetected = null;
							citySelected = null;
							updateRegionTextView(null);
							saveStringPreference(PREF_REGION, null);

						} else {
							loadingDialog.show();
						}

					}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		}
	}

	/**
	 * Updates the edit text behavior according to the point object. Will update
	 * the textbox and add corresponding edit event handler.
	 * 
	 * @param targetIndex
	 *            the index of the endpoint
	 * @param point
	 *            the point.
	 */
	private void updateEditTextBehavior(final int targetIndex, Point point) {
		// Set text once to remove the text change listener.
		endpointEditText[targetIndex]
				.setText(point.getEditTextRepresentation());
		endpointEditText[targetIndex].setSelectAllOnFocus(true);
		if (!point.isEditable()) {
			endpointEditText[targetIndex]
					.addTextChangedListener(textQueryReverter[targetIndex]);
		}
	}

	/**
	 * Gets the direction from server and show it in result when done. It will
	 * take from the internal {@link #endpointPoint}s attribute as parameter.
	 */
	private void getDirectionAndShowResult() {
		final Activity activity = this;
		final Point start = endpointPoint[ENDPOINT_START];
		final Point finish = endpointPoint[ENDPOINT_FINISH];
		try {
			request.findRoute(getResources().getString(R.string.iso639code),
					start.getLocation(), finish.getLocation(),
					new FindRouteResponseHandler() {
						@Override
						public void routeResponseReceived(Route route) {
							loadingDialog.dismiss();
							if (!cancelled) {
								resetUIState();
								Intent intent = new Intent(activity,
										DirectionActivity.class);
								intent.putExtra(
										DirectionActivity.EXTRA_DESTINATION,
										finish.getEditTextRepresentation());
								intent.putExtra(DirectionActivity.EXTRA_ROUTE,
										route);
								intent.putExtra(
										DirectionActivity.EXTRA_ADKEYWORDS,
										adKeywords);
								startActivity(intent);
							}
						}
					});
			loadingDialog.show();
		} catch (NullPointerException nfe) {
			Toast toast = Toast.makeText(getApplicationContext(),
					nfe.getMessage(), Toast.LENGTH_LONG);
			toast.show();
		}
	}

	@Override
	public void reportError(Object source, Throwable tr) {
		Toast toast = Toast.makeText(getApplicationContext(), tr.toString(),
				Toast.LENGTH_LONG);
		toast.show();
		Log.e(source.getClass().toString(), tr.getMessage(), tr);
		loadingDialog.cancel();
	}

	@Override
	protected void onDestroy() {
		LocationFinder.getInstance().stopLocationDetection();
		super.onDestroy();
	}

	/**
	 * Checks if one of the {@link #endpointPoint} is a text query and need to
	 * be hand picked by the user. Requires all place search query to be
	 * finished before calling this method. When all points has been hand
	 * picked, this method will trigger the {@link #getDirectionAndShowResult()}
	 * to request the directions.
	 */
	private void showPlaceOptionsPickDialog() {
		boolean ready = true;
		for (int i = 0; i < endpointPoint.length; i++) {
			Point endPoint = endpointPoint[i];
			if (endPoint instanceof TextQueryPoint
					&& endPoint.getLocation() == null) {
				ready = false;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(i == ENDPOINT_START ? R.string.from_
						: R.string.to_);
				builder.setItems(
						Place.getNames(((TextQueryPoint) endPoint).getPlaces()),
						this);
				builder.create();
				endpointDialog[i] = builder.show();
				break;
			}
		}
		if (ready) {
			getDirectionAndShowResult();
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// When one of the dialogs is touched, pick the place.
		for (int i = 0; i < endpointDialog.length; i++) {
			if (dialog == endpointDialog[i]) {
				endpointDialog[i] = null;
				((TextQueryPoint) endpointPoint[i]).pick(which);
				showPlaceOptionsPickDialog();
				break;
			}
		}
	}

	/**
	 * Resets the UI state, as if it's just started.
	 */
	private void resetUIState() {
		// By default, the endpoints are textual query.
		for (Point endpoint : endpointPoint) {
			if (endpoint != null) {
				endpoint.reset();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			Location location = (Location) data
					.getParcelableExtra(SelectOnMapActivity.EXTRA_LOCATION);
			endpointPoint[requestCode] = new SelectFromMapPoint(this, location);
			updateEditTextBehavior(requestCode, endpointPoint[requestCode]);
		}
	}

	/**
	 * Find at least one edit text with empty string
	 * 
	 * @param editTexts
	 *            the edit texts to check.
	 * @return the edit text who is empty, or null if all are filled.
	 */
	private static EditText validateEmptyEditText(EditText[] editTexts) {
		for (EditText editText : editTexts) {
			if (editText.getText().length() == 0) {
				return editText;
			}
		}
		return null;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (dialog == loadingDialog) {
			cancelled = true;
		}
	}

	/**
	 * Saves a string to permanent storage
	 * 
	 * @param key
	 *            the variable name
	 * @param value
	 *            the value to store or null to erase.
	 */
	private void saveStringPreference(String key, String value) {
		SharedPreferences settings = getSharedPreferences(this.getClass()
				.getName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		if (value == null) {
			editor.remove(key);
		} else {
			editor.putString(key, value);
		}
		editor.commit();
	}

	/**
	 * Retrieves String from permanent storage
	 * 
	 * @param key
	 *            the variable name
	 * @return the value, or null if not set before.
	 */
	private String getStringPreference(String key) {
		SharedPreferences settings = getSharedPreferences(this.getClass()
				.getName(), Context.MODE_PRIVATE);
		try {
			return settings.getString(key, null);
		} catch (ClassCastException cce) {
			// Fix for existing configuration
			saveStringPreference(key, null);
			return null;
		}
	}

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// switch (item.getItemId()) {
	// case R.id.menu_settings:
	// Intent intent = new Intent(this, SettingsActivity.class);
	// startActivity(intent);
	// return true;
	// default:
	// return super.onOptionsItemSelected(item);
	// }
	// }

	@Override
	public void onLocationChanged(Location location) {
		if (cityDetected == null) {
			cityDetected = City.findNearestCity(location);
			if (citySelected == null) {
				updateRegionTextView(cityDetected);
			}
		}
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

	private void updateRegionTextView(City city) {
		final String youAreInTemplate = getResources().getString(
				R.string.you_are_in);
		regionTextView
				.setText(Html.fromHtml(String.format(youAreInTemplate,
						city == null ? "..."
								: ("<a href=\"#\">" + city.name + "</a>"))));
	}
}
