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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
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

	public static final String BROACAST_START = "MockLocationService:Start";
	public static final String BROACAST_STOP = "MockLocationService:STOP";
	public static final String BROACAST_RESTART = "MockLocationService:Retart";

	private static final String LOCATION_PROVIDER_NAME = LocationManager.GPS_PROVIDER;

	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	private boolean running = false;
	private boolean loop = false;
	private File fileToLoad = null;

	private static final int notificationID = MockLocationService.class.hashCode();

	private LocationManager mLocationManager;
	private NotificationManager mNotificationManager;

	private Worker worker = null;
	private BroadcastMessageReceiver broadCastReceiver;

	private JDOM jdom;

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
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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

		jdom = new JDOM();

		broadCastReceiver = new BroadcastMessageReceiver();

		IntentFilter filter = new IntentFilter();
		filter.addAction(BROACAST_START);
		filter.addAction(BROACAST_STOP);
		filter.addAction(BROACAST_RESTART);
		registerReceiver(broadCastReceiver, filter);

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

		if(worker != null)
			worker.cancel(true);

		unregisterReceiver(broadCastReceiver);

		Log.i(TAG, "Service Stopped.");
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return mMessenger.getBinder();
	}

	/************** Notifications Stuff **************/

	private void showNotificaton()
	{

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle(getResources().getString(R.string.app_name));
		mBuilder.setContentText(getResources().getString(R.string.app_name) + " is now running");
		mBuilder.setTicker(getResources().getString(R.string.app_name) + " is now running");
		mBuilder.setProgress(0, 0, true);

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		mBuilder.setContentIntent(pIntent);

		mBuilder.addAction(android.R.drawable.ic_delete, "Stop", PendingIntent.getBroadcast(this, 0, new Intent(BROACAST_STOP), 0));
		mBuilder.addAction(android.R.drawable.ic_popup_sync, "Restart", PendingIntent.getBroadcast(this, 0, new Intent(BROACAST_RESTART), 0));

		// notificationID allows you to update the notification later on.
		Notification notification = mBuilder.build();
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotificationManager.notify(notificationID, notification);
	}

	private void clearNotificaton()
	{
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle(getResources().getString(R.string.app_name));
		mBuilder.setContentText(getResources().getString(R.string.app_name) + " is stopped");
		mBuilder.setTicker(getResources().getString(R.string.app_name) + " has stopped");

		Intent intent = new Intent(this, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		mBuilder.setContentIntent(pIntent);

		mBuilder.addAction(android.R.drawable.ic_media_play, "Start", PendingIntent.getBroadcast(this, 0, new Intent(BROACAST_START), 0));

		mNotificationManager.notify(notificationID, mBuilder.build());
	}

	/************** Inner Classes **************/

	private class IncomingHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what) {

			case MSG_START:

				Bundle b = msg.getData();
				fileToLoad = new File(b.getString(BUNDLE_KEY_FILE));
				//TODO Should verify if file exists? but it's an internal service...

				loop = !b.getBoolean(BUNDLE_KEY_MODE);

			case MSG_RESTART:
				if(worker != null && worker.getStatus() == Status.RUNNING)
					worker.cancel(true);

				worker = new Worker();
				worker.execute();

				break;
			case MSG_STOP:

				if(worker != null && worker.getStatus() == Status.RUNNING)
					worker.cancel(true);

				break;
			case MSG_GET_STATE:

				int status = (running) ? R.string.activity_main_status_running : R.string.activity_main_status_stopped;

				Message response = Message.obtain(null, MockLocationService.MSG_GET_STATE, status, 0);
				if(running) {
					Bundle b2 = new Bundle();
					b2.putString(BUNDLE_KEY_FILE, fileToLoad.getAbsolutePath());
					b2.putBoolean(BUNDLE_KEY_MODE, !loop);
					response.setData(b2);
				}

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

	private class BroadcastMessageReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if(intent.getAction().equals(BROACAST_START) || intent.getAction().equals(BROACAST_RESTART)) {

				if(worker != null && worker.getStatus() == Status.RUNNING) {
					worker.cancel(true);
				}
				worker = new Worker();
				worker.execute();

			} else if(intent.getAction().equals(BROACAST_STOP)) {
				Toast.makeText(context, "Stop", Toast.LENGTH_SHORT).show();

				if(worker != null && worker.getStatus() == Status.RUNNING)
					worker.cancel(true);
			}
		}
	}

	private class Worker extends AsyncTask<Void, Void, Void>
	{

		@Override
		protected void onPostExecute(Void result)
		{
			Log.i(TAG, "Worker stopped executing...");
			clearNotificaton();

			((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
		}

		@Override
		protected void onPreExecute()
		{
			running = true;
			Log.i(TAG, "Worker starting...");
			showNotificaton();
		}

		@Override
		protected void onCancelled()
		{
			super.onCancelled();

			running = false;
			Log.i(TAG, "Worker cancelled...");
			clearNotificaton();
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			GPX gpx;
			Location loc = new Location(LOCATION_PROVIDER_NAME);

			try {
				gpx = jdom.parse(fileToLoad);
				Log.i(TAG, "File parsed");
			} catch (ParsingException e) {
				e.printStackTrace();
				Toast.makeText(getApplicationContext(), "bug no parse do ficheiro", Toast.LENGTH_SHORT).show();
				return null;
			}
			running = true;

			while(true) {

				for(Track t : gpx.getTracks()) {
					for(TrackSegment s : t.getSegments())
						for(Waypoint w : s.getWaypoints()) {

							if(isCancelled())
								return null;

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
							} catch (NoSuchMethodException e) {
								e.printStackTrace();
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
							}
						}
				}
				if(!loop)
					break;

				Log.i(TAG, "Restarting loop");
			}

			running = false;

			return null;
		}
	}
}
