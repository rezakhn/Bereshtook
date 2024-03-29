package ir.blackgrape.bereshtook.service;

import ir.blackgrape.bereshtook.IXMPPRosterCallback;
import ir.blackgrape.bereshtook.MainWindow;
import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.data.RosterProvider;
import ir.blackgrape.bereshtook.exceptions.BereshtookXMPPException;
import ir.blackgrape.bereshtook.game.GameWindow;
import ir.blackgrape.bereshtook.util.ConnectionState;
import ir.blackgrape.bereshtook.util.PRIVATE_DATA;
import ir.blackgrape.bereshtook.util.StatusMode;
import ir.blackgrape.bereshtook.util.StringUtil;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jivesoftware.smackx.packet.DefaultPrivateData;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

public class XMPPService extends GenericService {

	private AtomicBoolean mConnectionDemanded = new AtomicBoolean(false); // should we try to reconnect?
	private static final int RECONNECT_AFTER = 5;
	private static final int RECONNECT_MAXIMUM = 10*60;
	private static final String RECONNECT_ALARM = "ir.blackgrape.bereshtook.RECONNECT_ALARM";
	private int mReconnectTimeout = RECONNECT_AFTER;
	private String mReconnectInfo = "";
	private Intent mAlarmIntent = new Intent(RECONNECT_ALARM);
	private PendingIntent mPAlarmIntent;
	private BroadcastReceiver mAlarmReceiver = new ReconnectAlarmReceiver();

	private ServiceNotification mServiceNotification = null;

	private Smackable mSmackable;
	private boolean create_account = false;
	private IXMPPRosterService.Stub mService2RosterConnection;
	private IXMPPChatService.Stub mServiceChatConnection;
	private IXMPPDataService.Stub mServiceDataConnection;

	private RemoteCallbackList<IXMPPRosterCallback> mRosterCallbacks = new RemoteCallbackList<IXMPPRosterCallback>();
	private HashSet<String> mIsBoundTo = new HashSet<String>();
	private Handler mMainHandler = new Handler();

	@Override
	public IBinder onBind(Intent intent) {
		super.onBind(intent);
		String action = intent.getAction();
		String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			resetNotificationCounter(chatPartner);
			mIsBoundTo.add(chatPartner);
			return mServiceChatConnection;
		}
		else if(action.equals("ir.blackgrape.bereshtook.XMPPSERVICE2")){
			return mServiceDataConnection;
		}

		return mService2RosterConnection;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			mIsBoundTo.add(chatPartner);
			resetNotificationCounter(chatPartner);
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			mIsBoundTo.remove(chatPartner);
		}
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		createServiceRosterStub();
		createServiceChatStub();
		createServiceDataStub();

		mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
		registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));

		BereshtookBroadcastReceiver.initNetworkStatus(getApplicationContext());

		if (mConfig.autoConnect && mConfig.jid_configured) {
			/*
			 * start our own service so it remains in background even when
			 * unbound
			 */
			Intent xmppServiceIntent = new Intent(this, XMPPService.class);
			startService(xmppServiceIntent);
		}

		mServiceNotification = ServiceNotification.getInstance();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
		mRosterCallbacks.kill();
		if (mSmackable != null) {
		    manualDisconnect();
		    mSmackable.unRegisterCallback();
		}
		unregisterReceiver(mAlarmReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logInfo("onStartCommand(), mConnectionDemanded=" + mConnectionDemanded.get());
		if (intent != null) {
			create_account = intent.getBooleanExtra("create_account", false);
			
			if ("disconnect".equals(intent.getAction())) {
				failConnection(getString(R.string.conn_no_network));
				return START_STICKY;
			} else
			if ("reconnect".equals(intent.getAction())) {
				// TODO: integrate the following steps into one "RECONNECT"
				failConnection(getString(R.string.conn_no_network));
				// reset reconnection timeout
				mReconnectTimeout = RECONNECT_AFTER;
				doConnect();
				return START_STICKY;
			} else
			if ("ping".equals(intent.getAction())) {
				if (mSmackable != null) {
					mSmackable.sendServerPing();
					return START_STICKY;
				}
				// if not yet connected, fall through to doConnect()
			}
		}
		
		mConnectionDemanded.set(mConfig.autoConnect);
		doConnect();
		return START_STICKY;
	}

	private void createServiceChatStub() {
		mServiceChatConnection = new IXMPPChatService.Stub() {

			public void sendMessage(String user, String message)
					throws RemoteException {
				if (mSmackable != null)
					mSmackable.sendMessage(user, message);
				else
					SmackableImp.sendOfflineMessage(getContentResolver(),
							user, message);
			}

			public boolean isAuthenticated() throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.isAuthenticated();
				}

				return false;
			}
			
			public void clearNotifications(String Jid) throws RemoteException {
				clearNotification(Jid);
			}
		};
	}

	private void createServiceRosterStub() {
		mService2RosterConnection = new IXMPPRosterService.Stub() {

			public void registerRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null)
					mRosterCallbacks.register(callback);
			}

			public void unregisterRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null)
					mRosterCallbacks.unregister(callback);
			}

			public int getConnectionState() throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.getConnectionState().ordinal();
				} else {
					return ConnectionState.OFFLINE.ordinal();
				}
			}

			public String getConnectionStateString() throws RemoteException {
				return XMPPService.this.getConnectionStateString();
			}


			public void setStatusFromConfig()
					throws RemoteException {
				if (mSmackable != null) { // this should always be true, but stil...
					mSmackable.setStatusFromConfig();
					//updateServiceNotification();
				}
			}

			public void addRosterItem(String user, String alias, String group)
					throws RemoteException {
				try {
					mSmackable.addRosterItem(user, alias, group);
				} catch (BereshtookXMPPException e) {
					shortToastNotify(e);
				}
			}

			public void addRosterGroup(String group) throws RemoteException {
				mSmackable.addRosterGroup(group);
			}

			public void removeRosterItem(String user) throws RemoteException {
				try {
					mSmackable.removeRosterItem(user);
				} catch (BereshtookXMPPException e) {
					shortToastNotify(e);
				}
			}

			public void moveRosterItemToGroup(String user, String group)
					throws RemoteException {
				try {
					mSmackable.moveRosterItemToGroup(user, group);
				} catch (BereshtookXMPPException e) {
					shortToastNotify(e);
				}
			}

			public void renameRosterItem(String user, String newName)
					throws RemoteException {
				try {
					mSmackable.renameRosterItem(user, newName);
				} catch (BereshtookXMPPException e) {
					shortToastNotify(e);
				}
			}

			public void renameRosterGroup(String group, String newGroup)
					throws RemoteException {
				mSmackable.renameRosterGroup(group, newGroup);
			}

			public void disconnect() throws RemoteException {
				manualDisconnect();
			}

			public void connect() throws RemoteException {
				mConnectionDemanded.set(true);
				mReconnectTimeout = RECONNECT_AFTER;
				doConnect();
			}

			public void sendPresenceRequest(String jid, String type)
					throws RemoteException {
				mSmackable.sendPresenceRequest(jid, type);
			}

		};
	}
	
	private void createServiceDataStub(){
		mServiceDataConnection = new IXMPPDataService.Stub() {
			
			@Override
			public void saveData(String key, String value){
				if(mSmackable != null && mSmackable.isAuthenticated()){
					DefaultPrivateData privateData = mSmackable.loadPrivateData(PRIVATE_DATA.GAME, PRIVATE_DATA.DOMAIN);
					privateData.setValue(key, value);
					mSmackable.savePrivateData(privateData);
				}
			}
			
			@Override
			public String loadData(String key){
				if(mSmackable != null && mSmackable.isAuthenticated()){
					DefaultPrivateData privateData = mSmackable.loadPrivateData(PRIVATE_DATA.GAME, PRIVATE_DATA.DOMAIN);
					if(privateData != null)
						return privateData.getValue(key);
				}
				return null;
			}
		};
	}

	private String getConnectionStateString() {
		StringBuilder sb = new StringBuilder();
		if (mSmackable != null && mSmackable.getLastError() != null) {
			sb.append(mReconnectInfo);
			sb.append("\n");
			if(mSmackable.getLastError().equals("SASL authentication failed using mechanism DIGEST-MD5"))
				sb.append(getString(R.string.conn_authentication_fail));
			else if(mSmackable.getLastError().equals("conflict(409)"))
				sb.append(getString(R.string.conn_conflict));
			else{
				sb.append(mSmackable.getLastError());
			}
		}
		else
			sb.append(mReconnectInfo);
		return sb.toString();
	}

	public String getStatusTitle(ConnectionState cs) {
		if (cs != ConnectionState.ONLINE)
			return mReconnectInfo;
		String status = getString(StatusMode.fromString(mConfig.statusMode).getTextId());

		if (mConfig.statusMessage.length() > 0) {
			status = status + " (" + mConfig.statusMessage + ")";
		}

		return status;
	}

	private void updateServiceNotification() {
		ConnectionState cs = ConnectionState.OFFLINE;
		if (mSmackable != null) {
			cs = mSmackable.getConnectionState();
		}

		// HACK to trigger show-offline when XEP-0198 reconnect is going on
		getContentResolver().notifyChange(RosterProvider.CONTENT_URI, null);
		getContentResolver().notifyChange(RosterProvider.GROUPS_URI, null);
		// end-of-HACK

		broadcastConnectionState(cs);

		// do not show notification if not a foreground service
		if (!mConfig.foregroundService)
			return;
		// no need for non-removable notification
		if(mConfig.foregroundService)
			return;

		if (cs == ConnectionState.OFFLINE) {
			mServiceNotification.hideNotification(this, SERVICE_NOTIFICATION);
			return;
		}
		Notification n = new Notification(R.drawable.ic_offline2, null,
				System.currentTimeMillis());
		n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;

		Intent notificationIntent = new Intent(this, MainWindow.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		n.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		if (cs == ConnectionState.ONLINE)
			n.icon = R.drawable.ic_online2;

		String title = getString(R.string.conn_title, mConfig.jabberID);
		String message = getStatusTitle(cs);
		n.setLatestEventInfo(this, title, message, n.contentIntent);

		mServiceNotification.showNotification(this, SERVICE_NOTIFICATION,
				n);
	}

	private void doConnect() {
		mReconnectInfo = getString(R.string.conn_connecting);
		updateServiceNotification();
		if (mSmackable == null) {
			createAdapter();
		}

		mSmackable.requestConnectionState(ConnectionState.ONLINE, create_account);
	}

	private void broadcastConnectionState(ConnectionState cs) {
		final int broadCastItems = mRosterCallbacks.beginBroadcast();

		for (int i = 0; i < broadCastItems; i++) {
			try {
				mRosterCallbacks.getBroadcastItem(i).connectionStateChanged(cs.ordinal());
			} catch (RemoteException e) {
				logError("caught RemoteException: " + e.getMessage());
			}
		}
		mRosterCallbacks.finishBroadcast();
	}

	private NetworkInfo getNetworkInfo() {
		Context ctx = getApplicationContext();
		ConnectivityManager connMgr =
				(ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		return connMgr.getActiveNetworkInfo();
	}
	private boolean networkConnected() {
		NetworkInfo info = getNetworkInfo();

		return info != null && info.isConnected();
	}
	private boolean networkConnectedOrConnecting() {
		NetworkInfo info = getNetworkInfo();

		return info != null && info.isConnectedOrConnecting();
	}

	// call this when Android tells us to shut down
	private void failConnection(String reason) {
		logInfo("failConnection: " + reason);
		mReconnectInfo = reason;
		updateServiceNotification();
		if (mSmackable != null)
			mSmackable.requestConnectionState(ConnectionState.DISCONNECTED);
	}

	// called from Smackable when connection broke down
	private void connectionFailed(String reason) {
		logInfo("connectionFailed: " + reason);
		//TODO: error message from downstream?
		//mLastConnectionError = reason;
		if (!networkConnected()) {
			mReconnectInfo = getString(R.string.conn_no_network);
			mSmackable.requestConnectionState(ConnectionState.RECONNECT_NETWORK);

		} else if (mConnectionDemanded.get()) {
				mReconnectInfo = getString(R.string.conn_reconnect, StringUtil.convertToPersian(Integer.valueOf(mReconnectTimeout).toString()));
				mSmackable.requestConnectionState(ConnectionState.RECONNECT_DELAYED);
				logInfo("connectionFailed(): registering reconnect in " + mReconnectTimeout + "s");
				((AlarmManager)getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP,
						System.currentTimeMillis() + mReconnectTimeout * 1000, mPAlarmIntent);
				mReconnectTimeout = mReconnectTimeout * 2;
				if (mReconnectTimeout > RECONNECT_MAXIMUM)
					mReconnectTimeout = RECONNECT_MAXIMUM;
		} 
		else {
			connectionClosed();
		}

	}

	private void connectionClosed() {
		logInfo("connectionClosed.");
		mReconnectInfo = "";
		mServiceNotification.hideNotification(this, SERVICE_NOTIFICATION);
	}

	public void manualDisconnect() {
		mConnectionDemanded.set(false);
		mReconnectInfo = getString(R.string.conn_disconnecting);
		performDisconnect();
	}

	public void performDisconnect() {
		if (mSmackable != null) {
			// this is non-blocking
			mSmackable.requestConnectionState(ConnectionState.OFFLINE);
		}
	}

	private void createAdapter() {
		System.setProperty("smack.debugEnabled", "" + mConfig.smackdebug);
		try {
			mSmackable = new SmackableImp(mConfig, getContentResolver(), this);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		mSmackable.registerCallback(new XMPPServiceCallback() {
			public void newMessage(String from, String message, boolean silent_notification) {
				logInfo("notification: " + from);
				String name = mSmackable.getNameForJID(from);
				if(!message.startsWith(GameWindow.GAME_CODE))
					notifyClient(from, name, message, !mIsBoundTo.contains(from), silent_notification, false);
				else if(message.endsWith(GameWindow.INVITE_CODE) || message.endsWith(GameWindow.ACCEPT_CODE) || message.endsWith(GameWindow.DENY_CODE)){
					String notifMsg = null;
					if(message.endsWith(GameWindow.INVITE_CODE))
						notifMsg = getString(R.string.she_invited_to_invite);
					else if(message.endsWith(GameWindow.ACCEPT_CODE))
						notifMsg = getString(R.string.she_accepted_invite);
					else if(message.endsWith(GameWindow.DENY_CODE))
						notifMsg = getString(R.string.she_denied_invite);
					
					notifyClient(from, name, notifMsg, !mIsBoundTo.contains(from), silent_notification, false);
					if(message.endsWith(GameWindow.ACCEPT_CODE))
						broadcastGameMsg(from, message);
				}
				else
					broadcastGameMsg(from, message);
			}

			private void broadcastGameMsg(String from, String message) {
				Intent i = new Intent();
				i.putExtra("from", from);
				i.putExtra("message", message);
				i.setAction("ir.blackgrape.bereshtook.NEWGAMEMSG");
				sendBroadcast(i);
			}

			public void messageError(final String from, final String error, final boolean silent_notification) {
				logInfo("error notification: " + from);
				mMainHandler.post(new Runnable() {
					public void run() {
						// work around Toast fallback for errors
						notifyClient(from, mSmackable.getNameForJID(from), error,
							!mIsBoundTo.contains(from), silent_notification, true);
					}});
				}

			public void rosterChanged() {
			}

			public void connectionStateChanged() {
				// TODO: OFFLINE is sometimes caused by XMPPConnection calling
				// connectionClosed() callback on an error, need to catch that?
				switch (mSmackable.getConnectionState()) {
				//case OFFLINE:
				case DISCONNECTED:
					connectionFailed(getString(R.string.conn_disconnected));
					updateServiceNotification();
					break;
				case ONLINE:
					mReconnectTimeout = RECONNECT_AFTER;
				default:
					updateServiceNotification();
				}
			}
		});
	}

	private class ReconnectAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			logInfo("Alarm received.");
			if (!mConnectionDemanded.get()) {
				return;
			}
			if (mSmackable != null && mSmackable.getConnectionState() == ConnectionState.ONLINE) {
				logError("Reconnect attempt aborted: we are connected again!");
				return;
			}
			doConnect();
		}
	}
}
