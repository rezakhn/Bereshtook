package ir.blackgrape.bereshtook.game.dotline;

import android.graphics.Bitmap;

public class Player {

    private String name;
    private Bitmap symbol;
    private int farbe;
    private PlayerType playerType;

    public Player(String name, Bitmap symbol, int farbe, PlayerType spielerTyp) {
        this.name = name;
        this.symbol = symbol;
        this.farbe = farbe;
        this.playerType = spielerTyp;
    }

    public String getName() {
        return name;
    }

    public Bitmap getSymbol() {
        return symbol;
    }

    public int getFarbe() {
        return farbe;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public boolean isComputerGegner() {
        return playerType.isComputerGegner();
    }

    @Override
    public String toString() {
        return "Spieler [name=" + name + ", farbe=" + farbe + "]";
    }

}
