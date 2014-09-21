package ir.blackgrape.bereshtook.game.rps;

import ir.blackgrape.bereshtook.game.Game;

public class RPSGame extends Game {
	public enum Choice {NONE, ROCK, PAPER, SCISSOR};
	public enum Turn {MY, HER, NONE, BOTH};
	public enum Winner {ME, SHE, DRAW};
	
	private Integer myScore;
	private Integer herScore;
	private Integer maxScore;
	private Choice myChoice;
	private Choice herChoice;
	private Turn trn;
	
	public Turn getTrn() {
		return trn;
	}
	public void setTrn(Turn turn) {
		this.trn = turn;
	}
	
	@Override
	public Integer getMyScore() {
		return myScore;
	}
	public void setMyScore(Integer myScore) {
		this.myScore = myScore;
	}
	
	@Override
	public Integer getHerScore() {
		return herScore;
	}
	public void setHerScore(Integer herScore) {
		this.herScore = herScore;
	}

	public Integer getMaxScore() {
		return maxScore;
	}
	public void setMaxScore(Integer maxScore) {
		this.maxScore = maxScore;
	}
	
	public Choice getMyChoice() {
		return myChoice;
	}
	public void setMyChoice(Choice myChoice) {
		this.myChoice = myChoice;
		if(trn == Turn.MY)
			trn = Turn.NONE;
		else if(trn == Turn.BOTH)
			trn = Turn.HER;
	}

	public Choice getHerChoice() {
		return herChoice;
	}
	public void setHerChoice(Choice herChoice) {
		this.herChoice = herChoice;
		if(trn == Turn.HER)
			trn = Turn.NONE;
		else if(trn == Turn.BOTH)
			trn = Turn.MY;
	}

	public void init(){
		myScore = 0;
		herScore = 0;
		maxScore = 5;
		trn = Turn.NONE;
		myChoice = Choice.NONE;
		herChoice = Choice.NONE;
	}
	
	public void nextRound(){
		trn = Turn.BOTH;
	}
	
	public Winner judge(){
		Winner wnr = Winner.DRAW;
		
		if(myChoice == herChoice)
			wnr = Winner.DRAW;
		else if(herChoice == Choice.NONE)
			wnr = Winner.ME;
		else if(myChoice == Choice.NONE)
			wnr = Winner.SHE;
		else if(myChoice == Choice.ROCK && herChoice == Choice.SCISSOR)
			wnr = Winner.ME;
		else if(herChoice == Choice.ROCK && myChoice == Choice.SCISSOR)
			wnr = Winner.SHE;
		else if(myChoice == Choice.SCISSOR && herChoice == Choice.PAPER)
			wnr = Winner.ME;
		else if(herChoice == Choice.SCISSOR && herChoice == Choice.SCISSOR)
			wnr = Winner.SHE;
		else if(myChoice == Choice.PAPER && herChoice == Choice.ROCK)
			wnr = Winner.ME;
		else if(herChoice == Choice.PAPER && myChoice == Choice.ROCK)
			wnr = Winner.SHE;
		
		if(wnr == Winner.ME)
			myScore++;
		else if(wnr == Winner.SHE)
			herScore++;
		return wnr;
	}
}
