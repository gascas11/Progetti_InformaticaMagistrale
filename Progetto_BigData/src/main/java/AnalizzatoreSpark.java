import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.bson.Document;

import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
//script spark che legge i dati dal database usando gli RDD
public class AnalizzatoreSpark {
    private String accountId;
    private String type;
    private String masterKey;
    private SparkSession spark;

    public AnalizzatoreSpark(String id, String type, String masterKey) {
        this.accountId = id;
        this.type = type;
        this.masterKey = masterKey;
        //rimuovo i print di warnings e info sulla console
        Logger.getLogger("org").setLevel(Level.ERROR);
        Logger.getLogger("akka").setLevel(Level.ERROR);

        //creo la sessione spark
        spark = SparkSession.builder()
                .master("local")
                .appName("MongoSparkConnectorIntro")
                .config("spark.mongodb.input.uri", "mongodb://"+type+":"+type+"@localhost:27017/myproject.anagrafiche?authSource=myproject")
                .getOrCreate();
    }

    //metodo che analizza i dati del database, in base al tipo di account
    public String analize() {
        String result = "failed";

        //codec per la formattazione dei JSON
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry());
        final DocumentCodec codec = new DocumentCodec(codecRegistry, new BsonTypeClassMap());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        //spark context e creazione dell'rdd
        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
        JavaMongoRDD<Document> rdd = MongoSpark.load(jsc);

        //inizializzo la query per ricavare i dati
        JavaMongoRDD<Document> queryRDD = null;

        if(type.equals("paziente")) {
            //se è un paziente visualizzo tutti i suoi dati (anagrafica, stile di vita, patologie, account
            //aggregazione per filtrare tra tutti i pazienti quello dell'account
            Document match = Document.parse("{ " +
                    "   $match: { " +
                    "       account: { " +
                    "           $oid : \"" + accountId + "\" " +
                    "       } " +
                    "   } " +
                    "}");

            //'join' tra l'anagrafica e lo stile di vita
            Document lookupLifestyle = Document.parse("{ " +
                    "   $lookup: { " +
                    "       from:\"comportamenti\", " +
                    "       localField:\"comportamento\", " +
                    "       foreignField:\"_id\", " +
                    "       as:\"comportamentiJoin\" " +
                    "   } " +
                    "} ");

            //'join' tra l'anagrafica e le patologie
            Document lookupClinical = Document.parse("{ " +
                    "   $lookup: { " +
                    "       from:\"clinici\", " +
                    "       localField:\"patologie.id\", " +
                    "       foreignField:\"_id\"," +
                    "       as:\"patologieJoin\" " +
                    "   }" +
                    "}");

            //'join' tra l'anagrafica e l'account
            Document lookupAccount = Document.parse("{ " +
                    "   $lookup: { " +
                    "       from:\"utenti\", " +
                    "       localField:\"account\", " +
                    "       foreignField:\"_id\"," +
                    "       as:\"Account\" " +
                    "   }" +
                    "}");

            //aggregazione che pulisce tutti i dati e visualizza solo quelli voluti
            Document project = Document.parse("{ " +
                    "   $project: { " +
                    "       _id: 0" +
                    "       nome : 1, " +
                    "       cognome : 1, " +
                    "       genere : 1, " +
                    "       data_nascita : 1, " +
                    "       codice_fiscale : 1, " +
                    "       indirizzo : 1, " +
                    "       comportamentiJoin : 1, " +
                    "       patologieJoin : 1, " +
                    "       Account : 1, " +
                    "   }" +
                    "}");

            //esecuzione della query
            queryRDD = rdd.withPipeline(Arrays.asList(match, lookupLifestyle, lookupClinical, lookupAccount, project));
        }else  if(type.equals("medico")){
            //se è un medico visualizzo tutti i dati di tutti i suoi pazienti, tranne l'account (anagrafica, stile di vita, patologie, account
            //aggregazione per filtrare tra tutti i pazienti quello dell'account
            Document match = Document.parse("{ " +
                    "   $match: { " +
                    "       _id: { " +
                    "           $oid : \""+accountId+"\" " +
                    "       } " +
                    "   } " +
                    "}");

            //'join' tra l'anagrafica e lo stile di vita
            Document lookupLifestyle = Document.parse("{ " +
                    "   $lookup: { " +
                    "       from:\"comportamenti\", " +
                    "       localField:\"comportamento\", " +
                    "       foreignField:\"_id\", " +
                    "       as:\"comportamentiJoin\" " +
                    "   } " +
                    "} ");

            //'join' tra l'anagrafica e le patologie
            Document lookupClinical = Document.parse("{ " +
                    "   $lookup: { " +
                    "       from:\"clinici\", " +
                    "       localField:\"patologie.id\", " +
                    "       foreignField:\"_id\"," +
                    "       as:\"patologieJoin\" " +
                    "   }" +
                    "}");

            //aggregazione che pulisce tutti i dati e visualizza solo quelli voluti
            Document project = Document.parse("{ " +
                    "   $project: { " +
                    "       _id: 0" +
                    "       nome : 1, " +
                    "       cognome : 1, " +
                    "       genere : 1, " +
                    "       data_nascita : 1, " +
                    "       codice_fiscale : 1, " +
                    "       indirizzo : 1, " +
                    "       comportamentiJoin : 1, " +
                    "       patologieJoin : 1, " +
                    "   }" +
                    "}");

            //esecuzione della query
            queryRDD = rdd.withPipeline(Arrays.asList(match, lookupLifestyle, lookupClinical, project));
        }else if(this.type.equals("amministrazione")){
            //aggregazione per filtrare tra tutti i pazienti quello dell'account
            Document match = Document.parse("{ " +
                    "   $match: { " +
                    "       _id: { " +
                    "           $oid : \""+accountId+"\" " +
                    "       } " +
                    "   } " +
                    "}");

            //'join' tra l'anagrafica e l'account
            Document lookupAccount = Document.parse("{ " +
                    "   $lookup: { " +
                    "       from:\"utenti\", " +
                    "       localField:\"account\", " +
                    "       foreignField:\"_id\"," +
                    "       as:\"Account\" " +
                    "   }" +
                    "}");

            //aggregazione che pulisce tutti i dati e visualizza solo quelli voluti
            Document project = Document.parse("{ " +
                    "   $project: { " +
                    "       _id: 0" +
                    "       nome : 1, " +
                    "       cognome : 1, " +
                    "       genere : 1, " +
                    "       data_nascita : 1, " +
                    "       codice_fiscale : 1, " +
                    "       indirizzo : 1, " +
                    "       Account : 1, " +
                    "   }" +
                    "}");

            //esecuzione della query
            queryRDD = rdd.withPipeline(Arrays.asList(match, lookupAccount, project));
        }

        if(queryRDD != null) {
            //prendo il risultato della query
            Document finaldoc = queryRDD.first();
            //lo converto in json 'formattato'
            JsonElement je = JsonParser.parseString(decrypt(finaldoc).toJson(codec));
            result = gson.toJson(je);
        }

        //choido il context
        jsc.close();

        //restituisco la stringa contenente il json
        return result;
    }

    //metodo che dato un documento criptato lo decripta
    private Document decrypt(Document encryptedDoc) {
        JSONObject doc = new JSONObject(encryptedDoc.toJson());
        try {
            //creo il cifratore con la chiave dell'account
            Cripto chiper = new Cripto(this.masterKey);

            //sostituisco tutti i dati del documento con gli stessi ma decriptati
            doc.put("nome", chiper.decrypt(doc.getString("nome")));
            doc.put("cognome", chiper.decrypt(doc.getString("cognome")));
            doc.put("data_nascita", chiper.decrypt(doc.getString("data_nascita")));
            doc.put("codice_fiscale", chiper.decrypt(doc.getString("codice_fiscale")));
            doc.put("genere", chiper.decrypt(doc.getString("genere")));

            JSONObject address = doc.getJSONObject("indirizzo");
            address.put("via", chiper.decrypt(address.getString("via")));
            address.put("CAP", chiper.decrypt(address.getString("CAP")));
            address.put("citta", chiper.decrypt(address.getString("citta")));
            doc.put("indirizzo", address);

            if(this.type.equals("medico") || this.type.equals("paziente")) {
                JSONArray patologie = doc.getJSONArray("patologieJoin");
                int dim = patologie.length();
                for (int i = 0; i < dim; i++) {
                    JSONObject patologia = patologie.getJSONObject(i);
                    patologia.put("nome", chiper.decrypt(patologia.getString("nome")));
                }
                doc.put("patologieJoin", patologie);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Document.parse(doc.toString());
    }
}
