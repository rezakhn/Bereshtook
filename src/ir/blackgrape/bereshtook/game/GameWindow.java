package ir.blackgrape.bereshtook.game;

import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.XMPPDataServiceAdapter;
import ir.blackgrape.bereshtook.chat.XMPPChatServiceAdapter;
import ir.blackgrape.bereshtook.service.IXMPPChatService;
import ir.blackgrape.bereshtook.service.IXMPPDataService;
import ir.blackgrape.bereshtook.service.XMPPService;
import ir.blackgrape.bereshtook.util.PRIVATE_DATA;
import ir.blackgrape.bereshtook.util.StringUtil;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.internal.widget.IcsToast;

public abstract class GameWindow extends SherlockActivity {

	private static final String TAG = "Bereshtook.GameWindow";
	public static final String GAME_CODE = "#BERESHTOOK#GAME#";
	public static final String STATUS_MSG = GAME_CODE + "STATUS#";
	public static final String INVITE_CODE = "INVITE#";
	public static final String ACCEPT_CODE = "ACCEPT#";
	public static final String DENY_CODE = "DENY#";
	public static final String EXIT_CODE = "EXIT#";

	private Intent mServiceIntent;
	private ServiceConnection mServiceConnection;
	protected XMPPChatServiceAdapter mServiceAdapter;

	private Intent dataServiceIntent;
	private ServiceConnection dataServiceConnection;
	private XMPPDataServiceAdapter dataServiceAdapter;
	protected Integer mCoins = null;

	protected TextView txtStatusUp;
	protected TextView txtStatusDown;

	protected String withJabberID;
	protected Boolean isGuest;
	protected Context mContext;

	protected MediaPlayer soundCheer;
	protected MediaPlayer soundCry;
	protected MediaPlayer soundWin;
	protected MediaPlayer soundLose;
	protected MediaPlayer soundDraw;
	protected MediaPlayer soundChoice;
	protected MediaPlayer soundError;
	protected boolean dataSaved = false;

	protected abstract Game getGame();

	protected abstract void onReceiveMsg(String msg);

	protected abstract void startGame();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		withJabberID = getIntent().getStringExtra("jid");
		GameBroadcastReceiver.setGame(this, withJabberID);
		isGuest = getIntent().getBooleanExtra("isGuest", false);

		soundCheer = MediaPlayer.create(this, R.raw.sound_cheer);
		soundCry = MediaPlayer.create(this, R.raw.sound_cry);
		soundChoice = MediaPlayer.create(this, R.raw.sound_choice);
		soundWin = MediaPlayer.create(this, R.raw.sound_win);
		soundDraw = MediaPlayer.create(this, R.raw.sound_draw);
		soundLose = MediaPlayer.create(this, R.raw.sound_lose);
		soundError = MediaPlayer.create(this, R.raw.sound_error);
		registerXMPPService();
		registerDataService();
	}

	protected void endGame() {
		if (getGame().getMyScore() > getGame().getHerScore()) {
			soundCheer.start();
			if (mCoins == null)
				mCoins = loadCoins();
			mCoins += 200;
			saveCoins(mCoins);
			saveData(PRIVATE_DATA.WINS, loadData(PRIVATE_DATA.WINS) + 1);
			saveData(PRIVATE_DATA.LEFTS, loadData(PRIVATE_DATA.LEFTS) - 1);
			winDialog();
		} else if (getGame().getMyScore() < getGame().getHerScore()) {
			soundCry.start();
			saveData(PRIVATE_DATA.LOSSES, loadData(PRIVATE_DATA.LOSSES) + 1);
			saveData(PRIVATE_DATA.LEFTS, loadData(PRIVATE_DATA.LEFTS) - 1);
			loseDialog();
		}
	}

	private void loseDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(R.drawable.ic_coins);
		dialog.setTitle(R.string.lose_game_title);
		dialog.setMessage(getString(R.string.lose_game_message, StringUtil.convertToPersian(Integer.valueOf(100).toString())));
		dialog.setPositiveButton(R.string.lose_game_button,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		dialog.setCancelable(false);
		dialog.show();
	}

	private void winDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(R.drawable.ic_coins);
		dialog.setTitle(R.string.won_game_title);
		dialog.setMessage(getString(R.string.won_game_message, StringUtil.convertToPersian(Integer.valueOf(100).toString())));
		dialog.setPositiveButton(R.string.won_game_button,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		dialog.setCancelable(false);
		dialog.show();
	}

	protected void sheLeft() {
		mCoins += 100;
		saveCoins(mCoins);
		saveData(PRIVATE_DATA.LEFTS, loadData(PRIVATE_DATA.LEFTS) - 1);

		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.opponent_leaved_title)
				.setMessage(R.string.opponent_leaved_message)
				.setCancelable(false)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								finish();
							}
						}).show();
	}

	protected void sendMsg(String msg) {
		if (weAreOnline())
			mServiceAdapter.sendMessage(withJabberID, msg);
		else {
			Toast tNotSent = IcsToast.makeText(this, "you are not online",
					IcsToast.LENGTH_SHORT);
			tNotSent.show();
		}
	}

	protected Boolean weAreOnline() {
		if (mServiceAdapter != null && mServiceAdapter.isServiceAuthenticated())
			return true;
		return false;
	}

	public static Boolean isGameMsg(String msg) {
		if (msg == null)
			return false;
		return msg.startsWith(GAME_CODE) && !msg.endsWith(INVITE_CODE)
				&& !msg.endsWith(ACCEPT_CODE) && !msg.endsWith(DENY_CODE);
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

	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mServiceIntent = new Intent(this, XMPPService.class);
		Uri chatURI = Uri.parse(withJabberID);
		mServiceIntent.setData(chatURI);
		mServiceIntent.setAction("ir.blackgrape.bereshtook.XMPPSERVICE");

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

	private void registerDataService() {
		Log.i(TAG, "called startGameService()");
		dataServiceIntent = new Intent(this, XMPPService.class);
		dataServiceIntent.setAction("ir.blackgrape.bereshtook.XMPPSERVICE2");

		dataServiceConnection = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {

				dataServiceAdapter = new XMPPDataServiceAdapter(
						IXMPPDataService.Stub.asInterface(service));
				if (!dataSaved) {
					mCoins = loadCoins();
					if (mCoins != null) {
						mCoins -= 100;
						saveCoins(mCoins);
					}
					saveData(PRIVATE_DATA.PLAYED_GAMES,
							loadData(PRIVATE_DATA.PLAYED_GAMES) + 1);
					saveData(PRIVATE_DATA.LEFTS,
							loadData(PRIVATE_DATA.LEFTS) + 1);
					dataSaved = true;
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "Game service called onServiceDisconnected()");
			}
		};
	}

	protected OnClickListener statusClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			LayoutInflater li = LayoutInflater.from(mContext);
			View promptsView = li.inflate(R.layout.prompts, null);

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
					mContext);

			alertDialogBuilder.setView(promptsView);

			final EditText userInput = (EditText) promptsView
					.findViewById(R.id.editTextDialogUserInput);

			alertDialogBuilder
					.setCancelable(false)
					.setPositiveButton(R.string.go,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									txtStatusDown.setText(userInput.getText());
									sendMsg(STATUS_MSG + userInput.getText());
								}
							})
					.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});

			AlertDialog alertDialog = alertDialogBuilder.create();

			alertDialog.show();
		}
	};

	protected Integer loadCoins() {
		if (dataServiceAdapter == null)
			return null;

		String strCoins = dataServiceAdapter.loadGameData(PRIVATE_DATA.COINS);
		if (strCoins != null)
			return Integer.parseInt(strCoins);
		return null;
	}

	protected void saveCoins(Integer coins) {
		if (dataServiceAdapter == null || coins == null)
			return;
		if (coins < 0)
			coins = 0;
		dataServiceAdapter.saveGameData(PRIVATE_DATA.COINS, coins.toString());
	}

	protected Integer loadData(String key) {
		if (dataServiceAdapter == null || key == null)
			return null;

		String strValue = dataServiceAdapter.loadGameData(key);
		if (strValue != null)
			return Integer.parseInt(strValue);
		return null;
	}

	protected void saveData(String key, Integer value) {
		if (dataServiceAdapter == null || value == null)
			return;
		dataServiceAdapter.saveGameData(key, value.toString());
	}

	@Override
	protected void onResume() {
		super.onResume();
		bindXMPPServices();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindXMPPServices();
	}

	private void bindXMPPServices() {
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		bindService(dataServiceIntent, dataServiceConnection, BIND_AUTO_CREATE);
	}

	private void unbindXMPPServices() {
		try {
			unbindService(mServiceConnection);
			unbindService(dataServiceConnection);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't bound!");
		}
	}
}
