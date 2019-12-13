package fr.emse.master.controller;

import fr.emse.master.Utils;
import fr.emse.master.model.Disponibilite;
import fr.emse.master.model.Station;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.swing.text.Position;
import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static fr.emse.master.Utils.readUrl;


@Controller
public class AddController {

    @GetMapping("/add")
    public String main(Model model){


        return "add";
    }

    @PostMapping("/add")
    public String postMain(@RequestParam("cityName") String cityName
            ,@RequestParam("link") String link,
                           @RequestParam("linkDispo") String linkDispo, Model model){
        System.out.println(cityName +"   fvfg    "+ link + " dispo link "+linkDispo);

        //on a le nom de la ville dans cityName et le lien dans link

        //on ajoute la ville
        String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX vocab: <http://localhost/>\n" +
                "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                "Prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                "prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "prefix dbr: <http://dbpedia.org/resource/>" +
                "prefix schema: <http://schema.org/>" +
                "\n" +
                "INSERT DATA{\n" +
                "dbr:"+cityName+"  a schema:City ;\n" +
                " rdfs:label  \""+cityName+"\"@fr ." +
                "}";
        updateFuseki(query, Utils.server);

        System.out.println("La ville : " + cityName+" a été ajoutée");

        String queryDispoLink = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX vocab: <http://localhost/>\n" +
                "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                "PREFIX voc: <http://voc.odw.tw/>\n" +
                "PREFIX dbr: <http://dbpedia.org/resource/>\n" +
                "Prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "\n" +
                "prefix : <urn:ex:>\n" +
                "\n" +
                "INSERT DATA {\n" +
                "dbr:"+cityName+" vocab:hasDispoLink \""+linkDispo+"\".\n" +
                "}\n" +
                "\n" +
                "    ";
        updateFuseki(queryDispoLink,Utils.server);
        System.out.println("Lien disponibilité ajoutées");
        JSONObject json;
        String queryStation = "";
        try {
                //  System.out.println("dans le IF");
                json = new JSONObject(readUrl(link));
                //on recupere le noeud "data"
                JSONObject data = json.getJSONObject("data");
                //on recupere la liste des etats des stations
                JSONArray stations = data.getJSONArray("stations");
                JSONObject traitement;
                //POUR CHAQUE STATIONS
             queryStation = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX vocab: <http://localhost/>\n" +
                    "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                    "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                    "Prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                    "prefix dbr: <http://dbpedia.org/resource/>" +
                    "prefix schema: <http://schema.org/>" +
                     "INSERT DATA{\n" +
                    "\n";
                for (int i=0; i < stations.length(); i++) {
                    System.out.println("Etape : "+i);
                    traitement = stations.getJSONObject(i);
                    String idStation = traitement.get("station_id").toString();
                    String nameStation = traitement.get("name").toString();
                    String latStation = traitement.get("lat").toString();
                    String lonStation = traitement.get("lon").toString();
                    String capacityStation = traitement.get("capacity").toString();
                    queryStation = queryStation.concat(
                            "\n" +
                                    "\n"+"" +
                            "<http://ex.com/"+cityName+idStation+">  a       vocab:station ;\n" +
                            "        rdfs:label           \""+nameStation+"\"@fr ;\n" +
                            "        rdfs:locationCity    dbr:"+cityName+" ;\n" +
                            "        dbo:id         \""+cityName+idStation+"\" ;\n" +
                            "        dbo:capacity       \""+capacityStation+"\" ;\n" +
                            "        geo:lat              \""+latStation+"\" ;\n" +
                            "        geo:lon              "+lonStation+" ." +
                            "\n");

                }
        }catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        queryStation = queryStation.concat("}");
        updateFuseki(queryStation,Utils.server);
        return "success";
    }

    /**
     * update the fuseki server with query
     * @param query
     * @param url
     */
    public void updateFuseki(String query, String url){
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create()
                .destination(url)
                .queryEndpoint("sparql")
                .updateEndpoint("update")
                .gspEndpoint("get");

        UpdateRequest update = UpdateFactory.create(query);
        UpdateProcessor processorUpdate = UpdateExecutionFactory.createRemote(update, Utils.server+"/update");
        processorUpdate.execute();
    }
}
