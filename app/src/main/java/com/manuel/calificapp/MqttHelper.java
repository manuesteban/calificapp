package com.manuel.calificapp;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttHelper {

    // Variables de la Conexión a MQTT
    private static String mqttHost = "tcp://fossilowl256.cloud.shiftr.io:1883"; // IP del Servidor MQTT
    private static String IdUsuario = "AppAndroid"; // Nombre del dispositivo que se conectará
    private static String User = "fossilowl256"; // Usuario
    private static String Pass = "juMg9MliAV7I7Ejo"; // Contraseña o Token

    private MqttClient mqttClient;

    // Constructor de la clase
    public MqttHelper() {
        try {
            // Creación de un cliente MQTT
            mqttClient = new MqttClient(mqttHost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());

            // Conexión al servidor MQTT
            mqttClient.connect(options);
            System.out.println("Conectado al Servidor MQTT");

            // Configuración del callback para manejar eventos
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Conexión perdida");
                }

                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                    System.out.println("Mensaje recibido del tópico " + topic + ": " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
                    System.out.println("Entrega completada");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Método para verificar si el cliente está conectado
    public boolean isConectado() {
        return mqttClient != null && mqttClient.isConnected();
    }

    // Método para desconectar del broker
    public void desconectar() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                System.out.println("Desconectado del Servidor MQTT");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
