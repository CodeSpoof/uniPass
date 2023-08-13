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
        StringBuilder ret = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyz";
        String signs = ".,-_:#*+=!";
        String numbers = "0123456789";
        SecureRandom secureRandomGenerator;
        try {
            secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < 8; i++) {
            ret.append(chars.charAt(secureRandomGenerator.nextInt(chars.length())));
        }
        int cap = secureRandomGenerator.nextInt(255)+1;
        int upper = 0;
        for (int i = 0; i < 8; i++) {
            if (((cap >> i) & 1) > 0) {
                ret.replace(i, i+1, (ret.substring(i,i+1)).toUpperCase());
                upper++;
            }
        }
        int done = 0;
        while (done < 1) {
            for (int i = 0; i < 8; i++) {
                if (ret.substring(i, i + 1).toLowerCase().equals(ret.substring(i, i + 1)) ^ upper > 4) {
                    if (secureRandomGenerator.nextBoolean()) continue;
                    done++;
                    int ran = secureRandomGenerator.nextInt(signs.length());
                    ret.replace(i, i+1, signs.substring(ran, ran+1));
                    if (upper > 4) {
                        upper--;
                    } else {
                        upper++;
                    }
                    break;
                }
            }
        }
        done = 0;
        while (done < 2) {
            for (int i = 0; i < 8; i++) {
                if ((ret.substring(i, i + 1).toLowerCase().equals(ret.substring(i, i + 1)) ^ upper > 4) && chars.contains(ret.substring(i, i + 1).toLowerCase())) {
                    if (secureRandomGenerator.nextBoolean()) continue;
                    done++;
                    int ran = secureRandomGenerator.nextInt(numbers.length());
                    ret.replace(i, i+1, numbers.substring(ran, ran+1));
                    if (upper > 4) {
                        upper--;
                    } else {
                        upper++;
                    }
                    break;
                }
            }
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
        resetButton.setOnClickListener(v -> {
            Cursor c = dbManager.fetch();
            c.moveToFirst();
            if (c.getCount() > 0) {
                String oldPass = c.getString(2);
                String newPass = generatePassword();
                ChangePasswordTask task = new ChangePasswordTask("chrsal2", oldPass, newPass, "login.schulen-wetteraukreis.de", getApplicationContext(), dbManager, adapter, editPassword);
                task.execute();
            } else {
                Toast.makeText(getApplicationContext(), R.string.history_empty, Toast.LENGTH_LONG).show();
            }
            c.close();
        });
        Button repairButton = findViewById(R.id.buttonRepair);
        repairButton.setOnClickListener(v -> {
            if (editPassword.isEnabled()) {
                dbManager.insert(String.valueOf(editPassword.getText()));
                adapter.changeCursor(dbManager.fetch());
                adapter.notifyDataSetChanged();
                editPassword.setEnabled(false);
                editPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                editPassword.setEnabled(true);
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