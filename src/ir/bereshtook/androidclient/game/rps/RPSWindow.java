package ir.bereshtook.androidclient.game.rps;

import ir.bereshtook.androidclient.R;
import ir.bereshtook.androidclient.game.GameWindow;
import ir.bereshtook.androidclient.game.rps.RPSGame.Choice;
import ir.bereshtook.androidclient.game.rps.RPSGame.Turn;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.internal.widget.IcsToast;

public class RPSWindow extends GameWindow {

	public static final String RPS_GAME = GAME_CODE + "RPS#";
	public static final String INVITE_MSG = RPS_GAME + "INVITE#";
	public static final String ACCEPT_MSG = RPS_GAME + "ACCEPT#";
	public static final String DENY_MSG = RPS_GAME + "DENY#";
	private static final String ROCK_MSG = RPS_GAME + "ROCK#";
	private static final String PAPER_MSG = RPS_GAME + "PAPER#";
	private static final String SCISSOR_MSG = RPS_GAME + "SCISSOR#";
	
	private RPSGame game;
	private Button btnRockUp;
	private Button btnRockDown;
	private Button btnPaperUp;
	private Button btnPaperDown;
	private Button btnScissorUp;
	private Button btnScissorDown;
	private ImageView choiceUp;
	private ImageView choiceDown;
	private TextView txtScoreUp;
	private TextView txtScoreDown;
	private Context mContext;
	
	private MediaPlayer soundChoice;
	private MediaPlayer soundVictory;
	private MediaPlayer soundDraw;
	private MediaPlayer soundLose;
	private MediaPlayer soundError;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.rps_game);
		
		btnRockUp = (Button) findViewById(R.id.btnRockUp);
		btnRockDown = (Button) findViewById(R.id.btnRockDown);
		btnPaperUp = (Button) findViewById(R.id.btnPaperUp);
		btnPaperDown = (Button) findViewById(R.id.btnPaperDown);
		btnScissorUp = (Button) findViewById(R.id.btnScissorUp);
		btnScissorDown = (Button) findViewById(R.id.btnScissorDown);
		
		choiceUp = (ImageView)findViewById(R.id.imgChoiceUp);
		choiceDown = (ImageView)findViewById(R.id.imgChoiceDown);
		
		txtScoreUp = (TextView)findViewById(R.id.btnScoreUp);
		txtScoreDown = (TextView)findViewById(R.id.btnScoreDown);
		
		soundChoice = MediaPlayer.create(this, R.raw.sound_choice);
		soundVictory = MediaPlayer.create(this, R.raw.sound_victory);
		soundDraw = MediaPlayer.create(this, R.raw.sound_draw);
		soundLose = MediaPlayer.create(this, R.raw.sound_lose);
		soundError = MediaPlayer.create(this, R.raw.sound_error);
		
		btnRockDown.setOnClickListener(mClickListener);
		btnPaperDown.setOnClickListener(mClickListener);
		btnScissorDown.setOnClickListener(mClickListener);
		
		startGame();
	}

	private OnClickListener mClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!weAreOnline()){
				soundError.start();
				Toast tNotSent = IcsToast.makeText(mContext, "you are not online", IcsToast.LENGTH_SHORT);
				tNotSent.show();
				return;
			}
			else if(game.getTrn() != Turn.MY && game.getTrn() != Turn.BOTH){
				soundError.start();
				Toast notTurn = IcsToast.makeText(mContext, "not your turn", IcsToast.LENGTH_SHORT);
				notTurn.show();
				return;
			}
			if(game.getTrn() == Turn.BOTH)
				soundChoice.start();
			
			switch(v.getId()){
			case R.id.btnRockDown:
				sendMsg(ROCK_MSG);
				game.setMyChoice(Choice.ROCK);
				choiceDown.setBackgroundResource(R.raw.a_rock_down);
				break;
			case R.id.btnPaperDown:
				sendMsg(PAPER_MSG);
				game.setMyChoice(Choice.PAPER);
				choiceDown.setBackgroundResource(R.raw.a_paper_down);
				break;
			case R.id.btnScissorDown:
				sendMsg(SCISSOR_MSG);
				game.setMyChoice(Choice.SCISSOR);
				choiceDown.setBackgroundResource(R.raw.a_scissor_down);
				break;
			}
			
			if(game.getTrn() == Turn.NONE){
				switch (game.getHerChoice()) {
				case ROCK:
					choiceUp.setBackgroundResource(R.raw.a_rock_up);
					break;

				case PAPER:
					choiceUp.setBackgroundResource(R.raw.a_paper_up);
					break;
					
				case SCISSOR:
					choiceUp.setBackgroundResource(R.raw.a_scissor_up);
					break;
					
				case NONE:
					choiceUp.setBackgroundResource(R.raw.square);
					break;
				}
				checkWinner();
			}
		}
	};
	
	protected void receiveMsg(String msg) {
		if(msg.equals(ROCK_MSG) || msg.equals(PAPER_MSG) || msg.equals(SCISSOR_MSG)){
			if(game.getTrn() == Turn.BOTH)
				soundChoice.start();
			int background = R.raw.tick;
			if(msg.equals(ROCK_MSG)){
				game.setHerChoice(Choice.ROCK);
				background = R.raw.a_rock_up;
			}
			else if(msg.equals(PAPER_MSG)){
				game.setHerChoice(Choice.PAPER);
				background = R.raw.a_paper_up;
			}
			else if(msg.equals(SCISSOR_MSG)){
				game.setHerChoice(Choice.SCISSOR);
				background = R.raw.a_scissor_up;
			}
			if(game.getTrn() == Turn.MY)
				background = R.raw.tick;
			choiceUp.setBackgroundResource(background);
			if(game.getTrn() == Turn.NONE)
				checkWinner();
		}
			
	}
	
	private void checkWinner() {
		enableChoices(false);
		Toast result = null;
		switch (game.judge()) {
		case ME:
			soundVictory.start();
			result = IcsToast.makeText(mContext, getString(R.string.you_win), IcsToast.LENGTH_SHORT);
			break;
		case HER:
			soundLose.start();
			result = IcsToast.makeText(mContext, getString(R.string.you_lose), IcsToast.LENGTH_SHORT);
			break;
		case DRAW:
			soundDraw.start();
			result = IcsToast.makeText(mContext, getString(R.string.draw), IcsToast.LENGTH_SHORT);
			break;
		}
		result.show();
		txtScoreUp.setText(game.getHerScore().toString());
		txtScoreDown.setText(game.getMyScore().toString());
		nextRound(false);
	}

	private void enableChoices(boolean b) {
		btnRockDown.setClickable(b);
		btnPaperDown.setClickable(b);
		btnScissorDown.setClickable(b);
		
	}

	protected void startGame() {
		game = new RPSGame();
		game.init();
		nextRound(true);
	}
	
	private void nextRound(boolean newGame) {
		game.nextRound();
		final Handler handler = new Handler();
		if(!newGame){
			handler.postDelayed(new Runnable() {
			    @Override
			    public void run() {
					choiceUp.setBackgroundResource(R.raw.square);
					choiceDown.setBackgroundResource(R.raw.square);
					enableChoices(true);
			    }
			}, 1750);
		}
	}
}
