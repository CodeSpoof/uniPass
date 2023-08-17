package io.codespoof.univpassm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private DBManager dbManager;

    final String[] from = new String[] {DatabaseHelper.DATETIME, DatabaseHelper.CONTENT};

    final int[] to = new int[] {R.id.datetime, R.id.content};

    PasswordGenerator generator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbManager = new DBManager(this);
        dbManager.open();
        Cursor cursor = dbManager.fetch();
        if(cursor.getCount() > 0) {
            ((EditText) findViewById(R.id.editPassword)).setText(cursor.getString(2));
            cursor.moveToFirst();
        }

        ListView listView = (ListView) findViewById(R.id.listHistory);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.history, cursor, from, to, 0);
        adapter.notifyDataSetChanged();

        listView.setAdapter(adapter);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        generator = new PasswordGenerator(preferences.getBoolean("pref_url_compat", false), Integer.parseInt(preferences.getString("pref_level", "10")));

        EditText editPassword = findViewById(R.id.editPassword);
        Button resetButton = findViewById(R.id.buttonReset);
        Button repairButton = findViewById(R.id.buttonRepair);
        Button settingsButton = findViewById(R.id.buttonSettings);
        resetButton.setOnClickListener(v -> {
            Cursor c = dbManager.fetch();
            c.moveToFirst();
            if (c.getCount() > 0) {
                String oldPass = c.getString(2);
                String newPass = generator.generatePassword();
                ChangePasswordTask task = new ChangePasswordTask(preferences.getString("pref_username", ""), oldPass, newPass, preferences.getString("pref_server_address", ""), getApplicationContext(), dbManager, adapter, editPassword, repairButton, resetButton);
                task.execute();
            } else {
                Toast.makeText(getApplicationContext(), R.string.history_empty, Toast.LENGTH_LONG).show();
            }
            c.close();
        });
        repairButton.setOnClickListener(v -> {
            if (editPassword.isEnabled()) {
                repairButton.setText(R.string.repair);
                resetButton.setEnabled(true);
                dbManager.insert(String.valueOf(editPassword.getText()));
                adapter.changeCursor(dbManager.fetch());
                adapter.notifyDataSetChanged();
                editPassword.setEnabled(false);
                editPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                repairButton.setText(R.string.repair_ok);
                resetButton.setEnabled(false);
                editPassword.setEnabled(true);
                editPassword.setText("");
                editPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        });
        settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        if (preferences.getString("pref_username", "").equals(""))
            startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}