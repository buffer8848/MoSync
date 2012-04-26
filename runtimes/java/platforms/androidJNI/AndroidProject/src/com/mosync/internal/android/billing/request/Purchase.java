/* Copyright (C) 2012 MoSync AB

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License,
version 2, as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
MA 02110-1301, USA.
*/

package com.mosync.internal.android.billing.request;


import java.lang.reflect.Method;

import com.android.vending.billing.IMarketBillingService;
import com.mosync.internal.android.billing.BillingService;
import com.mosync.internal.android.billing.Consts;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import static com.mosync.internal.generated.MAAPI_consts.MA_PURCHASE_RES_OK;

/**
 * Wrapper class for sending purchase requests to the Google Play server.
 * You send this request when a user indicates that he or she wants to purchase
 * an item in your application. Google Play then handles the financial transaction
 * by displaying the checkout user interface.
 * @author emma
 */
public class Purchase extends BaseRequest
{
    public Purchase(IMarketBillingService service, String productID, Activity appActivity)
    {
        // This object is never created as a side effect of starting this
        // service so we pass -1 as the startId to indicate that we should
        // not stop this service after executing this request.
        super(-1, service);

        mProductID = productID;
        mActivity = appActivity;

        // Check compatibility of startIntentSender method, available only from Android 2.0.
        try {
            mStartIntentSender = mActivity.getClass().getMethod("startIntentSender",
                    START_INTENT_SENDER_SIG);
        } catch (SecurityException e) {
            mStartIntentSender = null;
        } catch (NoSuchMethodException e) {
            mStartIntentSender = null;
        }
    }

	@Override
	protected void run() throws RemoteException
	{
		// ToDo check if we need responseCode
		Bundle request = BillingService
				.createRequestBundle(Consts.METHOD_REQUEST_PURCHASE);
		request.putString(Consts.BILLING_REQUEST_ITEM_ID, mProductID);
		Bundle response = mService.sendBillingRequest(request);

		PendingIntent pendingIntent = response.getParcelable(Consts.BILLING_RESPONSE_PURCHASE_INTENT);
		if (pendingIntent == null)
		{
			Log.e("@@MoSync", "maPurchaseRequest Error  etc...............");
			//return Consts.BILLING_RESPONSE_INVALID_REQUEST_ID;
			mRequestId = Consts.BILLING_RESPONSE_INVALID_REQUEST_ID;
			return;
		}

		mRequestId = response.getLong(
				Consts.BILLING_RESPONSE_REQUEST_ID,
				Consts.BILLING_RESPONSE_INVALID_REQUEST_ID);

		// Get response code.

		// Launch the buy page activity.
		Intent intent = new Intent();
		launchCheckoutUI(pendingIntent, intent);
	}

	/**
	 * Start the buy page activity.
	 * On Android 1.6, you must use the pending intent to launch the checkout UI in
	 * its own separate task instead of your application's activity stack.
	 * On Android 2.0 and higher, you can use the pending intent to launch the
	 * checkout UI on your application's activity stack.
	 *
	 * @param pendingIntent
	 * @param intent
	 */
	private void launchCheckoutUI(PendingIntent pendingIntent, Intent intent)
	{
        if (mStartIntentSender != null)
        {
            // On Android 2.0 and beyond.
            try {
                // This implements the method call:
                // mActivity.startIntentSender(pendingIntent.getIntentSender(),
                //     intent, 0, 0, 0);
                mStartIntentSenderArgs[0] = pendingIntent.getIntentSender();
                mStartIntentSenderArgs[1] = intent;
                mStartIntentSenderArgs[2] = Integer.valueOf(0);
                mStartIntentSenderArgs[3] = Integer.valueOf(0);
                mStartIntentSenderArgs[4] = Integer.valueOf(0);
                mStartIntentSender.invoke( mActivity, mStartIntentSenderArgs);
            } catch (Exception e) {
                Log.e("@@MoSync", "error starting activity", e);
            }
        }
        else
        {
            // On Android version 1.6.
            try {
                pendingIntent.send(mActivity, 0 /* code */, intent);
            } catch (CanceledException e) {
                Log.e("@@MoSync", "error starting activity", e);
            }
        }
	}
	/************************ Class members ************************/
    private String mProductID = null;

    private Activity mActivity;

    private Method mStartIntentSender;
    private Object[] mStartIntentSenderArgs = new Object[5];
    private static final Class[] START_INTENT_SENDER_SIG = new Class[] {
        IntentSender.class, Intent.class, int.class, int.class, int.class
    };
}
