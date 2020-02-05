package com.example.googledrive;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
    static final String DISPLAY_MESSAGE_ACTION = "org.example.eventos.DISPLAY_MESSAGE";
    private static Handler manejador = new Handler();
    private static Handler carga = new Handler();
    private static ProgressDialog dialogo;
    private Boolean noAutoriza = false;
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
    static void mostrarMensaje(final Context context, final String mensaje){
        manejador.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show();
            }
        });
    }
    static void mostrarCarga(final Context context, final String mensaje){
        carga.post(new Runnable() {
            @Override
            public void run() {
                dialogo = new ProgressDialog(context);
                dialogo.setMessage(mensaje);
                dialogo.show();
            }
        });
    }
    static void ocultarCarga(final Context context){
        carga.post(new Runnable() {
            @Override
            public void run() {
                dialogo.dismiss();
            }
        });
    }
}