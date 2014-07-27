package ir.bereshtook.androidclient.game.battleship;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GLBattleshipView extends GLSurfaceView {

	private GLBattleshipRenderer renderer;

	public GLBattleshipView(Context context) {
		super(context);
		
		setEGLContextClientVersion(2);
		setRenderer(renderer);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

}
