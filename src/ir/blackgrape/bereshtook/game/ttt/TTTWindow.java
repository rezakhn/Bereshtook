package ir.blackgrape.bereshtook.game.ttt;

import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.game.Game;
import ir.blackgrape.bereshtook.game.GameWindow;
import ir.blackgrape.bereshtook.game.ttt.TTTGame.Turn;
import ir.blackgrape.bereshtook.game.ttt.TTTGame.WinType;
import ir.blackgrape.bereshtook.game.ttt.TTTGame.Winner;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.internal.widget.IcsToast;

public class TTTWindow extends GameWindow {
	
	public static final String TTT_GAME = GAME_CODE + "TTT#";
	public static final String INVITE_MSG = TTT_GAME + "INVITE#";
	public static final String ACCEPT_MSG = TTT_GAME + "ACCEPT#";
	public static final String DENY_MSG = TTT_GAME + "DENY#";
	public static final String EXIT_MSG = TTT_GAME + "EXIT#";
	
	private static final String ONE_MSG = TTT_GAME + "ONE#";
	private static final String TWO_MSG = TTT_GAME + "TWO#";
	private static final String THREE_MSG = TTT_GAME + "THREE#";
	private static final String FOUR_MSG = TTT_GAME + "FOUR#";
	private static final String FIVE_MSG = TTT_GAME + "FIVE#";
	private static final String SIX_MSG = TTT_GAME + "SIX#";
	private static final String SEVEN_MSG = TTT_GAME + "SEVEN#";
	private static final String EIGHT_MSG = TTT_GAME + "EIGHT#";
	private static final String NINE_MSG = TTT_GAME + "NINE#";
	
	private Context mContext;
	private TTTGame game;
	private ImageView iv[][];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.ttt_game);
		iv = new ImageView[3][3];
		
		iv[0][0] = (ImageView) findViewById(R.id.cell_one);
		iv[0][1] = (ImageView) findViewById(R.id.cell_two);
		iv[0][2] = (ImageView) findViewById(R.id.cell_three);
		
		iv[1][0] = (ImageView) findViewById(R.id.cell_four);
		iv[1][1] = (ImageView) findViewById(R.id.cell_five);
		iv[1][2] = (ImageView) findViewById(R.id.cell_six);
		
		iv[2][0] = (ImageView) findViewById(R.id.cell_seven);
		iv[2][1] = (ImageView) findViewById(R.id.cell_eight);
		iv[2][2] = (ImageView) findViewById(R.id.cell_nine);
		
		for(int j=0; j<3; j++)
			for(int i=0; i<3; i++)
				iv[j][i].setOnClickListener(mClickListener);
		
		txtStatusUp = (TextView) findViewById(R.id.txt_status_up);
		txtStatusDown = (TextView) findViewById(R.id.txt_status_down);
		txtStatusDown.setOnClickListener(statusClickListener);
		startGame();
	}
	
	private OnClickListener mClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(!weAreOnline()){
				soundError.start();
				Toast tNotSent = IcsToast.makeText(mContext, getString(R.string.you_are_not_online), IcsToast.LENGTH_SHORT);
				tNotSent.show();
				return;
			}
			else if(game.getTrn() != Turn.MY){
				soundError.start();
				Toast notTurn = IcsToast.makeText(mContext, getString(R.string.not_your_turn), IcsToast.LENGTH_SHORT);
				notTurn.show();
				return;
			}
				
			switch(v.getId()){
			case R.id.cell_one:
				if(game.map[0][0]!=-1)
					break;
				game.setMyChoice(0, 0);
				sendMsg(ONE_MSG);
				iv[0][0].setImageResource(R.raw.ttt_ex);
				break;
			case R.id.cell_two:
				if(game.map[0][1]!=-1)
					break;
				game.setMyChoice(0, 1);
				sendMsg(TWO_MSG);
				iv[0][1].setImageResource(R.raw.ttt_ex);
				break;
			case R.id.cell_three:
				if(game.map[0][2]!=-1)
					break;
				game.setMyChoice(0, 2);
				sendMsg(THREE_MSG);
				iv[0][2].setImageResource(R.raw.ttt_ex);
				break;
			case R.id.cell_four:
				if(game.map[1][0]!=-1)
					break;
				game.setMyChoice(1, 0);
				sendMsg(FOUR_MSG);
				iv[1][0].setImageResource(R.raw.ttt_ex);
				break;
			case R.id.cell_five:
				if(game.map[1][1]!=-1)
					break;
				game.setMyChoice(1, 1);
				sendMsg(FIVE_MSG);
				iv[1][1].setImageResource(R.raw.ttt_ex);
				break;
			case R.id.cell_six:
				if(game.map[1][2]!=-1)
					break;
				game.setMyChoice(1, 2);
				sendMsg(SIX_MSG);
				iv[1][2].setImageResource(R.raw.ttt_ex);
				break;
			case R.id.cell_seven:
				if(game.map[2][0]!=-1)
					break;
				game.setMyChoice(2, 0);
				sendMsg(SEVEN_MSG);
				iv[2][0].setImageResource(R.raw.ttt_ex);
				break;
			case R.id.cell_eight:
				if(game.map[2][1]!=-1)
					break;
				game.setMyChoice(2, 1);
				sendMsg(EIGHT_MSG);
				iv[2][1].setImageResource(R.raw.ttt_ex);
				break;
			case R.id.cell_nine:
				if(game.map[2][2]!=-1)
					break;
				game.setMyChoice(2, 2);
				sendMsg(NINE_MSG);
				iv[2][2].setImageResource(R.raw.ttt_ex);
				break;
			}
			checkWinner();
		}
	};
	
	@Override
	protected void onReceiveMsg(String msg) {
		if(msg.equals(ONE_MSG)){
			game.setHerChoice(0, 0);
			iv[0][0].setImageResource(R.raw.ttt_oh);
		}
		else if(msg.equals(TWO_MSG)){
			game.setHerChoice(0, 1);
			iv[0][1].setImageResource(R.raw.ttt_oh);
		}
		else if(msg.equals(THREE_MSG)){
			game.setHerChoice(0, 2);
			iv[0][2].setImageResource(R.raw.ttt_oh);
		}
		else if(msg.equals(FOUR_MSG)){
			game.setHerChoice(1, 0);
			iv[1][0].setImageResource(R.raw.ttt_oh);
		}
		else if(msg.equals(FIVE_MSG)){
			game.setHerChoice(1, 1);
			iv[1][1].setImageResource(R.raw.ttt_oh);
		}
		else if(msg.equals(SIX_MSG)){
			game.setHerChoice(1, 2);
			iv[1][2].setImageResource(R.raw.ttt_oh);
		}
		else if(msg.equals(SEVEN_MSG)){
			game.setHerChoice(2, 0);
			iv[2][0].setImageResource(R.raw.ttt_oh);
		}
		else if(msg.equals(EIGHT_MSG)){
			game.setHerChoice(2, 1);
			iv[2][1].setImageResource(R.raw.ttt_oh);
		}
		else if(msg.equals(NINE_MSG)){
			game.setHerChoice(2, 2);
			iv[2][2].setImageResource(R.raw.ttt_oh);
		}
		if(msg.equals(ONE_MSG) || msg.equals(TWO_MSG) || msg.equals(THREE_MSG)
				|| msg.equals(FOUR_MSG) || msg.equals(FIVE_MSG) || msg.equals(SIX_MSG)
				|| msg.equals(SEVEN_MSG) || msg.equals(EIGHT_MSG) || msg.equals(NINE_MSG)){
			checkWinner();
		}
		else if(msg.equals(STATUS_MSG)){
			String status = msg.replaceFirst(STATUS_MSG, "");
			txtStatusUp.setText(status);
		}
		else if(msg.equals(EXIT_MSG)){
			sheLeft();
		}
	}

	private void checkWinner(){
		final Pair<Winner, WinType> result = game.judge();
		if(result.first == Winner.DRAW && !game.noRemainedCell())
			return; // continue game!
		else if(result.first == Winner.DRAW && game.noRemainedCell()){
			playSound(result.first);
			nextRound(false);
		}
		else{
			int crossId = 0;
			switch (result.second) {
			case ONE:
				crossId = R.id.cross1;
				break;
			case TWO:
				crossId = R.id.cross2;
				break;				
			case THREE:
				crossId = R.id.cross3;
				break;
			case FOUR:
				crossId = R.id.cross4;
				break;
			case FIVE:
				crossId = R.id.cross5;
				break;
			case SIX:
				crossId = R.id.cross6;
				break;
			case SEVEN:
				crossId = R.id.cross7;
				break;
			case EIGHT:
				crossId = R.id.cross8;
				break;
			}
			
			final ImageView cross = (ImageView) findViewById(crossId);
			
	    	cross.setVisibility(View.VISIBLE);
	    	playSound(result.first);
			updateWinnerScore(result.first);
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
			    @Override
			    public void run() {
			    	cross.setVisibility(View.GONE);
			    }
			}, 2000);
			nextRound(false);
		}
	}
	
	private void playSound(Winner first) {
		if(first == Winner.ME)
			soundWin.start();
		else if(first == Winner.SHE)
			soundLose.start();
		else
			soundDraw.start();
	}

	protected void updateWinnerScore(Winner wnr) {
		int numId = 0;
		int score;
		int scoreboardId;
		
		if(wnr.equals(Winner.ME)){
			score = game.getMyScore();
			scoreboardId = R.id.score_down;
		}
		else{
			score = game.getHerScore();
			scoreboardId = R.id.score_up;
		}
		switch (score) {
		case 0:
			numId = R.raw.ttt_co0;
			break;
		case 1:
			numId = R.raw.ttt_co1;
			break;
		case 2:
			numId = R.raw.ttt_co2;
			break;
		case 3:
			numId = R.raw.ttt_co3;
			break;
		case 4:
			numId = R.raw.ttt_co4;
			break;
		case 5:
			numId = R.raw.ttt_co5;
			break;
		case 6:
			numId = R.raw.ttt_co6;
			break;
		case 7:
			numId = R.raw.ttt_co7;
			break;
		case 8:
			numId = R.raw.ttt_co8;
			break;
		case 9:
			numId = R.raw.ttt_co9;
			break;
		}
		
		ImageView scoreboard = (ImageView) findViewById(scoreboardId);
		scoreboard.setImageResource(numId);
	}

	@Override
	protected void startGame() {
		game = new TTTGame();
		game.init(!isGuest);
		nextRound(true);
	}
	
	@Override
	protected Game getGame() {
		return game;
	}
	
	private void nextRound(boolean isNewGame){
		final Handler handler = new Handler();
		if(!isNewGame){
			handler.postDelayed(new Runnable() {
			    @Override
			    public void run() {
					for(int j=0; j<3; j++)
						for(int i=0; i<3; i++)
							iv[j][i].setImageResource(android.R.color.transparent);
			    }
			}, 2000);
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
}
