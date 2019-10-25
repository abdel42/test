package fr.emse.master.controller;

import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;





import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.datatypes.xsd.XSDDatatype;
// import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.pfunction.library.str;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

//import org.apache.jena.vocabulary.

import org.apache.jena.query.* ;





import fr.emse.master.model.*;


import static fr.emse.master.Utils.*;

@Controller
public class WelcomeController {

    // inject via application.properties
    @Value("${welcome.message}")
    private String message;

    private List<String> tasks = Arrays.asList("a", "b", "c", "d", "e", "f", "g");

    @GetMapping("/")
    public String main(Model model) {
        /*     spring     */

        model.addAttribute("message", message);
        model.addAttribute("tasks", tasks);




        /*   */

        String serviceURI = "http://localhost:3030/Test";
        DatasetAccessorFactory factory = null;
        DatasetAccessor accessor;
        accessor = factory.createHTTP(serviceURI);


        /*   */



        /*     Model    */

        // create an empty Modelx
        //il faut mettre le chemin complet pour ne pas provoquer de conflit avec Spring
        org.apache.jena.rdf.model.Model modele = ModelFactory.createDefaultModel();


        /* Définition */
        String geo = "http://www.w3.org/2003/01/geo/wgs84_pos#";
        String ex = "http://www.example.com/";
        String xsd = "http://www.w3.org/2001/XMLSchema#";
        String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
        String wd = "http://www.wikidata.org/entity/";

        /* Prefix */
        modele.setNsPrefix("ex", ex);
        modele.setNsPrefix( "geo", geo );
        modele.setNsPrefix("xsd", xsd);
        modele.setNsPrefix("rdfs",rdfs);
        modele.setNsPrefix("wd", wd);



        /*   <---------------> */



        Property lat = modele.createProperty(geo + "lat");
        Property lon = modele.createProperty(geo + "lon");

        Resource r;

        Property spatial = modele.createProperty(geo + "SpatialThing");
        Property stationVelo = modele.createProperty(wd + "Q61663696"); //bicyle sharing station



        try {


            //On recupere le contenu de l'URL du JSON des data, l'infos sur les stations
            JSONObject json = new JSONObject(readUrl("https://saint-etienne-gbfs.klervi.net/gbfs/en/station_information.json"));


            int last_updated = (Integer) json.get("last_updated");

            //on recupere le noeud "data"
            JSONObject data = json.getJSONObject("data");


            //on recupere la liste des etats des stations
            JSONArray stations = data.getJSONArray("stations");

            //System.out.println(data.length());
            System.out.println("Il y a " + stations.length() + "stations");
            Ville ville = new Ville();
            ville.setNom("Saint-Etienne");



            List<Station> listeStation = new ArrayList<Station>();
            //List<MyType> myList = new ArrayList<>();


            Station station;

            //objet tempoaire de traitement pour acceder aux infos
            JSONObject traitement;
            for (int i=0; i < stations.length(); i++) {
                station = new Station();
                traitement = stations.getJSONObject(i);
                station.setId((String) traitement.get("station_id"));
                station.setNom((String) traitement.get("name"));
                station.setLatitude((Double) traitement.get("lat"));
                station.setLongitude((Double) traitement.get("lon"));
                station.setCapacite((Integer) traitement.get("capacity"));

                listeStation.add(station);
            }

            ville.setStations(listeStation);


            for(int i=0; i <listeStation.size(); i++){
                System.out.println("STATION <<<>>>  \n" +
                        "ID : "+ listeStation.get(i).getId() + "\n" +
                        "NOM : "+ listeStation.get(i).getNom())
                ;

                r = modele.createResource(ex + listeStation.get(i).getId());
                r.addProperty(lat, listeStation.get(i).getLatitude().toString(), XSDDatatype.XSDdecimal);
                r.addProperty(RDFS.label, listeStation.get(i).getNom(), "fr");
                r.addProperty(lon, listeStation.get(i).getLongitude().toString(), XSDDatatype.XSDdecimal);
                r.addProperty(RDF.type, stationVelo);
                r.addProperty(RDF.type, spatial);

               // r.addProperty()

            }
            System.out.println("LAST UPDATED >>> " + last_updated);

            modele.write(System.out, "Turtle");

            FileOutputStream out =new FileOutputStream(new File("C:\\Users\\madji\\Documents\\M2\\Semantic Web\\Projet\\TEST\\spring-boot\\SemWebProject\\OUT\\out.ttl"));
            modele.write(out,"Turtle");



        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }



        //on met sa sur le fuseki
        accessor.putModel(modele);



        //interrogation de fuseki
        String queryString =
                "PREFIX wdt: <http://www.wikidata.org/prop/direct/> \n" +
                        "PREFIX wd: <http://www.wikidata.org/entity/> \n" +
                        "SELECT ?subject ?predicate ?object WHERE { \n" +
                        "?subject ?predicate ?object\n" +
                        "}\n" ;

        queryFuseki(queryString);

        return "welcome"; //view
    }

    // /hello?name=kotlin
    @GetMapping("/hello")
    public String mainWithParam(
            @RequestParam(name = "name", required = false, defaultValue = "") String name, Model model) {

        model.addAttribute("message", name);

        return "welcome"; //view
    }
}