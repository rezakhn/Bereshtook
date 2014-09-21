package ir.blackgrape.bereshtook.game.dotline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GameField {

    private int breiteInBox;
    private int hoeheInBox;

    private Box[][] boxArrey;
    private List<Box> openBoxesList = new ArrayList<Box>();
    private Set<Line> linesWithoutOwner = new HashSet<Line>();
    
    private GameField(int breiteInBox, int hoeheInBox) {
        this.breiteInBox = breiteInBox;
        this.hoeheInBox = hoeheInBox;

        this.boxArrey = new Box[breiteInBox][hoeheInBox];
    }

    public List<Box> getBoxesList() {

        List<Box> boxesList = new ArrayList<Box>();

        for (int rasterX = 0; rasterX < breiteInBox; rasterX++) {
            for (int rasterY = 0; rasterY < hoeheInBox; rasterY++) {
                boxesList.add(boxArrey[rasterX][rasterY]);
            }
        }

        return Collections.unmodifiableList(boxesList);
    }

    public List<Box> getOpenBoxesList() {
        return Collections.unmodifiableList(openBoxesList);
    }

    public Set<Line> getLinesWithoutOwner() {
        return Collections.unmodifiableSet(linesWithoutOwner);
    }

    private void addBox(Box box) {
        boxArrey[box.getRasterX()][box.getRasterY()] = box;
        openBoxesList.add(box);
    }

    private void addLine(Line line) {
        linesWithoutOwner.add(line);
    }

    public Box getBox(int rasterX, int rasterY) {

        if (rasterX >= breiteInBox || rasterY >= hoeheInBox)
            return null;

        return boxArrey[rasterX][rasterY];
    }

    public int getBreiteInBox() {
        return breiteInBox;
    }

    public int getHoeheInBox() {
        return hoeheInBox;
    }

    private boolean schliesseAlleMoeglichenBox(Player zuzuweisenderBesitzer) {

        boolean boxKonnteGeschlossenWerden = false;

        Iterator<Box> openBoxIt = openBoxesList.iterator();

        while (openBoxIt.hasNext()) {

            Box box = openBoxIt.next();

            if (box.isAllLinesHaveOwner() && box.getOwner() == null) {
                box.setOwner(zuzuweisenderBesitzer);
                openBoxIt.remove();
                boxKonnteGeschlossenWerden = true;
            }
        }

        return boxKonnteGeschlossenWerden;
    }

    public boolean isAllBoxesHaveOwner() {
        return openBoxesList.isEmpty();
    }

    public boolean chooseLine(Line line, Player player) {
        line.setOwner(player);
        linesWithoutOwner.remove(line);
        return schliesseAlleMoeglichenBox(player);
    }

    public static GameField generate(int anzahlH, int anzahlV) {

        GameField gameField = new GameField(anzahlH, anzahlV);

        for (int rasterX = 0; rasterX < anzahlH; rasterX++) {
            for (int rasterY = 0; rasterY < anzahlV; rasterY++) {

                gameField.addBox(new Box(rasterX, rasterY));
            }
        }

        for (int rasterX = 0; rasterX < anzahlH; rasterX++) {
            for (int rasterY = 0; rasterY < anzahlV; rasterY++) {

                Box box = gameField.getBox(rasterX, rasterY);

                Box boxBelow = null;
                Box boxRight = null;

                if (rasterY < anzahlV - 1)
                    boxBelow = gameField.getBox(rasterX, rasterY + 1);

                if (rasterX < anzahlH - 1)
                    boxRight = gameField.getBox(rasterX + 1, rasterY);

                Line lineBelow = new Line(box, boxBelow, null, null);
                Line lineRight = new Line(null, null, box, boxRight);

                if (boxRight != null) {
                    box.setLineRight(lineRight);
                    boxRight.setLineLeft(lineRight);
                    gameField.addLine(lineRight);
                }

                if (boxBelow != null) {
                    box.setLineDown(lineBelow);
                    boxBelow.setLineUp(lineBelow);
                    gameField.addLine(lineBelow);
                }
            }
        }

        return gameField;
    }

}
