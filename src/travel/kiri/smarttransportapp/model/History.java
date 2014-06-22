package travel.kiri.smarttransportapp.model;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;

import travel.kiri.smarttransportapp.R;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class History {
	
	public static final String PREF_HISTORY = "travel.kiri.smarttransportapp.model.History";
	
	public static final int CAPACITY = 3;
	
	private LinkedList<Item> items;
	
	public History() {
		this.items = new LinkedList<History.Item>();
	}
	
	public void addToHistory(Item newItem) {
		items.addFirst(newItem);
		while (items.size() > CAPACITY) {
			items.removeLast();
		}
	}
	
	public List<Item> getHistory() {
		return items;
	}
	
	/**
	 * @deprecated Multiple problems in this method:
	 * perhaps too slow to load/save, too big to store in SharedPreferences,
	 * and currently still buggy that only the first item is saved.
	 * @param activity
	 */
	public void saveToPersistence(Activity activity) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			SharedPreferences statisticsSaver = activity.getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = statisticsSaver.edit();
			editor.putString(PREF_HISTORY, toJSONString());
			editor.commit();
		}
	}
	
	/**
	 * @deprecated Multiple problems in this method:
	 * perhaps too slow to load/save, too big to store in SharedPreferences,
	 * and currently still buggy that only the first item is saved.
	 * @param activity
	 */
	public void loadFromPersistence(Activity activity) {
		SharedPreferences statisticsSaver = activity.getPreferences(Context.MODE_PRIVATE);
		String jsonHistory = statisticsSaver.getString(PREF_HISTORY, null);
		if (jsonHistory != null) {
			fromJSONString(jsonHistory);
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public String toJSONString() {
		StringWriter stringWriter = new StringWriter();
		JsonWriter json = new JsonWriter(stringWriter);
		
		try {
			json.beginArray();
			for (Item item: items) {
				json.beginObject();
				json.name("from").value(item.from);
				json.name("to").value(item.to);
				JsonWriter jsonAdKeywords = json.name("adKeywords");
				jsonAdKeywords.beginArray();
				for (String adKeyword: item.adKeywords) {
					jsonAdKeywords.value(adKeyword);
				}
				jsonAdKeywords.endArray();
				JsonWriter jsonResult = json.name("result");
				jsonResult.beginObject();
				jsonResult.name("travelTime").value(item.result.travelTime);
				JsonWriter jsonSteps = jsonResult.name("steps");
				jsonSteps.beginArray();
				for (Route.Step step: item.result.steps) {
					jsonSteps.beginObject();
					jsonSteps.name("means").value(step.means);
					jsonSteps.name("meansDetail").value(step.meansDetail);
					jsonSteps.name("description").value(step.description);
					JsonWriter jsonPath = jsonSteps.name("path");
					jsonPath.beginArray();
					for (LatLng latLng: step.path) {
						jsonPath.value(LocationUtilities.locationToString(LocationUtilities.createLocation((float)latLng.latitude, (float)latLng.longitude)));
					}
					jsonPath.endArray();
					jsonSteps.endObject();
				}
				jsonSteps.endArray();
				jsonResult.endObject();

				json.endObject();
			}
			json.endArray();
			json.close();
		} catch (IOException e) {
			return null;
		}
		return stringWriter.toString();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void fromJSONString(String input) {
		LinkedList<Item> tempItems = new LinkedList<History.Item>();
		JsonReader json = new JsonReader(new StringReader(input));
		
		try {
			json.beginArray();
			while (json.hasNext()) {
				json.beginObject();
				Item newItem = new Item();
				while (json.hasNext()) {
					String name = json.nextName();
					if (name.equals("from")) {
						newItem.from = json.nextString();
					} else if (name.equals("to")) {
						newItem.to = json.nextString();
					} else if (name.equals("adKeywords")) {
						json.beginArray();
						newItem.adKeywords = new ArrayList<String>();
						while (json.hasNext()) {
							newItem.adKeywords.add(json.nextString());
						}
						json.endArray();
					} else if (name.equals("result")) {
						json.beginObject();
						newItem.result = new Route();
						while (json.hasNext()) {
							String name2 = json.nextName();
							if (name2.equals("travelTime")) {
								newItem.result.travelTime = json.nextString();
							} else if (name2.equals("steps")) {
								json.beginArray();
								newItem.result.steps = new ArrayList<Route.Step>();
								while (json.hasNext()) {
									json.beginObject();
									Route.Step newStep = new Route.Step();
									while (json.hasNext()) {
										String name3 = json.nextName();
										if (name3.equals("means")) {
											newStep.means = json.nextString();
										} else if (name3.equals("meansDetail")) {
											newStep.meansDetail = json.nextString();
										} else if (name3.equals("description")) {
											newStep.description = json.nextString();
										} else if (name3.equals("path")) {
											json.beginArray();
											newStep.path = new ArrayList<LatLng>();
											while (json.hasNext()) {
												newStep.path.add(LocationUtilities.convertToLatLng(LocationUtilities.createLocation(json.nextString())));
											}
											json.endArray();
										}
									}
									newItem.result.steps.add(newStep);
									json.endObject();
								}
								json.endArray();
							}
						}
						json.endObject();
					}
				}
				tempItems.add(newItem);
			}
			items = tempItems;
		} catch (IOException e) {
			// void
		}
	}	
	
	public static class Item {
		public String from;
		public String to;
		public ArrayList<String> adKeywords;
		public Route result;
		public Item(String from, String to, ArrayList<String> adKeywords, Route result) {
			super();
			this.from = from;
			this.to = to;
			this.adKeywords = adKeywords;
			this.result = result;
		}
		public Item() {
			// void
		}
	}
	
	public static class Adapter extends BaseAdapter {
		
		LayoutInflater inflater;
		Context context;
		History history;
		public Adapter(Context context, History history) {
			inflater = LayoutInflater.from(context);
			this.context = context;
			this.history = history;
		}

		@Override
		public int getCount() {
			return history.items.size();
		}

		@Override
		public Object getItem(int position) {
			return history.items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
			TextView tv = (TextView) v.findViewById(android.R.id.text1);
			String label = history.items.get(position).from
					+ context.getResources().getString(R.string._to_)
					+ history.items.get(position).to;
			tv.setText(label);
			tv.setContentDescription(label);
			return v;
		}
	}
}
