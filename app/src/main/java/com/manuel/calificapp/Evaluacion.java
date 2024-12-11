package com.manuel.calificapp;

public class Evaluacion {
    private String id;
    private String name;
    private String grade;
    private String weight;

    public Evaluacion() {}

    public Evaluacion(String id, String name, String grade, String weight) {
        this.id = id;
        this.name = name;
        this.grade = grade;
        this.weight = weight;
    }

    public String getId() {
        return id; }

    public void setId(String id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }
}
