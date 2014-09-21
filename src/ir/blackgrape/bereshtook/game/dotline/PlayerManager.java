package ir.blackgrape.bereshtook.game.dotline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerManager {

    private List<Player> playerList = new ArrayList<Player>();

    private Player currentPlayer;

    public PlayerManager() {
    }

    public void addPlayer(Player player) {
        playerList.add(player);
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(playerList);
    }

    public Player getCurrentPlayer() {
        if (currentPlayer == null)
            throw new RuntimeException("Vor Abfrage des Spielers muss 'neuerZug' mindestens einmal aufgerufen worden sein!");
        return currentPlayer;
    }

    public void selectNextPlayer() {

        int indexAktSpieler = playerList.indexOf(currentPlayer);

        int indexNaechsterSpieler = indexAktSpieler + 1;
        if (indexNaechsterSpieler > playerList.size() - 1)
            indexNaechsterSpieler = 0;

        currentPlayer = playerList.get(indexNaechsterSpieler);
    }

}
