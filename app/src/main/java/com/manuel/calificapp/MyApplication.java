package com.manuel.calificapp;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class MyApplication extends Application {
    private MqttHelper mqttHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);

        // Inicializar MQTT
        iniciarConexionMQTT();
    }

    private void iniciarConexionMQTT() {
        try {
            mqttHelper = new MqttHelper(); // Crear una instancia de MqttHelper
            if (mqttHelper.isConectado()) {
                // Log para confirmar conexión
                System.out.println("Conexión MQTT establecida.");
            } else {
                System.out.println("Error al conectar MQTT.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MqttHelper getMqttHelper() {
        return mqttHelper; // Proveer acceso global a mqttHelper
    }
}
