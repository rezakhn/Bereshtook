package ir.blackgrape.bereshtook.game.dotline;

import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.game.Game;
import ir.blackgrape.bereshtook.game.GameWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;

public class DotlineWindow extends GameWindow {
	
	public static final String DOTLINE_GAME = GAME_CODE + "DOTLINE#";
	public static final String INVITE_MSG = DOTLINE_GAME + "INVITE#";
	public static final String ACCEPT_MSG = DOTLINE_GAME + "ACCEPT#";
	public static final String DENY_MSG = DOTLINE_GAME + "DENY#";
	public static final String EXIT_MSG = DOTLINE_GAME + "EXIT#";
	private static final int GAME_TIME = 30000;

    private GameFieldView gameFieldView;
    private GameField gameField;
    private PlayerManager playersManager;
    private DotlineGame mGame;

    private final Handler mHandler = new Handler();

    private volatile boolean running = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dotline_view);

        PlayerType spielerTyp1 = PlayerType.HUMAN;
        PlayerType spielerTyp2 = PlayerType.HUMAN;

        int feldGroesseX = 6;
        int feldGroesseY = 6;

        gameField = GameField.generate(feldGroesseX, feldGroesseY);
        playersManager = new PlayerManager();

        gameFieldView = (GameFieldView) findViewById(R.id.spielfeldView);
        gameFieldView.init(gameField);
        
        Player me = new Player("me",
                        BitmapFactory.decodeResource(getResources(), R.drawable.player_symbol_maus),
                        getResources().getColor(R.color.spieler_1_farbe), spielerTyp1);
        Player her = new Player(withJabberID,
        				BitmapFactory.decodeResource(getResources(), R.drawable.player_symbol_kaese),
        				getResources().getColor(R.color.spieler_2_farbe), spielerTyp2);

        if(isGuest){
        	playersManager.addPlayer(her);
        	playersManager.addPlayer(me);
        }
        else{
        	playersManager.addPlayer(me);
        	playersManager.addPlayer(her);
        }

		txtHerTimer = (TextView) findViewById(R.id.txt_timer_up);
		txtMyTimer = (TextView) findViewById(R.id.txt_timer_down);
		
		myTimer = new CountDownTimer(GAME_TIME, ONE_SECOND) {
			@Override
			public void onTick(long millisUntilFinished) {
				txtMyTimer.setText(millisUntilFinished / ONE_SECOND + " " + getString(R.string.second));
				if(millisUntilFinished < ONE_SECOND * 10){
					txtMyTimer.setTextColor(getResources().getColor(R.color.red));
					soundBeep.start();
				}
				else
					txtMyTimer.setTextColor(getResources().getColor(R.color.black));
			}
			@Override
			public void onFinish() {
				cancleTimers();
				txtMyTimer.setText(getString(R.string.time_out_game));
				timeOutDialog();
			}
		};
		
		herTimer = new CountDownTimer(GAME_TIME, ONE_SECOND) {
			@Override
			public void onTick(long millisUntilFinished) {
				txtHerTimer.setText(millisUntilFinished / ONE_SECOND + " " + getString(R.string.second));
				if(millisUntilFinished < ONE_SECOND * 10){
					txtHerTimer.setTextColor(getResources().getColor(R.color.red));
					soundBeep.start();
				}
				else
					txtHerTimer.setTextColor(getResources().getColor(R.color.black));
			}
			@Override
			public void onFinish() {
				cancleTimers();
				txtHerTimer.setText(getString(R.string.time_out_game));
				sheLeft();
			}
		};
		
        mGame = new DotlineGame();
        startGame();
    }

    @Override
    protected void onStop() {
        running = false;
        super.onStop();
    }

    private class GameLoopRunnable implements Runnable {

        public void run() {

            playersManager.selectNextPlayer();

            while (!isGameOver()) {

                final Player player = playersManager.getCurrentPlayer();

                mHandler.post(new Runnable() {
                    public void run() {

                        ImageView imageView = (ImageView) findViewById(R.id.aktuellerSpielerSymbol);
                        imageView.setImageBitmap(player.getSymbol());

                        TextView textView = (TextView) findViewById(R.id.punkteAnzeige);
                        textView.setText(String.valueOf(calculateScore(player)));
                    }
                });

                Line line = null;

                if (!player.isComputerGegner()) {	// always true

                    gameFieldView.resetLastLine();

                    while ((line = gameFieldView.getLastLine()) == null) {
                        try {
                            synchronized (gameFieldView) {
                                gameFieldView.wait();
                            }
                        } catch (InterruptedException ignore) {

                        }
                    }

                } else {	// never happens

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignore) {
                    }

                    line = computerGegnerZug(player.getSpielerTyp());
                }

                chooseLine(line);

                if (!running)
                    return;
            }

            if (isGameOver()) {

                mHandler.post(new Runnable() {

                    public void run() {

                        determineWinner();
                        endGame();
                    }
                });
            }
        }

    }

    private Line computerGegnerZug(PlayerType spielerTyp) {

        Line line = waehleLetztenOffenenStrichFuerKaestchen();

        if (line != null)
            return line;

        Line randomLine = randomChooseLine();

        if (spielerTyp == PlayerType.COMPUTER_MEDIUM) {

            int loopCounter = 0;

            while (randomLine.isKoennteUmliegendendesKaestchenSchliessen()) {

                randomLine = randomChooseLine();

                if (++loopCounter >= 30)
                    break;
            }
        }

        return randomLine;
    }

    private Line waehleLetztenOffenenStrichFuerKaestchen() {

        for (Box box : gameField.getOpenBoxesList())
            if (box.getLinesWithoutOwner().size() == 1)
                return box.getLinesWithoutOwner().get(0);

        return null;
    }

    private Line randomChooseLine() {

        List<Line> linesWithoutOwner = new ArrayList<Line>(gameField.getLinesWithoutOwner());
        Line randomLine = linesWithoutOwner.get(new Random().nextInt(linesWithoutOwner.size()));

        return randomLine;
    }

    private void chooseLine(Line strich) {

        if (strich.getOwner() != null)
            return;

        Player currentPlayer = playersManager.getCurrentPlayer();

        boolean kaestchenKonnteGeschlossenWerden = gameField.chooseLine(strich, currentPlayer);

        if (!kaestchenKonnteGeschlossenWerden)
            playersManager.selectNextPlayer();

        gameFieldView.anzeigeAktualisieren();
    }

    public boolean isGameOver() {
        return gameField.isAllBoxesHaveOwner();
    }

    public Player determineWinner() {

        Player winner = null;
        int maxScore = 0;

        for (Player player : playersManager.getPlayers()) {

            int score = calculateScore(player);
            
            if(player.getName().equals(withJabberID))
            	mGame.setHerScore(score);
            else
            	mGame.setMyScore(score);
            
            if (score > maxScore) {
                winner = player;
                maxScore = score;
            }
        }

        return winner;
    }

    public int calculateScore(Player player) {

        int score = 0;

        for (Box box : gameField.getBoxesList())
            if (box.getOwner() == player)
                score++;

        return score;
    }

	@Override
	protected Game getGame() {
		return mGame;
	}

	@Override
	protected void onReceiveMsg(String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void startGame() {
        Thread thread = new Thread(new GameLoopRunnable());
        thread.start();
        running = true;
	}

	@Override
	protected String getExitMsg() {
		return EXIT_MSG;
	}

}