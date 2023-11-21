package io.codespoof.univpassm;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public abstract class PasswordTask extends AsyncTask<Void, Void, Boolean> {
    protected String message;
    protected String host;
    private final Pattern cookiePattern = Pattern.compile("(?:^|; )UMCSessionId=([^;]+)");

    protected String createConnection(String path, String body, String cookies) throws IOException, JSONException {
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
            if (matcher.find())
                myConnection.setRequestProperty("X-Xsrf-Protection", matcher.group(1));
        }
        myConnection.setDoOutput(true);
        myConnection.getOutputStream().write(body.getBytes());
        switch (myConnection.getResponseCode()) {
            case 200:
                break;
            case 301 | 302 | 303 | 307 | 308:
                return createConnection(myConnection.getHeaderField("Location"), body, cookies);
            default:
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

    protected String doAuth(String username, String password) throws JSONException, IOException {
        JSONObject json = new JSONObject("{\"options\":{\"username\":\"\",\"password\":\"\"}}");
        json.getJSONObject("options").put("username", username);
        json.getJSONObject("options").put("password", password);
        return createConnection("/univention/auth", json.toString(), null);
    }

    protected boolean doChange(String oldPassword, String newPassword, String cookies) throws JSONException, IOException {
        JSONObject json = new JSONObject("{\"options\":{\"password\":{\"password\":\"\",\"new_password\":\"\"}}}");
        json.getJSONObject("options").getJSONObject("password").put("password", oldPassword);
        json.getJSONObject("options").getJSONObject("password").put("new_password", newPassword);
        return createConnection("/univention/set/password", json.toString(), cookies) != null;
    }
}
