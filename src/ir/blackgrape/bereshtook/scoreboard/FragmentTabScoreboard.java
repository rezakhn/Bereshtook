package ir.blackgrape.bereshtook.scoreboard;

import ir.blackgrape.bereshtook.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class FragmentTabScoreboard extends SherlockFragment {
	protected String factor;
	protected String username;
	protected TextView txtRank;
	protected TextView txtScore;
	protected Integer rank;
	protected Integer score;
	protected ListView lstTop10;
	protected ArrayList<HashMap<String, String>> oslist = new ArrayList<HashMap<String, String>>();

	private static final String RANK = "rank";
	private static final String USERNAME = "user";
	private static final String SCORE = "score";
	private static final String URL = "http://bereshtook.ir:3372/top10/";

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		setUserVisibleHint(true);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (rank == null) {
			String serverURL = URL + factor + "/" + username;
			new DataFetcher().execute(serverURL);
		}
	}

	class DataFetcher extends AsyncTask<String, Void, JSONObject> {

		private final HttpClient client = new DefaultHttpClient();
		private ProgressDialog dialog = new ProgressDialog(
				FragmentTabScoreboard.this.getActivity());

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			dialog.setMessage(getString(R.string.tops_loading));
			dialog.show();

		}

		@Override
		protected JSONObject doInBackground(String... urls) {
			String url = urls[0];
			InputStream inputStream = null;
			String result = "";

			try {
				HttpResponse httpResponse = client.execute(new HttpGet(url));
				inputStream = httpResponse.getEntity().getContent();

				if (inputStream != null)
					result = convertInputStreamToString(inputStream);
				else
					result = "Did not work!";

				try {
					return new JSONObject(result);
				} catch (JSONException e) {
					e.printStackTrace();
				}

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			try {
				if(result == null)
					return;
				rank = result.getInt("myRank");
				score = result.getInt("myScore");
				
				if(rank == null || score == null)
					return;
				
				txtRank.setText(getString(R.string.my_rank_is, rank));
				txtScore.setText(txtScore.getText() + score.toString());
				
				JSONObject tops = result.getJSONObject("json");
				
				for(Integer i=0; i<tops.length(); i++){
					JSONObject item = tops.getJSONObject(i.toString());
					HashMap<String, String> map = new HashMap<String, String>();
					Integer rank = i+1;
					map.put(RANK, rank.toString());
					map.put(USERNAME, item.getString(USERNAME));
					Integer score = item.getInt(factor);
					map.put(SCORE, score.toString());
					oslist.add(map);
				}
				
				ListAdapter adaptor = new SimpleAdapter(FragmentTabScoreboard.this.getActivity(), oslist, R.layout.tops_row,
						new String[] { RANK, USERNAME, SCORE}, new int[] {R.id.rank, R.id.username, R.id.score});
				lstTop10.setAdapter(adaptor);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			dialog.cancel();
			
		}

		private String convertInputStreamToString(InputStream inputStream)
				throws IOException {
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(inputStream));
			String line = "";
			String result = "";
			while ((line = bufferedReader.readLine()) != null)
				result += line;

			inputStream.close();
			return result;

		}

	}
}
