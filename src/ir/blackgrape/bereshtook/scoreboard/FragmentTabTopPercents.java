package ir.blackgrape.bereshtook.scoreboard;

import ir.blackgrape.bereshtook.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class FragmentTabTopPercents extends FragmentTabScoreboard {

	public FragmentTabTopPercents() {
		factor = "win_percent";
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.tab_top_percents, container, false);
		lstTop10 = (ListView) view.findViewById(R.id.tops_list);
		
		return view;
	}
	
}
