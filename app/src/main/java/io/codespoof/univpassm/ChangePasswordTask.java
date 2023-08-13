package io.codespoof.univpassm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class ChangePasswordTask extends AsyncTask<Void, Void, Boolean> {

    private final String username;
    private final String oldPassword;
    private final String newPassword;
    private final String host;
    private String message;
    @SuppressLint("StaticFieldLeak")
    private Context context;
    private final DBManager dbManager;
    private final SimpleCursorAdapter adapter;
    @SuppressLint("StaticFieldLeak")
    private final EditText editPassword;
    private final Pattern cookiePattern = Pattern.compile("(?:^|; )UMCSessionId=([^;]+)");
    private long id = -1;

    public ChangePasswordTask(String username, String oldPassword, String newPassword, String host, Context context, DBManager dbManager, SimpleCursorAdapter adapter, EditText editPassword) {
        this.username = username;
        this.newPassword = newPassword;
        this.oldPassword = oldPassword;
        this.host = host;
        this.context = context;
        this.dbManager = dbManager;
        this.adapter = adapter;
        this.editPassword = editPassword;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        id = dbManager.insert(newPassword);
        if (id < 0) this.cancel(true);
        Log.i("bg", "Old: " + oldPassword);
        Log.i("bg", "New: " + newPassword);
    }

    private String createConnection(String path, String body, String cookies) throws IOException, JSONException {
        URL endpoint;
        try {
            endpoint = new URL("https://" + host + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        HttpsURLConnection myConnection = (HttpsURLConnection) endpoint.openConnection();
        myConnection.setRequestMethod("POST");
        myConnection.setRequestProperty("Content-Type", "application/json");
        if (cookies != null) {
            myConnection.setRequestProperty("Cookie", cookies);
            final Matcher matcher = cookiePattern.matcher(cookies);
            if (matcher.find()) myConnection.setRequestProperty("X-Xsrf-Protection", matcher.group(1));
        }
        myConnection.setDoOutput(true);
        myConnection.getOutputStream().write(body.getBytes());
        if (myConnection.getResponseCode() != 200) {
            return null;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(myConnection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
        }
        System.out.println(response);
        String responseText = response.toString();
        JSONObject json = new JSONObject(responseText);
        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String key = it.next();
            switch (key) {
                case "status":
                    if (json.getInt(key) != 200) return null;
                    break;
                case "message":
                    message = json.getString(key);
                    break;
            }
        }
        StringBuilder ret = new StringBuilder();
        if (myConnection.getHeaderFields().containsKey("Set-Cookie")) {
            for (String s : Objects.requireNonNull(myConnection.getHeaderFields().get("Set-Cookie"))) {
                ret.append(ret.length() > 0 ? "; " : "").append(s.split(";")[0]);
            }
        } else {
            ret.append("UIC=UIC");
        }
        myConnection.disconnect();
        return ret.toString();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            String result = createConnection("/univention/auth", "{\"options\":{\"username\":\"" + username + "\",\"password\":\"" + oldPassword + "\"}}", null);
            if (result != null) {
                return createConnection("/univention/set", "{\"options\":{\"password\":{\"username\":\"" + username + "\",\"password\":\"" + oldPassword + "\",\"new_password\":\"" + newPassword + "\"}}}", result) != null;
            } else return false;
        } catch (IOException | JSONException e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if (!success) {
            Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show();
            dbManager.delete(id);
            return;
        }
        if (message != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        context = null;
        adapter.changeCursor(dbManager.fetch());
        adapter.notifyDataSetChanged();
        editPassword.setText(newPassword);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        dbManager.delete(id);
        Toast.makeText(context, R.string.db_error, Toast.LENGTH_SHORT).show();
    }
}
