package ir.bereshtook.androidclient.game.rps;

import ir.bereshtook.androidclient.R;
import ir.bereshtook.androidclient.game.GameWindow;
import ir.bereshtook.androidclient.game.rps.RPSGame.Choice;
import ir.bereshtook.androidclient.game.rps.RPSGame.Turn;
import ir.bereshtook.androidclient.game.rps.RPSGame.Winner;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
	public static final String STATUS_MSG = RPS_GAME + "STATUS#";
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
	private TextView txtStatusUp;
	private TextView txtStatusDown;
	private Context mContext;
	
	private MediaPlayer soundChoice;
	private MediaPlayer soundWin;
	private MediaPlayer soundDraw;
	private MediaPlayer soundLose;
	private MediaPlayer soundError;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
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
		
		soundChoice = MediaPlayer.create(this, R.raw.sound_choice);
		soundWin = MediaPlayer.create(this, R.raw.sound_victory);
		soundDraw = MediaPlayer.create(this, R.raw.sound_draw);
		soundLose = MediaPlayer.create(this, R.raw.sound_lose);
		soundError = MediaPlayer.create(this, R.raw.sound_error);
		
		btnRockDown.setOnClickListener(buttonClickListener);
		btnPaperDown.setOnClickListener(buttonClickListener);
		btnScissorDown.setOnClickListener(buttonClickListener);
		
		txtScoreDown.setOnClickListener(statusClickListener);
		
		startGame();
	}

	private OnClickListener buttonClickListener = new OnClickListener() {
		
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
	
	
	private OnClickListener statusClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			AlertDialog.Builder alert = new AlertDialog.Builder(RPSWindow.this);
			alert.setTitle("Set Status");
			
			final TextView input = new TextView(RPSWindow.this);
			alert.setView(input);

			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			    sendMsg(input.getText().toString());
			    txtStatusDown.setText(input.getText());
			  }
			});

			  alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			  public void onClick(DialogInterface dialog, int whichButton) {
			  }
			});
			alert.show();
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
		else if(msg.startsWith(STATUS_MSG)){
			String status = msg.replaceFirst(STATUS_MSG, "");
			txtStatusUp.setText(status);
		}
		else if(msg.equals(EXIT_MSG)){
		    new AlertDialog.Builder(this)
	        .setIcon(android.R.drawable.ic_dialog_alert)
	        .setTitle(R.string.opponent_leaved_title)
	        .setMessage(R.string.opponent_leaved_message)
	        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
	        {
		        @Override
		        public void onClick(DialogInterface dialog, int which) {
		            finish();   
		        }
	        })
	        .show();
		}			
	}
			
	
	private void checkWinner() {
		enableChoices(false);
		Toast toast = null;
		Winner result = game.judge();
		switch (game.judge()) {
		case ME:
			toast = IcsToast.makeText(mContext, getString(R.string.you_win), IcsToast.LENGTH_SHORT);
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
		txtScoreUp.setText(game.getHerScore().toString());
		txtScoreDown.setText(game.getMyScore().toString());
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
