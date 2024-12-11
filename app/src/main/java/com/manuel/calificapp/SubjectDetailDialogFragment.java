package com.manuel.calificapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SubjectDetailDialogFragment extends DialogFragment {

    private TextView tvSubjectName, tvMaxGrade, tvAverage, tvNoRecords, tvEvaluationsTitle;
    private TableLayout tableLayout;
    private ImageButton btnEditSubject;
    private MqttAndroidClient mqttAndroidClient;
    private final String brokerUrl = "tcp://broker.emqx.io:1883"; // Reemplázalo por la URL de tu broker
    private final String topic = "calificapp/average"; // Define el tema donde se publicarán los datos


    public SubjectDetailDialogFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_subject_details, container, false);

        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return view;
        }

        setupMqttClient();


        String subjectName = args.getString("subjectName", "Asignatura Desconocida");
        String subjectId = args.getString("subjectId", "");
        String evaluationId = args.getString("evaluationId", "");

        tvMaxGrade = view.findViewById(R.id.tv_max_grade);
        tvSubjectName = view.findViewById(R.id.tv_subject_name_details);
        tvEvaluationsTitle = view.findViewById(R.id.tv_evaluations_title);
        tvAverage = view.findViewById(R.id.tv_average);
        tvNoRecords = view.findViewById(R.id.tv_no_records);
        tableLayout = view.findViewById(R.id.tableLayout);
        btnEditSubject = view.findViewById(R.id.btn_edit_subject_details);

        tvSubjectName.setText(subjectName);

        getSubjectMaxGrade(subjectId);
        loadEvaluations(subjectId);

        btnEditSubject.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditSubjectActivity.class);
            intent.putExtra("subjectId", subjectId);
            intent.putExtra("subjectName", subjectName);
            startActivity(intent);
            dismiss();
        });

        return view;
    }

    private void getSubjectMaxGrade(String subjectId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("subjects").document(subjectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object maxGradeValue = documentSnapshot.get("maxGrade");

                        if (maxGradeValue != null) {
                            int maxGrade = ((Long) maxGradeValue).intValue();  // Asegurarte de que es un número
                            tvMaxGrade.setText("Nota máxima: " + String.valueOf(maxGrade));  // Mostrar en el TextView
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("SubjectDetail", "Error al obtener maxGrade", e));
    }

    private void loadEvaluations(String subjectId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (subjectId == null || subjectId.isEmpty()) {
            Log.e("Firestore", "El subjectId es nulo o vacío.");
            return;
        }

        Log.d("Firestore", "Materia ID: " + subjectId);

        db.collection("subjects")
                .document(subjectId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("Firestore", "Se recuperó el documento correctamente.");
                        tvEvaluationsTitle.setText("Evaluaciones:");
                        tvNoRecords.setVisibility(View.GONE); // Oculta el mensaje de 'sin registros'
                        tableLayout.setVisibility(View.VISIBLE); // Muestra la tabla de evaluaciones

                        List<Map<String, Object>> evaluations = (List<Map<String, Object>>) documentSnapshot.get("evaluations");

                        if (evaluations != null && !evaluations.isEmpty()) {
                            ArrayList<Evaluacion> evaluationsList = new ArrayList<>();

                            // Recorre todas las evaluaciones y agrega a la lista
                            for (Map<String, Object> evaluation : evaluations) {
                                String name = (String) evaluation.get("name");
                                String grade = (String) evaluation.get("grade");
                                String weight = (String) evaluation.get("weight");

                                if (name != null && grade != null && weight != null) {
                                    Evaluacion evaluationObj = new Evaluacion(subjectId, name, grade, weight);
                                    evaluationsList.add(evaluationObj);
                                } else {
                                    Log.e("Firestore", "Faltan campos en una de las evaluaciones.");
                                }
                            }

                            populateTable(evaluationsList);  // Rellena la tabla con las evaluaciones
                            calculateAverage(evaluationsList);  // Calcula el promedio
                        } else {
                            Log.e("Firestore", "El array de evaluaciones está vacío.");
                            tvNoRecords.setVisibility(View.VISIBLE); // Muestra el mensaje de 'sin registros'
                            tableLayout.setVisibility(View.GONE); // Oculta la tabla
                        }
                    } else {
                        Log.e("Firestore", "El documento de la materia no existe.");
                        tvNoRecords.setVisibility(View.VISIBLE);
                        tableLayout.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SubjectDetail", "Error al cargar evaluaciones: " + e.getMessage());
                    tvEvaluationsTitle.setText("Evaluaciones:");
                    tvNoRecords.setVisibility(View.VISIBLE);
                    tableLayout.setVisibility(View.GONE);
                });
    }




    private void populateTable(ArrayList<Evaluacion> evaluations) {
        tableLayout.removeAllViews();  // Elimina cualquier fila anterior

        // Agrega la fila de encabezado
        TableRow headerRow = new TableRow(getContext());
        headerRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView nameHeader = new TextView(getContext());
        nameHeader.setText("Nombre");
        nameHeader.setPadding(16, 8, 16, 8);
        nameHeader.setGravity(Gravity.CENTER); // Centrar el texto
        headerRow.addView(nameHeader);

        TextView gradeHeader = new TextView(getContext());
        gradeHeader.setText("Calificación");
        gradeHeader.setPadding(16, 8, 16, 8);
        gradeHeader.setGravity(Gravity.CENTER); // Centrar el texto
        headerRow.addView(gradeHeader);

        TextView weightHeader = new TextView(getContext());
        weightHeader.setText("(%)");
        weightHeader.setPadding(16, 8, 16, 8);
        weightHeader.setGravity(Gravity.CENTER); // Centrar el texto
        headerRow.addView(weightHeader);

        tableLayout.addView(headerRow);

        // Agrega una fila por cada evaluación
        for (Evaluacion evaluation : evaluations) {
            TableRow row = new TableRow(getContext());
            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            // Nombre de la evaluación
            TextView nameView = new TextView(getContext());
            nameView.setText(evaluation.getName());
            nameView.setPadding(16, 8, 16, 8);
            nameView.setGravity(Gravity.LEFT); // Centrar el texto
            TableRow.LayoutParams nameParams = new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            nameView.setLayoutParams(nameParams);
            row.addView(nameView);

            // Calificación
            TextView gradeView = new TextView(getContext());
            gradeView.setText(evaluation.getGrade());
            gradeView.setPadding(16, 8, 16, 8);
            gradeView.setGravity(Gravity.LEFT); // Centrar el texto
            TableRow.LayoutParams gradeParams = new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f);
            gradeView.setLayoutParams(gradeParams);
            row.addView(gradeView);

            // Ponderación
            TextView weightView = new TextView(getContext());
            weightView.setText(evaluation.getWeight());
            weightView.setPadding(16, 8, 16, 8);
            weightView.setGravity(Gravity.RIGHT); // Centrar el texto
            TableRow.LayoutParams weightParams = new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.5f);
            weightView.setLayoutParams(weightParams);
            row.addView(weightView);

            // Añadir la fila con todos los datos a la tabla
            tableLayout.addView(row);
        }
    }



    @Override
    public void onStart() {
        super.onStart();

        // Asegurarse de que el diálogo ocupe el 100% del ancho
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }


    private void calculateAverage(ArrayList<Evaluacion> evaluations) {
        if (evaluations != null && !evaluations.isEmpty()) {
            double totalGrade = 0;
            int count = 0;

            for (Evaluacion evaluation : evaluations) {
                try {
                    double grade = Double.parseDouble(evaluation.getGrade());
                    totalGrade += grade;
                    count++;
                } catch (NumberFormatException e) {
                    Log.e("SubjectDetail", "Error al convertir la calificación: " + e.getMessage());
                }
            }

            if (count > 0) {
                double average = totalGrade / count;
                tvAverage.setText("Promedio: " + String.format("%.2f", average));

                // Publicar el promedio
                publishAverageToBroker(average);
            } else {
                tvAverage.setText("Promedio: N/A");
            }
        } else {
            tvAverage.setText("Promedio: N/A");
        }
    }

    private void publishAverageToBroker(double average) {
        try {
            // Construye el mensaje con el promedio
            String message = String.format("{\"average\": %.2f}", average);

            // Publica el mensaje en el tema definido
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(message.getBytes());
            mqttMessage.setQos(1); // Define un QoS adecuado (0, 1 o 2)

            mqttAndroidClient.publish(topic, mqttMessage);
            Log.d("MQTT", "Promedio publicado: " + message);
        } catch (MqttException e) {
            Log.e("MQTT", "Error al publicar el promedio", e);
        }
    }


    private void setupMqttClient() {
        String clientId = MqttClient.generateClientId();
        mqttAndroidClient = new MqttAndroidClient(getContext(), brokerUrl, clientId);

        try {
            mqttAndroidClient.connect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MQTT", "Conexión exitosa al broker MQTT.");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Error al conectar al broker MQTT: " + exception.getMessage());
                }
            });

            mqttAndroidClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("MQTT", "Conexión perdida: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    Log.d("MQTT", "Mensaje recibido del tema " + topic + ": " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega completada.");
                }
            });
        } catch (MqttException e) {
            Log.e("MQTT", "Error al configurar cliente MQTT: " + e.getMessage());
        }
    }



}
