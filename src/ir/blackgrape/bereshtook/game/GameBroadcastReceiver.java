package ir.blackgrape.bereshtook.game;

import ir.blackgrape.bereshtook.game.battleship.BattleshipWindow;
import ir.blackgrape.bereshtook.game.dotline.DotlineWindow;
import ir.blackgrape.bereshtook.game.rps.RPSWindow;
import ir.blackgrape.bereshtook.game.ttt.TTTWindow;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GameBroadcastReceiver extends BroadcastReceiver {

	private static GameWindow mGameWindow;
	private static Context mContext;
	private static boolean isGameSet = false;
	private static boolean noEffect = false;
	
	public static void setContext(Context context){
		mContext = context;
	}
	public static void setNoEffect(boolean _noEffect){
		noEffect = _noEffect;
	}
	
	public static void setGame(GameWindow gameWindow){
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
			Intent game = null;
			if(message.startsWith(RPSWindow.RPS_GAME))
				game = new Intent(mContext, RPSWindow.class);
			else if(message.startsWith(TTTWindow.TTT_GAME))
				game = new Intent(mContext, TTTWindow.class);
			else if(message.startsWith(DotlineWindow.DOTLINE_GAME))
				game = new Intent(mContext, DotlineWindow.class);
			else if(message.startsWith(BattleshipWindow.BATTLESHIP_GAME))
				game = new Intent(mContext, BattleshipWindow.class);
			
			game.putExtra("jid", from);
			game.putExtra("isGuest", false);
			game.putExtra("noEffect", noEffect);
			game.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(game);
		}
		else if(isGameSet && from.equals(mGameWindow.getWithJabberID()))
			mGameWindow.onReceiveMsg(message);
	}

}
