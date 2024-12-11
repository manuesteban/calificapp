package com.manuel.calificapp;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class FirebaseRepository {
    private final FirebaseFirestore db;

    public FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Agregar una asignatura
    public void addSubject(String subjectName, int maxGrade, OnResultListener<Void> onResultListener) {
        Map<String, Object> subjectData = new HashMap<>();
        subjectData.put("subjectName", subjectName);
        subjectData.put("maxGrade", maxGrade);

        db.collection("subjects").add(subjectData)
                .addOnSuccessListener(documentReference -> onResultListener.onSuccess(null))
                .addOnFailureListener(onResultListener::onFailure);
    }

    // Obtener el siguiente ID para la evaluación
    public void getNextEvaluationId(String subjectID, final OnResultListener<String> onResultListener) {
        // Referencia al documento que mantiene el contador
        db.collection("subjects").document(subjectID)
                .collection("counters").document("evaluationIdCounter")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Incrementar el contador
                        long currentId = documentSnapshot.getLong("evaluationId");
                        String newEvaluationId = "sub" + (currentId + 1); // Generar ID como 'sub1', 'sub2', etc.

                        // Actualizar el contador en Firestore
                        documentSnapshot.getReference().update("evaluationId", currentId + 1)
                                .addOnSuccessListener(aVoid -> onResultListener.onSuccess(newEvaluationId))
                                .addOnFailureListener(onResultListener::onFailure);
                    } else {
                        // Si el contador no existe, crearlo
                        String newEvaluationId = "sub1";
                        Map<String, Object> newCounter = new HashMap<>();
                        newCounter.put("evaluationId", 2); // Empezamos en 2 para que el primer ID sea 'sub1'

                        db.collection("subjects").document(subjectID)
                                .collection("counters").document("evaluationIdCounter")
                                .set(newCounter)
                                .addOnSuccessListener(aVoid -> onResultListener.onSuccess(newEvaluationId))
                                .addOnFailureListener(onResultListener::onFailure);
                    }
                })
                .addOnFailureListener(onResultListener::onFailure);
    }

    // Agregar una evaluación con ID secuencial
    public void addEvaluation(String subjectID, String evaluationName, double grade, int weight, OnResultListener<Void> onResultListener) {
        // Obtener el siguiente ID para la evaluación
        getNextEvaluationId(subjectID, new OnResultListener<String>() {
            @Override
            public void onSuccess(String evaluationId) {
                // Una vez que tenemos el ID, crear el documento para la evaluación
                Map<String, Object> evaluationData = new HashMap<>();
                evaluationData.put("evaluationName", evaluationName);
                evaluationData.put("grade", grade);
                evaluationData.put("weight", weight);

                // Usamos el ID generado (por ejemplo, 'sub1', 'sub2', etc.)
                db.collection("subjects").document(subjectID)
                        .collection("evaluations")
                        .document(evaluationId) // Usamos el ID personalizado
                        .set(evaluationData)
                        .addOnSuccessListener(aVoid -> onResultListener.onSuccess(null))
                        .addOnFailureListener(onResultListener::onFailure);
            }

            @Override
            public void onFailure(Exception e) {
                onResultListener.onFailure(e);
            }
        });
    }

    public void updateMaxGradeToInt(String subjectID) {
        db.collection("subjects").document(subjectID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Obtener maxGrade como String
                        String maxGradeString = documentSnapshot.getString("maxGrade");

                        if (maxGradeString != null) {
                            try {
                                // Convertir maxGrade de String a int
                                int maxGrade = Integer.parseInt(maxGradeString);

                                // Actualizar el documento con el valor convertido
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("maxGrade", maxGrade);

                                documentSnapshot.getReference().update(updates)
                                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "maxGrade actualizado correctamente"))
                                        .addOnFailureListener(e -> Log.e("Firestore", "Error actualizando maxGrade", e));
                            } catch (NumberFormatException e) {
                                Log.e("Firestore", "Error al convertir maxGrade a int", e);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error al obtener el documento", e));
    }


    // Obtener todas las asignaturas
    public Query getSubjects() {
        return db.collection("subjects");
    }

    // Obtener evaluaciones de una asignatura
    public Query getEvaluations(String subjectID) {
        return db.collection("subjects").document(subjectID).collection("evaluations");
    }

    // Interfaz personalizada para manejar resultados
    public interface OnResultListener<T> {
        void onSuccess(T result);

        void onFailure(Exception e);
    }
}
