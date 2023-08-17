package io.codespoof.univpassm;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class MainActivity extends AppCompatActivity {

    private DBManager dbManager;

    final String[] from = new String[] {DatabaseHelper.DATETIME, DatabaseHelper.CONTENT};

    final int[] to = new int[] {R.id.datetime, R.id.content};

    private String generatePassword() {
        StringBuilder pool = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyz";
        String signs = ".,-_:#*+=!";
        String numbers = "0123456789";
        SecureRandom secureRandomGenerator;
        try {
            secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        int upper = 0;
        for (int i = 0; i < 5; i++) {
            String c = chars.substring(secureRandomGenerator.nextInt(chars.length())).substring(0, 1);
            if (upper < 4 && secureRandomGenerator.nextBoolean()) {
                c = c.toUpperCase();
                upper++;
            }
            pool.append(c);
        }
        pool.append(signs.charAt(secureRandomGenerator.nextInt(signs.length())));
        pool.append(numbers.charAt(secureRandomGenerator.nextInt(numbers.length())));
        pool.append(numbers.charAt(secureRandomGenerator.nextInt(numbers.length())));

        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int ind = secureRandomGenerator.nextInt(pool.length());
            ret.append(pool.charAt(ind));
            pool.deleteCharAt(ind);
        }
        return ret.toString();
    }

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

        EditText editPassword = findViewById(R.id.editPassword);
        Button resetButton = findViewById(R.id.buttonReset);
        Button repairButton = findViewById(R.id.buttonRepair);
        resetButton.setOnClickListener(v -> {
            Cursor c = dbManager.fetch();
            c.moveToFirst();
            if (c.getCount() > 0) {
                String oldPass = c.getString(2);
                String newPass = generatePassword();
                ChangePasswordTask task = new ChangePasswordTask("chrsal2", oldPass, newPass, "login.schulen-wetteraukreis.de", getApplicationContext(), dbManager, adapter, editPassword, repairButton, resetButton);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}