package com.manuel.calificapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnAddSubject;
    private RecyclerView rvSubjects;
    private SubjectAdapter adapter;
    private ArrayList<Asignatura> subjects = new ArrayList<>();
    private FirebaseFirestore db;
    private static final int REQUEST_EDIT_SUBJECT = 1; // Código de solicitud


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de Firebase
        db = FirebaseFirestore.getInstance();

        btnAddSubject = findViewById(R.id.btn_add_subject);
        rvSubjects = findViewById(R.id.rv_subjects);

        rvSubjects.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SubjectAdapter(subjects, this);
        rvSubjects.setAdapter(adapter);

        loadSubjects(); // Cargar las materias al inicio

        btnAddSubject.setOnClickListener(v -> openAddSubjectDialog()); // Acción de agregar materia
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSubjects(); // Recargar las materias cada vez que la actividad esté en primer plano
    }


    private void loadSubjects() {
        db.collection("subjects")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        subjects.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Asignatura subject = document.toObject(Asignatura.class);
                            subject.setId(document.getId()); // Asignar el ID del documento
                            subjects.add(subject);
                        }
                        adapter.updateSubjects(subjects);
                        Log.d("MainActivity", "Datos cargados: " + subjects.size() + " materias.");

                    } else {
                        Toast.makeText(this, "Error al cargar materias: ", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openAddSubjectDialog() {
        // Crear un diálogo para agregar una materia
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.add_subject_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialogView.findViewById(R.id.btn_save_subject).setOnClickListener(v -> {
            String subjectName = ((EditText) dialogView.findViewById(R.id.et_subject_name)).getText().toString();
            if (!subjectName.isEmpty()) {
                // Guardar nueva materia en Firestore en la colección "subjects"
                Map<String, Object> subject = new HashMap<>();
                subject.put("subjectName", subjectName);

                db.collection("subjects").add(subject)
                        .addOnSuccessListener(documentReference -> {
                            // Mostrar confirmación con Snackbar
                            Snackbar.make(findViewById(android.R.id.content), "Materia guardada", Snackbar.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadSubjects(); // Recargar las materias
                        })
                        .addOnFailureListener(e -> {
                            // Mostrar mensaje de error si la operación falla
                            Snackbar.make(findViewById(android.R.id.content), "Error al guardar materia", Snackbar.LENGTH_SHORT).show();
                        });
            } else {
                // Mostrar mensaje si el nombre está vacío
                Snackbar.make(findViewById(android.R.id.content), "Ingrese un nombre", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}