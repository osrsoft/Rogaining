package com.osrsoft.rogaining;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class KpView extends ActionBarActivity  {

    private int sumKp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kp);

        SharedPreferences sp = getSharedPreferences("rogaining", Context.MODE_PRIVATE);
        String filename = sp.getString("filename", "noname");

        int pos = 0;
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        try {
            // открываем поток для чтения
            BufferedReader br = new BufferedReader(new InputStreamReader(openFileInput(filename)));
            // читаем содержимое
            sumKp = 0;
            while (true) {
                SecretFile sf = new SecretFile();
                String str = br.readLine();
                String str2 = br.readLine();
                if (str == null) break;
                str = sf.encode(str, filename);
                str2 = sf.encode(str2, filename);

                // Выделяем вес КП
                String s[] = str.split("№");
                String vesKp = "";
                if (s.length > 1) {
                    vesKp = s[1].substring(0, 1);
                    sumKp += Integer.valueOf(vesKp);
                }

                HashMap<String, String> map = new HashMap<String, String>();
                map.put("qr-code", str);
                map.put("time", str2);
                list.add(map);
                pos++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ListView lv = (ListView) findViewById(R.id.listView);
        // создаем адаптер
        SimpleAdapter adapter = new SimpleAdapter(this, list, android.R.layout.simple_list_item_2,
                new String[] {"qr-code", "time"},
                new int[] {android.R.id.text1, android.R.id.text2});

        // Создаем футер
        View foot = getLayoutInflater().inflate(R.layout.button, null);

        lv.addFooterView(foot);
        lv.setAdapter(adapter);

        TextView tvItog = (TextView)findViewById(R.id.tvItog);
        tvItog.setText(getString(R.string.itog) + String.valueOf(sumKp));

        lv.smoothScrollToPosition(pos);
    }


    public void onClick(View view) {
        finish();
    }
}
