package ir.blackgrape.bereshtook.scoreboard;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class ScoreboardPagerAdapter extends FragmentPagerAdapter {

	final int PAGE_COUNT = 3;
	private String username;
	
	public ScoreboardPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int arg0) {
		switch (arg0) {
		case 0:
			FragmentTabTopWins tabTopWins = new FragmentTabTopWins();
			tabTopWins.setUsername(username);
			return tabTopWins;
		
		case 1:
			FragmentTabTopCoins tabTopCoins = new FragmentTabTopCoins();
			tabTopCoins.setUsername(username);
			return tabTopCoins;
			
		case 2:
			FragmentTabTopPercents tabTopPercents = new FragmentTabTopPercents();
			tabTopPercents.setUsername(username);
			return tabTopPercents;
		
		default:
			return null;
		}
	}

	@Override
	public int getCount() {
		return PAGE_COUNT;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
