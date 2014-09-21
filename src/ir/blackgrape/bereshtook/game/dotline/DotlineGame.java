package ir.blackgrape.bereshtook.game.dotline;

import ir.blackgrape.bereshtook.game.Game;

public class DotlineGame extends Game {

	private Integer herScore;
	private Integer myScore;
	
	@Override
	protected Integer getHerScore() {
		return herScore;
	}

	@Override
	protected Integer getMyScore() {
		return myScore;
	}
	
	protected void setHerScore(Integer herScore){
		this.herScore = herScore;
	}
	
	protected void setMyScore(Integer myScore){
		this.myScore = myScore;
	}

}
