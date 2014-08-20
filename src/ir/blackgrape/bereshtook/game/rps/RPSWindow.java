package ir.blackgrape.bereshtook.game.rps;

import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.game.Game;
import ir.blackgrape.bereshtook.game.GameWindow;
import ir.blackgrape.bereshtook.game.rps.RPSGame.Choice;
import ir.blackgrape.bereshtook.game.rps.RPSGame.Turn;
import ir.blackgrape.bereshtook.game.rps.RPSGame.Winner;
import ir.blackgrape.bereshtook.util.StringUtil;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
	public static final String EXIT_MSG = RPS_GAME + "EXIT#";
	
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rps_game);
		
		btnRockUp = (Button) findViewById(R.id.btnRockUp);
		btnRockDown = (Button) findViewById(R.id.btn_rock_down);
		btnPaperUp = (Button) findViewById(R.id.btnPaperUp);
		btnPaperDown = (Button) findViewById(R.id.btn_paper_down);
		btnScissorUp = (Button) findViewById(R.id.btnScissorUp);
		btnScissorDown = (Button) findViewById(R.id.btn_scissor_down);
		
		choiceUp = (ImageView)findViewById(R.id.img_choice_up);
		choiceDown = (ImageView)findViewById(R.id.img_choice_down);
		
		txtScoreUp = (TextView)findViewById(R.id.btn_score_up);
		txtScoreDown = (TextView)findViewById(R.id.btn_score_down);
		
		txtStatusUp = (TextView) findViewById(R.id.txt_status_up);
		txtStatusDown = (TextView) findViewById(R.id.txt_status_down);
		
		btnRockDown.setOnClickListener(buttonClickListener);
		btnPaperDown.setOnClickListener(buttonClickListener);
		btnScissorDown.setOnClickListener(buttonClickListener);
		
		txtStatusDown.setOnClickListener(statusClickListener);
		
		startGame();
		txtScoreUp.setText(StringUtil.convertToPersian(game.getHerScore().toString()) + "/" + StringUtil.convertToPersian(game.getMaxScore().toString()));
		txtScoreDown.setText(StringUtil.convertToPersian(game.getMyScore().toString()) + "/" + StringUtil.convertToPersian(game.getMaxScore().toString()));
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
			else if(game.getTrn() != Turn.MY && game.getTrn() != Turn.BOTH){
				soundError.start();
				Toast notTurn = IcsToast.makeText(mContext, getString(R.string.not_your_turn), IcsToast.LENGTH_SHORT);
				notTurn.show();
				return;
			}
			if(game.getTrn() == Turn.BOTH)
				soundChoice.start();
			
			switch(v.getId()){
			case R.id.btn_rock_down:
				sendMsg(ROCK_MSG);
				game.setMyChoice(Choice.ROCK);
				choiceDown.setBackgroundResource(R.raw.a_rock_down);
				break;
			case R.id.btn_paper_down:
				sendMsg(PAPER_MSG);
				game.setMyChoice(Choice.PAPER);
				choiceDown.setBackgroundResource(R.raw.a_paper_down);
				break;
			case R.id.btn_scissor_down:
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
	
	@Override
	protected void onReceiveMsg(String msg) {
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
		Winner result = game.judge();
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
		txtScoreUp.setText(game.getHerScore().toString() + "/" + game.getMaxScore());
		txtScoreDown.setText(game.getMyScore().toString() + "/" + game.getMaxScore());
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
		game = new RPSGame();
		game.init();
		nextRound(true);
	}
	
	@Override
	protected Game getGame() {
		return game;
	}

	private void nextRound(boolean isNewGame) {
		game.nextRound();
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
		if(game.getMyScore() < game.getMaxScore() && game.getHerScore() < game.getMaxScore())
			game.nextRound();
		else
			endGame();
	}
	
	@Override
	public void onBackPressed() {
	    new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle(R.string.exit_game_title)
	        .setMessage(R.string.exit_game_message)
	        .setPositiveButton(R.string.exit_game_confirm, new DialogInterface.OnClickListener()
		    {
		        @Override
		        public void onClick(DialogInterface dialog, int which) {
		        	sendMsg(EXIT_MSG);
		            finish();
		        }
	
		    })
		    .setNegativeButton(R.string.exit_game_cancel, null)
		    .show();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if(!gameEnded){
			sendMsg(EXIT_MSG);
        	finish();
		}
	}
}
