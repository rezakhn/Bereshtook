package ir.bereshtook.androidclient.game.battleship;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Ship {
	private FloatBuffer vertexBuffer;
	
	static final int COORDS_PER_VERTEX = 3;
	static final float shipCoords[] = {
        0.0f,  0.622008459f, 0.0f,
       -0.5f, -0.311004243f, 0.0f,
        0.5f, -0.311004243f, 0.0f 
	};
	
	float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };
	
	public Ship(){
		ByteBuffer bb = ByteBuffer.allocateDirect(shipCoords.length * 4);
		bb.order(ByteOrder.nativeOrder());
		vertexBuffer = bb.asFloatBuffer();
		vertexBuffer.put(shipCoords);
		vertexBuffer.position(0);
	}
}
