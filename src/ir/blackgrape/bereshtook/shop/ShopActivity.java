/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ir.blackgrape.bereshtook.shop;

import ir.blackgrape.bereshtook.R;
import ir.blackgrape.bereshtook.XMPPDataServiceAdapter;
import ir.blackgrape.bereshtook.service.IXMPPDataService;
import ir.blackgrape.bereshtook.service.XMPPService;
import ir.blackgrape.bereshtook.util.PRIVATE_DATA;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


/**
 * Example game using in-app billing version 3.
 *
 * Before attempting to run this sample, please read the README file. It
 * contains important information on how to set up this project.
 *
 * All the game-specific logic is implemented here in MainActivity, while the
 * general-purpose boilerplate that can be reused in any app is provided in the
 * classes in the util/ subdirectory. When implementing your own application,
 * you can copy over util/*.java to make use of those utility classes.
 *
 * This game is a simple "driving" game where the player can buy gas
 * and drive. The car has a tank which stores gas. When the player purchases
 * gas, the tank fills up (1/4 tank at a time). When the player drives, the gas
 * in the tank diminishes (also 1/4 tank at a time).
 *
 * The user can also purchase a "premium upgrade" that gives them a red car
 * instead of the standard blue one (exciting!).
 *
 * The user can also purchase a subscription ("infinite gas") that allows them
 * to drive without using up any gas while that subscription is active.
 *
 * It's important to note the consumption mechanics for each item.
 *
 * PREMIUM: the item is purchased and NEVER consumed. So, after the original
 * purchase, the player will always own that item. The application knows to
 * display the red car instead of the blue one because it queries whether
 * the premium "item" is owned or not.
 *
 * INFINITE GAS: this is a subscription, and subscriptions can't be consumed.
 *
 * GAS: when gas is purchased, the "gas" item is then owned. We consume it
 * when we apply that item's effects to our app's world, which to us means
 * filling up 1/4 of the tank. This happens immediately after purchase!
 * It's at this point (and not when the user drives) that the "gas"
 * item is CONSUMED. Consumption should always happen when your game
 * world was safely updated to apply the effect of the purchase. So,
 * in an example scenario:
 *
 * BEFORE:      tank at 1/2
 * ON PURCHASE: tank at 1/2, "gas" item is owned
 * IMMEDIATELY: "gas" is consumed, tank goes to 3/4
 * AFTER:       tank at 3/4, "gas" item NOT owned any more
 *
 * Another important point to notice is that it may so happen that
 * the application crashed (or anything else happened) after the user
 * purchased the "gas" item, but before it was consumed. That's why,
 * on startup, we check if we own the "gas" item, and, if so,
 * we have to apply its effects to our world and consume it. This
 * is also very important!
 *
 * @author Bruno Oliveira (Google)
 */
public class ShopActivity extends Activity {
    // Debug tag, for logging
    static final String TAG = "CoinShop";

    static final String SKU_PACK0 = "coins_pack0";
    static final String SKU_PACK1 = "coins_pack1";
    static final String SKU_PACK2 = "coins_pack2";
    static final String SKU_PACK3 = "coins_pack3";
    static final String SKU_PACK4 = "coins_pack4";

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    private Integer mCoins;
    private TextView currentCoins;
    
    // The helper object
    IabHelper mHelper;
    
	private Intent dataServiceIntent;
	private ServiceConnection dataServiceConnection;
	private XMPPDataServiceAdapter dataServiceAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.coin_shop);
        currentCoins = (TextView) findViewById(R.id.current_coins);
        setWaitScreen(true);
        registerDataService();
        
        /* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
         * (that you got from the Google Play developer console). This is not your
         * developer public key, it's the *app-specific* public key.
         *
         * Instead of just storing the entire literal string here embedded in the
         * program,  construct the key at runtime from pieces or
         * use bit manipulation (for example, XOR with some other string) to hide
         * the actual key.  The key itself is not secret information, but we don't
         * want to make it easy for an attacker to replace the public key with one
         * of their own and then fake messages from the server.
         */
        String base64EncodedPublicKey = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwDtmJzf4knEWv6TgFB+K83hMlyqXxCO/akd8XFKBx4Zuz7ZvMAhIREgMVPww2S/sMjjSL1ltXayJQ3Eo7U5FaQpl2BtoQvVFUpdETze9Y0ue0dteS+gQXsszLCn12XXn8S+PKA+TgtUfFj+g4jjGp4fIJJppBBGf0WCRazt7qTcC52kp0HidQRr8vKAFuTB37nEgkG73JjEsBgZWYX+2X2w02qiWW/MYpbCL8qbZ98CAwEAAQ==";

        // Create the helper, passing it our context and the public key to verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("مشکل در ارتباط با کافه بازار" + "\n" + result);
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                //Log.d(TAG, "Setup successful. Querying inventory.");
                //mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("مشکل در گرفتن اطلاعات حساب" + "\n" + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            Purchase gasPurchase = inventory.getPurchase(SKU_PACK0);
            if (gasPurchase != null && verifyDeveloperPayload(gasPurchase)) {
                Log.d(TAG, "We have gas. Consuming it.");
                //mHelper.consumeAsync(inventory.getPurchase(SKU_PACK0), mConsumeFinishedListener);
                return;
            }
            setWaitScreen(false);
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    public void onClickPack1(View arg0) {
    	buy(SKU_PACK1);
    }
    public void onClickPack2(View arg0) {
    	buy(SKU_PACK2);
    }
    public void onClickPack3(View arg0) {
    	buy(SKU_PACK3);
    }
    public void onClickPack4(View arg0) {
    	buy(SKU_PACK4);
    }
    
    public void buy(String sku){
        // launch the gas purchase UI flow.
        // We will be notified of completion via mPurchaseFinishedListener
        setWaitScreen(true);
        Log.d(TAG, "Launching purchase flow for gas.");

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";

        mHelper.launchPurchaseFlow(this, sku, RC_REQUEST,
                mPurchaseFinishedListener, payload);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("خرید انجام نشد" + "\n" + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");
            Log.d(TAG, "Purchase is gas. Starting gas consumption.");
            mHelper.consumeAsync(purchase, mConsumeFinishedListener);
        }
    };

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "Consumption successful. Provisioning.");
                
                int extraCoins = 0;
                if(purchase.getSku().equals(SKU_PACK0))
                	extraCoins += 100;
                else if(purchase.getSku().equals(SKU_PACK1))
                	extraCoins += 500;
                else if(purchase.getSku().equals(SKU_PACK2))
                	extraCoins += 1000;
                else if(purchase.getSku().equals(SKU_PACK3))
                	extraCoins += 2000;
                else if(purchase.getSku().equals(SKU_PACK4))
                	extraCoins += 9000;
                
                mCoins += extraCoins;
                saveCoins(mCoins);
                mCoins = loadCoins();
                currentCoins.setText(getString(R.string.current_amount_coins, mCoins));
                successDialog(extraCoins);
            }
            else {
                complain("مشکل در اضافه کردن سکه به حساب" + "\n" + result);
            }
            setWaitScreen(false);
            Log.d(TAG, "End consumption flow.");
        }
    };

    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    protected void successDialog(int extraCoins) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(R.drawable.ic_coins);
		dialog.setTitle(R.string.successful_payment);
		dialog.setMessage(getString(R.string.add_coins_success, extraCoins));
		dialog.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    	dialog.cancel();
                    }
                });
		dialog.show();
	}

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        alert("خطا: " + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    void saveCoins(Integer coins) {
    	dataServiceAdapter.saveGameData(PRIVATE_DATA.COINS, coins.toString());
    }

    Integer loadCoins() {
		String strCoins = dataServiceAdapter.loadGameData(PRIVATE_DATA.COINS);
		return Integer.parseInt(strCoins);
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	bindService(dataServiceIntent, dataServiceConnection, BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	unbindService(dataServiceConnection);
    }
    
	private void registerDataService(){
		Log.i(TAG, "called startGameService()");
		dataServiceIntent = new Intent(this, XMPPService.class);
		dataServiceIntent.setAction("ir.blackgrape.bereshtook.XMPPSERVICE2");
		dataServiceIntent.putExtra("isGameService", true);
		
		dataServiceConnection = new ServiceConnection() {
			
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				dataServiceAdapter = new XMPPDataServiceAdapter(
						IXMPPDataService.Stub.asInterface(service));
				mCoins = loadCoins();
				currentCoins.setText(getString(R.string.current_amount_coins, mCoins));
				setWaitScreen(false);
			}
			
			@Override
			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "Game service called onServiceDisconnected()");
			}
		};
	}
}
