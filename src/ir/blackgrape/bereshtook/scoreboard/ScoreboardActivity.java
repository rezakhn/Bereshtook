package ir.blackgrape.bereshtook.scoreboard;

import ir.blackgrape.bereshtook.R;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ScoreboardActivity extends SherlockFragmentActivity {
	
	private ActionBar mActionBar;
	private ViewPager mPager;
	private String username;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scoreboard);
		
		username = getIntent().getStringExtra("username");
		
		mActionBar = getSupportActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		//mActionBar.setHomeButtonEnabled(true);
		//mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setTitle(R.string.top_bereshtooks);
		
		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setOffscreenPageLimit(2);
		FragmentManager fm = getSupportFragmentManager();
		
		ViewPager.SimpleOnPageChangeListener viewPagerListener = new ViewPager.SimpleOnPageChangeListener(){
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				mActionBar.setSelectedNavigationItem(position);
			}
		};
		
		mPager.setOnPageChangeListener(viewPagerListener);
		ScoreboardPagerAdapter viewPagerAdapter = new ScoreboardPagerAdapter(fm);
		viewPagerAdapter.setUsername(username);
		mPager.setAdapter(viewPagerAdapter);
		
		ActionBar.TabListener tabListener = new ActionBar.TabListener() {
			
			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				
			}
			
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				mPager.setCurrentItem(tab.getPosition());
			}
			
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				
			}
		};
		
		Tab tabTopWins = mActionBar.newTab().setText(getString(R.string.wins_num)).setTabListener(tabListener);
		mActionBar.addTab(tabTopWins);
		
		Tab tabTopCoins = mActionBar.newTab().setText(getString(R.string.coins_num)).setTabListener(tabListener);
		mActionBar.addTab(tabTopCoins);
		
		Tab tabTopPercents = mActionBar.newTab().setText(getString(R.string.win_percent)).setTabListener(tabListener);
		mActionBar.addTab(tabTopPercents);
		
	}
	
}
