package ir.blackgrape.bereshtook.game.dotline;

import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.game.Game;
import ir.blackgrape.bereshtook.game.GameWindow;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DotlineWindow extends GameWindow {
	
	public static final String DOTLINE_GAME = GAME_CODE + "DOTLINE#";
	public static final String LINE_CODE = GAME_CODE + "LINE#";
	public static final String INVITE_MSG = DOTLINE_GAME + "INVITE#";
	public static final String ACCEPT_MSG = DOTLINE_GAME + "ACCEPT#";
	public static final String DENY_MSG = DOTLINE_GAME + "DENY#";
	public static final String EXIT_MSG = DOTLINE_GAME + "EXIT#";
	private static final int GAME_TIME = 30000;

    private GameFieldView gameFieldView;
    private GameField gameField;
    private PlayerManager playersManager;
    private DotlineGame mGame;
    
	private TextView txtScoreUp;
	private TextView txtScoreDown;
	
	private RelativeLayout layout_up;
	private RelativeLayout layout_down;

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
        
        Player me = new Player(PlayerManager.myName,
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

		txtStatusUp = (TextView) findViewById(R.id.txt_status_up);
		txtStatusDown = (TextView) findViewById(R.id.txt_status_down);
		txtStatusDown.setOnClickListener(statusClickListener);
		
		txtScoreUp = (TextView)findViewById(R.id.txt_score_up);
		txtScoreDown = (TextView)findViewById(R.id.txt_score_down);
		
		layout_up = (RelativeLayout) findViewById(R.id.layout_up);
		layout_down = (RelativeLayout) findViewById(R.id.layout_down);
		
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
		
        ImageView imageHer = (ImageView) findViewById(R.id.img_symbol_up);
        imageHer.setImageBitmap(her.getSymbol());
        
        ImageView imageMe = (ImageView) findViewById(R.id.img_symbol_down);
        imageMe.setImageBitmap(me.getSymbol());
		
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
                    	if(player.getName().equals(PlayerManager.myName)){
                    		layout_down.setBackgroundColor(getResources().getColor(R.color.red));
                    		layout_up.setBackgroundColor(getResources().getColor(R.color.grayed_out));
                    		txtScoreDown.setText(String.valueOf(calculateScore(player)));
                    		herTimer.cancel();
                    		myTimer.start();
                    	}
                    	else{
                    		layout_up.setBackgroundColor(getResources().getColor(R.color.red));
                    		layout_down.setBackgroundColor(getResources().getColor(R.color.grayed_out));
                    		txtScoreUp.setText(String.valueOf(calculateScore(player)));
                            myTimer.cancel();
                            herTimer.start();
                    	}
                    }
                });

                Line line = null;

                if (player.getName().equals(PlayerManager.myName)) {

                    gameFieldView.resetMyLastLine();
                    gameFieldView.resetMyLastMessage();

                    while ((line = gameFieldView.getMyLastLine()) == null) {
                        try {
                            synchronized (gameFieldView) {
                                gameFieldView.wait();
                            }
                        } catch (InterruptedException ignore) {

                        }
                    }
                    if(gameFieldView.getMyLastMessage() != null)
                    	sendMsg(gameFieldView.getMyLastMessage());
                } else {
                	gameFieldView.resetHerLastLine();
                    while ((line = gameFieldView.getHerLastLine()) == null) {
                        try {
                            synchronized (gameFieldView) {
                                gameFieldView.wait();
                            }
                        } catch (InterruptedException ignore) {
                        }
                    }
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

    private void chooseLine(Line line) {

        if (line.getOwner() != null)
            return;

        Player currentPlayer = playersManager.getCurrentPlayer();

        boolean kaestchenKonnteGeschlossenWerden = gameField.chooseLine(line, currentPlayer);

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
		if(msg.startsWith(LINE_CODE))
			gameFieldView.onReceiveMove(msg);
		else if(msg.startsWith(STATUS_MSG)){
			String status = msg.replaceFirst(STATUS_MSG, "");
			txtStatusUp.setText(status);
		}
		else if(msg.equals(EXIT_MSG)){
			sheLeft();
		}
	}

	@Override
	protected void startGame() {
        Thread thread = new Thread(new GameLoopRunnable());
        thread.start();
        running = true;
        
        txtScoreUp.setText("0");
        txtScoreDown.setText("0");
        if(isGuest)
        	herTimer.start();
        else
        	myTimer.start();
	}

	@Override
	protected String getExitMsg() {
		return EXIT_MSG;
	}

}