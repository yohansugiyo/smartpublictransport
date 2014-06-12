package travel.kiri.smarttransportapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import travel.kiri.smarttransportapp.model.StatisticCounter;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SettingsActivity extends ActionBarActivity {

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void setupActionBar() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getActionBar() != null) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		

		// Construct settings items
		Resources resources = getResources();
		List<Map<String, String>> items = new ArrayList<Map<String, String>>();
		final String FIRST = "first", SECOND = "second";
		Map<String, String> item = new HashMap<String, String>(2);
        item.put(FIRST, resources.getString(R.string.subscription));
        item.put(SECOND, resources.getString(R.string.pay_subscription));
        items.add(item);
        item = new HashMap<String, String>(2);
        item.put(FIRST, resources.getString(R.string.about));
        item.put(SECOND, resources.getString(R.string.knowmore));
        items.add(item);
        item = new HashMap<String, String>(2);
        item.put(FIRST, String.format(resources.getString(R.string._km_tracked), 0.001 * StatisticCounter.getInstance().getTotalDistance()));
        item.put(SECOND, resources.getString(R.string.share_to_friend));
        items.add(item);
        
		SimpleAdapter adapter = new SimpleAdapter(this, items,
                android.R.layout.simple_list_item_2, 
                new String[] {FIRST, SECOND }, 
                new int[] {android.R.id.text1, android.R.id.text2 });
		
		ListView settingsListView = (ListView) findViewById(R.id.settingsListView);
		settingsListView.setAdapter(adapter);
		
		setupActionBar();
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
