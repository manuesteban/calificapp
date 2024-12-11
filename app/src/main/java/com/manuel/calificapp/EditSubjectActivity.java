package com.manuel.calificapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditSubjectActivity extends AppCompatActivity {
    private LinearLayout layoutEvaluations;
    private int evaluationCount = 0;
    private static final int MAX_EVALUATIONS = 10;
    private FirebaseFirestore db; // Referencia a Firestore
    private TableLayout tableEvaluations;
    private EditText etSubjectName; // Referencia al EditText para el nombre de la asignatura
    private Spinner spinnerMaxGrade;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_subject);

        // Configurar ImageButton para volver
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            // Volver al fragmento anterior
            onBackPressed();
        });

        // Inicializar Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        tableEvaluations = findViewById(R.id.table_evaluations);
        etSubjectName = findViewById(R.id.et_subject_name);
        spinnerMaxGrade = findViewById(R.id.spinner_max_grade); // Inicializar Spinner
        Button btnAddEvaluation = findViewById(R.id.btn_add_evaluation);
        layoutEvaluations = findViewById(R.id.layout_evaluations);
        Button btnSaveChanges = findViewById(R.id.btn_save);

        // Configurar Spinner
        setupSpinner();

        // Recuperar datos de Intent
        Intent intent = getIntent();
        String subjectId = intent.getStringExtra("subjectId");

        // Cargar datos de la asignatura y evaluaciones existentes desde Firestore
        if (subjectId != null) {
            loadSubjectData(subjectId);
        }

        // Mostrar campos de evaluación al presionar "Añadir evaluación"
        btnAddEvaluation.setOnClickListener(v -> showEvaluationFields());

        // Configurar el botón de guardar cambios
        btnSaveChanges.setOnClickListener(v -> showConfirmationDialog(subjectId));
    }

    // Mostrar un diálogo de confirmación antes de guardar los cambios
    private void showConfirmationDialog(String subjectId) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmación")
                .setMessage("¿Desea guardar los cambios?")
                .setPositiveButton("Sí", (dialog, which) -> saveSubjectData(subjectId))
                .setNegativeButton("No", null)
                .show();
    }

    private void setupSpinner() {
        // Valores para el Spinner
        Integer[] grades = {7, 10, 100};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, grades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMaxGrade.setAdapter(adapter);
    }

    // Cargar datos de la asignatura y evaluaciones desde Firestore
    private void loadSubjectData(String subjectId) {
        db.collection("subjects").document(subjectId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Obtener y mostrar el nombre de la asignatura
                        String subjectName = document.getString("subjectName"); // Cambiar "name" por "subjectName"
                        if (subjectName != null) {
                            etSubjectName.setText(subjectName);
                        } else {
                            etSubjectName.setHint("Nombre no encontrado");
                        }

                        // Configurar el Spinner según el valor de maxGrade
                        if (document.contains("maxGrade")) {
                            int maxGrade = document.getLong("maxGrade").intValue();  // Cambiar a int
                            setSpinnerValue(maxGrade);
                        }

                        // Obtener y mostrar las evaluaciones
                        if (document.get("evaluations") != null) {
                            ArrayList<Map<String, String>> evaluations =
                                    (ArrayList<Map<String, String>>) document.get("evaluations");

                            for (Map<String, String> eval : evaluations) {
                                addEvaluationToTable(eval.get("name"), eval.get("grade"), eval.get("weight"));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al cargar datos.", Toast.LENGTH_SHORT).show());
    }

    // Método para establecer el valor predeterminado en el Spinner
    private void setSpinnerValue(int maxGrade) {
        ArrayAdapter<Integer> adapter = (ArrayAdapter<Integer>) spinnerMaxGrade.getAdapter();
        int position = adapter.getPosition(maxGrade);
        if (position >= 0) {
            spinnerMaxGrade.setSelection(position);
        }
    }


    // Agregar evaluación a la tabla
    private void addEvaluationToTable(String name, String grade, String weight) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        row.addView(tvName);

        TextView tvGrade = new TextView(this);
        tvGrade.setText(grade);
        row.addView(tvGrade);

        TextView tvWeight = new TextView(this);
        tvWeight.setText(weight);
        row.addView(tvWeight);

        // Botón de menú
        ImageButton btnMenu = new ImageButton(this);
        btnMenu.setImageResource(android.R.drawable.ic_menu_more);
        btnMenu.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        btnMenu.setOnClickListener(v -> showPopupMenu(v, row, name, grade, weight));
        row.addView(btnMenu);

        tableEvaluations.addView(row);
    }

    // Mostrar el PopupMenu
    private void showPopupMenu(View view, TableRow row, String name, String grade, String weight) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add("Editar");
        popupMenu.getMenu().add("Eliminar");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Editar")) {
                showEditDialog(row, name, grade, weight);
            } else if (item.getTitle().equals("Eliminar")) {
                deleteEvaluation(row, name, grade, weight);
            }
            return true;
        });

        popupMenu.show();
    }

    // Mostrar un diálogo para editar la evaluación
    private void showEditDialog(TableRow row, String name, String grade, String weight) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Evaluación");

        // Vista personalizada para editar
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_evaluation, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.et_evaluation_name);
        EditText etGrade = dialogView.findViewById(R.id.et_evaluation_grade);
        EditText etWeight = dialogView.findViewById(R.id.et_evaluation_weight);

        etName.setText(name);
        etGrade.setText(grade);
        etWeight.setText(weight);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newGrade = etGrade.getText().toString().trim();
            String newWeight = etWeight.getText().toString().trim();

            if (!newName.isEmpty() && !newGrade.isEmpty() && !newWeight.isEmpty()) {
                updateEvaluation(row, newName, newGrade, newWeight);
            } else {
                Toast.makeText(this, "Todos los campos son obligatorios.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // Actualizar evaluación en la fila y Firestore
    private void updateEvaluation(TableRow row, String newName, String newGrade, String newWeight) {
        ((TextView) row.getChildAt(0)).setText(newName);
        ((TextView) row.getChildAt(1)).setText(newGrade);
        ((TextView) row.getChildAt(2)).setText(newWeight);

        // Actualiza también en Firestore
        String subjectId = getIntent().getStringExtra("subjectId");
        if (subjectId != null) {
            Map<String, String> updatedEvaluation = new HashMap<>();
            updatedEvaluation.put("name", newName);
            updatedEvaluation.put("grade", newGrade);
            updatedEvaluation.put("weight", newWeight);

            db.collection("subjects").document(subjectId)
                    .update("evaluations", FieldValue.arrayRemove(updatedEvaluation))
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Evaluación actualizada.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // Eliminar evaluación
    private void deleteEvaluation(TableRow row, String name, String grade, String weight) {
        tableEvaluations.removeView(row);

        // Eliminar de Firestore
        String subjectId = getIntent().getStringExtra("subjectId");
        if (subjectId != null) {
            Map<String, String> evaluation = new HashMap<>();
            evaluation.put("name", name);
            evaluation.put("grade", grade);
            evaluation.put("weight", weight);

            db.collection("subjects").document(subjectId)
                    .update("evaluations", FieldValue.arrayRemove(evaluation))
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Evaluación eliminada.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void saveSubjectData(String subjectId) {
        String subjectName = etSubjectName.getText().toString().trim();
        int maxGrade = (int) spinnerMaxGrade.getSelectedItem();

        if (subjectName.isEmpty()) {
            Toast.makeText(this, "El nombre de la asignatura no puede estar vacío.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> subjectData = new HashMap<>();
        subjectData.put("subjectName", subjectName);
        subjectData.put("maxGrade", maxGrade);

        db.collection("subjects").document(subjectId)
                .update(subjectData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Datos actualizados.", Toast.LENGTH_SHORT).show();
                    // Cerrar la ventana actual y abrir MainActivity
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Cerrar la actividad actual
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    // Mostrar campos de evaluación para ingresar datos
    private void showEvaluationFields() {
        layoutEvaluations.removeAllViews(); // Limpiar campos previos

        View evaluationView = getLayoutInflater().inflate(R.layout.item_evaluation, layoutEvaluations, false);
        layoutEvaluations.addView(evaluationView);

        Button btnSaveEvaluation = evaluationView.findViewById(R.id.btn_save_evaluation);
        btnSaveEvaluation.setOnClickListener(v -> saveEvaluation());
    }

    // Guardar evaluación como documento nuevo en Firestore
    private void saveEvaluation() {
        EditText etEvaluationName = findViewById(R.id.et_evaluation_name);
        EditText etEvaluationGrade = findViewById(R.id.et_evaluation_grade);
        EditText etEvaluationWeight = findViewById(R.id.et_evaluation_weight);

        String name = etEvaluationName.getText().toString().trim();
        String grade = etEvaluationGrade.getText().toString().trim();
        String weight = etEvaluationWeight.getText().toString().trim();

        if (name.isEmpty() || grade.isEmpty() || weight.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> evaluation = new HashMap<>();
        evaluation.put("name", name);
        evaluation.put("grade", grade);
        evaluation.put("weight", weight);

        String subjectId = getIntent().getStringExtra("subjectId");
        if (subjectId != null) {
            db.collection("subjects").document(subjectId)
                    .update("evaluations", FieldValue.arrayUnion(evaluation))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Evaluación añadida correctamente.", Toast.LENGTH_SHORT).show();
                        addEvaluationToTable(name, grade, weight); // Actualizar tabla
                        layoutEvaluations.removeAllViews(); // Limpiar campos
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error al guardar evaluación: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}
