package fr.emse.master;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.util.FileManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class Utils {



    /**
     * Permet d'interroger http://localhost:3030/Test avec une requete SPARQL
     * @param queryString
     * @return la liste des résultats
     */
    public static ResultSet queryFuseki(String queryString){
        //connection
        org.apache.jena.rdf.model.Model model = FileManager.get().loadModel("http://localhost:3030/Test", "TTL");
        /*
            String queryString =
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/> \n" +
                    "PREFIX wd: <http://www.wikidata.org/entity/> \n" +
                    "SELECT ?subject ?predicate ?object WHERE { \n" +
                    "?subject ?predicate ?object\n" +
                    "}\n" ;
         */
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);

        ResultSet results = qexec.execSelect();
        if(results.hasNext()) {
            System.out.println("has results!");
        }
        else {
            System.out.println("No Results!");
        }

        while(results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            Literal r = soln.getLiteral("meshId");
            System.out.println(soln);
        }

        return results;
    }




    /**
     * Met le contenu de l'URL dans une variable String
     * SOURCE : https://stackoverflow.com/questions/7467568/parsing-json-from-url
     * @param urlString URL que l'on souhaite avoir
     * @return Contenu de l'URL dans un String
     * @throws Exception
     */
    public static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

}
