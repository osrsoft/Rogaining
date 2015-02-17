package com.osrsoft.rogaining;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private String filename;
    private String outputfile;
    private String url;
    private String user;
    private String pass;
    private String distance; // Идентификатор дистанции
    private String txtKp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button scanButton = (Button) findViewById(R.id.scan_button);
        Button kpButton = (Button) findViewById(R.id.kp_button);
        Button uploadButton = (Button) findViewById(R.id.upload_button);
        scanButton.setOnClickListener(this);
        kpButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);

        // Считываем номер версии программы из мета-данных
        try {
            PackageInfo manager=getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
            String ver = manager.versionName;
            getSupportActionBar().setTitle(getString(R.string.app_name)+" ver "+ver);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        SharedPreferences sp = getSharedPreferences("rogaining", Context.MODE_PRIVATE);
        if (sp.getBoolean("upload_enabled", false)) {
            uploadButton.setEnabled(true);
        }
        filename = sp.getString("filename", "");
        if (filename.equals("")) {  // Генерируем имя файла если его еще нет
            SecretFile sf = new SecretFile();
            filename = sf.init();
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("filename", filename);
            editor.commit();
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.scan_button){
            //scan
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.initiateScan();
        }
        if(view.getId()==R.id.kp_button){
            // список КП
            Intent intent = new Intent(this, KpView.class);
            startActivity(intent);
        }
        if(view.getId()==R.id.upload_button){
            // Выгрузка данных
            uploadData();
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Получаем данные со сканера
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
            try {
                // отрываем поток для записи
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(openFileOutput(filename, MODE_APPEND)));
                // пишем данные
                String s = scanningResult.getContents();
                s = s.replace("\r\n", ",");
                // выделяем название дистанции
                String s1[] = s.split(",");
                if (s1.length > 1) {
                    txtKp = s1[1];
                }
                if (txtKp.equals(getString(R.string.start))) {  // КП СТАРТ
                    showStartDialog();
                    distance = s1[0]; // Установили идентификатор дистанции

                    SharedPreferences sp = getSharedPreferences("rogaining", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("distance", distance);
                    editor.commit();

                } else {
                    String finish = getString(R.string.finish);
                    if (txtKp.equals(finish)) {   // КП ФИНИШ
                        Button upload_btn = (Button) findViewById(R.id.upload_button);
                        upload_btn.setEnabled(true);

                        if (s1.length < 5) { // Не правильно оформлено КП ФИНИШ
                            showIncorrectFinishDialog();
                            return;
                        }

                        SharedPreferences sp = getSharedPreferences("rogaining", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putBoolean("upload_enabled", true);
                        editor.putString("upload_url", s1[2]);
                        editor.putString("upload_user", s1[3]);
                        editor.putString("upload_pass", s1[4]);
                        editor.commit();
                    }
                    if (s1.length < 3) {
                        showIncorrectKpDialog();
                        return;
                    }
                    String strKp = txtKp + ", " + s1[2]; // Строка для занесения в таблицу взятых КП
                    SharedPreferences sp = getSharedPreferences("rogaining", Context.MODE_PRIVATE);
                    int dTime = sp.getInt("dtime", 0);
                    distance = sp.getString("distance", "");
                    if (notDouble(strKp)) {  // Информации о КП еще нет в файле
                        if (s1[0].equals(distance)) {
                            SecretFile sf = new SecretFile();
                            bw.write(sf.encode(strKp, filename));  // Информация с QR-кода
                            bw.newLine();
                            String cur = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(System.currentTimeMillis() - dTime);
                            bw.write(sf.encode(cur, filename));
                            bw.newLine();

                            Intent intent2 = new Intent(this, KpView.class);
                            startActivity(intent2);
                        } else {
                            showIncorrectDistanceDialog();
                        }
                    } else {
                        // Повторное снятие КП
                        showDoubleDialog(txtKp);
                    }
                    // закрываем поток
                    bw.flush();
                    bw.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private boolean notDouble(String s) { // Проверяет есть ли дубль строки s в таблице
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(openFileInput(filename)));
            while (true) {
                SecretFile sf = new SecretFile();
                String str = br.readLine();
                String str2 = br.readLine();
                if (str == null) break;
                str = sf.encode(str, filename);
                if (s.equals(str)) return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private AlertDialog showStartDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.start_title));
        dialog.setMessage(getString(R.string.start_message));
        dialog.setPositiveButton(getString(R.string.start_btn_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteFile(filename);

                Button upload_btn = (Button) findViewById(R.id.upload_button);
                upload_btn.setEnabled(false);

                Intent intentStart = new Intent(getApplication(), StartActivity.class);
                startActivity(intentStart);
            }
        });
        dialog.setNegativeButton(getString(R.string.start_btn_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        return dialog.show();
    }

    private AlertDialog showDoubleDialog(String kp) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.double_title));
        dialog.setMessage(kp+" "+getString(R.string.double_message));
        dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return dialog.show();
    }

    private AlertDialog showIncorrectFinishDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.incorrect_finish_title));
        dialog.setMessage(getString(R.string.incorrect_finish_message));
        dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return dialog.show();
    }

    private void uploadData() {

        SharedPreferences sp = getSharedPreferences("rogaining", Context.MODE_PRIVATE);
        filename = sp.getString("filename", "");
        url = sp.getString("upload_url", "");
        user = sp.getString("upload_user", "");
        pass = sp.getString("upload_pass", "");

        String category = sp.getString("category", "");
        String pol = sp.getString("pol", "");
        String comand = sp.getString("comand", "");
        String comandNum = sp.getString("comand_num", "");
        String fio = sp.getString("fio", "");

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
            String kp;
            String time;
            String str;
            int sumKp = 0;
            while (true) {
                SecretFile sf = new SecretFile();
                kp = br.readLine();
                time = br.readLine();
                if (kp == null) break;
                kp = sf.encode(kp, filename);
                time = sf.encode(time, filename);
                // Выделяем вес КП
                String s[] = kp.split("№");
                String vesKp = "";
                if (s.length > 1) {
                    vesKp = s[1].substring(0, 1);
                    sumKp += Integer.valueOf(vesKp);
                    str = comandNum + ";" + comand + ";" + fio + ";" + category + ";" + pol + ";" + kp + ";" + time + ";" + vesKp;
                } else {
                    str = comandNum + ";" + comand + ";" + fio + ";" + category + ";" + pol + ";" + kp + ";" + time + ";" + String.valueOf(sumKp);
                }
                bw.write(str);
                bw.newLine();
                //
                Log.d("===", str);
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
    }

    class FtpTask extends AsyncTask<Void, Void, Void> {

        boolean successUpload = false;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                FileInputStream fs = openFileInput(outputfile);
                FTPClient mFTP = new FTPClient();
                mFTP.setConnectTimeout(2000);
                String[] urlPort = url.split(":");
                if (urlPort.length > 1) {
                    mFTP.connect(urlPort[0], Integer.valueOf(urlPort[1]));
                } else {
                    mFTP.connect(url);
                }
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
        }

        private AlertDialog showUploadDialog(boolean success) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
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

    private AlertDialog showIncorrectDistanceDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle(getString(R.string.error));
        dialog.setMessage(getString(R.string.incorrect_distance_message));
        dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return dialog.show();
    }

    private AlertDialog showIncorrectKpDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle(getString(R.string.error));
        dialog.setMessage(getString(R.string.incorrect_kp_message));
        dialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return dialog.show();
    }

}