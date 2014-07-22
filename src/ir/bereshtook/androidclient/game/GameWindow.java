package ir.bereshtook.androidclient.game;

import ir.bereshtook.androidclient.R;
import ir.bereshtook.androidclient.chat.XMPPChatServiceAdapter;
import ir.bereshtook.androidclient.service.IXMPPChatService;
import ir.bereshtook.androidclient.service.XMPPService;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.internal.widget.IcsToast;

public abstract class GameWindow extends SherlockActivity {

	private static final String TAG = "Bereshtook.GameWindow";
	public static final String GAME_CODE = "#BERESHTOOK#GAME#";
	public static final String INVITE_CODE = "INVITE#";
	public static final String ACCEPT_CODE = "ACCEPT#";
	public static final String DENY_CODE = "DENY#";
	public static final String EXIT_CODE = "EXIT#";
	
	private Intent mServiceIntent;
	private ServiceConnection mServiceConnection;
	protected XMPPChatServiceAdapter mServiceAdapter;
	
	protected String withJabberID;
	protected Boolean isGuest;
	
	protected abstract void receiveMsg(String msg);
	protected abstract void startGame();
	
	
	protected void sendMsg(String msg){
		if(weAreOnline())
			mServiceAdapter.sendMessage(withJabberID, msg);
		else{
			Toast tNotSent = IcsToast.makeText(this, "you are not online", IcsToast.LENGTH_SHORT);
			tNotSent.show();
		}
	}
	
	protected Boolean weAreOnline(){
		if(mServiceAdapter != null && mServiceAdapter.isServiceAuthenticated())
			return true;
		return false;
	}
	
	public static Boolean isGameMsg(String msg){
		if(msg == null)
			return false;
		return msg.startsWith(GAME_CODE) && !msg.endsWith(INVITE_CODE) 
				&& !msg.endsWith(ACCEPT_CODE) && !msg.endsWith(DENY_CODE);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		withJabberID =  getIntent().getStringExtra("jid");
		GameBroadcastReceiver.setGame(this, withJabberID);
		isGuest = getIntent().getBooleanExtra("isGuest", false);
		registerXMPPService();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		GameBroadcastReceiver.removeGame();
	}
	
	public void invitationAccepted(String from) {
		Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("invite Game");
		dialog.setMessage(from + " accepted your invitaion!");
		dialog.setNeutralButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
		dialog.create();
		dialog.show();
	}
	
	protected void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mServiceIntent = new Intent(this, XMPPService.class);
		Uri chatURI = Uri.parse(withJabberID);
		mServiceIntent.setData(chatURI);
		mServiceIntent.setAction("ir.bereshtook.androidclient.XMPPSERVICE");

		mServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				mServiceAdapter = new XMPPChatServiceAdapter(
						IXMPPChatService.Stub.asInterface(service),
						withJabberID);
			}
			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}

		};
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus)
			bindXMPPService();
		else
			unbindXMPPService();
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
	
}
