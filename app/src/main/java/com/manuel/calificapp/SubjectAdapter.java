package com.manuel.calificapp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {

    private ArrayList<Asignatura> subjects;
    private Context context;


    public SubjectAdapter(ArrayList<Asignatura> subjects, Context context) {
        this.subjects = subjects;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.subject_item, parent, false);
        return new ViewHolder(view);
    }

    public void updateSubjects(ArrayList<Asignatura> newSubjects) {
        this.subjects = newSubjects;
        notifyDataSetChanged(); // Notificar que los datos han cambiado
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Asignatura subject = subjects.get(position);

        String subjectName = subject.getSubjectName() != null && !subject.getSubjectName().isEmpty() ? subject.getSubjectName() : "Sin nombre";
        holder.tvSubjectName.setText(subjectName);

        holder.itemView.setOnClickListener(v -> {
            // Cuando el elemento de la lista es tocado, mostrar el diálogo de detalles
            SubjectDetailDialogFragment dialogFragment = new SubjectDetailDialogFragment();

            // Crear el Bundle con el ID y el nombre de la asignatura
            Bundle args = new Bundle();
            args.putString("subjectName", subjectName);
            args.putString("subjectId", subject.getId()); // Aquí pasamos el ID

            dialogFragment.setArguments(args);

            // Mostrar el diálogo
            dialogFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "SubjectDetailsDialog");
        });

        holder.btn_show_subject.setOnClickListener(v -> {
            // Este es el botón de edición, que llevará a la actividad de edición
            SubjectDetailDialogFragment dialogFragment = new SubjectDetailDialogFragment();

            Bundle args = new Bundle();
            args.putString("subjectName", subjectName);
            args.putString("subjectId", subject.getId()); // Aquí pasamos el ID también

            dialogFragment.setArguments(args);
            dialogFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "SubjectDetailsDialog");
        });
    }


    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName;
        ImageButton btn_show_subject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tv_subject_name);
            btn_show_subject = itemView.findViewById(R.id.btn_show_subject);
        }
    }
}
