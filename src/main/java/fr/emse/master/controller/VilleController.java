package fr.emse.master.controller;

import fr.emse.master.Utils;
import fr.emse.master.model.Station;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.util.FileManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.swing.text.Position;
import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static fr.emse.master.Utils.readUrl;


@Controller
public class VilleController {

    @GetMapping("/ville")
    public String mainWithParam(
            @RequestParam(name = "name", required = false, defaultValue = "") String name, Model model) throws Exception {

        Double lat, lon;
        if(name.isEmpty()){
            return "erreur";
        }
        else
        {
            //la ville n'est pas nulle, donc on cherche les stations

            String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX vocab: <http://localhost/>\n" +
                    "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> \n" +
                    "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                    "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "\n" +
                    "SELECT ?label ?stp ?nomStation ?capacity ?latitude ?longitude ?id\n" +
                    "WHERE {\n" +
                    "  ?object rdfs:locationCity ?label.\n" +
                    "  ?label rdfs:label ?stp.\n" +
                    "  ?object rdfs:label ?nomStation.\n" +
                    "  ?object dbo:capacity ?capacity.\n" +
                    "  ?object geo:lat ?latitude.\n" +
                    "  ?object geo:lon ?longitude.\n" +
                    "  ?object dbo:id ?id.\n" +
                    "  FILTER regex(?stp, \""+name+"\", \"i\")\n" +
                    "}";


            List<Station> listeResultatStation = queryFuseki(query, Utils.server);

            if (listeResultatStation.size()==0)
            {
                //soit le parametre de l'url a ete modifie, soit il y a un problement en base
                return "villeInconnue";
            }


            String city=name;
            String dispoLink = getDispoLink("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX vocab: <http://localhost/>\n" +
                    "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                    "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                    "PREFIX dbr: <http://dbpedia.org/resource/>\n" +
                    "Prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "\n" +
                    "prefix : <urn:ex:>\n" +
                    "\n" +
                    "SELECT ?link WHERE {\n" +
                    "  dbr:"+city+" vocab:hasDispoLink ?link.\n" +
                    "}\n" +
                    "    ",Utils.server);





            String idStation;
            JSONObject json;
            //  System.out.println("dans le IF");
            json = new JSONObject(readUrl(dispoLink));
            //https://vannes-gbfs.klervi.net/gbfs/en/station_status.json
            //https://saint-etienne-gbfs.klervi.net/gbfs/en/station_status.json
            //on recupere le noeud "data"
            JSONObject data = json.getJSONObject("data");
            //on recupere la liste des etats des stations
            JSONArray stations = data.getJSONArray("stations");
            JSONObject traitement;
            for(int i=0; i<listeResultatStation.size();i++){
                String idStationTmp = listeResultatStation.get(i).getId();

                int veloDispo = 0;
                try {
                    //  System.out.println("APRES le try");
                    //System.out.println(city);
                    if(true){


                        if(listeResultatStation.size()<30) {
                            for (int j = 0; j < stations.length(); j++) {
                                System.out.println("Je suis en train de chercher le bon ID");
                                traitement = stations.getJSONObject(j);
                                String idTemp = traitement.get("station_id").toString();
                                System.out.println("ID recherche = " + idStationTmp);
                                System.out.println("ID actuel  = " + idTemp);
                                String recherche = city.concat(idTemp);
                                if (recherche.equals(idStationTmp)) {
                                    System.out.println("je suis dedans");
                                    veloDispo = Integer.parseInt(traitement.get("num_bikes_available").toString());
                                    listeResultatStation.get(i).setNb_velo_disp(veloDispo);
                                    System.out.println("==========================je sors");
                                    break;
                                }
                            }
                        }


                    }
                }catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            model.addAttribute("results", listeResultatStation);
            //la liste des latitudes et longitudes :
            List<Point2D> listePoints = new ArrayList<>();



            for(int i = 0 ; i<listeResultatStation.size(); i++)
            {
                Double x = listeResultatStation.get(i).getLatitude();
                Double y = listeResultatStation.get(i).getLongitude();
                Point2D point = new Point2D.Double(x,y);
                listePoints.add(point);
            }


            lat = listePoints.get(0).getX();
            lon = listePoints.get(0).getY();



                    model.addAttribute("listePoint", listePoints);

        }

        model.addAttribute("nomVille", name);

        //get meteo of city

        JSONObject json;
        try{
            String meteoLink = "http://api.openweathermap.org/data/2.5/weather?lat="+lat+"&lon="+lon+"&appid=e18cfcccce94eb6258d4f2ae8d03caee";
            json = new JSONObject(readUrl(meteoLink));
            JSONArray weather = json.getJSONArray("weather");
            JSONObject meteo = weather.getJSONObject(0);
            String etat = meteo.get("main").toString();
            System.out.println("meteo ============>   "+etat);
            Double temperature = (Double.parseDouble(json.getJSONObject("main").get("temp").toString())-273.15);
            DecimalFormat df = new DecimalFormat("#.##");

            model.addAttribute("temperature",df.format(temperature));
            model.addAttribute("meteo", etat);
        }catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }





        return "ville"; //view
    }



    /**
     * Permet d'interroger http://localhost:3030/Test avec une requete SPARQL
     * @param queryString
     * @return la liste des résultats
     */
    public List<Station> queryFuseki(String queryString, String url){
        //connection
        org.apache.jena.rdf.model.Model model = FileManager.get().loadModel(url, "TTL");

        List<Station> listeResultatStation = new ArrayList<>();

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);

        ResultSet results = qexec.execSelect();

        System.out.println(queryString);
        if(results.hasNext()) {
            System.out.println("has results!");
        }
        else {
            System.out.println("Aucun Results!");
        }


        while(results.hasNext()) {
            Station temp = new Station();
            String temporaire;
            int tempo;
            double Double;
            QuerySolution soln = results.nextSolution();
            Literal r = soln.getLiteral("meshId");

            // on traite le nom de la station

            temporaire = soln.get("nomStation").toString();
            temporaire = temporaire.split("@")[0];
            temp.setNom(temporaire);




            try{
                tempo = Integer.parseInt(soln.get("capacity").toString());
                temp.setCapacite(tempo);
            }catch (Exception e){
                temp.setCapacite(0);
            }



            temporaire = soln.get("latitude").toString();
            temporaire = temporaire.split("e")[0];
            Double = java.lang.Double.parseDouble(temporaire);
            temp.setLatitude(Double);


            temporaire = soln.get("longitude").toString();
            temporaire = temporaire.split("e")[0];
            temporaire = temporaire.split("\\^")[0];
            System.out.println(temporaire);
            Double = java.lang.Double.parseDouble(temporaire);
            temp.setLongitude(Double);

            temporaire = soln.get("id").toString();
            temp.setId(temporaire);

            listeResultatStation.add(temp);
            System.out.println(soln);
        }

        return listeResultatStation;
    }

    public String getDispoLink(String queryString, String url){
        //connection
        org.apache.jena.rdf.model.Model model = FileManager.get().loadModel(url, "TTL");

        List<Station> listeResultatStation = new ArrayList<>();

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query,  model);

        ResultSet results = qexec.execSelect();
        if(results.hasNext()) {
            System.out.println("has results!");
        }
        else {
            System.out.println("No Results!");
        }

        String temporaire ="";

        while(results.hasNext()) {
            Station temp = new Station();

            int tempo;
            double Double;
            QuerySolution soln = results.nextSolution();
            Literal r = soln.getLiteral("meshId");

            // on traite le nom de la station

            temporaire = soln.get("link").toString();

        }
        return temporaire;
    }
}
