package com.osrsoft.rogaining;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;


public class PhotoActivity extends ActionBarActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        Button button = (Button) findViewById(R.id.buttonBack);
        button.setOnClickListener(this);

        String fpath = getIntent().getExtras().getString("fname");
        File file = new File(fpath);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageURI(Uri.fromFile(file));
    }

    @Override
    public void onClick(View view) {
        // Закрываем активити
        finish();
    }


}
