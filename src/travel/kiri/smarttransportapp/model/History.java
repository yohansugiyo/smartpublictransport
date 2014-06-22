package travel.kiri.smarttransportapp.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import travel.kiri.smarttransportapp.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class History {
	
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
	
	public static class Item {
		public final String from;
		public final String to;
		public final ArrayList<String> adKeywords;
		public final Route result;
		public Item(String from, String to, ArrayList<String> adKeywords, Route result) {
			super();
			this.from = from;
			this.to = to;
			this.adKeywords = adKeywords;
			this.result = result;
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
