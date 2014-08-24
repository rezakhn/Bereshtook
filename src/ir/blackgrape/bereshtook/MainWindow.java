package ir.blackgrape.bereshtook;

import ir.blackgrape.bereshtook.IXMPPRosterCallback.Stub;
import ir.blackgrape.bereshtook.data.BereshtookConfiguration;
import ir.blackgrape.bereshtook.data.ChatProvider;
import ir.blackgrape.bereshtook.data.ChatProvider.ChatConstants;
import ir.blackgrape.bereshtook.data.RosterProvider;
import ir.blackgrape.bereshtook.data.RosterProvider.RosterConstants;
import ir.blackgrape.bereshtook.dialogs.AddRosterItemDialog;
import ir.blackgrape.bereshtook.dialogs.FirstStartDialog;
import ir.blackgrape.bereshtook.game.GameBroadcastReceiver;
import ir.blackgrape.bereshtook.location.BestLocationListener;
import ir.blackgrape.bereshtook.location.BestLocationProvider;
import ir.blackgrape.bereshtook.location.BestLocationProvider.LocationType;
import ir.blackgrape.bereshtook.preferences.MainPrefs;
import ir.blackgrape.bereshtook.scoreboard.ScoreboardActivity;
import ir.blackgrape.bereshtook.service.IXMPPDataService;
import ir.blackgrape.bereshtook.service.IXMPPRosterService;
import ir.blackgrape.bereshtook.service.XMPPService;
import ir.blackgrape.bereshtook.shop.ShopActivity;
import ir.blackgrape.bereshtook.util.ConnectionState;
import ir.blackgrape.bereshtook.util.ConstantKeys;
import ir.blackgrape.bereshtook.util.CropOption;
import ir.blackgrape.bereshtook.util.CropOptionAdapter;
import ir.blackgrape.bereshtook.util.Decryptor;
import ir.blackgrape.bereshtook.util.PRIVATE_DATA;
import ir.blackgrape.bereshtook.util.PreferenceConstants;
import ir.blackgrape.bereshtook.util.SimpleCursorTreeAdapter;
import ir.blackgrape.bereshtook.util.StatusMode;
import ir.blackgrape.bereshtook.util.StatusUtil;
import ir.blackgrape.bereshtook.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockExpandableListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.Window;
import com.crashlytics.android.Crashlytics;
import com.nullwire.trace.ExceptionHandler;

public class MainWindow extends SherlockExpandableListActivity {

	private static final String TAG = "Bereshtook.MainWindow";

	private BereshtookConfiguration mConfig;

	private Handler mainHandler = new Handler();

	private Intent xmppServiceIntent;
	private ServiceConnection xmppServiceConnection;
	private XMPPRosterServiceAdapter serviceAdapter;
	private Stub rosterCallback;
	private RosterExpListAdapter rosterListAdapter;
	private TextView mConnectingText;

	private ContentObserver mRosterObserver = new RosterObserver();
	private ContentObserver mChatObserver = new ChatObserver();
	private HashMap<String, Boolean> mGroupsExpanded = new HashMap<String, Boolean>();

	private ActionBar actionBar;
	private String mTheme;

	private BestLocationProvider mBestLocationProvider;
	private BestLocationListener mBestLocationListener;
	private Location mLocation;

	private Intent dataServiceIntent;
	private ServiceConnection dataServiceConnection;
	private XMPPDataServiceAdapter dataServiceAdapter;

	private String androidId;

	private boolean isNewAccount = false;
	private boolean isStatusSet = false;
	private HashSet<String> groups = new HashSet<String>();
	private Drawable mAvatar;
	private Uri mImageCaptureUri;

	public void setIsNewAccount(boolean isNew) {
		isNewAccount = isNew;
	}

	enum COMMAND {
		FIND, INSERT
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, getString(R.string.version_name));
		mConfig = BereshtookApplication.getConfig(this);
		mTheme = mConfig.theme;
		androidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		setTheme(mConfig.getTheme());
		StatusUtil.setContext(getApplicationContext());
		GameBroadcastReceiver.setContext(getApplicationContext());
		super.onCreate(savedInstanceState);

		Crashlytics.start(this);
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		actionBar = getSupportActionBar();
		// no difference!!!
		// actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE,
		// ActionBar.DISPLAY_SHOW_TITLE);
		actionBar.setTitle(getString(R.string.me));
		actionBar.setHomeButtonEnabled(true);
		registerCrashReporter();

		if (!mConfig.jid_configured)
			checkUserAccounts();
		else if (!mConfig.accountPushed)
			pushNewAccount();

		getContentResolver().registerContentObserver(
				RosterProvider.CONTENT_URI, true, mRosterObserver);
		getContentResolver().registerContentObserver(ChatProvider.CONTENT_URI,
				true, mChatObserver);
		registerXMPPService();
		createUICallback();
		setupContenView();
		registerListAdapter();
		registerDataService();
		checkVersion();
	}

	private void checkVersion() {
		if (!mConfig.jid_configured)
			return;
		try {
			String serverURL = Decryptor.convert(ConstantKeys.URL_VERSION);
			VersionChecker vc = new VersionChecker();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				vc.executeOnExecutor(VersionChecker.THREAD_POOL_EXECUTOR,
						serverURL);
			else
				vc.execute(serverURL);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void checkUserAccounts() {
		try {
			if (isNetworkConnected()) {
				String serverURL = Decryptor.convert(ConstantKeys.URL_FIND)
						+ androidId;
				UserChecker df = new UserChecker();
				df.setCmd(COMMAND.FIND);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
					df.executeOnExecutor(UserChecker.THREAD_POOL_EXECUTOR,
							serverURL);
				else
					df.execute(serverURL);
			} else
				showToastNotification(R.string.no_internet_connection);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void pushNewAccount() {
		try {
			PackageManager manager = MainWindow.this.getPackageManager();
			PackageInfo info;
			info = manager.getPackageInfo(MainWindow.this.getPackageName(), 0);
			PreferenceManager
					.getDefaultSharedPreferences(MainWindow.this)
					.edit()
					.putString(PreferenceConstants.VERSION_NAME,
							info.versionName).commit();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		if (mConfig.versionName == null)
			mConfig.versionName = "-1";
		try {
			String serverURL = Decryptor.convert(ConstantKeys.URL_INSERT)
					+ androidId + "/" + mConfig.userName + "/"
					+ mConfig.password + "/" + mConfig.versionName;
			UserChecker df = new UserChecker();
			df.setCmd(COMMAND.INSERT);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				df.executeOnExecutor(UserChecker.THREAD_POOL_EXECUTOR,
						serverURL);
			else
				df.execute(serverURL);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null)
			return false;
		return true;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getContentResolver().unregisterContentObserver(mRosterObserver);
		getContentResolver().unregisterContentObserver(mChatObserver);
	}

	public int getStatusActionIcon() {
		boolean showOffline = !isConnected() || isConnecting()
				|| getStatusMode() == null;

		if (showOffline) {
			return StatusMode.offline.getDrawableId();
		}

		return getStatusMode().getDrawableId();
	}

	// need this to workaround unwanted OnGroupCollapse/Expand events
	boolean groupClicked = false;

	void handleGroupChange(int groupPosition, boolean isExpanded) {
		if (groupClicked) {
			try {
				String groupName = getGroupName(groupPosition);
				Log.d(TAG, "group status change: " + groupName + " -> "
						+ isExpanded);
				mGroupsExpanded.put(groupName, isExpanded);
			} catch (NullPointerException e) {
				// sometimes, it fails to obtain the cursor. We can ignore it
			}
			groupClicked = false;
		}
	}

	void setupContenView() {
		setContentView(R.layout.main);
		mConnectingText = (TextView) findViewById(R.id.error_view);
		registerForContextMenu(getExpandableListView());
		getExpandableListView().requestFocus();

		getExpandableListView().setOnGroupClickListener(
				new ExpandableListView.OnGroupClickListener() {
					public boolean onGroupClick(ExpandableListView parent,
							View v, int groupPosition, long id) {
						groupClicked = true;
						return false;
					}
				});
		getExpandableListView().setOnGroupCollapseListener(
				new ExpandableListView.OnGroupCollapseListener() {
					public void onGroupCollapse(int groupPosition) {
						handleGroupChange(groupPosition, false);
					}
				});
		getExpandableListView().setOnGroupExpandListener(
				new ExpandableListView.OnGroupExpandListener() {
					public void onGroupExpand(int groupPosition) {
						handleGroupChange(groupPosition, true);
					}
				});
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (serviceAdapter != null)
			serviceAdapter.unregisterUICallback(rosterCallback);

		// BereshtookApplication.getApp(this).mMTM.unbindDisplayActivity(this);
		unbindXMPPServices();
		storeExpandedState();

		initLocation();
		mBestLocationProvider.stopLocationUpdates();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mConfig.theme.equals(mTheme) == false) {
			// restart
			Intent restartIntent = new Intent(this, MainWindow.class);
			restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(restartIntent);
			finish();
		}
		displayOwnStatus();
		bindXMPPServices();

		initLocation();
		mBestLocationProvider
				.startLocationUpdatesWithListener(mBestLocationListener);

		updateStatus();
		// BereshtookApplication.getApp(this).mMTM.bindDisplayActivity(this);

		// handle SEND action
		handleSendIntent();
	}

	private void updateStatus() {
		String strStatus = getMyStatusMsg();
		if (isConnected() && strStatus.contains("S")
				&& (!isStatusSet || !strStatus.equals(mConfig.statusMessage))) {
			isStatusSet = true;
			PreferenceManager.getDefaultSharedPreferences(MainWindow.this)
					.edit()
					.putString(PreferenceConstants.STATUS_MESSAGE, strStatus)
					.commit();
			serviceAdapter.setStatusFromConfig();
		}
	}

	public void handleSendIntent() {
		Intent intent = getIntent();
		String action = intent.getAction();
		if ((action != null) && (action.equals(Intent.ACTION_SEND))) {
			showToastNotification(R.string.chooseContact);
			setTitle(R.string.chooseContact);
		}
	}

	public void handleJabberIntent() {
		Intent intent = getIntent();
		String action = intent.getAction();
		Uri data = intent.getData();
		if ((action != null) && (action.equals(Intent.ACTION_SENDTO))
				&& data != null && data.getHost().equals("jabber")) {
			String jid = data.getPathSegments().get(0);
			Log.d(TAG, "handleJabberIntent: " + jid);

			List<String[]> contacts = getRosterContacts();
			for (String[] c : contacts) {
				if (jid.equalsIgnoreCase(c[0])) {
					// found it
					startChatActivity(c[0], c[1], null);
					finish();
					return;
				}
			}
			// did not find in roster, try to add
			if (!addToRosterDialog(jid))
				finish();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "onConfigurationChanged");
		getExpandableListView().requestFocus();
	}

	private boolean isConnected() {
		return serviceAdapter != null && serviceAdapter.isAuthenticated();
	}

	private boolean isConnecting() {
		return serviceAdapter != null
				&& serviceAdapter.getConnectionState() == ConnectionState.CONNECTING;
	}

	public void updateRoster() {
		loadUnreadCounters();
		rosterListAdapter.requery();
		restoreGroupsExpanded();
	}

	private String getPackedItemRow(long packedPosition, String rowName) {
		int flatPosition = getExpandableListView().getFlatListPosition(
				packedPosition);
		Cursor c = (Cursor) getExpandableListView().getItemAtPosition(
				flatPosition);
		return c.getString(c.getColumnIndex(rowName));
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		ExpandableListView.ExpandableListContextMenuInfo info;

		try {
			info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			Log.e(TAG, "bad menuinfo: ", e);
			return;
		}

		long packedPosition = info.packedPosition;
		boolean isChild = isChild(packedPosition);

		// get the entry name for the item
		String menuName;
		if (isChild) {
			getMenuInflater().inflate(R.menu.roster_item_contextmenu, menu);
			menuName = String.format("%s",
					getPackedItemRow(packedPosition, RosterConstants.ALIAS));
		} else {
			menuName = getPackedItemRow(packedPosition, RosterConstants.GROUP);
			if (menuName.equals(""))
				return; // no options for default menu
			getMenuInflater().inflate(R.menu.roster_group_contextmenu, menu);
		}

		menu.setHeaderTitle(getString(R.string.roster_contextmenu_title,
				menuName));
	}

	void doMarkAllAsRead(final String JID) {
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);

		getContentResolver().update(
				ChatProvider.CONTENT_URI,
				values,
				ChatProvider.ChatConstants.JID + " = ? AND "
						+ ChatConstants.DIRECTION + " = "
						+ ChatConstants.INCOMING + " AND "
						+ ChatConstants.DELIVERY_STATUS + " = "
						+ ChatConstants.DS_NEW, new String[] { JID });
	}

	void removeChatHistory(final String JID) {
		getContentResolver().delete(ChatProvider.CONTENT_URI,
				ChatProvider.ChatConstants.JID + " = ?", new String[] { JID });
	}

	void removeChatHistoryDialog(final String JID, final String userName) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.deleteChatHistory_title)
				.setMessage(
						getString(R.string.deleteChatHistory_text, userName))
				.setPositiveButton(R.string.confirm,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								removeChatHistory(JID);
							}
						}).setNegativeButton(R.string.cancel, null).create()
				.show();
	}

	void removeRosterItemDialog(final String JID, final String userName) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.deleteRosterItem_title)
				.setMessage(
						getString(R.string.deleteRosterItem_text, userName, JID))
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								serviceAdapter.removeRosterItem(JID);
							}
						}).setNegativeButton(android.R.string.no, null)
				.create().show();
	}

	boolean addToRosterDialog(String jid) {
		if (serviceAdapter != null && serviceAdapter.isAuthenticated()) {
			new AddRosterItemDialog(this, serviceAdapter, jid).show();
			return true;
		} else {
			showToastNotification(R.string.Global_authenticate_first);
			return false;
		}
	}

	void rosterAddRequestedDialog(final String jid, String message) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.subscriptionRequest_title)
				.setMessage(
						getString(R.string.subscriptionRequest_text, jid,
								message))
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								serviceAdapter.sendPresenceRequest(jid,
										"subscribed");
								addToRosterDialog(jid);
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								serviceAdapter.sendPresenceRequest(jid,
										"unsubscribed");
							}
						}).create().show();
	}

	abstract class EditOk {
		abstract public void ok(String result);
	}

	void editTextDialog(int titleId, CharSequence message, String text,
			final EditOk ok) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.edittext_dialog,
				(ViewGroup) findViewById(R.id.layout_root));

		TextView messageView = (TextView) layout.findViewById(R.id.text);
		messageView.setText(message);
		final EditText input = (EditText) layout.findViewById(R.id.editText);
		input.setTransformationMethod(android.text.method.SingleLineTransformationMethod
				.getInstance());
		input.setText(text);
		new AlertDialog.Builder(this)
				.setTitle(titleId)
				.setView(layout)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								String newName = input.getText().toString();
								if (newName.length() != 0)
									ok.ok(newName);
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.create().show();
	}

	void renameRosterItemDialog(final String JID, final String userName) {
		editTextDialog(R.string.RenameEntry_title,
				getString(R.string.RenameEntry_summ, userName, JID), userName,
				new EditOk() {
					public void ok(String result) {
						serviceAdapter.renameRosterItem(JID, result);
					}
				});
	}

	void renameRosterGroupDialog(final String groupName) {
		editTextDialog(R.string.RenameGroup_title,
				getString(R.string.RenameGroup_summ, groupName), groupName,
				new EditOk() {
					public void ok(String result) {
						serviceAdapter.renameRosterGroup(groupName, result);
					}
				});
	}

	void moveRosterItemToFriendsGroup(final String jabberID) {
		serviceAdapter.moveRosterItemToGroup(jabberID,
				getString(R.string.friends_group));
		/*
		 * LayoutInflater inflater = (LayoutInflater)getSystemService(
		 * LAYOUT_INFLATER_SERVICE); View group =
		 * inflater.inflate(R.layout.moverosterentrytogroupview, null, false);
		 * final GroupNameView gv =
		 * (GroupNameView)group.findViewById(R.id.moverosterentrytogroupview_gv
		 * ); gv.setGroupList(getRosterGroups()); new AlertDialog.Builder(this)
		 * .setTitle(R.string.MoveRosterEntryToGroupDialog_title)
		 * .setView(group) .setPositiveButton(android.R.string.ok, new
		 * DialogInterface.OnClickListener() { public void
		 * onClick(DialogInterface dialog, int which) { Log.d(TAG, "new group: "
		 * + gv.getGroupName()); serviceAdapter.moveRosterItemToGroup(jabberID,
		 * gv.getGroupName()); } }) .setNegativeButton(android.R.string.cancel,
		 * null) .create().show();
		 */
	}

	void moveRosterItemToBereshtooksGroup(final String jabberID) {
		if (groups.size() == 2) {
			for (String groupName : groups) {
				if (!groupName.equals(getString(R.string.friends_group)))
					serviceAdapter.moveRosterItemToGroup(jabberID, groupName);
			}
		}
	}

	public boolean onContextItemSelected(MenuItem item) {
		return applyMenuContextChoice(item);
	}

	private boolean applyMenuContextChoice(MenuItem item) {

		ExpandableListContextMenuInfo contextMenuInfo = (ExpandableListContextMenuInfo) item
				.getMenuInfo();
		long packedPosition = contextMenuInfo.packedPosition;

		if (isChild(packedPosition)) {

			String userJid = getPackedItemRow(packedPosition,
					RosterConstants.JID);
			String userName = getPackedItemRow(packedPosition,
					RosterConstants.ALIAS);
			Log.d(TAG, "action for contact " + userName + "/" + userJid);

			int itemID = item.getItemId();

			switch (itemID) {
			case R.id.roster_contextmenu_contact_open_chat:
				startChatActivity(userJid, userName, null);
				return true;

			case R.id.roster_contextmenu_contact_mark_all_as_read:
				doMarkAllAsRead(userJid);
				return true;

			case R.id.roster_contextmenu_contact_delmsg:
				removeChatHistoryDialog(userJid, userName);
				return true;

			case R.id.roster_contextmenu_contact_delete:
				if (!isConnected()) {
					showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				removeRosterItemDialog(userJid, userName);
				return true;

			case R.id.roster_contextmenu_contact_rename:
				if (!isConnected()) {
					showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				renameRosterItemDialog(userJid, userName);
				return true;

			case R.id.roster_contextmenu_contact_request_auth:
				if (!isConnected()) {
					showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				serviceAdapter.sendPresenceRequest(userJid, "subscribe");
				return true;

			case R.id.roster_contextmenu_move_to_friends:
				if (!isConnected()) {
					showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				moveRosterItemToFriendsGroup(userJid);
				return true;

			case R.id.roster_contextmenu_move_to_bereshtooks:
				if (!isConnected()) {
					showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				moveRosterItemToBereshtooksGroup(userJid);
				return true;

			}
		} else {

			int itemID = item.getItemId();
			String seletedGroup = getPackedItemRow(packedPosition,
					RosterConstants.GROUP);
			Log.d(TAG, "action for group " + seletedGroup);

			switch (itemID) {
			case R.id.roster_contextmenu_group_rename:
				if (!isConnected()) {
					showToastNotification(R.string.Global_authenticate_first);
					return true;
				}
				renameRosterGroupDialog(seletedGroup);
				return true;

			}
		}
		return false;
	}

	private boolean isChild(long packedPosition) {
		int type = ExpandableListView.getPackedPositionType(packedPosition);
		return (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD);
	}

	private void startChatActivity(String user, String userName, String message) {
		Intent chatIntent = new Intent(this,
				ir.blackgrape.bereshtook.chat.ChatWindow.class);
		Uri userNameUri = Uri.parse(user);
		chatIntent.setData(userNameUri);
		chatIntent.putExtra(
				ir.blackgrape.bereshtook.chat.ChatWindow.INTENT_EXTRA_USERNAME,
				userName);
		if (message != null) {
			chatIntent
					.putExtra(
							ir.blackgrape.bereshtook.chat.ChatWindow.INTENT_EXTRA_MESSAGE,
							message);
		}
		startActivity(chatIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.roster_options, menu);
		return true;
	}

	void setMenuItem(Menu menu, int itemId, int iconId, CharSequence title) {
		com.actionbarsherlock.view.MenuItem item = menu.findItem(itemId);
		if (item == null)
			return;
		item.setIcon(iconId);
		item.setTitle(title);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		setMenuItem(menu, R.id.menu_connect, getConnectDisconnectIcon(),
				getConnectDisconnectText());
		setMenuItem(menu, R.id.menu_show_hide, getShowHideMenuIcon(),
				getShowHideMenuText());
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		return applyMainMenuChoice(item);
	}

	private int getShowHideMenuIcon() {
		TypedValue tv = new TypedValue();
		if (mConfig.showOffline) {
			getTheme().resolveAttribute(R.attr.OnlineFriends, tv, true);
			return tv.resourceId;
		}
		getTheme().resolveAttribute(R.attr.AllFriends, tv, true);
		return tv.resourceId;
	}

	private String getShowHideMenuText() {
		return mConfig.showOffline ? getString(R.string.menu_hideOff)
				: getString(R.string.menu_showOff);
	}

	public StatusMode getStatusMode() {
		return StatusMode.fromString(mConfig.statusMode);
	}

	public void setAndSaveStatus(StatusMode statusMode, String message) {
		SharedPreferences.Editor prefedit = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		// do not save "offline" to prefs, or else!
		if (statusMode != StatusMode.offline)
			prefedit.putString(PreferenceConstants.STATUS_MODE,
					statusMode.name());
		if (!message.equals(mConfig.statusMessage)) {
			List<String> smh = new ArrayList<String>(
					java.util.Arrays.asList(mConfig.statusMessageHistory));
			if (!smh.contains(message))
				smh.add(message);
			String smh_joined = android.text.TextUtils.join("\036", smh);
			prefedit.putString(PreferenceConstants.STATUS_MESSAGE_HISTORY,
					smh_joined);
		}
		prefedit.putString(PreferenceConstants.STATUS_MESSAGE, message);
		prefedit.commit();

		// check if we are connected and want to go offline
		boolean needToDisconnect = (statusMode == StatusMode.offline)
				&& isConnected();
		// check if we want to reconnect
		boolean needToConnect = (statusMode != StatusMode.offline)
				&& serviceAdapter.getConnectionState() == ConnectionState.OFFLINE;

		if (needToConnect || needToDisconnect)
			toggleConnection();
		else if (isConnected())
			serviceAdapter.setStatusFromConfig();
	}

	private void displayOwnStatus() {
		loadCoins();
		loadAvatar();
	}

	private void loadCoins() {
		if (mConfig.coins != null)
			actionBar.setSubtitle(StringUtil.convertToPersian(mConfig.coins
					.toString()) + " " + getString(R.string.coin));
		if (!isConnected() || dataServiceAdapter == null)
			return;
		CoinLoader cl = new CoinLoader();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			cl.executeOnExecutor(CoinLoader.THREAD_POOL_EXECUTOR);
		else
			cl.execute();
	}

	private void loadAvatar() {
		actionBar.setIcon(getStatusActionIcon());
		if (mAvatar != null && isConnected())
			actionBar.setIcon(mAvatar);
		if (mConfig.isAvatarSet) {
			OfflineAvatarLoader offlineLoader = new OfflineAvatarLoader();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				offlineLoader
						.executeOnExecutor(OfflineAvatarLoader.THREAD_POOL_EXECUTOR);
			else
				offlineLoader.execute();
			return;
		}

		if (!isConnected() || dataServiceAdapter == null)
			return;
		OnlineAvatarLoader onlineLoader = new OnlineAvatarLoader();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			onlineLoader
					.executeOnExecutor(OnlineAvatarLoader.THREAD_POOL_EXECUTOR);
		else
			onlineLoader.execute();
	}

	private void aboutDialog() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View about = inflater.inflate(R.layout.aboutview, null, false);
		// String versionTitle = getString(R.string.AboutDialog_title);
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			// versionTitle += " v" + pi.versionName;
		} catch (NameNotFoundException e) {
		}

		// fix translator-credits: hide if unset, format otherwise

		// TextView tcv = (TextView)about.findViewById(R.id.translator_credits);
		// if (tcv.getText().equals("translator-credits"))
		// tcv.setVisibility(View.GONE);

		new AlertDialog.Builder(this)
				// .setTitle(versionTitle)
				.setIcon(android.R.drawable.ic_dialog_info).setView(about)
				.setPositiveButton(R.string.ok, null)
				// .setNeutralButton(R.string.AboutDialog_Vote, new
				// DialogInterface.OnClickListener() {
				// public void onClick(DialogInterface dialog, int item) {
				// Intent market = new Intent(Intent.ACTION_VIEW,
				// Uri.parse("market://details?id=" + getPackageName()));
				// market.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
				// Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				// try {
				// startActivity(market);
				// } catch (Exception e) {
				// // do not crash
				// Log.e(TAG, "could not go to market: " + e);
				// }
				// }
				// })
				.create().show();
	}

	private boolean applyMainMenuChoice(com.actionbarsherlock.view.MenuItem item) {

		int itemID = item.getItemId();

		switch (itemID) {
		case R.id.menu_connect:
			toggleConnection();
			return true;

		case R.id.menu_add_friend:
			addToRosterDialog(null);
			return true;

		case R.id.menu_show_hide:
			setOfflinceContactsVisibility(!mConfig.showOffline);
			updateRoster();
			return true;

		case android.R.id.home:
		case R.id.menu_status:
			if (isConnected())
				chooseAvatar();
			else if (!isConnecting())
				toggleConnection();
			return true;

		case R.id.menu_exit:
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.putBoolean(PreferenceConstants.CONN_STARTUP, false)
					.commit();
			stopService(xmppServiceIntent);
			finish();
			return true;

		case R.id.menu_settings:
			startActivity(new Intent(this, MainPrefs.class));
			return true;

		case R.id.menu_about:
			aboutDialog();
			return true;

		case R.id.menu_account:
			checkUserAccounts();
			;
			return true;

		case R.id.menu_coins:
			if (isConnected())
				startActivity(new Intent(this, ShopActivity.class));
			else if (!isConnecting())
				toggleConnection();
			return true;
		case R.id.menu_scoreboard:
			if (isConnected())
				startActivity(new Intent(this, ScoreboardActivity.class)
						.putExtra("username", mConfig.userName));
			else if (!isConnecting())
				toggleConnection();
			return true;
		}

		return false;

	}

	private final int REQUEST_GALLERY = 0;
	private final int REQUEST_CAMERA = 1;
	private final int REQUEST_CROP_IMAGE = 2;

	private void chooseAvatar() {
		final String[] items = new String[] { getString(R.string.camera),
				getString(R.string.gallery) };
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.select_dialog_item, items);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(R.string.choose_profile_image);
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) { // pick from
																	// camera
				if (item == 0) {
					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

					mImageCaptureUri = Uri.fromFile(new File(Environment
							.getExternalStorageDirectory(), "tmp_avatar_"
							+ String.valueOf(System.currentTimeMillis())
							+ ".jpg"));

					intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
							mImageCaptureUri);

					try {
						intent.putExtra("return-data", true);

						startActivityForResult(intent, REQUEST_CAMERA);
					} catch (ActivityNotFoundException e) {
						e.printStackTrace();
					}
				} else {
					Intent intent = new Intent();

					intent.setType("image/*");
					intent.setAction(Intent.ACTION_GET_CONTENT);

					startActivityForResult(Intent.createChooser(intent,
							"Complete action using"), REQUEST_GALLERY);
				}
			}
		});

		final AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		if (resultCode != RESULT_OK)
			return;

		switch (requestCode) {
		case REQUEST_GALLERY:
			mImageCaptureUri = imageReturnedIntent.getData();
			doCrop();

		case REQUEST_CAMERA:
			doCrop();
			break;

		case REQUEST_CROP_IMAGE:
			Bundle extras = imageReturnedIntent.getExtras();

			if (extras != null) {
				Bitmap photo = extras.getParcelable("data");
				if (photo == null)
					return;

				mAvatar = new BitmapDrawable(getResources(), photo);
				actionBar.setIcon(mAvatar);

				AvatarUploader as = new AvatarUploader();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
					as.executeOnExecutor(AvatarUploader.THREAD_POOL_EXECUTOR,
							photo);
				else
					as.execute(photo);
			}
			break;
		}

	}

	private void doCrop() {
		final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");

		List<ResolveInfo> list = getPackageManager().queryIntentActivities(
				intent, 0);

		int size = list.size();

		if (size == 0) {
			Toast.makeText(this, R.string.crop_app_not_found,
					Toast.LENGTH_SHORT).show();

			return;
		} else {
			intent.setData(mImageCaptureUri);

			intent.putExtra("outputX", 100);
			intent.putExtra("outputY", 100);
			intent.putExtra("aspectX", 1);
			intent.putExtra("aspectY", 1);
			intent.putExtra("scale", true);
			intent.putExtra("return-data", true);

			if (size == 0) {
				Intent i = new Intent(intent);
				ResolveInfo res = list.get(0);

				i.setComponent(new ComponentName(res.activityInfo.packageName,
						res.activityInfo.name));

				startActivityForResult(i, REQUEST_CROP_IMAGE);
			} else {
				for (ResolveInfo res : list) {
					final CropOption co = new CropOption();

					co.title = getPackageManager().getApplicationLabel(
							res.activityInfo.applicationInfo);
					co.icon = getPackageManager().getApplicationIcon(
							res.activityInfo.applicationInfo);
					co.appIntent = new Intent(intent);

					co.appIntent
							.setComponent(new ComponentName(
									res.activityInfo.packageName,
									res.activityInfo.name));

					cropOptions.add(co);
				}

				CropOptionAdapter adapter = new CropOptionAdapter(
						getApplicationContext(), cropOptions);

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.choose_cropper);
				builder.setAdapter(adapter,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								startActivityForResult(
										cropOptions.get(item).appIntent,
										REQUEST_CROP_IMAGE);
							}
						});

				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						if (mImageCaptureUri != null) {
							// getContentResolver().delete(mImageCaptureUri,
							// null, null );
							mImageCaptureUri = null;
						}
					}
				});

				AlertDialog croppersdialog = builder.create();
				croppersdialog.show();
			}
		}
	}

	/** Sets if all contacts are shown in the roster or online contacts only. */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	// required for Sherlock's invalidateOptionsMenu */
	private void setOfflinceContactsVisibility(boolean showOffline) {
		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putBoolean(PreferenceConstants.SHOW_OFFLINE, showOffline)
				.commit();
		invalidateOptionsMenu();
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {

		long packedPosition = ExpandableListView.getPackedPositionForChild(
				groupPosition, childPosition);
		Cursor c = (Cursor) getExpandableListView().getItemAtPosition(
				getExpandableListView().getFlatListPosition(packedPosition));
		String userJid = c.getString(c
				.getColumnIndexOrThrow(RosterConstants.JID));
		String userName = c.getString(c
				.getColumnIndexOrThrow(RosterConstants.ALIAS));
		Intent i = getIntent();
		if (i.getAction() != null && i.getAction().equals(Intent.ACTION_SEND)) {
			// delegate ACTION_SEND to child window and close self
			startChatActivity(userJid, userName,
					i.getStringExtra(Intent.EXTRA_TEXT));
			finish();
		} else {
			StatusMode s = StatusMode.values()[c.getInt(c
					.getColumnIndexOrThrow(RosterConstants.STATUS_MODE))];
			if (s == StatusMode.subscribe)
				rosterAddRequestedDialog(userJid, c.getString(c
						.getColumnIndexOrThrow(RosterConstants.STATUS_MESSAGE)));
			else
				startChatActivity(userJid, userName, null);
		}

		return true;
	}

	private void updateConnectionState(ConnectionState cs) {
		Log.d(TAG, "updateConnectionState: " + cs);
		displayOwnStatus();
		updateStatus();
		boolean spinTheSpinner = false;
		switch (cs) {
		case CONNECTING:
		case DISCONNECTING:
			spinTheSpinner = true;
		case DISCONNECTED:
		case RECONNECT_NETWORK:
		case RECONNECT_DELAYED:
		case OFFLINE:
			if (cs == ConnectionState.OFFLINE) // override with "Offline"
												// string, no error message
				mConnectingText.setText(R.string.conn_offline);
			else
				mConnectingText.setText(serviceAdapter
						.getConnectionStateString());
			mConnectingText.setVisibility(View.VISIBLE);
			setSupportProgressBarIndeterminateVisibility(spinTheSpinner);
			break;
		case ONLINE:
			mConnectingText.setVisibility(View.GONE);
			setSupportProgressBarIndeterminateVisibility(false);

			if (!mConfig.jid_configured) {
				PreferenceManager.getDefaultSharedPreferences(this).edit()
						.putBoolean(PreferenceConstants.JID_CONFIGURED, true)
						.commit();
				if (isNewAccount)
					pushNewAccount();
				checkVersion();
			}
			String strStatus = getMyStatusMsg();
			if (strStatus.contains("S")
					&& !strStatus.equals(mConfig.statusMessage)) {
				isStatusSet = true;
				PreferenceManager
						.getDefaultSharedPreferences(MainWindow.this)
						.edit()
						.putString(PreferenceConstants.STATUS_MESSAGE,
								strStatus).commit();
				serviceAdapter.setStatusFromConfig();
			}
			break;
		}
	}

	public void startConnection(boolean create_account) {
		xmppServiceIntent.putExtra("create_account", create_account);
		startService(xmppServiceIntent);
	}

	// this function changes the prefs to keep the connection
	// according to the requested state
	private void toggleConnection() {
		if (!mConfig.jid_configured) {
			checkUserAccounts();
			return;
		}
		boolean oldState = isConnected() || isConnecting();

		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putBoolean(PreferenceConstants.CONN_STARTUP, !oldState)
				.commit();
		if (oldState) {
			serviceAdapter.disconnect();
			stopService(xmppServiceIntent);
		} else
			startConnection(false);
	}

	private int getConnectDisconnectIcon() {
		if (isConnected() || isConnecting()) {
			return R.drawable.ic_menu_unplug;
		}
		return R.drawable.ic_menu_plug;
	}

	private String getConnectDisconnectText() {
		if (isConnected() || isConnecting()) {
			return getString(R.string.menu_disconnect);
		}
		return getString(R.string.menu_connect);
	}

	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		xmppServiceIntent = new Intent(this, XMPPService.class);
		xmppServiceIntent.setAction("ir.blackgrape.bereshtook.XMPPSERVICE");

		xmppServiceConnection = new ServiceConnection() {

			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			// required for Sherlock's invalidateOptionsMenu */
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				serviceAdapter = new XMPPRosterServiceAdapter(
						IXMPPRosterService.Stub.asInterface(service));
				serviceAdapter.registerUICallback(rosterCallback);
				Log.i(TAG,
						"getConnectionState(): "
								+ serviceAdapter.getConnectionState());
				invalidateOptionsMenu(); // to load the action bar contents on
											// time for access to
											// icons/progressbar
				ConnectionState cs = serviceAdapter.getConnectionState();
				updateConnectionState(cs);
				updateRoster();

				// when returning from prefs to main activity, apply new config
				if (mConfig.reconnect_required && cs == ConnectionState.ONLINE) {
					// login config changed, force reconnection
					serviceAdapter.disconnect();
					serviceAdapter.connect();
				} else if (mConfig.presence_required && isConnected())
					serviceAdapter.setStatusFromConfig();

				// handle server-related intents after connecting to the backend
				handleJabberIntent();
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}
		};
	}

	private void registerDataService() {
		Log.i(TAG, "called startGameService()");
		dataServiceIntent = new Intent(this, XMPPService.class);
		dataServiceIntent.setAction("ir.blackgrape.bereshtook.XMPPSERVICE2");
		dataServiceIntent.putExtra("isGameService", true);

		dataServiceConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				dataServiceAdapter = new XMPPDataServiceAdapter(
						IXMPPDataService.Stub.asInterface(service));
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "Game service called onServiceDisconnected()");
			}
		};
	}

	private void unbindXMPPServices() {
		try {
			unbindService(xmppServiceConnection);
			unbindService(dataServiceConnection);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't bound!");
		}
	}

	private void bindXMPPServices() {
		bindService(xmppServiceIntent, xmppServiceConnection, BIND_AUTO_CREATE);
		bindService(dataServiceIntent, dataServiceConnection, BIND_AUTO_CREATE);
	}

	private void registerListAdapter() {

		rosterListAdapter = new RosterExpListAdapter(this);
		setListAdapter(rosterListAdapter);
	}

	private void createUICallback() {
		rosterCallback = new IXMPPRosterCallback.Stub() {
			@Override
			public void connectionStateChanged(final int connectionstate)
					throws RemoteException {
				mainHandler.post(new Runnable() {
					@TargetApi(Build.VERSION_CODES.HONEYCOMB)
					// required for Sherlock's invalidateOptionsMenu */
					public void run() {
						ConnectionState cs = ConnectionState.values()[connectionstate];
						// Log.d(TAG, "connectionStatusChanged: " + cs);
						updateConnectionState(cs);
						invalidateOptionsMenu();
					}
				});
			}
		};
	}

	// store mGroupsExpanded into prefs (this is a hack, but SQLite /
	// content providers suck wrt. virtual groups)
	public void storeExpandedState() {
		SharedPreferences.Editor prefedit = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		for (HashMap.Entry<String, Boolean> item : mGroupsExpanded.entrySet()) {
			prefedit.putBoolean("expanded_" + item.getKey(), item.getValue());
		}
		prefedit.commit();
	}

	// get the name of a roster group from the cursor
	public String getGroupName(int groupId) {
		return getPackedItemRow(
				ExpandableListView.getPackedPositionForGroup(groupId),
				RosterConstants.GROUP);
	}

	public void restoreGroupsExpanded() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		for (int count = 0; count < getExpandableListAdapter().getGroupCount(); count++) {
			String name = getGroupName(count);
			if (!mGroupsExpanded.containsKey(name))
				mGroupsExpanded.put(name,
						prefs.getBoolean("expanded_" + name, true));
			Log.d(TAG, "restoreGroupsExpanded: " + name + ": "
					+ mGroupsExpanded.get(name));
			if (mGroupsExpanded.get(name))
				getExpandableListView().expandGroup(count);
			else
				getExpandableListView().collapseGroup(count);
		}
	}

	private void showFirstStartUpDialog(boolean enable, String username,
			String password) {
		Log.i(TAG, "showFirstStartUpDialog, JID: " + mConfig.jabberID);
		// load preference defaults
		PreferenceManager.setDefaultValues(this, R.layout.mainprefs, false);
		PreferenceManager.setDefaultValues(this, R.layout.accountprefs, false);

		// prevent a start-up with empty JID
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.edit().putBoolean(PreferenceConstants.CONN_STARTUP, true)
				.commit();

		// show welcome dialog
		new FirstStartDialog(this, serviceAdapter, enable, username, password)
				.show();
	}

	public static Intent createIntent(Context context) {
		Intent i = new Intent(context, MainWindow.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return i;
	}

	protected void showToastNotification(int message) {
		Toast tmptoast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		tmptoast.show();
	}

	private void registerCrashReporter() {
		if (mConfig.reportCrash) {
			ExceptionHandler.register(this,
					"http://bereshtook.ir/crash_report/");
		}
	}

	private static final String OFFLINE_EXCLUSION = RosterConstants.STATUS_MODE
			+ " != " + StatusMode.offline.ordinal();
	private static final String countAvailableMembers = "SELECT COUNT() FROM "
			+ RosterProvider.TABLE_ROSTER + " inner_query"
			+ " WHERE inner_query." + RosterConstants.GROUP + " = "
			+ RosterProvider.QUERY_ALIAS + "." + RosterConstants.GROUP
			+ " AND inner_query." + OFFLINE_EXCLUSION;
	private static final String countMembers = "SELECT COUNT() FROM "
			+ RosterProvider.TABLE_ROSTER + " inner_query"
			+ " WHERE inner_query." + RosterConstants.GROUP + " = "
			+ RosterProvider.QUERY_ALIAS + "." + RosterConstants.GROUP;
	private static final String[] GROUPS_QUERY = new String[] {
			RosterConstants._ID, RosterConstants.GROUP, };
	private static final String[] GROUPS_QUERY_COUNTED = new String[] {
			RosterConstants._ID,
			RosterConstants.GROUP,
			"(" + countAvailableMembers + ") || '/' || (" + countMembers
					+ ") AS members" };

	final String countAvailableMembersTotals = "SELECT COUNT() FROM "
			+ RosterProvider.TABLE_ROSTER + " inner_query"
			+ " WHERE inner_query." + OFFLINE_EXCLUSION;
	final String countMembersTotals = "SELECT COUNT() FROM "
			+ RosterProvider.TABLE_ROSTER;
	final String[] GROUPS_QUERY_CONTACTS_DISABLED = new String[] {
			RosterConstants._ID,
			"'' AS " + RosterConstants.GROUP,
			"(" + countAvailableMembersTotals + ") || '/' || ("
					+ countMembersTotals + ") AS members" };

	private static final String[] GROUPS_FROM = new String[] {
			RosterConstants.GROUP, "members" };
	private static final int[] GROUPS_TO = new int[] { R.id.groupname,
			R.id.members };
	private static final String[] ROSTER_QUERY = new String[] {
			RosterConstants._ID, RosterConstants.JID, RosterConstants.ALIAS,
			RosterConstants.STATUS_MODE, RosterConstants.STATUS_MESSAGE, };

	public List<String> getRosterGroups() {
		// we want all, online and offline
		List<String> list = new ArrayList<String>();
		Cursor cursor = getContentResolver().query(RosterProvider.GROUPS_URI,
				GROUPS_QUERY, null, null, RosterConstants.GROUP);
		int idx = cursor.getColumnIndex(RosterConstants.GROUP);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			list.add(cursor.getString(idx));
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	public List<String[]> getRosterContacts() {
		// we want all, online and offline
		List<String[]> list = new ArrayList<String[]>();
		Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI,
				ROSTER_QUERY, null, null, RosterConstants.ALIAS);
		int JIDIdx = cursor.getColumnIndex(RosterConstants.JID);
		int aliasIdx = cursor.getColumnIndex(RosterConstants.ALIAS);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String jid = cursor.getString(JIDIdx);
			String alias = cursor.getString(aliasIdx);
			if ((alias == null) || (alias.length() == 0))
				alias = jid;
			list.add(new String[] { jid, alias });
			cursor.moveToNext();
		}
		cursor.close();
		return list;
	}

	private class RosterObserver extends ContentObserver {
		public RosterObserver() {
			super(mainHandler);
		}

		public void onChange(boolean selfChange) {
			Log.d(TAG, "RosterObserver.onChange: " + selfChange);
			// work around race condition in ExpandableListView, which collapses
			// groups rand-f**king-omly
			if (getExpandableListAdapter() != null)
				mainHandler.postDelayed(new Runnable() {
					public void run() {
						restoreGroupsExpanded();
					}
				}, 100);
		}
	}

	private HashMap<String, Integer> mUnreadCounters = new HashMap<String, Integer>();

	private void loadUnreadCounters() {
		final String[] PROJECTION = new String[] { ChatConstants.JID,
				"count(*)" };
		final String SELECTION = ChatConstants.DIRECTION + " = "
				+ ChatConstants.INCOMING + " AND "
				+ ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW
				+ ") GROUP BY (" + ChatConstants.JID; // hack!

		Cursor c = getContentResolver().query(ChatProvider.CONTENT_URI,
				PROJECTION, SELECTION, null, null);
		mUnreadCounters.clear();
		if (c != null) {
			while (c.moveToNext())
				mUnreadCounters.put(c.getString(0), c.getInt(1));
			c.close();
		}
	}

	private class ChatObserver extends ContentObserver {
		public ChatObserver() {
			super(mainHandler);
		}

		public void onChange(boolean selfChange) {
			updateRoster();
		}
	}

	private void initLocation() {
		if (mBestLocationListener == null) {
			mBestLocationListener = new BestLocationListener() {

				@Override
				public void onStatusChanged(String provider, int status,
						Bundle extras) {
					// Log.i(TAG, "onStatusChanged PROVIDER:" + provider +
					// " STATUS:" + String.valueOf(status));
				}

				@Override
				public void onProviderEnabled(String provider) {
					isStatusSet = false;
				}

				@Override
				public void onProviderDisabled(String provider) {
					isStatusSet = false;
				}

				@Override
				public void onLocationUpdateTimeoutExceeded(LocationType type) {
					// Log.w(TAG, "onLocationUpdateTimeoutExceeded PROVIDER:" +
					// type);
				}

				@Override
				public void onLocationUpdate(Location location,
						LocationType type, boolean isFresh) {
					Log.i(TAG, "onLocationUpdate TYPE:" + type + " Location:"
							+ mBestLocationProvider.locationToString(location));
					mLocation = location;
				}

			};

			if (mBestLocationProvider == null) {
				mBestLocationProvider = new BestLocationProvider(this, true,
						true, 60000, 60000, 10000, 100);
			}
		}
	}

	private String getMyStatusMsg() {
		if (mLocation != null && mConfig.coins != null)
			return mConfig.coins.toString() + "S" + mLocation.getLatitude()
					+ "#" + mLocation.getLongitude();
		else if (mConfig.coins != null)
			return mConfig.coins.toString() + "S";
		else
			return mConfig.statusMessage; // never should happens but...
	}

	public class RosterExpListAdapter extends SimpleCursorTreeAdapter {

		public RosterExpListAdapter(Context context) {
			super(context, /* cursor = */null, R.layout.maingroup_row,
					GROUPS_FROM, GROUPS_TO, R.layout.mainchild_row,
					new String[] { RosterConstants.ALIAS,
							RosterConstants.STATUS_MESSAGE,
							RosterConstants.STATUS_MODE }, new int[] {
							R.id.roster_screenname, R.id.roster_statusmsg,
							R.id.roster_icon });
		}

		public void requery() {
			String selectWhere = null;
			if (!mConfig.showOffline)
				selectWhere = OFFLINE_EXCLUSION;

			String[] query = GROUPS_QUERY_COUNTED;
			if (!mConfig.enableGroups) {
				query = GROUPS_QUERY_CONTACTS_DISABLED;
			}
			Cursor cursor = getContentResolver().query(
					RosterProvider.GROUPS_URI, query, selectWhere, null,
					RosterConstants.GROUP);
			Cursor oldCursor = getCursor();
			changeCursor(cursor);
			if (oldCursor != null)
				stopManagingCursor(oldCursor);
		}

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			// Given the group, we return a cursor for all the children within
			// that group
			String selectWhere;
			int idx = groupCursor.getColumnIndex(RosterConstants.GROUP);
			String groupname = groupCursor.getString(idx);
			String[] args = null;

			if (!mConfig.enableGroups) {
				selectWhere = mConfig.showOffline ? "" : OFFLINE_EXCLUSION;
			} else {
				selectWhere = mConfig.showOffline ? "" : OFFLINE_EXCLUSION
						+ " AND ";
				selectWhere += RosterConstants.GROUP + " = ?";
				args = new String[] { groupname };
			}
			return getContentResolver().query(RosterProvider.CONTENT_URI,
					ROSTER_QUERY, selectWhere, args, null);
		}

		@Override
		protected void bindGroupView(View view, Context context, Cursor cursor,
				boolean isExpanded) {
			super.bindGroupView(view, context, cursor, isExpanded);
			if (cursor.getString(
					cursor.getColumnIndexOrThrow(RosterConstants.GROUP))
					.length() == 0) {
				TextView groupname = (TextView) view
						.findViewById(R.id.groupname);
				groupname.setText(R.string.friends_group);
			}
			groups.add(cursor.getString(cursor
					.getColumnIndex(RosterConstants.GROUP)));
		}

		@Override
		protected void bindChildView(View view, Context context, Cursor cursor,
				boolean isLastChild) {
			super.bindChildView(view, context, cursor, isLastChild);
			TextView statusMsg = (TextView) view
					.findViewById(R.id.roster_statusmsg);
			boolean hasStatus = statusMsg.getText() != null
					&& statusMsg.getText().length() > 0;
			statusMsg.setVisibility(hasStatus ? View.VISIBLE : View.GONE);

			String herStatus;
			if (hasStatus && mLocation != null) {
				herStatus = statusMsg.getText().toString();
				statusMsg.setText(StatusUtil.makeStatusWithLocation(mLocation,
						herStatus));
			} else if (hasStatus) {
				herStatus = statusMsg.getText().toString();
				statusMsg.setText(StatusUtil.makeStatus(herStatus));
			} else
				statusMsg.setText("");

			String jid = cursor.getString(cursor
					.getColumnIndex(RosterConstants.JID));
			TextView unreadmsg = (TextView) view
					.findViewById(R.id.roster_unreadmsg_cnt);
			Integer count = mUnreadCounters.get(jid);
			if (count == null)
				count = 0;
			unreadmsg.setText(count.toString());
			unreadmsg.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
			unreadmsg.bringToFront();
		}

		protected void setViewImage(ImageView v, String value) {
			int presenceMode = Integer.parseInt(value);
			v.setImageResource(getIconForPresenceMode(presenceMode));
		}

		private int getIconForPresenceMode(int presenceMode) {
			if (!isConnected()) // override icon if we are offline
				presenceMode = 0;
			return StatusMode.values()[presenceMode].getDrawableId();
		}
	}
	

	class UserChecker extends AsyncTask<String, Void, JSONObject> {

		private final HttpClient client = new DefaultHttpClient();
		private ProgressDialog dialog = new ProgressDialog(MainWindow.this);
		private COMMAND cmd;

		public void setCmd(COMMAND cmd) {
			this.cmd = cmd;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			if (cmd == COMMAND.FIND) {
				dialog.setMessage(MainWindow.this
						.getString(R.string.connecting_bereshtook));
				dialog.show();
			}

		}

		@Override
		protected JSONObject doInBackground(String... urls) {
			String url = urls[0];
			InputStream inputStream = null;
			String result = "";

			try {
				HttpResponse httpResponse = client.execute(new HttpGet(url));
				inputStream = httpResponse.getEntity().getContent();

				if (inputStream != null) {
					result = convertInputStreamToString(inputStream);
					inputStream.close();
				} else
					result = "problem";

				try {
					return new JSONObject(result);
				} catch (JSONException e) {
					e.printStackTrace();
				}

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(JSONObject json) {
			try {
				if (json == null || json.equals("problem")) {
					dialog.cancel();
					showToastNotification(R.string.error_fetch_data);
					return;
				}
				String result = json.getString("result");
				if (result == null || result.equals("error"))
					return;
				if (cmd == COMMAND.FIND) {
					if (result.equals("not_exist"))
						showFirstStartUpDialog(true, null, null);
					else if (result.equals("user_exist")) {
						String username = json.getString("username");
						String password = json.getString("password");
						showFirstStartUpDialog(false, username, password);
					}
				} else if (cmd == COMMAND.INSERT) {
					PreferenceManager
							.getDefaultSharedPreferences(MainWindow.this)
							.edit()
							.putBoolean(PreferenceConstants.ACCOUNT_PUSHED,
									true).commit();
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}
			if (dialog != null)
				dialog.cancel();

		}

	}

	class OfflineAvatarLoader extends AsyncTask<Void, Void, Drawable> {

		@Override
		protected Drawable doInBackground(Void... arg0) {
			Bitmap tempBitmap = loadAvatarFromStorage(mConfig.userName);
			if (tempBitmap != null)
				;
			return new BitmapDrawable(getResources(), tempBitmap);
		}

		@Override
		protected void onPostExecute(Drawable result) {
			super.onPostExecute(result);
			if (result != null) {
				mAvatar = result;
				actionBar.setIcon(mAvatar);
			}
		}
	}

	class OnlineAvatarLoader extends AsyncTask<Void, Void, Drawable> {

		@Override
		protected Drawable doInBackground(Void... arg0) {
			Bitmap bm = dataServiceAdapter.loadAvatar(mConfig.jabberID);

			if (bm != null) {
				saveAvatarToStorage(bm, mConfig.userName);
				return new BitmapDrawable(getResources(), bm);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Drawable result) {
			super.onPostExecute(result);
			if (result != null)
				mAvatar = result;
			else
				mAvatar = getResources().getDrawable(getStatusActionIcon());
			
			actionBar.setIcon(mAvatar);
		}
	}

	class AvatarUploader extends AsyncTask<Bitmap, Void, Drawable> {

		@Override
		protected Drawable doInBackground(Bitmap... args) {
			if (dataServiceAdapter != null)
				dataServiceAdapter.saveAvatar(args[0]);

			saveAvatarToStorage(args[0], mConfig.userName);

			Drawable avatar = new BitmapDrawable(getResources(), args[0]);
			return avatar;
		}

		@Override
		protected void onPostExecute(Drawable result) {
			super.onPostExecute(result);
			if (result != null)
				actionBar.setIcon(result);
		}
	}

	private boolean saveAvatarToStorage(Bitmap image, String filename) {

		try {
			FileOutputStream fos = MainWindow.this.openFileOutput(filename,
					Context.MODE_PRIVATE);

			image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
			fos.close();

			PreferenceManager.getDefaultSharedPreferences(MainWindow.this)
					.edit().putBoolean(PreferenceConstants.IS_AVATAR_SET, true)
					.commit();
			return true;
		} catch (Exception e) {
			Log.e("saveToInternalStorage()", e.getMessage());
			return false;
		}
	}

	private Bitmap loadAvatarFromStorage(String filename) {
		Bitmap thumbnail = null;
		try {
			File filePath = getFileStreamPath(filename);
			FileInputStream fi = new FileInputStream(filePath);
			thumbnail = BitmapFactory.decodeStream(fi);
		} catch (Exception ex) {
			Log.e("getThumbnail() on internal storage", ex.getMessage());
		}
		return thumbnail;
	}

	class CoinLoader extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... arg0) {
			return dataServiceAdapter.loadGameData(PRIVATE_DATA.COINS);
		}

		@Override
		protected void onPostExecute(String coins) {
			super.onPostExecute(coins);
			if (coins == null)
				return;
			PreferenceManager.getDefaultSharedPreferences(MainWindow.this)
					.edit()
					.putInt(PreferenceConstants.COINS, Integer.parseInt(coins))
					.commit();
			actionBar.setSubtitle(StringUtil.convertToPersian(mConfig.coins
					.toString()) + " " + getString(R.string.coin));
			updateStatus();
		}

	}

	class VersionChecker extends AsyncTask<String, Void, String> {
		private final HttpClient client = new DefaultHttpClient();

		@Override
		protected String doInBackground(String... urls) {
			try {
				PackageManager manager = MainWindow.this.getPackageManager();
				PackageInfo info = manager.getPackageInfo(
						MainWindow.this.getPackageName(), 0);
				String version = info.versionName;

				if (version == null || version.equals(mConfig.versionName))
					return null;

				String url = urls[0] + mConfig.userName + "/" + version;

				HttpResponse httpResponse = client.execute(new HttpGet(url));
				InputStream inputStream = httpResponse.getEntity().getContent();

				if (inputStream != null) {
					inputStream.close();
				}

				return version;

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("Bereshtook", "Error getting version");
			}
			return null;
		}

		@Override
		protected void onPostExecute(String versionName) {
			super.onPostExecute(versionName);
			if (versionName != null) {
				PreferenceManager
						.getDefaultSharedPreferences(MainWindow.this)
						.edit()
						.putString(PreferenceConstants.VERSION_NAME,
								versionName).commit();
			}
		}

	}

	private String convertInputStreamToString(InputStream inputStream)
			throws IOException {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(inputStream));
		String line = "";
		String result = "";
		while ((line = bufferedReader.readLine()) != null)
			result += line;

		inputStream.close();
		return result;

	}

}
