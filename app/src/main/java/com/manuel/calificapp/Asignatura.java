package com.manuel.calificapp;

public class Asignatura {
    private String id;
    private String subjectName;
    private int maxGrade;

    // Constructor vacío necesario para Firebase
    public Asignatura() {
    }

    // Constructor con todos los campos
    public Asignatura(String id, String subjectName, int maxGrade) {
        this.id = id;
        this.subjectName = subjectName;
        this.maxGrade = maxGrade;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getMaxGrade() {
        return maxGrade;
    }

    public void setMaxGrade(int maxGrade) {
        this.maxGrade = maxGrade;
    }
}
