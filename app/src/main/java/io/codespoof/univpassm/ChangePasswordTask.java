package io.codespoof.univpassm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;

public class ChangePasswordTask extends PasswordTask {

    private final String username;
    private final String oldPassword;
    private final String newPassword;
    @SuppressLint("StaticFieldLeak")
    private Context context;
    private final DBManager dbManager;
    private final SimpleCursorAdapter adapter;
    @SuppressLint("StaticFieldLeak")
    private final EditText editPassword;
    @SuppressLint("StaticFieldLeak")
    private final Button repairButton;
    @SuppressLint("StaticFieldLeak")
    private final Button resetButton;
    private long id = -1;

    public ChangePasswordTask(String username, String oldPassword, String newPassword, String host, Context context, DBManager dbManager, SimpleCursorAdapter adapter, EditText editPassword, Button repairButton, Button resetButton) {
        this.username = username;
        this.newPassword = newPassword;
        this.oldPassword = oldPassword;
        this.host = host;
        this.context = context;
        this.dbManager = dbManager;
        this.adapter = adapter;
        this.editPassword = editPassword;
        this.repairButton = repairButton;
        this.resetButton = resetButton;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        doWithButtons(false);
        id = dbManager.insert(newPassword);
        if (id < 0) this.cancel(true);
        Log.i("bg", "Old: " + oldPassword);
        Log.i("bg", "New: " + newPassword);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            String result = doAuth(username, oldPassword);
            if (result != null) {
                return doChange(oldPassword, newPassword, result);
            } else return false;
        } catch (IOException | JSONException e) {
            return false;
        }
    }

    private void doWithButtons(boolean enabled) {
        repairButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        doWithButtons(true);
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
        doWithButtons(true);
        dbManager.delete(id);
        Toast.makeText(context, R.string.db_error, Toast.LENGTH_SHORT).show();
    }
}
