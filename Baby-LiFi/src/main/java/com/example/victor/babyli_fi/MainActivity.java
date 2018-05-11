package com.example.victor.babyli_fi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Date;

import static java.lang.System.currentTimeMillis;

public class MainActivity extends AppCompatActivity implements SensorEventListener {//On implément la class Sensor

    Button Exe;
    Button Upd;
    Button Strt;
    Button Recept;
    Button Em;

    EditText Mot_2_Send; //Partie Emission
    private android.hardware.Camera camera;
    private Object updateThread = new Object();
    String word_trs;
    String word_entete = "10101010"; //mot absent dans la table ascii

    SensorManager sensorManager; //Partie Réception
    float l = -1;
    Sensor light;
    float Seuil=-1;
    int flag = 1;
    Long Time = currentTimeMillis();
    int Periodms = 550;
    String mot="";
    boolean debut = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Exe = (Button) findViewById(R.id.Execute);
        Upd = (Button) findViewById(R.id.Update);
        Strt = (Button) findViewById(R.id.Start);
        Recept = (Button) findViewById(R.id.Reception);
        Em = (Button) findViewById(R.id.Emission);

        Context context = this;
        PackageManager pm = context.getPackageManager();

        camera = android.hardware.Camera.open();

        Mot_2_Send = (EditText) findViewById(R.id.textView4);

        final RelativeLayout Accueil = (RelativeLayout) findViewById(R.id.Acceuil);
        final RelativeLayout Emission = (RelativeLayout) findViewById(R.id.EMISSION);
        Emission.setVisibility(View.INVISIBLE);
        final RelativeLayout Reception = (RelativeLayout) findViewById(R.id.RECEPTION);
        Reception.setVisibility(View.INVISIBLE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        Recept.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Accueil.setVisibility(View.INVISIBLE);
                Reception.setVisibility(View.VISIBLE);
            }
        });

        Em.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Accueil.setVisibility(View.INVISIBLE);
                Emission.setVisibility(View.VISIBLE);
            }
        });

        Upd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MaJ_Seuil(Seuil);
            }
        });

        Strt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                debut = true;
                Time = currentTimeMillis();
                mot = "";
            }
        });

        Exe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Data_Enter();
            }
        });

    }

    @Override
    protected void onPause() { //Si l'app est mis en pause
        sensorManager.unregisterListener(this, light);
        super.onPause();
    }

    @Override
    protected void onResume() { //Si l'app sort de pause
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_FASTEST);
        super.onResume();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {//On s'en fou il faut juste l'implémenté
    }

    @Override
    public void onSensorChanged(SensorEvent event) { //Quand la valeur du capteur change
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) { //Si la valeur a changer
            l = event.values[0]; //On récupere la valeur
            int nbr;
            Seuil = Seuil + 100;
            float temps_max =0;

            if(debut == true) {
                do {

                    if (l > Seuil) {
                        if (flag == 0) {
                            nbr = (int) ((currentTimeMillis() - Time) / (Periodms));
                            Time = currentTimeMillis();
                            for (int j = 0; j < nbr; j++) {
                                mot = mot + "0";
                            }
                        }
                        flag = 1;
                    }

                    if (l < Seuil) {
                        if (flag == 1) {
                            nbr = (int) ((currentTimeMillis() - Time) / (Periodms));
                            Time = currentTimeMillis();
                            for (int j = 0; j < nbr; j++) {
                                mot = mot + "1";
                            }
                        }
                        flag = 0;
                    }

                    if (mot == word_entete) {
                        mot = "";
                    }
                    temps_max = (float) (currentTimeMillis() - Time);
                }
                while (temps_max < 30000); //vu que dans nos charactère on a jamais de suite de 0 > à 5
                // si on attend au moins 3 sec sans changer -> soit > 6 '0' ou 6 '1'
                //c'est fini !
                conversion();
            }

        }
    }

    public void conversion(){
        String result ="";
        for(int j = 1 ; j < mot.length() ; j++){
            if(j%7==0){
                char a = (char)Integer.parseInt(mot.substring(j-7),2);
                result = result + a ;
            }
        }
        result = result +(char)Integer.parseInt(mot.substring(mot.length()-7,mot.length()),2);
        final TextView textView1 = (TextView) findViewById(R.id.textView1);
        textView1.setText(result);
    }
    public void MaJ_Seuil(float seuil) {
        seuil = l;

    }
    public void Data_Enter (){
        String word, word_bin, word_fin, word_int;
        word = Mot_2_Send.getText().toString();
        byte[] bytes = word.getBytes();
        StringBuilder binary = new StringBuilder();
        for(byte b:bytes)
        {
            int val = b;
            for(int i = 0; i<8;i++){
                binary.append((val & 128)==0 ? 0 : 1);
                val <<=1;
            }
        }
        word_bin = binary.toString();
        word_fin = "00001101"; //equivalent retour chariot
        word_int = word_entete+word_fin;
        word_trs = word_entete + word_bin + word_fin; //on transmet entete + data binaire + retour chariot
        if(word_trs != word_int)
        {
            Log.d("Mot : ", word_trs);
            envoi();
        }
    }
    public void envoi(){
        char[] Tableau_binaire = word_trs.toCharArray();
        int a = word_trs.length();
        for(int i = 0 ; i < a ; i++)
        {
            final android.hardware.Camera.Parameters p = camera.getParameters();

            if(Tableau_binaire[i] == '0'){
                try {//Try/catch obligatoire pour faire un "wait"
                    synchronized (updateThread) { //idem
                        p.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(p);
                        updateThread.wait(500); //On attend ...s
                    }
                    //Il est necessaire car on ne peut modifier
                    //l'interface graphique uniquement sur le UI principal
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(Tableau_binaire[i]=='1'){
                try {//Try/catch obligatoire pour faire un "wait"
                    synchronized (updateThread) { //idem
                        p.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(p);
                        updateThread.wait(500); //On attend ... s
                    }
                    //Il est necessaire car on ne peut modifier
                    //l'interface graphique uniquement sur le UI principal
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}