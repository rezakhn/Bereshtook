package ir.blackgrape.bereshtook.scoreboard;

import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.data.RosterProvider.RosterConstants;
import ir.blackgrape.bereshtook.util.ConstantKeys;
import ir.blackgrape.bereshtook.util.Decryptor;
import ir.blackgrape.bereshtook.util.PersianUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class FragmentTabScoreboard extends SherlockFragment {
	protected String factor;
	protected String myUsername;
	protected Integer myRank;
	protected Integer myScore;
	protected ListView lstTop10;
	protected ArrayList<HashMap<String, String>> oslist = new ArrayList<HashMap<String, String>>();

	private static final String RANK = "rank";
	private static final String USERNAME = "user";
	private static final String SCORE = "score";

	public void setUsername(String username) {
		this.myUsername = username;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		setUserVisibleHint(true);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (myRank == null) {
			try {
				String serverURL = Decryptor.convert(ConstantKeys.URL_TOPS) + factor + "/" + myUsername;
				new DataFetcher().execute(serverURL);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
					result = "problem";

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
				if(result == null || result.equals("problem"))
					return;
				String status = result.getString("result");
				if(status == null || !status.equals("success"))
					return;
				
				myRank = result.getInt("myRank");
				myScore = result.getInt("myScore");
				
				if(myRank == null || myScore == null)
					return;
				
				JSONObject tops = result.getJSONObject("json");
				
				for(Integer i=0; i<tops.length(); i++){
					JSONObject item = tops.getJSONObject(i.toString());
					HashMap<String, String> map = new HashMap<String, String>();
					Integer rank = i+1;
					map.put(RANK, PersianUtil.convertToPersian(rank.toString()));
					map.put(USERNAME, item.getString(USERNAME));
					Integer score = item.getInt(factor);
					map.put(SCORE, PersianUtil.convertToPersian(score.toString()));
					oslist.add(map);
				}
				
				if(myRank > tops.length()){
					HashMap<String, String> me = new HashMap<String, String>();
					me.put(RANK, PersianUtil.convertToPersian(myRank.toString()));
					me.put(USERNAME, myUsername);
					me.put(SCORE, PersianUtil.convertToPersian(myScore.toString()));
					oslist.add(me);
				}
				
				TopsListAdaptor adaptor = new TopsListAdaptor(FragmentTabScoreboard.this.getActivity(), oslist, R.layout.tops_row,
						new String[] { RANK, USERNAME, SCORE}, new int[] {R.id.rank, R.id.username, R.id.score});
				adaptor.setMyRank(myRank);
				lstTop10.setAdapter(adaptor);
				lstTop10.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						TextView UserText = (TextView) view.findViewById(R.id.username);
						String username = (String) UserText.getText();
						if(myUsername == null || myUsername.equals(username))
							return;
						String userJid = username + "@bereshtook.ir";
						Intent chatIntent = new Intent(FragmentTabScoreboard.this.getActivity(),
								ir.blackgrape.bereshtook.chat.ChatWindow.class);
						Uri userNameUri = Uri.parse(userJid);
						chatIntent.setData(userNameUri);
						chatIntent.putExtra(
								ir.blackgrape.bereshtook.chat.ChatWindow.INTENT_EXTRA_USERNAME,
								username);
						
						startActivity(chatIntent);
					}
				});
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
