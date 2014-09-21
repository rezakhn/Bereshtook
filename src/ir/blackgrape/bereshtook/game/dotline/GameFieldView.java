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
    private volatile Line lastLine;

    public GameFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(GameField spielfeld) {
        this.gameField = spielfeld;
        setOnTouchListener(this);
    }

    public Line getLastLine() {
        return lastLine;
    }

    public void resetLastLine() {
        lastLine = null;
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

        canvas.drawColor(getResources().getColor(R.color.hintergrund_farbe));

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

        if (lastLine != null)
            return true;

        int errechnetRasterX = (int) event.getX() / BOX_LENGTH;
        int errechnetRasterY = (int) event.getY() / BOX_LENGTH;

        Box box = gameField.getBox(errechnetRasterX, errechnetRasterY);

        if (box == null || box.getOwner() != null)
            return true;

        Line line = box.findLine((int) event.getX(), (int) event.getY());

        if (line == null)
            return true;

        lastLine = line;

        synchronized (this) {
            this.notifyAll();
        }

        return true;
    }

    public void anzeigeAktualisieren() {
        postInvalidate(); // View zwingen, neu zu zeichnen
    }

}
