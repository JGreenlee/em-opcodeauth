package edu.berkeley.eecs.emission.cordova.jwtauth;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;

import edu.berkeley.eecs.emission.cordova.connectionsettings.ConnectionSettings;
import edu.berkeley.eecs.emission.cordova.unifiedlogger.Log;
import edu.berkeley.eecs.emission.cordova.usercache.UserCacheFactory;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import org.apache.cordova.CordovaPlugin;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by shankari on 8/21/17.
 *
 * Implementation of the prompted auth code to allow developers to login with multiple user IDs
 * for testing + to provide another exemplar of logging in properly :)
 */

class PromptedAuth implements AuthTokenCreator {
    private CordovaPlugin mPlugin;
    private AuthPendingResult mAuthPending;
    private Context mCtxt;

    private static final String TAG = "PromptedAuth";
    private static final String METHOD_PARAM_KEY = "method";
    private static final String TOKEN_PARAM_KEY = "token";
    private static final String EXPECTED_HOST = "auth";
    private static final String EXPECTED_METHOD = "prompted-auth";

    // This has to be a class instance instead of a singleton like in
    // iOS because we are not supposed to store contexts in static variables
    // singleton pattern has static GoogleAccountManagerAuth -> mCtxt
    PromptedAuth(Context ctxt) {
        mCtxt = ctxt;
    }

    @Override
    public AuthPendingResult uiSignIn(CordovaPlugin plugin) {
        this.mAuthPending = new AuthPendingResult();
        this.mPlugin = plugin;

        final String devJSScript = "window.cordova.plugins.BEMJWTAuth.launchPromptedAuth('"+getPrompt()+"')";
        Log.d(mCtxt, TAG, "About to execute script: "+devJSScript);
        final CordovaPlugin finalPlugin = plugin;
        plugin.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finalPlugin.webView.loadUrl("javascript:"+devJSScript);
            }
        });
        return mAuthPending;
    }

    @Override
    public AuthPendingResult getUserEmail() {
        return readStoredUserAuthEntry(mCtxt);
    }

    @Override
    public AuthPendingResult getServerToken() {
        // For the prompted-auth case, the token is the user email
        return readStoredUserAuthEntry(mCtxt);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(mCtxt, TAG, "onActivityResult(" + requestCode + "," + resultCode);
        Log.i(mCtxt, TAG, "onActivityResult unused in `prompted-auth, ignoring call...");
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i(mCtxt, TAG, "received intent with url "+intent.getDataString());
        Uri launchUrl = intent.getData();
        if (launchUrl == null) {
            Log.i(mCtxt, TAG, "url = null, not a callback, early return...");
            return;
        }
        if (EXPECTED_HOST.equals(launchUrl.getHost())) {
            String method = launchUrl.getQueryParameter(METHOD_PARAM_KEY);
            if(method != null && EXPECTED_METHOD.equals(method)) {
                String token = launchUrl.getQueryParameter(TOKEN_PARAM_KEY);
                if (token != null) {
                    try {
                        writeStoredUserAuthEntry(mCtxt, token);
                    AuthResult authResult = new AuthResult(
                            new Status(CommonStatusCodes.SUCCESS),
                            token,
                            token);
                    mAuthPending.setResult(authResult);
                    } catch (JSONException e) {
                        AuthResult authResult = new AuthResult(
                                new Status(CommonStatusCodes.ERROR),
                                e.getLocalizedMessage(),
                                e.getLocalizedMessage());
                        mAuthPending.setResult(authResult);
                    }
                } else {
                    Log.i(mCtxt, TAG, "Received uri with query params = "+launchUrl.getQuery()
                            +" key "+TOKEN_PARAM_KEY+" missing, ignoring");
                }
            } else {
                Log.i(mCtxt, TAG, "Received uri with query params = "+launchUrl.getQuery()
                        +" key "+METHOD_PARAM_KEY+" missing or incorrect, ignoring");
            }
        } else {
            // TODO: Should I return the callback with an error? It is possible that there are multiple URLs being handled,
            // in which case we should not return prematurely, but wait for _our_ URL to complete. But if we don't look
            // for it, we may be stuck overever.
            Log.i(mCtxt, TAG, "Received uri with feature = "+launchUrl.getHost()
                    +" expected "+EXPECTED_HOST+" ignoring ");
        }
    }

    private AuthPendingResult readStoredUserAuthEntry(Context ctxt) {
        AuthPendingResult authPending = new AuthPendingResult();
        AuthResult result = null;
        try {
            String token = null;
            JSONObject dbStorageObject = UserCacheFactory.getUserCache(ctxt).getLocalStorage(EXPECTED_METHOD, false);
            if (dbStorageObject == null) {
                Log.i(ctxt, TAG, "Auth not found in local storage, copying from user profile");
                String profileToken = UserProfile.getInstance(ctxt).getUserEmail();
                Log.i(ctxt, TAG, "Profile token = " + profileToken);
                dbStorageObject = new JSONObject();
                dbStorageObject.put(TOKEN_PARAM_KEY, profileToken);
                UserCacheFactory.getUserCache(ctxt).putLocalStorage(EXPECTED_METHOD, dbStorageObject);
                token = profileToken;
            } else {
                token = dbStorageObject.getString(TOKEN_PARAM_KEY);
                Log.i(ctxt, TAG,"Auth found in local storage, now it should be stable");
            }
            result = new AuthResult(
                new Status(CommonStatusCodes.SUCCESS),
                    token, token);
        } catch (JSONException e) {
            result = new AuthResult(
                    new Status(CommonStatusCodes.ERROR),
                    e.getLocalizedMessage(), e.getLocalizedMessage());
        }
        authPending.setResult(result);
        return authPending;
    }

    protected void writeStoredUserAuthEntry(Context ctxt, String token) throws JSONException {
        JSONObject dbStorageObject = new JSONObject();
        dbStorageObject.put(TOKEN_PARAM_KEY, token);
        UserCacheFactory.getUserCache(ctxt).putLocalStorage(EXPECTED_METHOD, dbStorageObject);
    }

    @NonNull
    private String getPrompt() {
        String configPrompt = ConnectionSettings.getAuthValue(mCtxt, "prompt");
        if (configPrompt == null) {
            // return prompted-auth prompt by default to continue supporting config-less
            // development
            configPrompt = "Dummy dev mode: Enter email";
        }
        return configPrompt;
    }
}
