package ir.blackgrape.bereshtook.scoreboard;

import ir.blackgrape.bereshtook.R;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

public class TopsListAdaptor extends SimpleAdapter {
	
	private Context context;
	private Integer myRank;
	
	public void setMyRank(Integer rank){
		this.myRank = rank;
	}
	public TopsListAdaptor(Context context,
			List<? extends Map<String, ?>> data, int resource, String[] from,
			int[] to) {
		super(context, data, resource, from, to);
		this.context = context;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view =  super.getView(position, convertView, parent);
		if(myRank != null && position == myRank-1)
			view.setBackgroundColor(context.getResources().getColor(R.color.top_me_background));
		else
			view.setBackgroundColor(context.getResources().getColor(R.color.tops_row_background));
		return view;
	}

}
