package fr.emse.master.model;

import lombok.Data;

@Data
public class Station {
    String id;
    String nom;
    Double latitude;
    Double longitude;
    int capacite;
    int nb_velo_disp;
    boolean indisponible;
    String commentaire;
    Ville ville;
    int dispoMatin;
    int dispoSoir;
    int dispoAprem;


}
/*
 */