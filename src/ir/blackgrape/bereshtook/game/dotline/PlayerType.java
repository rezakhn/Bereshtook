package ir.blackgrape.bereshtook.game.dotline;

public enum PlayerType {

    HUMAN,
    COMPUTER_EASY,
    COMPUTER_MEDIUM,
    COMPUTER_HARD;

    public static PlayerType parse(String string) {

        if (string == null)
            return null;

        if (string.equals("Mensch") || string.equals("Human"))
            return HUMAN;

        if (string.equals("KI Leicht") || string.equals("KI Easy"))
            return COMPUTER_EASY;

        if (string.equals("KI Mittel") || string.equals("KI Medium"))
            return COMPUTER_MEDIUM;

        if (string.equals("KI Schwer") || string.equals("KI Hard"))
            return COMPUTER_HARD;

        throw new IllegalArgumentException("Unbekannter SpielerTyp: " + string);
    }

    public boolean isComputerGegner() {
        return this == PlayerType.COMPUTER_EASY
                || this == PlayerType.COMPUTER_MEDIUM
                || this == PlayerType.COMPUTER_HARD;
    }

}
