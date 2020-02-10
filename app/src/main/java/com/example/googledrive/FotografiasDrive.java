package com.example.googledrive;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.google.android.gms.common.util.SharedPreferencesUtils;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.User;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

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
    static final int SOLICITUD_SELECCION_CUENTA = 1;
    static final int SOLICITUD_AUTORIZACION = 2;
    static final int SOLICITUD_SELECCIONAR_FOTOGRAFIA = 3;
    static final int SOLICITUD_HACER_FOTOGRAFIA = 4;
    private static Uri uriFichero;
    private String idCarpeta = "";
    private String idCarpetaEvento = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fotografias_drive);
        registerReceiver(mHandleMessageReceiver, new IntentFilter(DISPLAY_MESSAGE_ACTION));
        mDisplay = findViewById(R.id.display);
        Bundle extras = getIntent().getExtras();
        //evento = extras.getString("evento");
        Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        credential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE));
        SharedPreferences prefs = getSharedPreferences("Preferencias", Context.MODE_PRIVATE);
        nombreCuenta = prefs.getString("NombreCuenta", null);
        noAutoriza = prefs.getBoolean("Noautoriza", false);
        idCarpeta = prefs.getString("idCarpeta", null);
        idCarpetaEvento = prefs.getString("idCarpeta_" + evento, null);
        if (!noAutoriza) {
            if (nombreCuenta == null) {
                PedirCredenciales();
            } else {
                credential.setSelectedAccountName(nombreCuenta);
                servicio = obtenerServicioDrive(credential);
                if (idCarpetaEvento == null) {
                    crearCarpetaEnDrive(evento, idCarpeta);
                }else{
                    listarFicheros(this.findViewById(android.R.id.content));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_drive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        View vista = findViewById(android.R.id.content);
        int id = item.getItemId();
        switch (id) {
            case R.id.action_camara:
                if (!noAutoriza){
                    hacerFoto(vista);
                }
                break;
            case R.id.action_galeria:
                if (!noAutoriza){
                    seleccionarFoto(vista);
                }
                break;
            case R.id.action_fotografiasDrive:
                Intent intent = new Intent(getBaseContext(), FotografiasDrive.class);
                intent.putExtra("evento", evento);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    static void mostrarMensaje(final Context context, final String mensaje) {
        manejador.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show();
            }
        });
    }

    static void mostrarCarga(final Context context, final String mensaje) {
        carga.post(new Runnable() {
            @Override
            public void run() {
                dialogo = new ProgressDialog(context);
                dialogo.setMessage(mensaje);
                dialogo.show();
            }
        });
    }

    static void ocultarCarga(final Context context) {
        carga.post(new Runnable() {
            @Override
            public void run() {
                dialogo.dismiss();
            }
        });
    }

    private void PedirCredenciales() {
        if (nombreCuenta == null) {
            startActivityForResult(credential.newChooseAccountIntent(), SOLICITUD_SELECCION_CUENTA);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SOLICITUD_SELECCION_CUENTA:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    nombreCuenta = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (nombreCuenta != null) {
                        credential.setSelectedAccountName(nombreCuenta);
                        servicio = obtenerServicioDrive(credential);
                        SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("nombre cuenta", nombreCuenta);
                        editor.commit();
                        crearCarpetaEnDrive(evento, idCarpeta);
                    }
                }
                break;
            case SOLICITUD_HACER_FOTOGRAFIA:
                if(resultCode == Activity.RESULT_OK){
                    guardarFicheroEnDrive(this.findViewById(android.R.id.content));
                }
                break;
            case SOLICITUD_SELECCIONAR_FOTOGRAFIA:
                if(requestCode == Activity.RESULT_OK){
                    Uri ficheroSeleccionado = data.getData();
                    String[] proyeccion ={MediaStore.Images.Media.DATA};
                    Cursor cursor = managedQuery(ficheroSeleccionado, proyeccion, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    uriFichero = Uri.fromFile(new java.io.File(cursor.getString(column_index)));
                    guardarFicheroEnDrive(this.findViewById(android.R.id.content));
                }
                break;
            case SOLICITUD_AUTORIZACION:
                if (resultCode == Activity.RESULT_OK){
                    crearCarpetaEnDrive(evento, idCarpeta);
                }else{
                    noAutoriza = true;
                    SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("noAutorizo", true);
                    editor.commit();
                    mostrarMensaje(this, "El usuario no Autoriza usar GoogleDriver");
                }
                break;
        }
    }

    private Drive obtenerServicioDrive(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
    }

    private void crearCarpetaEnDrive(final String nombreCarpeta, final String carpetaPadre) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String idCarpetaPadre = carpetaPadre;
                    mostrarCarga(FotografiasDrive.this, "Creando Carpeta...");
                    //Crear Carpeta Eventos Drive
                    if (idCarpeta == null) {
                        File metadataFichero = new File();
                        metadataFichero.setName("EventosDrive");
                        metadataFichero.setMimeType("application/vnd.google-apps.folder");
                        File fichero = servicio.files().create(metadataFichero).setFields("id").execute();
                        if (fichero.getId() != null) {
                            SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("idCarpeta" + evento, fichero.getId());
                            editor.commit();
                            idCarpetaPadre = fichero.getId();

                        }
                    }
                    File metadataFichero = new File();
                    metadataFichero.setName(nombreCarpeta);
                    metadataFichero.setMimeType("Application/vnd.google-apps.folder");
                    if (!idCarpetaPadre.equals("")) {
                        metadataFichero.setParents(Collections.singletonList(idCarpetaPadre));
                    }
                    File fichero = servicio.files().create(metadataFichero).setFields("id").execute();
                    if (fichero.getId() != null) {
                        SharedPreferences prefs = getSharedPreferences("preferencias", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("idCarpeta_" + evento, fichero.getId());
                        editor.commit();
                        idCarpetaEvento = fichero.getId();
                        mostrarMensaje(FotografiasDrive.this, "Carpeta Creada");
                    }
                    ocultarCarga(FotografiasDrive.this);
                } catch (UserRecoverableAuthIOException e) {
                    ocultarCarga(FotografiasDrive.this);
                    startActivityForResult(e.getIntent(), SOLICITUD_AUTORIZACION);

                } catch (IOException e) {
                    mostrarMensaje(FotografiasDrive.this, "Error;" + e.getMessage());
                    ocultarCarga(FotografiasDrive.this);
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
    public void hacerFoto(View v){
        if(nombreCuenta == null){
            mostrarMensaje(this, "Debes de seleccionar una cuenta de Google Drive");
        }else{
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(takePictureIntent.resolveActivity(getPackageManager())!= null){
                java.io.File ficheroFoto = null;
                try{
                    ficheroFoto = crearFicheroImagen();
                    if(ficheroFoto != null){
                        Uri fichero = FileProvider.getUriForFile(FotografiasDrive.this,
                                BuildConfig.APPLICATION_ID + ".provider", ficheroFoto);
                        uriFichero = Uri.parse("content://" + ficheroFoto.getAbsolutePath());
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fichero);
                        startActivityForResult(takePictureIntent, SOLICITUD_HACER_FOTOGRAFIA);
                    }
                }catch (IOException ex){
                    return;
                }
            }
        }
    }
    private java.io.File crearFicheroImagen() throws IOException{
        String tiempo = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String nombreFichero = "JPEG_" + tiempo + "_";
        java.io.File dirAlmacenaje = new java.io.File(Environment.DIRECTORY_DCIM, "Camera");
        java.io.File ficheroImagen = java.io.File.createTempFile(nombreFichero,".jpg", dirAlmacenaje);
        return ficheroImagen;
    }
    public void seleccionarFoto(View v){
        if(nombreCuenta == null){
            mostrarMensaje(this, "Debes seleccionar una cuenta de Google Drive");
        }else{
            Intent seleccionarFotografiaIntent = new Intent();
            seleccionarFotografiaIntent.setType("image/*");
            seleccionarFotografiaIntent.setAction(Intent.ACTION_PICK);
            startActivityForResult(Intent.createChooser(seleccionarFotografiaIntent, "Seleccionar fotografia"),
                    SOLICITUD_SELECCIONAR_FOTOGRAFIA);
        }
    }
    private void guardarFicheroEnDrive(final View view){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    mostrarCarga(FotografiasDrive.this, "Subiendo imagen...");
                    java.io.File ficheroJava = new java.io.File(uriFichero.getPath());
                    FileContent contenido = new FileContent("image/jpeg", ficheroJava);
                    File ficheroDrive = new File();
                    ficheroDrive.setName(ficheroJava.getName());
                    ficheroDrive.setMimeType("image/jpeg");
                    ficheroDrive.setParents(Collections.singletonList(idCarpetaEvento));
                    File ficheroSubido = servicio.files().create(ficheroDrive,
                            contenido).setFields("id").execute();
                    if(ficheroSubido.getId() != null){
                        mostrarMensaje(FotografiasDrive.this, "¡Foto subida!");
                        listarFicheros(view);
                    }
                    ocultarCarga(FotografiasDrive.this);
                }catch (UserRecoverableAuthIOException e){
                    ocultarCarga(FotografiasDrive.this);
                    startActivityForResult(e.getIntent(),SOLICITUD_AUTORIZACION);
                }catch (IOException e){
                    mostrarMensaje(FotografiasDrive.this, "Error;" + e.getMessage());
                    ocultarCarga(FotografiasDrive.this);
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String nuevoMensaje = intent.getExtras().getString("mensaje");
            mDisplay.append(nuevoMensaje + "\n");
        }
    };
    static void mostrarTexto(Context contexto, String mensaje){
        Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
        intent.putExtra("mensaje", mensaje);
        contexto.sendBroadcast(intent);
    }
    public void listarFicheros(View v){
        if (nombreCuenta == null){
            mostrarMensaje(this, "Debes de seleccionar una cuenta de Google Drive");
        }else{
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        mostrarCarga(FotografiasDrive.this, "Listando archivos...");
                        FileList ficheros = servicio.files().list()
                        .setQ("'" + idCarpetaEvento + "' in parents").setFields("*").execute();
                        for (File fichero : ficheros.getFiles()){
                            mostrarTexto(getBaseContext(), fichero.getOriginalFilename());
                        }
                        mostrarMensaje(FotografiasDrive.this, "¡Archivoa listados!");
                        ocultarCarga(FotografiasDrive.this);
                    }catch (UserRecoverableAuthIOException e){
                        ocultarCarga(FotografiasDrive.this);
                        startActivityForResult(e.getIntent(), SOLICITUD_AUTORIZACION);
                    }catch (IOException e) {
                        mostrarMensaje(FotografiasDrive.this, "Error;" + e.getMessage());
                        ocultarCarga(FotografiasDrive.this);
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }
}