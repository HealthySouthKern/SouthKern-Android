package com.eddierangel.southkern.android.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.eddierangel.southkern.android.R;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/* This class allows this android application to authenticate with the google calendar API using oauth2 authentication flow.
 * Once an admin user intends to manage calendar events this asynchronous task executes and allows the user to provide explicit
 * permission to access their calendar.
 *
 * You can use this class by creating a new instance and calling its execute method e.g.
 *
 * CalendarAuthorization calAuth = new CalendarAuthorization();
 * calAuth.execute(params); --(See params definition below)
 *
 * You can then get the result of the operation by:
 *
 * Credentials credentials = calAuth.get();
 *
 * you can then build the calendar service using these credentials.
 *
 * @param Object params - takes an object that contains an http network transport object (key = "transport")
 *                        and a context (key = "context").
 * */

public class CalendarAuthorization extends AsyncTask<Object, Void, Credential> {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "com/eddierangel/southkern/android/utils/tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    @Override
    protected Credential doInBackground(Object... params) {
        // do work here
        try {
            return getCredentials(params);
        } catch (IOException e) {
            Log.e("Credentials IO exception", "" + e);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(Credential result) {
        super.onPostExecute(result);
    }

    /**
     * Creates an authorized Credential object.
     * @param params contains the context and http transport we will use to build oauth credentials.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(Object[] params) throws IOException {
        // Pull params from param object.
        HashMap mapParams = (HashMap) params[0];
        final Context mContext = (Context) mapParams.get("context");
        NetHttpTransport HTTP_TRANSPORT = (NetHttpTransport) mapParams.get("transport");
        Log.i("params", HTTP_TRANSPORT + " : " + mContext);

        // Load client secrets.
        InputStream in = mContext.getResources().openRawResource(R.raw.credentials);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        File tokensDir = new File(mContext.getCacheDir(), TOKENS_DIRECTORY_PATH);

        if (!tokensDir.exists()) {
            tokensDir.mkdir();
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        AuthorizationCodeInstalledApp authCodeApp = new AuthorizationCodeInstalledApp(flow, receiver) {
            protected void onAuthorization(AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
                String url = (authorizationUrl.build());
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                mContext.startActivity(browserIntent);
            }
        };

        return authCodeApp.authorize("user");
    }
}
