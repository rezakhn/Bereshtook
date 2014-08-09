package ir.blackgrape.bereshtook;

import ir.blackgrape.bereshtook.data.BereshtookConfiguration;
import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

public class BereshtookApplication extends Application {
	// identity name and type, see:
	// http://xmpp.org/registrar/disco-categories.html
	public static final String XMPP_IDENTITY_NAME = "Bereshtook";
	public static final String XMPP_IDENTITY_TYPE = "phone";

	// MTM is needed globally for both the backend (connect)
	// and the frontend (display dialog)
	//public MemorizingTrustManager mMTM;

	private BereshtookConfiguration mConfig;

	public BereshtookApplication() {
		super();
	}

	@Override
	public void onCreate() {
		//mMTM = new MemorizingTrustManager(this);
		mConfig = new BereshtookConfiguration(PreferenceManager
				.getDefaultSharedPreferences(this));
	}

	public static BereshtookApplication getApp(Context ctx) {
		return (BereshtookApplication)ctx.getApplicationContext();
	}

	public static BereshtookConfiguration getConfig(Context ctx) {
		return getApp(ctx).mConfig;
	}
}

