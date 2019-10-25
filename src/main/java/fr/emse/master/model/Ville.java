package fr.emse.master.model;

import lombok.Data;

import java.util.List;

@Data
public class Ville {
    String id;
    String nom;
    int departement;
    Double latitude;
    Double longitude;
    List<Station> stations;
}
