package com.renatoalmeida.gpxmocklocations;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.divbyzero.gpx.GPX;
import net.divbyzero.gpx.Track;
import net.divbyzero.gpx.TrackSegment;
import net.divbyzero.gpx.Waypoint;
import net.divbyzero.gpx.parser.JDOM;
import net.divbyzero.gpx.parser.ParsingException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class MockLocationService extends Service
{
	private static final String TAG = MockLocationService.class.getSimpleName();

	/* Message definition */
	public static final int MSG_START = 0x50;
	public static final int MSG_STOP = 0x51;
	public static final int MSG_RESTART = 0x52;

	public static final int MSG_GET_STATE = 0x70;
	public static final int MSG_GET_FILE = 0x71;
	public static final int MSG_GET_MODE = 0x72;

	public static final String BUNDLE_KEY_FILE = "MockLocationService:File";
	public static final String BUNDLE_KEY_MODE = "MockLocationService:Mode";
	public static final String BUNDLE_KEY_STATE = "MockLocationService:State";

	private static final String LOCATION_PROVIDER_NAME = LocationManager.GPS_PROVIDER;

	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	private boolean running = false;
	private boolean loop = false;
	private File fileToLoad = null;

	private LocationManager mLocationManager;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		return START_STICKY;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		//Setup Test Provider
		mLocationManager.addTestProvider(LOCATION_PROVIDER_NAME, true, //requiresNetwork,
				false, // requiresSatellite,
				true, // requiresCell,
				false, // hasMonetaryCost,
				false, // supportsAltitude,
				false, // supportsSpeed,
				false, // upportsBearing,
				Criteria.POWER_MEDIUM, // powerRequirement
				Criteria.ACCURACY_FINE); // accuracy

		Log.i(TAG, "Service Started.");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		//Remove GPS Provider
		if(mLocationManager.getProvider(LOCATION_PROVIDER_NAME) != null) {
			mLocationManager.removeTestProvider(LOCATION_PROVIDER_NAME);
		}

		Log.i(TAG, "Service Stopped.");
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mMessenger.getBinder();
	}

	private class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what) {

			case MSG_START:

				Bundle b = msg.getData();
				fileToLoad = new File(b.getString(BUNDLE_KEY_FILE));
				//TODO Should verify if file exists? but it's internal service...

				loop = !b.getBoolean(BUNDLE_KEY_MODE);

				Toast.makeText(getApplicationContext(), "Start " + fileToLoad + " " + loop, Toast.LENGTH_SHORT).show();

				Thread t = new Thread() {

					@Override
					public void run()
					{
						JDOM j = new JDOM();

						try {
							GPX gpx = j.parse(fileToLoad);
							running = true;

							do {
								for(Track t : gpx.getTracks()) {
									for(TrackSegment s : t.getSegments())
										for(Waypoint w : s.getWaypoints()) {
											Location loc = new Location(LOCATION_PROVIDER_NAME);
											loc.setLatitude(w.getCoordinate().getLatitude());
											loc.setLongitude(w.getCoordinate().getLongitude());
											loc.setAltitude(w.getElevation());
											loc.setTime(System.currentTimeMillis() + 2000);
											loc.setAccuracy(1);

											//Some workaround to complete the Location object
											//Source http://jgrasstechtips.blogspot.pt/2012/12/android-incomplete-location-object.html
											try {
												Method locationJellyBeanFixMethod = Location.class.getMethod("makeComplete");

												if(locationJellyBeanFixMethod != null) {
													locationJellyBeanFixMethod.invoke(loc);
												}
											} catch (NoSuchMethodException e1) {
												e1.printStackTrace();
											} catch (IllegalArgumentException e) {
												e.printStackTrace();
											} catch (IllegalAccessException e) {
												e.printStackTrace();
											} catch (InvocationTargetException e) {
												e.printStackTrace();
											}

											Log.d("SendLocation", "Sending update for " + LOCATION_PROVIDER_NAME + " location:" + loc);

											mLocationManager.setTestProviderLocation(LOCATION_PROVIDER_NAME, loc);

											try {
												Thread.sleep(3000);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
										}
								}
							} while(loop);

							running = false;

						} catch (ParsingException e) {
							e.printStackTrace();
						}
					}
				};
				t.start();

				break;
			case MSG_STOP:
				Toast.makeText(getApplicationContext(), "Stop", Toast.LENGTH_SHORT).show();
				break;
			case MSG_RESTART:
				Toast.makeText(getApplicationContext(), "Restart", Toast.LENGTH_SHORT).show();
				break;
			case MSG_GET_STATE:
				Toast.makeText(getApplicationContext(), "Get State", Toast.LENGTH_SHORT).show();

				int status = (running) ? R.string.activity_main_status_running : R.string.activity_main_status_stopped;

				Message response = Message.obtain(null, MockLocationService.MSG_GET_STATE, status, 0);

				try {
					msg.replyTo.send(response);
				} catch (RemoteException e) {
					e.printStackTrace();
				}

				break;

			default:
				super.handleMessage(msg);
			}
		}
	}
}
