package travel.kiri.smarttransportapp.model.protocol;

import travel.kiri.smarttransportapp.model.Route;

public interface FindRouteResponseHandler {
	public void routeResponseReceived(Route route);
}
