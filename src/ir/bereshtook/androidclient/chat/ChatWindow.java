package ir.bereshtook.androidclient.chat;

import ir.bereshtook.androidclient.BereshtookApplication;
import ir.bereshtook.androidclient.MainWindow;
import ir.bereshtook.androidclient.R;
import ir.bereshtook.androidclient.data.ChatProvider;
import ir.bereshtook.androidclient.data.ChatProvider.ChatConstants;
import ir.bereshtook.androidclient.data.RosterProvider;
import ir.bereshtook.androidclient.game.GameBroadcastReceiver;
import ir.bereshtook.androidclient.game.GameWindow;
import ir.bereshtook.androidclient.game.battleship.BattleshipWindow;
import ir.bereshtook.androidclient.game.rps.RPSWindow;
import ir.bereshtook.androidclient.game.ttt.TTTWindow;
import ir.bereshtook.androidclient.location.LocationUtil;
import ir.bereshtook.androidclient.service.IXMPPChatService;
import ir.bereshtook.androidclient.service.XMPPService;
import ir.bereshtook.androidclient.util.StatusMode;
import ir.bereshtook.androidclient.util.chat.MessageUtils;
import ir.bereshtook.androidclient.util.chat.MessageUtils.SmileyImageSpan;
import ir.bereshtook.androidclient.util.chat.QuickAction;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.internal.widget.IcsToast;
import com.actionbarsherlock.view.Window;

@SuppressWarnings("deprecation") /* recent ClipboardManager only available since API 11 */
public class ChatWindow extends SherlockListActivity implements OnKeyListener,
		TextWatcher {

	public static final String INTENT_EXTRA_USERNAME = ChatWindow.class.getName() + ".username";
	public static final String INTENT_EXTRA_MESSAGE = ChatWindow.class.getName() + ".message";
	
	private static final String TAG = "Bereshtook.ChatWindow";
	private static final String[] PROJECTION_FROM = new String[] {
			ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
			ChatProvider.ChatConstants.DIRECTION, ChatProvider.ChatConstants.JID,
			ChatProvider.ChatConstants.MESSAGE, ChatProvider.ChatConstants.DELIVERY_STATUS };

	private static final int[] PROJECTION_TO = new int[] { R.id.chat_date,
			R.id.chat_from, R.id.chat_message };
	
	private static final int DELAY_NEWMSG = 2000;

	private ContentObserver mContactObserver = new ContactObserver();
	private ImageView mStatusMode;
	private TextView mTitle;
	private TextView mSubTitle;
	private View.OnClickListener clickLisener;
	private Button mSendButton = null;
	private ImageButton mSmileyButton = null;
    private QuickAction mSmileyPopup;
    private AdapterView.OnItemClickListener mSmileySelectListener;
    private Button mRequestPlayButton = null; 
	private EditText mChatInput = null;
	private String mWithJabberID = null;
	private String mUserScreenName = null;
	private Intent mServiceIntent;
	private ServiceConnection mServiceConnection;
	private XMPPChatServiceAdapter mServiceAdapter;
	private int mChatFontSize;
	private Context mActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(BereshtookApplication.getConfig(this).getTheme());
		super.onCreate(savedInstanceState);
		mActivity = this;
	
		mChatFontSize = Integer.valueOf(BereshtookApplication.getConfig(this).chatFontSize);

		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.compose_message);
		
		getContentResolver().registerContentObserver(RosterProvider.CONTENT_URI,
				true, mContactObserver);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		registerForContextMenu(getListView());
		setContactFromUri();
		registerXMPPService();
		setOnClickListener();
		setSendButton();
		setSmileyButton();
		setSmileyListener();
		setUserInput();
		
		
		String titleUserid;
		if (mUserScreenName != null) {
			titleUserid = mUserScreenName;
		} else {
			titleUserid = mWithJabberID;
		}

		setCustomTitle(titleUserid);

		setChatWindowAdapter();
	}

	private void setCustomTitle(String title) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.chat_action_title, null);
		mStatusMode = (ImageView)layout.findViewById(R.id.action_bar_status);
		mTitle = (TextView)layout.findViewById(R.id.action_bar_title);
		mSubTitle = (TextView)layout.findViewById(R.id.action_bar_subtitle);
		mTitle.setText(title);
		
		mRequestPlayButton = (Button)layout.findViewById(R.id.btnRequestPlay);
		mRequestPlayButton.setOnClickListener(clickLisener);

		setTitle(null);
		getSupportActionBar().setCustomView(layout);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
	}

	private void setChatWindowAdapter() {
		String selection = ChatConstants.JID + "='" + mWithJabberID + "'";
		Cursor cursor = managedQuery(ChatProvider.CONTENT_URI, PROJECTION_FROM,
				selection, null, null);
		ListAdapter adapter = new ChatWindowAdapter(cursor, PROJECTION_FROM,
				PROJECTION_TO, mWithJabberID, mUserScreenName);

		setListAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateContactStatus();
		GameBroadcastReceiver.setContext(this);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus)
			bindXMPPService();
		else
			unbindXMPPService();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (hasWindowFocus()) unbindXMPPService();
		getContentResolver().unregisterContentObserver(mContactObserver);
	}

	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mServiceIntent = new Intent(this, XMPPService.class);
		Uri chatURI = Uri.parse(mWithJabberID);
		mServiceIntent.setData(chatURI);
		mServiceIntent.setAction("ir.bereshtook.androidclient.XMPPSERVICE");

		mServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				mServiceAdapter = new XMPPChatServiceAdapter(
						IXMPPChatService.Stub.asInterface(service),
						mWithJabberID);
				
				mServiceAdapter.clearNotifications(mWithJabberID);
				updateContactStatus();
			}
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}

		};
	}

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}
	
	private void setOnClickListener(){
		clickLisener = getOnSetListener();
	}

	private void setSendButton() {
		mSendButton = (Button) findViewById(R.id.btnChatSend);
		mSendButton.setOnClickListener(clickLisener);
		mSendButton.setEnabled(false);
	}

	private void setSmileyButton() {
		mSmileyButton = (ImageButton) findViewById(R.id.btnChatSmiley);
		mSmileyButton.setOnClickListener(clickLisener);
	}
	
	private void setSmileyListener(){
        mSmileySelectListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Editable text = mChatInput.getText();
                int startPos = mChatInput.getSelectionStart();
                int endPos = mChatInput.getSelectionEnd();

                if (startPos < 0) startPos = text.length();
                if (endPos < 0) endPos = startPos;
                int startMin = Math.min(startPos, endPos);

                // add unicode emoji
                char[] value = Character.toChars((int) id);
                text.replace(startMin, Math.max(startPos, endPos),
                    String.valueOf(value), 0, value.length);

                // textview change listener will do the rest

                // dismiss smileys popup
                // TEST mSmileyPopup.dismiss();
            }
        };
	}
	
	private void setUserInput() {
		Intent i = getIntent();
		mChatInput = (EditText) findViewById(R.id.txtChatInput);
		mChatInput.addTextChangedListener(this);
		if (i.hasExtra(INTENT_EXTRA_MESSAGE)) {
			mChatInput.setText(i.getExtras().getString(INTENT_EXTRA_MESSAGE));
		}
		
		mChatInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			    // convert smiley codes
				mChatInput.removeTextChangedListener(this);
                MessageUtils.convertSmileys(mActivity, s, SmileyImageSpan.SIZE_EDITABLE);
                mChatInput.addTextChangedListener(this);

                // enable the send button if there is something to send
                mSendButton.setEnabled(s.length() > 0);
			}
		});

	}
	
	private void setContactFromUri() {
		Intent i = getIntent();
		mWithJabberID = i.getDataString().toLowerCase();
		if (i.hasExtra(INTENT_EXTRA_USERNAME)) {
			mUserScreenName = i.getExtras().getString(INTENT_EXTRA_USERNAME);
		} else {
			mUserScreenName = mWithJabberID;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		View target = ((AdapterContextMenuInfo)menuInfo).targetView;
		TextView from = (TextView)target.findViewById(R.id.chat_from);
		getMenuInflater().inflate(R.menu.chat_contextmenu, menu);
		if (!from.getText().equals(getString(R.string.chat_from_me))) {
			menu.findItem(R.id.chat_contextmenu_resend).setEnabled(false);
		}
	}

	private CharSequence getMessageFromContextMenu(MenuItem item) {
		View target = ((AdapterContextMenuInfo)item.getMenuInfo()).targetView;
		TextView message = (TextView)target.findViewById(R.id.chat_message);
		return message.getText();
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.chat_contextmenu_copy_text:
			ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(getMessageFromContextMenu(item));
			return true;
		case R.id.chat_contextmenu_resend:
			sendMessage(getMessageFromContextMenu(item).toString());
			Log.d(TAG, "resend!");
			return true;
		default:
			return super.onContextItemSelected((android.view.MenuItem) item);
		}
	}
	

	private View.OnClickListener getOnSetListener() {
		return new View.OnClickListener() {

			public void onClick(View v) {
				switch(v.getId()){
				
				case R.id.btnChatSend:
					sendMessageIfNotNull();
					break;
					
				case R.id.btnChatSmiley:
					showSmileysPopup(v);
					break;
					
				case R.id.btnRequestPlay:
					requestPlay();
					break;
					
				}
			}
		};
	}

	private void sendMessageIfNotNull() {
		if (mChatInput.getText().length() >= 1) {
			sendMessage(mChatInput.getText().toString());
		}
	}

	public void sendMessage(String message) {
		mChatInput.setText(null);
		mSendButton.setEnabled(false);
		mServiceAdapter.sendMessage(mWithJabberID, message);
		if (!mServiceAdapter.isServiceAuthenticated())
			showToastNotification(R.string.toast_stored_offline);
	}

	private void showSmileysPopup(View anchor) {
        if (mSmileyPopup == null)
            mSmileyPopup = MessageUtils.smileysPopup(this, mSmileySelectListener);
        mSmileyPopup.show(anchor);
	}
	
	private void requestPlay(){
		if(mServiceAdapter != null && mServiceAdapter.isServiceAuthenticated()){
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
            		ChatWindow.this,
                    android.R.layout.select_dialog_singlechoice);
            arrayAdapter.add("سنگ کاغذ قیچی");
            arrayAdapter.add("دوز");
            //arrayAdapter.add("نبرد کشتی ها");
            
			AlertDialog.Builder chooseGameDialog = new AlertDialog.Builder(
					ChatWindow.this);
            chooseGameDialog.setIcon(R.drawable.ic_launcher);
            chooseGameDialog.setTitle(R.string.choose_a_game);
            chooseGameDialog.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

            chooseGameDialog.setAdapter(arrayAdapter, 
            		new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
	                        	sendMessage(RPSWindow.INVITE_MSG);
								break;
							case 1:
	                        	sendMessage(TTTWindow.INVITE_MSG);
								break;
							case 2:
								sendMessage(BattleshipWindow.INVITE_MSG);
								break;
							}
                            dialog.dismiss();
						}
					});
            chooseGameDialog.show();
		}
		else{
			Toast tNotSent = IcsToast.makeText(this, getString(R.string.you_are_not_online), IcsToast.LENGTH_SHORT);
			tNotSent.show();
		}
	}
	
	private void markAsReadDelayed(final int id, final int delay) {
		new Thread() {
			@Override
			public void run() {
				try { Thread.sleep(delay); } catch (Exception e) {}
				markAsRead(id);
			}
		}.start();
	}
	
	private void markAsRead(int id) {
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY
			+ "/" + ChatProvider.TABLE_NAME + "/" + id);
		Log.d(TAG, "markAsRead: " + rowuri);
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		getContentResolver().update(rowuri, values, null, null);
	}

	class ChatWindowAdapter extends SimpleCursorAdapter {
		String mScreenName, mJID;

		ChatWindowAdapter(Cursor cursor, String[] from, int[] to,
				String JID, String screenName) {
			super(ChatWindow.this, android.R.layout.simple_list_item_1, cursor,
					from, to);
			mScreenName = screenName;
			mJID = JID;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ChatItemWrapper wrapper = null;
			Cursor cursor = this.getCursor();
			cursor.moveToPosition(position);

			long dateMilliseconds = cursor.getLong(cursor
					.getColumnIndex(ChatProvider.ChatConstants.DATE));

			int _id = cursor.getInt(cursor
					.getColumnIndex(ChatProvider.ChatConstants._ID));
			String date = getDateString(dateMilliseconds);
			String message = cursor.getString(cursor
					.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
			boolean from_me = (cursor.getInt(cursor
					.getColumnIndex(ChatProvider.ChatConstants.DIRECTION)) ==
					ChatConstants.OUTGOING);
			String jid = cursor.getString(cursor
					.getColumnIndex(ChatProvider.ChatConstants.JID));
			int delivery_status = cursor.getInt(cursor
					.getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS));

			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.chatrow, null);
				wrapper = new ChatItemWrapper(row, ChatWindow.this);
				row.setTag(wrapper);
			} else {
				wrapper = (ChatItemWrapper) row.getTag();
			}

			if (!from_me && delivery_status == ChatConstants.DS_NEW) {
				markAsReadDelayed(_id, DELAY_NEWMSG);
			}

			String from = jid;
			if (jid.equals(mJID))
				from = mScreenName;
			wrapper.populateFrom(date, from_me, from, message, delivery_status);

			return row;
		}
	}

	private String getDateString(long milliSeconds) {
		SimpleDateFormat dateFormater = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
		Date date = new Date(milliSeconds);
		return dateFormater.format(date);
	}

	public class ChatItemWrapper {
		private LinearLayout mLinearLayout = null;
		private TextView mDateView = null;
		private TextView mFromView = null;
		private TextView mMessageView = null;
		private ImageView mIconView = null;
		private Button btnAccept = null;
		private Button btnDeny = null;
		private Boolean isSystemMsg = null;

		private final View mRowView;
		private ChatWindow chatWindow;

		ChatItemWrapper(View row, ChatWindow chatWindow) {
			this.mRowView = row;
			this.chatWindow = chatWindow;
		}

		void populateFrom(String date, boolean from_me, String from, final String message,
				int delivery_status) {
//			Log.i(TAG, "populateFrom(" + from_me + ", " + from + ", " + message + ")");
			String myMessage = message;
			getDateView().setText(date);
			TypedValue tv = new TypedValue();
			if (from_me) {
				getTheme().resolveAttribute(R.attr.ChatMsgHeaderMeColor, tv, true);
				getDateView().setTextColor(tv.data);
				getFromView().setText(getString(R.string.chat_from_me));
				getFromView().setTextColor(tv.data);
				getLinearLayout().setBackgroundResource(R.drawable.bubble_me);
				getLinearLayout().setGravity(Gravity.RIGHT);
				((LinearLayout)mRowView).setGravity(Gravity.RIGHT);
			} else {
				getTheme().resolveAttribute(R.attr.ChatMsgHeaderYouColor, tv, true);
				getDateView().setTextColor(tv.data);
				getFromView().setText(from + ":");
				getFromView().setTextColor(tv.data);
				getLinearLayout().setBackgroundResource(R.drawable.bubble_notme);
				getLinearLayout().setGravity(Gravity.LEFT);
				((LinearLayout)mRowView).setGravity(Gravity.LEFT);
			}
			isSystemMsg = false;
			btnAccept = (Button) getLinearLayout().findViewById(R.id.btnAccept);
			btnDeny = (Button) getLinearLayout().findViewById(R.id.btnDeny);
			btnAccept.setVisibility(View.GONE);
			btnDeny.setVisibility(View.GONE);
			
			if(message.endsWith(GameWindow.INVITE_CODE)){
				isSystemMsg = true;
				if(from_me){
					myMessage = getString(R.string.you_invited_to_game);
				}
				else{
					myMessage = getString(R.string.would_you_accept);
					btnAccept.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							Intent game = null;
							if(message.startsWith(RPSWindow.RPS_GAME)){
								sendMessage(RPSWindow.ACCEPT_MSG);
								game = new Intent(mActivity, RPSWindow.class);
							}
							else if(message.startsWith(TTTWindow.TTT_GAME)){
								sendMessage(TTTWindow.ACCEPT_MSG);
								game = new Intent(mActivity, TTTWindow.class);
							}							
							else if(message.startsWith(BattleshipWindow.BATTLESHIP_GAME)){
								sendMessage(BattleshipWindow.ACCEPT_MSG);
								game = new Intent(mActivity, BattleshipWindow.class);
							}
							game.putExtra("jid", mWithJabberID);
							game.putExtra("isGuest", true);
							mActivity.startActivity(game);
						}
					});
					btnAccept.setVisibility(View.VISIBLE);
					btnDeny.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							sendMessage(RPSWindow.DENY_MSG);
						}
					});
					btnDeny.setVisibility(View.VISIBLE);
				}
			}
			else if(message.endsWith(GameWindow.ACCEPT_CODE)){
				isSystemMsg = true;
				if(from_me)
					myMessage = getString(R.string.you_accepted_invite);
				else
					myMessage = getString(R.string.she_accepted_invite);
			}
			else if(message.endsWith(GameWindow.DENY_CODE)){
				isSystemMsg = true;
				if(from_me)
					myMessage = getString(R.string.you_denied_invite);
				else
					myMessage = getString(R.string.she_denied_invite);
			}
			
			switch (delivery_status) {
			case ChatConstants.DS_NEW:
				ColorDrawable layers[] = new ColorDrawable[2];
				getTheme().resolveAttribute(R.attr.ChatNewMessageColor, tv, true);
				layers[0] = new ColorDrawable(tv.data);
				if (from_me) {
					// message stored for later transmission
					getTheme().resolveAttribute(R.attr.ChatStoredMessageColor, tv, true);
					layers[1] = new ColorDrawable(tv.data);
				} else {
					layers[1] = new ColorDrawable(0x00000000);
				}
				TransitionDrawable backgroundColorAnimation = new
					TransitionDrawable(layers);
				int l = mRowView.getPaddingLeft();
				int t = mRowView.getPaddingTop();
				int r = mRowView.getPaddingRight();
				int b = mRowView.getPaddingBottom();
				mRowView.setBackgroundDrawable(backgroundColorAnimation);
				mRowView.setPadding(l, t, r, b);
				backgroundColorAnimation.setCrossFadeEnabled(true);
				backgroundColorAnimation.startTransition(DELAY_NEWMSG);
				getIconView().setImageResource(R.drawable.ic_chat_msg_status_queued);
				break;
			case ChatConstants.DS_SENT_OR_READ:
				getIconView().setImageResource(R.drawable.ic_chat_msg_status_unread);
				mRowView.setBackgroundColor(0x00000000); // default is transparent
				break;
			case ChatConstants.DS_ACKED:
				getIconView().setImageResource(R.drawable.ic_chat_msg_status_ok);
				mRowView.setBackgroundColor(0x00000000); // default is transparent
				break;
			case ChatConstants.DS_FAILED:
				getIconView().setImageResource(R.drawable.ic_chat_msg_status_failed);
				mRowView.setBackgroundColor(0x30ff0000); // default is transparent
				break;
			}
			getMessageView().setText(myMessage);
			getMessageView().setTextSize(TypedValue.COMPLEX_UNIT_SP, chatWindow.mChatFontSize);
			if(isSystemMsg)
				getMessageView().setTextColor(getResources().getColor(R.color.system_message_color));
			else
				getMessageView().setTextColor(getResources().getColor(R.color.person_message_color));
			getDateView().setTextSize(TypedValue.COMPLEX_UNIT_SP, chatWindow.mChatFontSize*2/3);
			getFromView().setTextSize(TypedValue.COMPLEX_UNIT_SP, chatWindow.mChatFontSize*2/3);
			
		}
		
		TextView getDateView() {
			if (mDateView == null) {
				mDateView = (TextView) mRowView.findViewById(R.id.chat_date);
			}
			return mDateView;
		}

		TextView getFromView() {
			if (mFromView == null) {
				mFromView = (TextView) mRowView.findViewById(R.id.chat_from);
			}
			return mFromView;
		}

		TextView getMessageView() {
			if (mMessageView == null) {
				mMessageView = (TextView) mRowView
						.findViewById(R.id.chat_message);
			}
			return mMessageView;
		}

		ImageView getIconView() {
			if (mIconView == null) {
				mIconView = (ImageView) mRowView
						.findViewById(R.id.iconView);
			}
			return mIconView;
		}
		
		LinearLayout getLinearLayout(){
			if(mLinearLayout == null)
				mLinearLayout = (LinearLayout) mRowView.findViewById(R.id.chat_linear_layout);
			return mLinearLayout;
		}

	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& keyCode == KeyEvent.KEYCODE_ENTER) {
			sendMessageIfNotNull();
			return true;
		}
		return false;

	}

	public void afterTextChanged(Editable s) {
		if (mChatInput.getText().length() >= 1) {
			mChatInput.setOnKeyListener(this);
			mSendButton.setEnabled(true);
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub

	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	private void showToastNotification(int message) {
		Toast toastNotification = Toast.makeText(this, message,
				Toast.LENGTH_SHORT);
		toastNotification.show();
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainWindow.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private static final String[] STATUS_QUERY = new String[] {
		RosterProvider.RosterConstants.STATUS_MODE,
		RosterProvider.RosterConstants.STATUS_MESSAGE,
	};
	private void updateContactStatus() {
		Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI, STATUS_QUERY,
					RosterProvider.RosterConstants.JID + " = ?", new String[] { mWithJabberID }, null);
		int MODE_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE);
		int MSG_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE);

		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			int status_mode = cursor.getInt(MODE_IDX);
			String status_message = cursor.getString(MSG_IDX);
			Log.d(TAG, "contact status changed: " + status_mode + " " + status_message);
			mSubTitle.setVisibility((status_message != null && status_message.length() != 0)?
					View.VISIBLE : View.GONE);
			
			String mStatus = BereshtookApplication.getConfig(this).statusMessage;
			if(status_message != null && status_message.contains("#") && mStatus != null && mStatus.contains("#"))
				mSubTitle.setText(LocationUtil.findDistance(BereshtookApplication.getConfig(this).statusMessage, status_message));
			else
				mSubTitle.setText("");
			
			if (mServiceAdapter == null || !mServiceAdapter.isServiceAuthenticated())
				status_mode = 0; // override icon if we are offline
			mStatusMode.setImageResource(StatusMode.values()[status_mode].getDrawableId());
		}
		cursor.close();
	}

	private class ContactObserver extends ContentObserver {
		public ContactObserver() {
			super(new Handler());
		}

		public void onChange(boolean selfChange) {
			Log.d(TAG, "ContactObserver.onChange: " + selfChange);
			updateContactStatus();
		}
	}
}
