package ir.blackgrape.bereshtook.game;

import ir.blackgrape.bereshtook.BereshtookApplication;
import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.XMPPDataServiceAdapter;
import ir.blackgrape.bereshtook.chat.XMPPChatServiceAdapter;
import ir.blackgrape.bereshtook.data.BereshtookConfiguration;
import ir.blackgrape.bereshtook.service.IXMPPChatService;
import ir.blackgrape.bereshtook.service.IXMPPDataService;
import ir.blackgrape.bereshtook.service.XMPPService;
import ir.blackgrape.bereshtook.util.PRIVATE_DATA;
import ir.blackgrape.bereshtook.util.PreferenceConstants;
import ir.blackgrape.bereshtook.util.StringUtil;

import java.util.Map;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
	public static final int ONE_SECOND = 1000;

	private Intent mServiceIntent;
	private ServiceConnection mServiceConnection;
	protected XMPPChatServiceAdapter mServiceAdapter;

	private Intent dataServiceIntent;
	private ServiceConnection dataServiceConnection;
	private XMPPDataServiceAdapter dataServiceAdapter;
	private Integer mCoins;
	private Integer mLefts;
	private Integer mPlayedGames;
	private Integer mWins;
	private Integer mLosses;
	private Map<String, String> gameMap;

	protected TextView txtStatusUp;
	protected TextView txtStatusDown;

	protected String withJabberID;
	protected boolean isGuest;
	protected boolean noEffect;
	protected Context mContext;

	protected MediaPlayer soundCheer;
	protected MediaPlayer soundCry;
	protected MediaPlayer soundWin;
	protected MediaPlayer soundLose;
	protected MediaPlayer soundDraw;
	protected MediaPlayer soundChoice;
	protected MediaPlayer soundError;
	protected MediaPlayer soundBeep;
	protected boolean dataSaved = false;
	protected boolean gameEnded = false;
	
	protected CountDownTimer myTimer;
	protected CountDownTimer herTimer;
	protected TextView txtMyTimer;
	protected TextView txtHerTimer;
	
	private BereshtookConfiguration mConfig;
	private boolean finished = false;

	protected abstract Game getGame();
	protected abstract void onReceiveMsg(String msg);
	protected abstract void startGame();
	protected abstract String getExitMsg();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		mConfig = BereshtookApplication.getConfig(this);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		withJabberID = getIntent().getStringExtra("jid");
		GameBroadcastReceiver.setGame(this, withJabberID);
		isGuest = getIntent().getBooleanExtra("isGuest", false);
		noEffect = getIntent().getBooleanExtra("noEffect", false);

		soundCheer = MediaPlayer.create(this, R.raw.sound_cheer);
		soundCry = MediaPlayer.create(this, R.raw.sound_cry);
		soundChoice = MediaPlayer.create(this, R.raw.sound_choice);
		soundWin = MediaPlayer.create(this, R.raw.sound_win);
		soundDraw = MediaPlayer.create(this, R.raw.sound_draw);
		soundLose = MediaPlayer.create(this, R.raw.sound_lose);
		soundError = MediaPlayer.create(this, R.raw.sound_error);
		soundBeep = MediaPlayer.create(this, R.raw.sound_beep);
		registerXMPPService();
		registerDataService();
	}

	protected void endGame() {
		gameEnded = true;
		if (getGame().getMyScore() > getGame().getHerScore()) {
			soundCheer.start();
			mCoins += 200;
			mLefts--;
			mWins++;
			asyncSave();
			winDialog();
		} else if (getGame().getMyScore() < getGame().getHerScore()) {
			soundCry.start();
			mLefts--;
			mLosses++;
			asyncSave();
			loseDialog();
		}
	}

	private void asyncSave() {
		if(noEffect)
			return;
		FinalCommit fc = new FinalCommit();
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			fc.executeOnExecutor(FinalCommit.THREAD_POOL_EXECUTOR);
		else
			fc.execute();
	}

	private void save() {
		PreferenceManager.getDefaultSharedPreferences(this).edit()
		.putInt(PreferenceConstants.COINS, mCoins)
		.commit();
		gameMap.put(PRIVATE_DATA.COINS, mCoins.toString());
		gameMap.put(PRIVATE_DATA.PLAYED_GAMES, mPlayedGames.toString());
		gameMap.put(PRIVATE_DATA.LEFTS, mLefts.toString());
		gameMap.put(PRIVATE_DATA.WINS, mWins.toString());
		gameMap.put(PRIVATE_DATA.LOSSES, mLosses.toString());
		
		if(dataServiceAdapter != null)
			dataServiceAdapter.saveGameData(gameMap);
	}
	
	private boolean load(){
		if(dataServiceAdapter != null)
			gameMap = dataServiceAdapter.loadGameData();
		if(gameMap == null)
			return false;
		mCoins = Integer.valueOf(gameMap.get(PRIVATE_DATA.COINS));
		mLefts = Integer.valueOf(gameMap.get(PRIVATE_DATA.LEFTS));
		mPlayedGames = Integer.valueOf(gameMap.get(PRIVATE_DATA.PLAYED_GAMES));
		mWins = Integer.valueOf(gameMap.get(PRIVATE_DATA.WINS));
		mLosses = Integer.valueOf(gameMap.get(PRIVATE_DATA.LOSSES));
		return true;
	}

	private void loseDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(R.drawable.ic_coins);
		dialog.setTitle(R.string.lose_game_title);
		int amount = noEffect ? 0 : 100;
		dialog.setMessage(getString(R.string.lose_game_message, StringUtil.convertToPersian(Integer.valueOf(amount).toString())));
		dialog.setPositiveButton(R.string.lose_game_button,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(finished)
							finish();
						else
							finished = true;
					}
				});
		dialog.setCancelable(false);
		dialog.show();
	}

	private void winDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(R.drawable.ic_coins);
		dialog.setTitle(R.string.won_game_title);
		int amount = noEffect ? 0 : 100;
		dialog.setMessage(getString(R.string.won_game_message, StringUtil.convertToPersian(Integer.valueOf(amount).toString())));
		dialog.setPositiveButton(R.string.won_game_button,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(finished)
							finish();
						else
							finished = true;
					}
				});
		dialog.setCancelable(false);
		dialog.show();
	}

	protected void sheLeft() {
		gameEnded = true;
		mCoins += 100;
		mLefts--;
		mPlayedGames--;
		asyncSave();
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
								if(finished)
									finish();
								else
									finished = true;
							}
						}).show();
	}

	protected void timeOutDialog() {
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(R.string.time_out_title)
		.setMessage(R.string.time_out_message)
		.setCancelable(false)
		.setPositiveButton(R.string.sorry,
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
				if (!dataSaved && !noEffect) {
					InitCommit ic = new InitCommit();
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
						ic.executeOnExecutor(InitCommit.THREAD_POOL_EXECUTOR);
					else
						ic.execute();
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
	
	class InitCommit extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			boolean success = false;
			int count = 0;
			do{
				if(count > 10){
					loadProblemDialog();
					return null;
				}
				success = load();
				count++;
			}while(!success);
			
			mCoins -= 100;
			mLefts++;
			mPlayedGames++;
			save();
			return null;
		}
	}

	class FinalCommit extends AsyncTask<Void, Void, Void>{
		private ProgressDialog waitDialog;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			waitDialog = new ProgressDialog(GameWindow.this);
			waitDialog.setCancelable(false);
			waitDialog.show();
		}
		@Override
		protected Void doInBackground(Void... params) {
			save();
			return null;
		}
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			waitDialog.cancel();
			if(finished)
				GameWindow.this.finish();
			else
				finished = true;
		}
	}
	
	private void loadProblemDialog(){
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(R.string.load_problem_title)
		.setMessage(R.string.load_problem_message)
		.setCancelable(false)
		.setPositiveButton(R.string.load_problem_button,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
							sendMsg(getExitMsg());
							finish();
					}
				}).show();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(myTimer != null)
			myTimer.cancel();
		if(herTimer != null)
			herTimer.cancel();
		if(!gameEnded){
			sendMsg(getExitMsg());
        	finish();
		}		
	}
	
	@Override
	public void onBackPressed() {
	    new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle(R.string.exit_game_title)
	        .setMessage(R.string.exit_game_message)
	        .setPositiveButton(R.string.exit_game_confirm, new DialogInterface.OnClickListener()
		    {
		        @Override
		        public void onClick(DialogInterface dialog, int which) {
		        	sendMsg(getExitMsg());
		            finish();
		        }
	
		    })
		    .setNegativeButton(R.string.exit_game_cancel, null)
		    .show();
	}
	
	public String getWithJabberID(){
		return withJabberID;
	}
}
