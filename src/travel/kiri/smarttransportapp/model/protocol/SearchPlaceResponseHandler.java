package travel.kiri.smarttransportapp.model.protocol;

import java.util.List;

import travel.kiri.smarttransportapp.model.Place;

public interface SearchPlaceResponseHandler {
	public void searchPlaceResponseReceived(List<Place> places, List<String> attributions);
}
