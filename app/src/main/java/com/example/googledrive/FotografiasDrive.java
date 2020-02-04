package com.example.googledrive;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.Drive;

public class FotografiasDrive extends AppCompatActivity {
    final int SOLICITUD_FOTOGRAFIAS_DRIVE = 102;
    public TextView mDisplay;
    String evento;
    static Drive servicio = null;
    static GoogleAccountCredential credential = null;
    static String nombreCuenta = null;
    static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fotografias_drive);
        Bundle extras = getIntent().getExtras();
        evento = extras.getString("evento");
        Toolbar toolbar =(Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_drive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        View vista = (View) findViewById(android.R.id.content);
        int id = item.getItemId();
        switch (id) {
            case R.id.action_camara:
                break;
            case R.id.action_galeria:
                break;
            case  R.id.action_fotografiasDrive:
                Intent intent = new Intent(getBaseContext(),FotografiasDrive.class);
                intent.putExtra("evento", evento);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
