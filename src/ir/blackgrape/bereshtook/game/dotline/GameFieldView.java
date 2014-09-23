package ir.blackgrape.bereshtook.game.dotline;

import ir.blackgrape.bereshtook.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class GameFieldView extends View implements OnTouchListener {

    public static int BOX_LENGTH = 50;
    public static int PADDING = 5;

    private GameField gameField;
    private volatile Line myLastLine;
    private volatile String myLastMessage;
    private volatile Line herLastLine;

    public GameFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(GameField spielfeld) {
        this.gameField = spielfeld;
        setOnTouchListener(this);
    }

    public Line getMyLastLine() {
        return myLastLine;
    }

    public void resetMyLastLine() {
        myLastLine = null;
    }
    
    public String getMyLastMessage(){
    	return myLastMessage;
    }
    
    public void resetMyLastMessage(){
    	myLastMessage = null;
    }
    
    public Line getHerLastLine() {
        return herLastLine;
    }

    public void resetHerLastLine() {
        herLastLine = null;
    }    

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        if (gameField == null)
            return;

        int maxBreite = (w - PADDING * 2) / gameField.getBreiteInBox();
        int maxHoehe = (h - PADDING * 2) / gameField.getHoeheInBox();
        BOX_LENGTH = Math.min(maxBreite, maxHoehe);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(getResources().getColor(R.color.dotline_background));

        if (gameField == null) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), new Paint());
            return;
        }

        for (Box box : gameField.getBoxesList())
            box.onDraw(canvas);
    }

    public boolean onTouch(View view, MotionEvent event) {

        if (event.getAction() != MotionEvent.ACTION_DOWN)
            return true;

        if (myLastLine != null)
            return true;

        int errechnetRasterX = (int) event.getX() / BOX_LENGTH;
        int errechnetRasterY = (int) event.getY() / BOX_LENGTH;

        Box box = gameField.getBox(errechnetRasterX, errechnetRasterY);

        if (box == null || box.getOwner() != null)
            return true;

        Line line = box.findLine((int) event.getX(), (int) event.getY());
        String lineDir = box.findLineDir((int) event.getX(), (int) event.getY());

        if (line == null)
            return true;

        myLastLine = line;
        myLastMessage = DotlineWindow.LINE_CODE +  errechnetRasterX + "#" + errechnetRasterY + "#" + lineDir;

        synchronized (this) {
            this.notifyAll();
        }

        return true;
    }
    
    public void onReceiveMove(String message){
    	message = message.replaceFirst(DotlineWindow.LINE_CODE, "");
    	String[] splited = message.split("#");
    	int errechnetRasterX = Integer.valueOf(splited[0]);
    	int errechnetRasterY = Integer.valueOf(splited[1]);
    	Box box = gameField.getBox(errechnetRasterX, errechnetRasterY);
    	char dir = splited[2].charAt(0);
    	switch (dir) {
			case 'U':
				herLastLine = box.getLineUp();
				break;
			case 'D':
				herLastLine = box.getLineDown();
				break;
			case 'L':
				herLastLine = box.getLineLeft();
				break;
			case 'R':
				herLastLine = box.getLineRight();
		}
        synchronized (this) {
            this.notifyAll();
        }
    }

    public void anzeigeAktualisieren() {
        postInvalidate(); // View zwingen, neu zu zeichnen
    }

}
