package travel.kiri.smarttransportapp.model;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

public class Route implements Parcelable {
	public String travelTime;
	public List<Route.Step> steps;

	public Route() {
		this.steps = new ArrayList<Route.Step>();
	}

	public Route(Parcel parcel) {
		this.steps = new ArrayList<Step>();
		travelTime = parcel.readString();
		parcel.readTypedList(this.steps, Step.CREATOR);
	}

	public static final Parcelable.Creator<Route> CREATOR = new Parcelable.Creator<Route>() {

		@Override
		public Route createFromParcel(Parcel source) {
			return new Route(source);
		}

		@Override
		public Route[] newArray(int size) {
			return new Route[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(travelTime);
		dest.writeTypedList(steps);
	}

	public static class Step implements Parcelable {
		public String means;
		public String meansDetail;
		public String description;
		/** Here we use {@link LatLng} instead of {@link Location} to save space. */
		public List<LatLng> path;

		public Step() {
			this.path = new ArrayList<LatLng>();
		}

		public Step(Parcel parcel) {
			this.path = new ArrayList<LatLng>();
			means = parcel.readString();
			meansDetail = parcel.readString();
			description = parcel.readString();
			parcel.readTypedList(path, LatLng.CREATOR);
		}

		public void addToPath(LatLng latlng) {
			path.add(latlng);
		}


		public static final Parcelable.Creator<Step> CREATOR = new Parcelable.Creator<Step>() {
			@Override
			public Step createFromParcel(Parcel source) {
				return new Step(source);
			}

			@Override
			public Step[] newArray(int size) {
				return new Step[size];
			}
		};

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(means);
			dest.writeString(meansDetail);
			dest.writeString(description);
			dest.writeTypedList(path);
		}
	}
}
