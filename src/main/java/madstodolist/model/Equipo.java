package madstodolist.model;

import java.io.Serializable;

public class Equipo implements Serializable{
    private String name;

    public Equipo(String name){
        this.name=name;
    }

    public String getNombre() {
        return name;
    }

}
