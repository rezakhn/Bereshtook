package ir.blackgrape.bereshtook.game.ttt;

import ir.blackgrape.bereshtook.game.Game;
import android.util.Pair;


public class TTTGame extends Game {
	
	public enum Turn {MY, HER};
	public enum Winner {ME, SHE, DRAW};
	public enum WinType {ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT};
	
	public int map[][];
	
	private Integer myScore;
	private Integer herScore;
	private Integer maxScore;
	
	private Turn trn;
	private Turn firstTurn;
	
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
	
	public void setMyChoice(int j, int i) {
		map[j][i] = 1;
		trn = Turn.HER;
	}

	public void setHerChoice(int j, int i) {
		map[j][i] = 0;
		trn = Turn.MY;
	}
	
	public void init(boolean isMyTurn){
		myScore = 0;
		herScore = 0;
		maxScore = 3;
		if(isMyTurn){
			trn = Turn.MY;
			firstTurn = Turn.HER; //actually first Turn is reverse!!!
		}
		else{
			trn = Turn.HER;
			firstTurn = Turn.MY;
		}
	}
	
    public Pair<Winner, WinType> judge() {
    	Winner w;
    	WinType wt;
    	
    	if(map[0][0]==1 && map[0][1]==1 && map[0][2]==1){
    		w = Winner.ME;
    		wt = WinType.ONE;
    	}
    	else if(map[0][2]==1 && map[1][2]==1 && map[2][2]==1){
    		w = Winner.ME;
    		wt = WinType.TWO;
    	}
    	else if(map[2][0]==1 && map[2][1]==1 && map[2][2]==1){
    		w = Winner.ME;
    		wt = WinType.THREE;
    	}
    	else if(map[0][0]==1 && map[1][0]==1 && map[2][0]==1){
    		w = Winner.ME;
    		wt = WinType.FOUR;
    	}
    	else if(map[0][0]==1 && map[1][1]==1 && map[2][2]==1){
    		w = Winner.ME;
    		wt = WinType.FIVE;
    	}
    	else if(map[0][2]==1 && map[1][1]==1 && map[2][0]==1){
    		w = Winner.ME;
    		wt = WinType.SIX;
    	}
    	else if(map[0][1]==1 && map[1][1]==1 && map[2][1]==1){
    		w = Winner.ME;
    		wt = WinType.SEVEN;
    	}
    	else if(map[1][0]==1 && map[1][1]==1 && map[1][2]==1){
    		w = Winner.ME;
    		wt = WinType.EIGHT;
    	}
    	
    	else if(map[0][0]==0 && map[0][1]==0 && map[0][2]==0){
    		w = Winner.SHE;
    		wt = WinType.ONE;
    	}
    	else if(map[0][2]==0 && map[1][2]==0 && map[2][2]==0){
    		w = Winner.SHE;
    		wt = WinType.TWO;
    	}
    	else if(map[2][0]==0 && map[2][1]==0 && map[2][2]==0){
    		w = Winner.SHE;
    		wt = WinType.THREE;
    	}
    	else if(map[0][0]==0 && map[1][0]==0 && map[2][0]==0){
    		w = Winner.SHE;
    		wt = WinType.FOUR;
    	}
    	else if(map[0][0]==0 && map[1][1]==0 && map[2][2]==0){
    		w = Winner.SHE;
    		wt = WinType.FIVE;
    	}
    	else if(map[0][2]==0 && map[1][1]==0 && map[2][0]==0){
    		w = Winner.SHE;
    		wt = WinType.SIX;
    	}
    	else if(map[0][1]==0 && map[1][1]==0 && map[2][1]==0){
    		w = Winner.SHE;
    		wt = WinType.SEVEN;
    	}
    	else if(map[1][0]==0 && map[1][1]==0 && map[1][2]==0){
    		w = Winner.SHE;
    		wt = WinType.EIGHT;
    	}
    	
    	else{
    		w = Winner.DRAW;
    		wt = null;
    	}
    	if(w == Winner.ME)
    		myScore++;
    	else if(w == Winner.SHE)
    		herScore++;
    	return new Pair<TTTGame.Winner, TTTGame.WinType>(w, wt);
    }
    
	public void nextRound() {
		if(firstTurn == Turn.MY)
			firstTurn = Turn.HER;
		else
			firstTurn = Turn.MY;
		trn = firstTurn;
		
		map = new int[3][3];
		for(int j=0; j<3; j++)
			for(int i=0; i<3; i++)
				map[j][i] = -1;
	}
	
	public boolean noRemainedCell() {
		for(int j=0; j<3; j++)
			for(int i=0; i<3; i++)
				if(map[j][i]==-1)
					return false;
		return true;
	}
}
