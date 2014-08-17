package ir.blackgrape.bereshtook.dialogs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.BereshtookApplication;
import ir.blackgrape.bereshtook.MainWindow;
import ir.blackgrape.bereshtook.XMPPRosterServiceAdapter;
import ir.blackgrape.bereshtook.data.BereshtookConfiguration;
import ir.blackgrape.bereshtook.exceptions.BereshtookXMPPAdressMalformedException;
import ir.blackgrape.bereshtook.preferences.AccountPrefs;
import ir.blackgrape.bereshtook.util.PreferenceConstants;
import ir.blackgrape.bereshtook.util.XMPPHelper;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class FirstStartDialog extends AlertDialog implements DialogInterface.OnClickListener,
		CompoundButton.OnCheckedChangeListener, TextWatcher {

	private MainWindow mainWindow;
	private Button mOkButton;
	private EditText mEditJabberID;
	private EditText mEditPassword;
	private EditText mRepeatPassword;
	private CheckBox mCreateAccount;
	private BereshtookConfiguration mConfig;
	
	private String androidId;
	private static final String URL_FIND = "http://bereshtook.ir:3372/users/find/";
	private static final String URL_INSERT = "http://bereshtook.ir:3372/users/insert/";

	public FirstStartDialog(MainWindow mainWindow,
			XMPPRosterServiceAdapter serviceAdapter, boolean disabled) {
		super(mainWindow);
		this.mainWindow = mainWindow;

		setTitle(R.string.StartupDialog_Title);

		LayoutInflater inflater = (LayoutInflater) mainWindow
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View group = inflater.inflate(R.layout.firststartdialog, null, false);
		setView(group);

		setButton(BUTTON_POSITIVE, mainWindow.getString(R.string.go), this);
		//setButton(BUTTON_NEUTRAL, mainWindow.getString(R.string.StartupDialog_advanced), this);

		mEditJabberID = (EditText) group.findViewById(R.id.StartupDialog_JID_EditTextField);
		mEditPassword = (EditText) group.findViewById(R.id.StartupDialog_PASSWD_EditTextField);
		mRepeatPassword = (EditText) group.findViewById(R.id.startup_password_repeat);
		mCreateAccount = (CheckBox) group.findViewById(R.id.create_account);

		mConfig = BereshtookApplication.getConfig(mainWindow);
		mEditJabberID.setText(mConfig.userName);
		mEditPassword.setText(mConfig.password);
		
		enableDialog(!disabled);
		
		androidId = Secure.getString(getContext().getContentResolver(), Secure.ANDROID_ID);
		String serverURL = URL_FIND + "/" + androidId;
		DataFetcher df = new DataFetcher();
		df.setCmd(COMMAND.FIND);
		//df.execute(serverURL);
		
		//mEditJabberID.addTextChangedListener(this);
		//mEditPassword.addTextChangedListener(this);
		mRepeatPassword.addTextChangedListener(this);
		mCreateAccount.setOnCheckedChangeListener(this);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mOkButton = getButton(BUTTON_POSITIVE);
	}

	private void enableDialog(boolean e){
		mEditJabberID.setEnabled(e);
		mEditPassword.setEnabled(e);
		mRepeatPassword.setEnabled(e);
		mCreateAccount.setEnabled(e);
	}
	
	private void enableCreateAccount(boolean e){
		mCreateAccount.setEnabled(e);
	}

	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case BUTTON_POSITIVE:
			verifyAndSavePreferences();
			boolean create_account = mCreateAccount.isChecked();
			mainWindow.startConnection(create_account);
			break;
		case BUTTON_NEUTRAL:
			verifyAndSavePreferences();
			mainWindow.startActivity(new Intent(mainWindow, AccountPrefs.class));
			break;
		}
	}

	private void verifyAndSavePreferences() {
		String password = mEditPassword.getText().toString();
		String jabberID;
		try {
			jabberID = XMPPHelper.verifyJabberID(mEditJabberID.getText() + "@bereshtook.ir");
		} catch (BereshtookXMPPAdressMalformedException e) {
			e.printStackTrace();
			jabberID = mEditJabberID.getText().toString();
		}
		String resource = String.format("%s.%08X",
			mainWindow.getString(R.string.app_name),
			new java.util.Random().nextInt());

		savePreferences(jabberID, password, resource);
		cancel();
	}

	private void updateDialog() {
		boolean is_ok = true;
		// verify jabber ID
		Editable jid = mEditJabberID.getText();
		try {
			XMPPHelper.verifyJabberID(jid + "@bereshtook.ir");
			//mOkButton.setOnClickListener(this);
			mEditJabberID.setError(null);
		} catch (BereshtookXMPPAdressMalformedException e) {
			is_ok = false;
			if(jid.toString().contains(" "))
				mEditJabberID.setError(mainWindow.getString(R.string.space_is_no_allowed));
			else if(jid.length() < 4)
				mEditJabberID.setError(mainWindow.getString(R.string.min_char_is_4));
			else if (jid.length() > 0)
				mEditJabberID.setError(mainWindow.getString(R.string.Global_JID_malformed));
		}
		if (mEditPassword.length() == 0)
			is_ok = false;
		if (mCreateAccount.isChecked()) {
			boolean passwords_match = mEditPassword.getText().toString().equals(
					mRepeatPassword.getText().toString());
			is_ok = is_ok && passwords_match;
			mRepeatPassword.setError((passwords_match || mRepeatPassword.length() == 0) ?
					null : mainWindow.getString(R.string.StartupDialog_error_password));
		}
		mOkButton.setEnabled(is_ok);
	}

	/* CompoundButton.OnCheckedChangeListener for mCreateAccount */
	@Override
	public void onCheckedChanged(CompoundButton btn,boolean isChecked) {
		mRepeatPassword.setVisibility(isChecked? View.VISIBLE : View.GONE);
		updateDialog();
	}
	@Override
	public void afterTextChanged(Editable s) {
		updateDialog();
	}
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	private void savePreferences(String jabberID, String password, String resource) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(mainWindow);
		Editor editor = sharedPreferences.edit();

		editor.putString(PreferenceConstants.JID, jabberID);
		editor.putString(PreferenceConstants.PASSWORD, password);
		editor.putString(PreferenceConstants.RESSOURCE, resource);
		editor.putString(PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT);
		editor.commit();
	}
	
	enum COMMAND{
		FIND, INSERT
	}
	
	class DataFetcher extends AsyncTask<String, Void, JSONObject> {

		private final HttpClient client = new DefaultHttpClient();
		private ProgressDialog dialog = new ProgressDialog(
				mainWindow);
		private COMMAND cmd;
		
		public void setCmd(COMMAND cmd){
			this.cmd = cmd;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			dialog.setMessage(mainWindow.getString(R.string.tops_loading));
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
		protected void onPostExecute(JSONObject json) {
			try {
				if(json == null || json.equals("problem"))
					return;
				String result = json.getString("result");
				if(result == null || result.equals("error"))
					return;
				
				enableDialog(true);
				
				if(result.equals("not_exist"))
					enableCreateAccount(true);
				else if(result.equals("user_exist")){
					String username = json.getString("username");
					String password = json.getString("password");
					enableCreateAccount(false);
				}
				
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
