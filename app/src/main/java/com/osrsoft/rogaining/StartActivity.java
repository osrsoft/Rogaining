package com.osrsoft.rogaining;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class StartActivity extends ActionBarActivity implements View.OnClickListener {

    String category;
    String pol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Button uploadButton = (Button) findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(this);

        final String[] data = getResources().getStringArray(R.array.category);
        final String[] data2 = getResources().getStringArray(R.array.pol);
        // адаптер
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, R.layout.spinner_item, data2);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = (Spinner) findViewById(R.id.catSpinner);
        spinner.setAdapter(adapter);

        Spinner spinner2 = (Spinner) findViewById(R.id.polSpinner);
        spinner2.setAdapter(adapter2);
        // заголовок
        spinner.setPrompt(getString(R.string.category));
        spinner2.setPrompt(getString(R.string.pol));
        // выделяем элемент
        spinner.setSelection(2);
        spinner2.setSelection(0);
        // устанавливаем обработчик нажатия
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                category = data[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pol = data2[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.upload_button){
            EditText nameEdit = (EditText) findViewById(R.id.nameEdit);
            EditText comandEdit = (EditText) findViewById(R.id.comandEdit);
            EditText comandNumEdit = (EditText) findViewById(R.id.comandNumEdit);
            EditText timeEdit = (EditText) findViewById(R.id.timeEdit);

            // Проверяем судейское время
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", new Locale("en"));
            long dTime = 0;
            long refMils = 0;
            long currentMils = 0;
            try
            {
                Date d = dateFormat.parse(timeEdit.getText().toString());
                refMils=d.getTime();
                Calendar cal = Calendar.getInstance();
                cal.set(1970, Calendar.JANUARY, 1);
                currentMils=cal.getTimeInMillis();
                dTime=(currentMils-refMils)/1000; // дельта времени в секундах
                if (dTime > 60) {
                    // Ошибка. Слишком большая дельта времени
                    showIncorrectTimeDialog();
                    return;
                }

            } catch (ParseException ex)
            {
                // Ошибка. Неверный формат времени
                showIncorrectTimeDialog();
                return;
            }

            // Сохраняем в preferences
            SharedPreferences sp = getSharedPreferences("rogaining", Context.MODE_PRIVATE);
            SharedPreferences.Editor e = sp.edit();
            e.putBoolean("upload_enabled", false);
            e.putString("category", category);
            e.putString("pol", pol);
            e.putString("fio", nameEdit.getText().toString());
            e.putString("comand", comandEdit.getText().toString());
            e.putString("comand_num", comandNumEdit.getText().toString());
            e.putInt("dtime", (int)dTime);
            e.commit();

            // Закрываем активити
            finish();
        }
    }

    private AlertDialog showIncorrectTimeDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(StartActivity.this);
        dialog.setTitle(getString(R.string.error));
        dialog.setMessage(getString(R.string.incorrect_time_message));
        dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return dialog.show();
    }

}
