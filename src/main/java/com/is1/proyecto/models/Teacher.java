package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("teachers")
public class Teacher extends Model {

    public String getName() {
        return getString("nameTeacher");
    }

    public void setName(String nameTeacher) {
        set("nameTeacher", nameTeacher);
    }

    public String getPassword() {
        return getString("passwordTeacher"); // Obtiene el valor de la columna 'password'
    }

    public void setPassword(String passwordTeacher) {
        set("passwordTeacher", passwordTeacher); // Establece el valor para la columna 'password'
    }
}
