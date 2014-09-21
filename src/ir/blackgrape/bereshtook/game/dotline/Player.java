package ir.blackgrape.bereshtook.game.dotline;

import android.graphics.Bitmap;

public class Player {

    private String name;
    private Bitmap symbol;
    private int farbe;
    private PlayerType spielerTyp;

    public Player(String name, Bitmap symbol, int farbe, PlayerType spielerTyp) {
        this.name = name;
        this.symbol = symbol;
        this.farbe = farbe;
        this.spielerTyp = spielerTyp;
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

    public PlayerType getSpielerTyp() {
        return spielerTyp;
    }

    public boolean isComputerGegner() {
        return spielerTyp.isComputerGegner();
    }

    @Override
    public String toString() {
        return "Spieler [name=" + name + ", farbe=" + farbe + "]";
    }

}
