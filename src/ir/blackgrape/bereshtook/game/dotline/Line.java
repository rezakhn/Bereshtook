package ir.blackgrape.bereshtook.game.dotline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Line {

    private Box boxUp;
    private Box boxDown;
    private Box boxLeft;
    private Box boxRight;

    private List<Box> boxList = new ArrayList<Box>();

    private Player owner;

    public Line(Box boxUp, Box boxDown,
            Box boxLeft, Box boxRight) {

        this.boxUp = boxUp;
        this.boxDown = boxDown;
        this.boxLeft = boxLeft;
        this.boxRight = boxRight;

        if (boxUp != null)
            boxList.add(boxUp);

        if (boxDown != null)
            boxList.add(boxDown);

        if (boxLeft != null)
            boxList.add(boxLeft);

        if (boxRight != null)
            boxList.add(boxRight);
    }

    
    public Box getBoxUp() {
		return boxUp;
	}
    
	public void setBoxUp(Box boxUp) {
		this.boxUp = boxUp;
	}

	public Box getBoxDown() {
		return boxDown;
	}

	public void setBoxDown(Box boxDown) {
		this.boxDown = boxDown;
	}

	public Box getBoxLeft() {
		return boxLeft;
	}

	public void setBoxLeft(Box boxLeft) {
		this.boxLeft = boxLeft;
	}

	public Box getBoxRight() {
		return boxRight;
	}

	public void setBoxRight(Box boxRight) {
		this.boxRight = boxRight;
	}

	public List<Box> getBoxList() {
        return Collections.unmodifiableList(boxList);
    }

    public boolean isKoennteUmliegendendesKaestchenSchliessen() {

        for (Box box : boxList)
            if (box.getLinesWithoutOwner().size() <= 2)
                return true;

        return false;
    }

    public Player getOwner() {
        return owner;
    }
    
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "Strich [kaestchenOben=" + boxUp + ", kaestchenUnten="
                + boxDown + ", kaestchenLinks=" + boxLeft
                + ", kaestchenRechts=" + boxRight + ", besitzer="
                + owner + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((boxLeft == null) ? 0 : boxLeft.hashCode());
        result = prime * result + ((boxUp == null) ? 0 : boxUp.hashCode());
        result = prime * result + ((boxRight == null) ? 0 : boxRight.hashCode());
        result = prime * result + ((boxDown == null) ? 0 : boxDown.hashCode());
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
        Line other = (Line) obj;
        if (boxLeft == null) {
            if (other.boxLeft != null)
                return false;
        } else if (!boxLeft.equals(other.boxLeft))
            return false;
        if (boxUp == null) {
            if (other.boxUp != null)
                return false;
        } else if (!boxUp.equals(other.boxUp))
            return false;
        if (boxRight == null) {
            if (other.boxRight != null)
                return false;
        } else if (!boxRight.equals(other.boxRight))
            return false;
        if (boxDown == null) {
            if (other.boxDown != null)
                return false;
        } else if (!boxDown.equals(other.boxDown))
            return false;
        return true;
    }

}
