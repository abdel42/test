package fr.emse.master.model;

import lombok.Data;

import java.awt.geom.Point2D;

@Data
public class Proximite {
    String place;
    Point2D location;
    double distance;
    String placeLabel;
    String instanceOfLabel;
    String image;
}
