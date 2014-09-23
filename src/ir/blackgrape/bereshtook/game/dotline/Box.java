package ir.blackgrape.bereshtook.game.dotline;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Box {

    private int rasterX;
    private int rasterY;
    
    private Player owner;

    private Line lineUp;
    private Line lineDown;
    private Line lineLeft;
    private Line lineRight;

    private Paint rahmenPaint = new Paint();

    public Box(int rasterX, int rasterY) {
        this.rasterX = rasterX;
        this.rasterY = rasterY;

        rahmenPaint.setStyle(Paint.Style.STROKE);
        rahmenPaint.setStrokeWidth(5);
    }

    public int getRasterX() {
        return rasterX;
    }

    public int getRasterY() {
        return rasterY;
    }

    public int getPixelX() {
        return rasterX * GameFieldView.BOX_LENGTH + GameFieldView.PADDING;
    }

    public int getPixelY() {
        return rasterY * GameFieldView.BOX_LENGTH + GameFieldView.PADDING;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Line getLineUp() {
		return lineUp;
	}

	public void setLineUp(Line lineUp) {
		this.lineUp = lineUp;
	}

	public Line getLineDown() {
		return lineDown;
	}

	public void setLineDown(Line lineDown) {
		this.lineDown = lineDown;
	}

	public Line getLineLeft() {
		return lineLeft;
	}

	public void setLineLeft(Line lineLeft) {
		this.lineLeft = lineLeft;
	}

	public Line getLineRight() {
		return lineRight;
	}

	public void setLineRight(Line lineRight) {
		this.lineRight = lineRight;
	}

	public List<Line> getLines() {
        List<Line> lines = new ArrayList<Line>();
        if (lineUp != null)
            lines.add(lineUp);
        if (lineDown != null)
            lines.add(lineDown);
        if (lineLeft != null)
            lines.add(lineLeft);
        if (lineRight != null)
            lines.add(lineRight);
        return lines;
    }

    public List<Line> getLinesWithoutOwner() {

        List<Line> lines = new ArrayList<Line>();
        if (lineUp != null && lineUp.getOwner() == null)
            lines.add(lineUp);
        if (lineDown != null && lineDown.getOwner() == null)
            lines.add(lineDown);
        if (lineLeft != null && lineLeft.getOwner() == null)
            lines.add(lineLeft);
        if (lineRight != null && lineRight.getOwner() == null)
            lines.add(lineRight);
        return lines;
    }

    public boolean isAllLinesHaveOwner() {
        return getLinesWithoutOwner().size() == 0;
    }

    public Rect getRectStrichOben() {

        if (lineUp == null)
            return null;

        return new Rect(getPixelX() + GameFieldView.BOX_LENGTH / 4, getPixelY() - GameFieldView.BOX_LENGTH / 4, getPixelX() + (int) (GameFieldView.BOX_LENGTH * 0.75), getPixelY() + GameFieldView.BOX_LENGTH / 4);
    }

    public Rect getRectStrichUnten() {

        if (lineDown == null)
            return null;

        return new Rect(getPixelX() + GameFieldView.BOX_LENGTH / 4, getPixelY() + (int) (GameFieldView.BOX_LENGTH * 0.75), getPixelX() + (int) (GameFieldView.BOX_LENGTH * 0.75), getPixelY() + GameFieldView.BOX_LENGTH + GameFieldView.BOX_LENGTH / 4);
    }

    public Rect getRectStrichLinks() {

        if (lineLeft == null)
            return null;

        return new Rect(getPixelX() - GameFieldView.BOX_LENGTH / 4, getPixelY() + GameFieldView.BOX_LENGTH / 4, getPixelX() + GameFieldView.BOX_LENGTH / 4, getPixelY() + (int) (GameFieldView.BOX_LENGTH * 0.75));
    }

    public Rect getRectStrichRechts() {

        if (lineRight == null)
            return null;

        return new Rect(getPixelX() + (int) (GameFieldView.BOX_LENGTH * 0.75), getPixelY() + GameFieldView.BOX_LENGTH / 4, getPixelX() + GameFieldView.BOX_LENGTH + GameFieldView.BOX_LENGTH / 4, getPixelY() + (int) (GameFieldView.BOX_LENGTH * 0.75));
    }

    public Line findLine(int pixelX, int pixelY) {

        if (getRectStrichOben() != null && getRectStrichOben().contains(pixelX, pixelY))
            return lineUp;

        if (getRectStrichUnten() != null && getRectStrichUnten().contains(pixelX, pixelY))
            return lineDown;

        if (getRectStrichLinks() != null && getRectStrichLinks().contains(pixelX, pixelY))
            return lineLeft;

        if (getRectStrichRechts() != null && getRectStrichRechts().contains(pixelX, pixelY))
            return lineRight;

        return null;
    }
    
    public String findLineDir(int pixelX, int pixelY) {

        if (getRectStrichOben() != null && getRectStrichOben().contains(pixelX, pixelY))
            return "U";

        if (getRectStrichUnten() != null && getRectStrichUnten().contains(pixelX, pixelY))
            return "D";

        if (getRectStrichLinks() != null && getRectStrichLinks().contains(pixelX, pixelY))
            return "L";

        if (getRectStrichRechts() != null && getRectStrichRechts().contains(pixelX, pixelY))
            return "R";

        return null;
    } 

    public void onDraw(Canvas canvas) {

        if (owner != null) {

            Paint fuellungPaint = new Paint();
            fuellungPaint.setColor(owner.getFarbe());

            Rect destRect = new Rect(getPixelX(), getPixelY(), getPixelX() + GameFieldView.BOX_LENGTH, getPixelY() + GameFieldView.BOX_LENGTH);
            canvas.drawBitmap(owner.getSymbol(), null, destRect, rahmenPaint);
        }

        if (lineUp == null) {
            rahmenPaint.setColor(Color.BLACK);
            canvas.drawLine(getPixelX(), getPixelY(), getPixelX() + GameFieldView.BOX_LENGTH, getPixelY(), rahmenPaint);
        }

        if (lineDown != null && lineDown.getOwner() != null)
            rahmenPaint.setColor(lineDown.getOwner().getFarbe());
        else if (lineDown != null)
            rahmenPaint.setColor(Color.LTGRAY);
        else
            rahmenPaint.setColor(Color.BLACK);

        canvas.drawLine(getPixelX(), getPixelY() + GameFieldView.BOX_LENGTH, getPixelX() + GameFieldView.BOX_LENGTH, getPixelY() + GameFieldView.BOX_LENGTH, rahmenPaint);

        if (lineLeft == null) {
            rahmenPaint.setColor(Color.BLACK);
            canvas.drawLine(getPixelX(), getPixelY(), getPixelX(), getPixelY() + GameFieldView.BOX_LENGTH, rahmenPaint);
        }

        if (lineRight != null && lineRight.getOwner() != null)
            rahmenPaint.setColor(lineRight.getOwner().getFarbe());
        else if (lineRight != null)
            rahmenPaint.setColor(Color.LTGRAY);
        else
            rahmenPaint.setColor(Color.BLACK);

        canvas.drawLine(getPixelX() + GameFieldView.BOX_LENGTH, getPixelY(), getPixelX() + GameFieldView.BOX_LENGTH, getPixelY() + GameFieldView.BOX_LENGTH, rahmenPaint);

        rahmenPaint.setColor(Color.BLACK);
        canvas.drawRect(getPixelX() - 1, getPixelY() - 1, getPixelX() + 1, getPixelY() + 1, rahmenPaint);
        canvas.drawRect(getPixelX() + GameFieldView.BOX_LENGTH - 1, getPixelY() - 1, getPixelX() + GameFieldView.BOX_LENGTH + 1, getPixelY() + 1, rahmenPaint);
        canvas.drawRect(getPixelX() - 1, getPixelY() + GameFieldView.BOX_LENGTH - 1, getPixelX() + 1, getPixelY() + GameFieldView.BOX_LENGTH + 1, rahmenPaint);
        canvas.drawRect(getPixelX() + GameFieldView.BOX_LENGTH - 1, getPixelY() + GameFieldView.BOX_LENGTH - 1, getPixelX() + GameFieldView.BOX_LENGTH + 1, getPixelY() + GameFieldView.BOX_LENGTH + 1, rahmenPaint);
    }

    @Override
    public String toString() {
        return "Kaestchen [rasterX=" + rasterX + ", rasterY=" + rasterY + ", besitzer=" + owner + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + rasterX;
        result = prime * result + rasterY;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Box other = (Box) obj;
        if (rasterX != other.rasterX)
            return false;
        if (rasterY != other.rasterY)
            return false;
        return true;
    }

}
