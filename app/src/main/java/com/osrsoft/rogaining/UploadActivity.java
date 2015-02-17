package com.osrsoft.rogaining;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


public class UploadActivity extends ActionBarActivity implements View.OnClickListener {

    private EditText nameEdit;
    private EditText comandEdit;

    private String filename;
    private String outputfile;
    private String url;
    private String user;
    private String pass;

    private boolean successUpload;
    private boolean saveName;
    private boolean onUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        nameEdit = (EditText) findViewById(R.id.nameEdit);
        comandEdit = (EditText) findViewById(R.id.comandEdit);
        Button uploadButton = (Button) findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(this);

        SharedPreferences sp = getSharedPreferences("rogaining", Context.MODE_PRIVATE);
        nameEdit.setText(sp.getString("fio", ""));
        comandEdit.setText(sp.getString("comand", ""));
        if (nameEdit.getText().toString().equals("")) {
            saveName = true;
        } else {
            saveName = false;
        }

    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.upload_button){

            if (nameEdit.getText().toString().equals("") || comandEdit.getText().toString().equals("")) {
                showEmptyNameDialog();
                return;
            }

            onUpload = true; // Флаг "Идет выгрузка". Блокирует кнопку Back

            SharedPreferences sp = getSharedPreferences("rogaining", Context.MODE_PRIVATE);
            filename = sp.getString("filename", "");
            url = sp.getString("upload_url", "");
            user = sp.getString("upload_user", "");
            pass = sp.getString("upload_pass", "");

            try {
                if (saveName) {
                    // Дописываем название команды и ФИО в файл
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(openFileOutput(filename, MODE_APPEND)));
                    SecretFile sf = new SecretFile();
                    bw.write(sf.encode(nameEdit.getText().toString(), filename));
                    bw.newLine();
                    bw.write(sf.encode(comandEdit.getText().toString(), filename));
                    bw.newLine();
                    bw.flush();
                    bw.close();

                    // Сохраняем в preferences
                    SharedPreferences.Editor e = sp.edit();
                    e.putString("fio",nameEdit.getText().toString());
                    e.putString("comand",comandEdit.getText().toString());
                    e.commit();
                }
                // Расшифровка файла
                outputfile = filename + ".csv";
                try {
                    // открываем поток для чтения
                    BufferedReader br = new BufferedReader(new InputStreamReader(openFileInput(filename)));
                    // Очистка файла перед записью
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(openFileOutput(outputfile, MODE_PRIVATE)));
                    bw.close();
                    // отрываем поток для записи
                    bw = new BufferedWriter(new OutputStreamWriter(openFileOutput(outputfile, MODE_APPEND), "cp1251"));
                    String str;
                    String str2;
                    String str3;
                    while (true) {
                        SecretFile sf = new SecretFile();
                        str = br.readLine();
                        str2 = br.readLine();
                        if (str == null) break;
                        str = sf.encode(str, filename);
                        str2 = sf.encode(str2, filename);
                        str3 = str2 + ";" + str;
                        bw.write(str3);
                        bw.newLine();
                    }
                    bw.flush();
                    bw.close();
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Выгрузка файла на сервер
                FtpTask ftp = new FtpTask();
                ftp.execute();


            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    public void onBackPressed() {
        if (!onUpload)  super.onBackPressed();
    }

    class FtpTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                FileInputStream fs = openFileInput(outputfile);
                FTPClient mFTP = new FTPClient();
                mFTP.setConnectTimeout(2000);
                mFTP.connect(url);
                mFTP.login(user, pass);
                mFTP.enterLocalPassiveMode();
                mFTP.setFileType(FTP.ASCII_FILE_TYPE);
                mFTP.storeFile(outputfile, fs);
                mFTP.logout();
                mFTP.disconnect();
                fs.close();
                successUpload = true;
            } catch (IOException e) {
                successUpload = false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            showUploadDialog(successUpload);
            onUpload = false;
        }

        private AlertDialog showUploadDialog(boolean success) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(UploadActivity.this);
            if (success) {
                dialog.setTitle(getString(R.string.success_upload_title));
                dialog.setMessage(getString(R.string.success_upload_message));
            } else {
                dialog.setTitle(getString(R.string.error_upload_title));
                dialog.setMessage(getString(R.string.error_upload_message));
            }
            dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            return dialog.show();
        }

    }

    private AlertDialog showEmptyNameDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(UploadActivity.this);
        dialog.setTitle(getString(R.string.empty_name_title));
        dialog.setMessage(getString(R.string.empty_name_message));
        dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return dialog.show();
    }

}
