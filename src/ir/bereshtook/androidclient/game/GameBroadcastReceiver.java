package ir.bereshtook.androidclient.game;

import ir.bereshtook.androidclient.game.rps.RPSWindow;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GameBroadcastReceiver extends BroadcastReceiver {

	private static GameWindow mGameWindow;
	private static Context mContext;
	private static Boolean isGameSet = false;
	
	public static void setContext(Context context){
		mContext = context;
	}
	
	public static void setGame(GameWindow gameWindow, String jid){
		mGameWindow = gameWindow;
		isGameSet = true;
	}
	public static void removeGame(){
		mGameWindow = null;
		isGameSet = false;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final String from = intent.getStringExtra("from");
		final String message = intent.getStringExtra("message");
		
		if(message.endsWith(GameWindow.ACCEPT_CODE) && mContext != null && mGameWindow == null){
			Intent game = new Intent(mContext, RPSWindow.class);
			game.putExtra("jid", from);
			game.putExtra("isGuest", false);
			mContext.startActivity(game);
		}
		else if(isGameSet)
			mGameWindow.receiveMsg(message);

	}

}
