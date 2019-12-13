package fr.emse.master.controller;

import fr.emse.master.Utils;
import fr.emse.master.model.Disponibilite;
import fr.emse.master.model.Station;
import org.apache.jena.base.Sys;
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
import org.springframework.web.bind.annotation.RequestParam;

import javax.swing.text.Position;
import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static fr.emse.master.Utils.readUrl;


@Controller
public class StationController {

    @GetMapping("/station")
    public String mainWithParam(@RequestParam(required = false) String name
                    ,@RequestParam(required = false) String city, Model model){

        model.addAttribute("cityName", city);
        int veloDispo = 0;
        String idStation;

        if(name.isEmpty()){
            return "erreur";
        }
        else if (city.isEmpty()){
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
                    "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "\n" +
                    "SELECT ?label ?stp ?nomStation ?capacity ?latitude ?longitude ?id\n" +
                    "WHERE {\n" +
                    "  ?object rdfs:locationCity ?label.\n" +
                    "  ?label rdfs:label ?stp.\n" +
                    "  ?object rdfs:label ?nomStation.\n" +
                    "  ?object dbo:capacity ?capacity.\n" +
                    "  ?object geo:lat ?latitude.\n" +
                    "  ?object geo:lon ?longitude.\n" +
                    "  ?object dbo:idNumber ?id.\n" +
                    "  FILTER regex(?stp, \""+city+"\", \"i\")\n" +
                    " FILTER regex(?nomStation, \""+name+"\", \"i\")" +
                    "}";

            String query2 = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX vocab: <http://localhost/>\n" +
                    "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> \n" +
                    "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                    "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "prefix : <urn:ex:>" +
                    "\n" +
                    "SELECT ?label ?stp ?nomStation ?capacity ?latitude ?longitude ?id ?dispoMatin ?dispoMidi ?dispoSoir ?dispoApresMidi ?freq\n" +
                    "WHERE {\n" +
                    "  ?object rdfs:locationCity ?label.\n" +
                    "  ?label rdfs:label ?stp.\n" +
                    "  ?object rdfs:label ?nomStation.\n" +
                    "  ?object dbo:capacity ?capacity.\n" +
                    "  ?object geo:lat ?latitude.\n" +
                    "  ?object geo:lon ?longitude.\n" +
                    "  ?object dbo:idNumber ?id.\n" +
                    "  ?object vocab:frequentation ?freq.\n" +
                    "  FILTER regex(?stp, \""+city+"\", \"i\")\n" +
                    " FILTER regex(?nomStation, \""+name+"\", \"i\")" +
                    "}";

            String query3 = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX vocab: <http://localhost/>\n" +
                    "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                    "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                    "PREFIX voc: <http://voc.odw.tw/>\n" +
                    "Prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "prefix owl: <http://www.w3.org/2002/07/owl#>\n" +
                    "\n" +
                    "prefix : <urn:ex:>\n" +
                    "\n" +
                    "SELECT DISTINCT ?nomStation ?capacity ?latitude ?longitude ?id ?date ?nombre\n" +
                    "WHERE {\n" +
                    "  ?object rdfs:locationCity ?label.\n" +
                    "  ?label rdfs:label ?stp.\n" +
                    "  ?object rdfs:label ?nomStation.\n" +
                    "  ?object dbo:capacity ?capacity.\n" +
                    "  ?object geo:lat ?latitude.\n" +
                    "  ?object geo:lon ?longitude.\n" +
                    "  ?object dbo:id ?id.\n" +
                    "  #?object vocab:hasDispo [:date ?date; :nombre ?nombre].\n" +
                    "  FILTER regex(?stp, \""+city+"\", \"i\")\n" +
                    " FILTER regex(?nomStation, \""+name+"\", \"i\")" +
                    "}";

            List<Station> listeResultatStation = queryFuseki(query3, Utils.server);

            if (listeResultatStation.size()==0)
            {
                //soit le parametre de l'url a ete modifie, soit il y a un problement en base

                return "erreur";
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
            model.addAttribute("listePoint", listePoints);


            //json pour recuperer les velos dispo

            idStation = listeResultatStation.get(0).getId();
            JSONObject json;
            //System.out.println("avant le try");

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

            try {
              //  System.out.println("APRES le try");
                //System.out.println(city);
                if(true){
                  //  System.out.println("dans le IF");
                     json = new JSONObject(readUrl(dispoLink));
                     //https://vannes-gbfs.klervi.net/gbfs/en/station_status.json
                    //https://saint-etienne-gbfs.klervi.net/gbfs/en/station_status.json
                    //on recupere le noeud "data"
                    JSONObject data = json.getJSONObject("data");
                    //on recupere la liste des etats des stations
                    JSONArray stations = data.getJSONArray("stations");

                    JSONObject traitement;

                    for (int i=0; i < stations.length(); i++) {
                        System.out.println("Je suis en train de chercher le bon ID");
                        traitement = stations.getJSONObject(i);
                        String idTemp = traitement.get("station_id").toString();
                        System.out.println("ID recherche = " + idStation);
                        System.out.println("ID actuel  = " + idTemp);
                        String recherche = city.concat(idTemp);
                        if(recherche.equals(idStation)){
                            System.out.println("je suis dedans");
                            veloDispo = Integer.parseInt(traitement.get("num_bikes_available").toString());
                        }
                    }
                    model.addAttribute("veloDispo", veloDispo);
                }
            }catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }



            JSONObject json2;
            try{
                String meteoLink = "http://api.openweathermap.org/data/2.5/weather?lat="+listeResultatStation.get(0).getLatitude()+"&lon="+listeResultatStation.get(0).getLongitude()+"&appid=e18cfcccce94eb6258d4f2ae8d03caee";
                json2 = new JSONObject(readUrl(meteoLink));
                JSONArray weather = json2.getJSONArray("weather");
                JSONObject meteo = weather.getJSONObject(0);
                String etat = meteo.get("main").toString();
                System.out.println("meteo ============>   "+etat);
                Double temperature = (Double.parseDouble(json2.getJSONObject("main").get("temp").toString())-273.15);
                DecimalFormat df = new DecimalFormat("#.##");

                model.addAttribute("temperature",df.format(temperature));
                model.addAttribute("meteo", etat);
            }catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }



        }

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);


        /*
        if(timeOfDay >= 5 && timeOfDay < 12){
            model.addAttribute("periode", "matin");
        }else if(timeOfDay >= 12 && timeOfDay < 17){
            model.addAttribute("periode", "aprem");
        }else if(timeOfDay >= 17 && timeOfDay < 5){
            model.addAttribute("periode", "soir");
        }
        */

        model.addAttribute("nomVille", name);

        LocalDate localDate = LocalDate.now();
        String dateActuelle = DateTimeFormatter.ofPattern("yyy/MM/dd").format(localDate).replace("/","-");
        System.out.println("La date actuelle est : "+dateActuelle);
        System.out.println(dateActuelle);

        Date date = new Date();   // given date
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(date);   // assigns calendar to given date
        int heureActuelle = calendar.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format
        System.out.println("il est : "+heureActuelle);

        String idtest = name.concat(idStation);
        System.out.println(idStation);
        updateFuseki("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX vocab: <http://localhost/>\n" +
                "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                "Prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "\n" +
                "prefix : <urn:ex:>\n" +
                "\n" +
                "INSERT DATA{\n" +
                "  <http://ex.com/"+idStation+"> vocab:hasDispo [ :date    \""+dateActuelle+"\"^^xsd:date ;\n" +
                "                               :nombre  "+veloDispo+"\n; :heure "+heureActuelle +
                "                             ] .\n" +
                "}",Utils.server);


        List<Disponibilite> listeDispo = getListDispo("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX vocab: <http://localhost/>\n" +
                "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
                "PREFIX dbo: <http://dbpedia.org/ontology/>\n" +
                "Prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "\n" +
                "prefix : <urn:ex:>\n" +
                "\n" +
                "SELECT DISTINCT ?nomStation ?capacity ?id ?date ?nombre ?heure\n" +
                "WHERE {\n" +
                "  ?object rdfs:locationCity ?label.\n" +
                "  ?label rdfs:label ?stp.\n" +
                "  ?object rdfs:label ?nomStation.\n" +
                "  ?object dbo:id ?id.\n" +
                "  ?object vocab:hasDispo [:date ?date; :nombre ?nombre; :heure ?heure].\n" +
                "  FILTER regex(?stp, \""+city+"\", \"i\").\n" +
                "  FILTER regex(?nomStation, \""+name+"\", \"i\").\n" +
                "}" +
                "ORDER BY asc (?date) ASC (?heure)",Utils.server);


        //calcul de la moyenne
        double moyenne = 0;
        for(int i=0;i<listeDispo.size();i++){
            moyenne += listeDispo.get(i).getNombre();
        }
        moyenne = moyenne/listeDispo.size();

        model.addAttribute("listeDispo",listeDispo);
        System.out.println(listeDispo.get(0).getDate());

        model.addAttribute("avgDispo",moyenne);
        model.addAttribute("basedOn", listeDispo.size());





        /*
        proxi
         */
/*

        JSONObject jsonProxi;
        try{
            String q ="SELECT ?place ?placeLabel ?location ?dist ?instance_ofLabel ?image" +
                    "WHERE { " +
                    "  SERVICE wikibase:around { " +
                    "      ?place wdt:P625 ?location . " +
                    "      bd:serviceParam wikibase:center \"Point(4.4021442 45.4147359)\"^^geo:wktLiteral. " +
                    "      bd:serviceParam wikibase:radius \"0.9\" . " +
                    "      bd:serviceParam wikibase:distance ?dist." +
                    "  } " +
                    "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". } " +
                    "    OPTIONAL { ?place wdt:P18 ?image. }" +
                    "  OPTIONAL { ?place wdt:P31 ?instance_of. }" +
                    "} " +
                    "ORDER BY ASC(?dist)";

            String t="SELECT%20?place%20?placeLabel%20?location%20?dist%20?instance_ofLabel%20?image%20WHERE%20{SERVICE%20wikibase:around%20{?place%20wdt:P625%20?location%20.bd:serviceParam%20wikibase:center%20%22Point(4.4021442%2045.4147359)%22^^geo:wktLiteral.%20bd:serviceParam%20wikibase:radius%20%220.5%22%20.bd:serviceParam%20wikibase:distance%20?dist.}SERVICE%20wikibase:label%20{%20bd:serviceParam%20wikibase:language%20%22en%22.%20}OPTIONAL%20{%20?place%20wdt:P18%20?image.%20}OPTIONAL%20{%20?place%20wdt:P31%20?instance_of.%20}}ORDER%20BY%20ASC(?dist)";
            String proxiLink = "https://query.wikidata.org/sparql?query="+t+"&format=json&origin=*";
            jsonProxi = new JSONObject(readUrl(proxiLink));
            JSONObject result = jsonProxi.getJSONObject("results");

            JSONArray binding = result.getJSONArray("bindings");
            for(int taille=0;taille<binding.length();taille++){
                System.out.println(binding.get(taille));
            }


        }catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

 */

//test();
        return "station2"; //view
    }

    public void test()
    {
        org.apache.jena.rdf.model.Model model = FileManager.get().loadModel("https://query.wikidata.org/sparql", "RDF");
        Query query = QueryFactory.create("PREFIX wd: <http://www.wikidata.org/entity/>\n" +
                "PREFIX wds: <http://www.wikidata.org/entity/statement/>\n" +
                "PREFIX wdv: <http://www.wikidata.org/value/>\n" +
                "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
                "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
                "PREFIX p: <http://www.wikidata.org/prop/>\n" +
                "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n" +
                "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX bd: <http://www.bigdata.com/rdf#>" +
                "PREFIX geo: <http://www.opengis.net/ont/geosparql#>" +
                "SELECT ?place ?placeLabel ?location ?dist ?instance_ofLabel ?image \n" +
                "WHERE { \n" +
                "  SERVICE wikibase:around { \n" +
                "      ?place wdt:P625 ?location . \n" +
                "      bd:serviceParam wikibase:center \"Point(4.4021442 45.4147359)\"^^geo:wktLiteral. \n" +
                "      bd:serviceParam wikibase:radius \"0.9\" . \n" +
                "      bd:serviceParam wikibase:distance ?dist.\n" +
                "  } \n" +
                "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". } \n" +
                "    OPTIONAL { ?place wdt:P18 ?image. }\n" +
                "  OPTIONAL { ?place wdt:P31 ?instance_of. }\n" +
                "} \n" +
                "ORDER BY ASC(?dist)");
        QueryExecution qexec = QueryExecutionFactory.create(query,  model);
        ResultSet results = qexec.execSelect();

        while(results.hasNext()) {
            System.out.println("OK");
        }
    }
    public List<Disponibilite> getListDispo(String queryString, String url){
        //connection
        org.apache.jena.rdf.model.Model model = FileManager.get().loadModel(url, "TTL");
        List<Disponibilite> listeDesDispo = new ArrayList<>();
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query,  model);
        ResultSet results = qexec.execSelect();
        if(results.hasNext()) {
            System.out.println("has results!");
        }
        else {
            System.out.println("No Results!");
        }


        while(results.hasNext()) {

            Disponibilite temp = new Disponibilite();
            String temporaire;
            int tempo;
            double Double;
            QuerySolution soln = results.nextSolution();
            Literal r = soln.getLiteral("meshId");

            System.out.println(soln.get("date").toString());
            System.out.println(soln.get("date").toString().split("\\^")[0]);
            temporaire = soln.get("date").toString().split("\\^")[0];
            temp.setDate(temporaire);

            tempo = Integer.parseInt(soln.get("heure").toString().split("\\^")[0]);
            temp.setHeure(tempo);

            tempo = Integer.parseInt(soln.get("nombre").toString().split("\\^")[0]);
            temp.setNombre(tempo);


            listeDesDispo.add(temp);
            System.out.println(soln);

        }





        return listeDesDispo;
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
        QueryExecution qexec = QueryExecutionFactory.create(query,  model);

        ResultSet results = qexec.execSelect();
        if(results.hasNext()) {
            System.out.println("has results!");
        }
        else {
            System.out.println("No Results!");
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




            tempo = Integer.parseInt(soln.get("capacity").toString());
            temp.setCapacite(tempo);


            temporaire = soln.get("latitude").toString();
            temporaire = temporaire.split("e")[0];
            temporaire = temporaire.split("\\^")[0];
            Double = java.lang.Double.parseDouble(temporaire);
            temp.setLatitude(Double);


            temporaire = soln.get("longitude").toString();
            temporaire = temporaire.split("e")[0];
            temporaire = temporaire.split("\\^")[0];
            Double = java.lang.Double.parseDouble(temporaire);
            temp.setLongitude(Double);

            temporaire = soln.get("id").toString();
            temp.setId(temporaire);


            /*
            System.out.println(soln.get("dispoMatin").toString());
            System.out.println(soln.get("dispoMatin").toString().split("\\^")[0]);
            tempo = Integer.parseInt(soln.get("dispoMatin").toString().split("\\^")[0]);

            temp.setDispoMatin(tempo);

            tempo = Integer.parseInt(soln.get("dispoSoir").toString().split("\\^")[0]);
            temp.setDispoSoir(tempo);

            tempo = Integer.parseInt(soln.get("dispoApresMidi").toString().split("\\^")[0]);
            temp.setDispoAprem(tempo);

*/
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
