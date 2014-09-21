package ir.blackgrape.bereshtook.game.rps;

import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.game.Game;
import ir.blackgrape.bereshtook.game.GameWindow;
import ir.blackgrape.bereshtook.game.rps.RPSGame.Choice;
import ir.blackgrape.bereshtook.game.rps.RPSGame.Turn;
import ir.blackgrape.bereshtook.game.rps.RPSGame.Winner;
import ir.blackgrape.bereshtook.util.PersianUtil;
import android.os.Bundle;
import android.os.CountDownTimer;
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
	public static final String EXIT_MSG = RPS_GAME + "EXIT#";
	
	private static final String ROCK_MSG = RPS_GAME + "ROCK#";
	private static final String PAPER_MSG = RPS_GAME + "PAPER#";
	private static final String SCISSOR_MSG = RPS_GAME + "SCISSOR#";
	private static final int GAME_TIME = 30000;
	
	private RPSGame mGame;
	private Button btnRockDown;
	private Button btnPaperDown;
	private Button btnScissorDown;
	private ImageView choiceUp;
	private ImageView choiceDown;
	private TextView txtScoreUp;
	private TextView txtScoreDown;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rps_game);
		
		btnRockDown = (Button) findViewById(R.id.btn_rock_down);
		btnPaperDown = (Button) findViewById(R.id.btn_paper_down);
		btnScissorDown = (Button) findViewById(R.id.btn_scissor_down);
		
		choiceUp = (ImageView)findViewById(R.id.img_choice_up);
		choiceDown = (ImageView)findViewById(R.id.img_choice_down);
		
		txtScoreUp = (TextView)findViewById(R.id.btn_score_up);
		txtScoreDown = (TextView)findViewById(R.id.btn_score_down);
		
		txtStatusUp = (TextView) findViewById(R.id.txt_status_up);
		txtStatusDown = (TextView) findViewById(R.id.txt_status_down);
		
		txtHerTimer = (TextView) findViewById(R.id.txt_timer_up);
		txtMyTimer = (TextView) findViewById(R.id.txt_timer_down);
		
		btnRockDown.setOnClickListener(buttonClickListener);
		btnPaperDown.setOnClickListener(buttonClickListener);
		btnScissorDown.setOnClickListener(buttonClickListener);
		txtStatusDown.setOnClickListener(statusClickListener);
		
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
		
		startGame();
		txtScoreUp.setText(PersianUtil.convertToPersian(mGame.getHerScore().toString()) + "/" + PersianUtil.convertToPersian(mGame.getMaxScore().toString()));
		txtScoreDown.setText(PersianUtil.convertToPersian(mGame.getMyScore().toString()) + "/" + PersianUtil.convertToPersian(mGame.getMaxScore().toString()));
	}
	
	private OnClickListener buttonClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!weAreOnline()){
				soundError.start();
				Toast tNotSent = IcsToast.makeText(mContext, getString(R.string.you_are_not_online), IcsToast.LENGTH_SHORT);
				tNotSent.show();
				return;
			}
			else if(mGame.getTrn() != Turn.MY && mGame.getTrn() != Turn.BOTH){
				soundError.start();
				Toast notTurn = IcsToast.makeText(mContext, getString(R.string.not_your_turn), IcsToast.LENGTH_SHORT);
				notTurn.show();
				return;
			}
			myTimer.cancel();
			if(mGame.getTrn() == Turn.BOTH)
				soundChoice.start();
			
			switch(v.getId()){
			case R.id.btn_rock_down:
				sendMsg(ROCK_MSG);
				mGame.setMyChoice(Choice.ROCK);
				choiceDown.setBackgroundResource(R.raw.a_rock_down);
				break;
			case R.id.btn_paper_down:
				sendMsg(PAPER_MSG);
				mGame.setMyChoice(Choice.PAPER);
				choiceDown.setBackgroundResource(R.raw.a_paper_down);
				break;
			case R.id.btn_scissor_down:
				sendMsg(SCISSOR_MSG);
				mGame.setMyChoice(Choice.SCISSOR);
				choiceDown.setBackgroundResource(R.raw.a_scissor_down);
				break;
			}
			
			if(mGame.getTrn() == Turn.NONE){
				switch (mGame.getHerChoice()) {
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
	
	@Override
	protected void onReceiveMsg(String msg) {
		if(msg.equals(ROCK_MSG) || msg.equals(PAPER_MSG) || msg.equals(SCISSOR_MSG)){
			herTimer.cancel();
			if(mGame.getTrn() == Turn.BOTH)
				soundChoice.start();
			int background = R.raw.tick;
			if(msg.equals(ROCK_MSG)){
				mGame.setHerChoice(Choice.ROCK);
				background = R.raw.a_rock_up;
			}
			else if(msg.equals(PAPER_MSG)){
				mGame.setHerChoice(Choice.PAPER);
				background = R.raw.a_paper_up;
			}
			else if(msg.equals(SCISSOR_MSG)){
				mGame.setHerChoice(Choice.SCISSOR);
				background = R.raw.a_scissor_up;
			}
			if(mGame.getTrn() == Turn.MY)
				background = R.raw.tick;
			choiceUp.setBackgroundResource(background);
			if(mGame.getTrn() == Turn.NONE)
				checkWinner();
		}
		else if(msg.startsWith(STATUS_MSG)){
			String status = msg.replaceFirst(STATUS_MSG, "");
			txtStatusUp.setText(status);
		}
		else if(msg.equals(EXIT_MSG)){
			sheLeft();
		}			
	}
			
	
	private void checkWinner() {
		enableChoices(false);
		Toast toast = null;
		Winner result = mGame.judge();
		switch (result) {
		case ME:
			toast = IcsToast.makeText(mContext, getString(R.string.you_won), IcsToast.LENGTH_SHORT);
			break;
		case SHE:
			toast = IcsToast.makeText(mContext, getString(R.string.you_lose), IcsToast.LENGTH_SHORT);
			break;
		case DRAW:
			toast = IcsToast.makeText(mContext, getString(R.string.draw), IcsToast.LENGTH_SHORT);
			break;
		}
		playSound(result);
		toast.show();
		txtScoreUp.setText(mGame.getHerScore().toString() + "/" + mGame.getMaxScore());
		txtScoreDown.setText(mGame.getMyScore().toString() + "/" + mGame.getMaxScore());
		nextRound(false);
	}
	
	private void playSound(Winner first) {
		if(first == Winner.ME)
			soundWin.start();
		else if(first == Winner.SHE)
			soundLose.start();
		else
			soundDraw.start();
	}
	
	private void enableChoices(boolean b) {
		btnRockDown.setClickable(b);
		btnPaperDown.setClickable(b);
		btnScissorDown.setClickable(b);
		
	}
	
	@Override
	protected void startGame() {
		mGame = new RPSGame();
		mGame.init();
		nextRound(true);
	}
	
	@Override
	protected Game getGame() {
		return mGame;
	}

	private void nextRound(boolean isNewGame) {
		final Handler handler = new Handler();
		if(!isNewGame){
			handler.postDelayed(new Runnable() {
			    @Override
			    public void run() {
					choiceUp.setBackgroundResource(R.raw.square);
					choiceDown.setBackgroundResource(R.raw.square);
					enableChoices(true);
			    }
			}, 1750);
		}
		if(mGame.getMyScore() < mGame.getMaxScore() && mGame.getHerScore() < mGame.getMaxScore()){
			mGame.nextRound();
			myTimer.start();
			herTimer.start();
		}
		else
			endGame();
	}
	
	@Override
	protected String getExitMsg() {
		return EXIT_MSG;
	}
}
