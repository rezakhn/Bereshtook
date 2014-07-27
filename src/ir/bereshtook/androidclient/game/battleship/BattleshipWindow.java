package ir.bereshtook.androidclient.game.battleship;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import ir.bereshtook.androidclient.game.GameWindow;

public class BattleshipWindow extends GameWindow {

	public static final String BATTLESHIP_GAME = GAME_CODE + "BATTLESHIP#";
	public static final String INVITE_MSG = BATTLESHIP_GAME + "INVITE#";
	public static final String ACCEPT_MSG = BATTLESHIP_GAME + "ACCEPT#";
	public static final String DENY_MSG = BATTLESHIP_GAME + "DENY#";
	
	private GLSurfaceView mGLView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mGLView = new GLBattleshipView(this);
		setContentView(mGLView);
	}	
	
	@Override
	protected void receiveMsg(String msg) {

	}

	@Override
	protected void startGame() {

	}

}
