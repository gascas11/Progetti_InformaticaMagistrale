import java.util.*;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.github.javafaker.Address;
import org.apache.commons.lang3.ArrayUtils;
import org.bson.Document;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
//libreria usata per generare alcuni dati, altri sono stati presi da file in /resources
import com.github.javafaker.Faker;
import org.bson.types.ObjectId;

//classe usata per connettersi al database, contiene inoltre i metodi per riempire la base di dati
public class DBUtils {
    private MongoClient client;
    private Cripto cipher;
    private Faker faker;

    public DBUtils(String host, int port, String username, String password, String source) throws Exception {
        client = new MongoClient(new MongoClientURI( "mongodb://"+username+":"+password+"@"+host+":"+port+"/?authSource="+source+"" ));
        faker = new Faker(new Locale("it"));
    }

    //metodo che genera personale amministrativo e medici
    public void fillDoctorsAndAmministrative(int n, String type) {
        MongoDatabase database = client.getDatabase("myproject");
        MongoCollection<Document> collUtenti = database.getCollection("utenti");

        for(int i = 0; i < n; i++) {
            try {
                //creo la loro masterkey
                cipher = new Cripto(UUID.randomUUID().toString().replace("-",""));

                //creo i dati anagrafici (l'username è ricavato dal nome)
                Document info = getPrivateInfoPerson();
                //creo l'account a partire dai dati anagrafici
                Document user = getUserAccount(info, i, type);
                //inserisco l'account
                collUtenti.insertOne(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //metodo che genera i pazienti
    public void fillData(int n){
        MongoDatabase database = client.getDatabase("myproject");

        MongoCollection<Document> collAnagrafica = database.getCollection("anagrafiche");
        MongoCollection<Document> collComportamento = database.getCollection("comportamenti");
        MongoCollection<Document> collClinici = database.getCollection("clinici");
        MongoCollection<Document> collUtenti = database.getCollection("utenti");

        for(int i = 0; i < n; i++) {
            try {
                //creo la masterkey
                cipher = new Cripto(UUID.randomUUID().toString().replace("-",""));

                //genero i dati anagrafici
                Document info = getPrivateInfoPerson();
                //genero i dati dell'account
                Document user = getUserAccount(info, i, "paziente");
                //inserisco l'account nella collezione utenti
                collUtenti.insertOne(user);
                //ricavo la chiave del nuovo elemento
                //(da inserire successivamente nell collezione anagrafiche per effettuare il join)
                ObjectId IDUser = (ObjectId) user.get( "_id" );

                //genero i dati dello stile di vita
                Document lifeStyle = getLifeStyle();
                //inserisco  nella collezione comportamenti
                collComportamento.insertOne(lifeStyle);
                //ricavo la chiave del nuovo elemento
                //(da inserire successivamente nell collezione anagrafiche per effettuare il join)
                ObjectId IDLifeStyle = (ObjectId) lifeStyle.get( "_id" );

                //calcolo l'età dalla data di nascita
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                String datanascita = cipher.decrypt(info.getString("data_nascita")).substring(6,10);
                int bithYear = Integer.parseInt(datanascita);
                //genero degli anni a partire dall'età, per ogni anno creerò una patologia
                int[] years = getRandomYears(currentYear-bithYear);
                List<Document> pathologies=new ArrayList<Document>();
                for (int year : years) {
                    Document pathology = new Document();
                    //genero i dati della patologia
                    pathology.append("nome", getRandomString("src/main/resources/patologie.txt", true))
                            .append("data_diagnosi", year);
                    //inserisco  nella collezione clinici
                    collClinici.insertOne(pathology);
                    //ricavo la chiave del nuovo elemento
                    //(da inserire successivamente nell collezione anagrafiche per effettuare il join)
                    ObjectId IDPathology = (ObjectId) pathology.get( "_id" );
                    pathologies.add(new Document("id", IDPathology));
                }

                //inserisco tutti riferimenti esterni nel documento dell'anagrafica
                info.append("patologie", pathologies);
                info.append("comportamento", IDLifeStyle);
                info.append("account", IDUser);

                //inserisco l'anagrafica
                collAnagrafica.insertOne(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //metodo che genera uno stile di vita
    private Document getLifeStyle(){
        return new Document()
                .append("Sport", getRandomString("src/main/resources/sport.txt", false))
                .append("Alimentazione", getRandomString("src/main/resources/alimentazioni.txt", false))
                .append("Lavoro", getRandomString("src/main/resources/lavori.txt", false));
    }

    //metodo che dai dati anagrafici crea un account (da usare nel login)
    private Document getUserAccount(Document info, int i, String type) {
        Document account = null;
        if(type.equals("paziente")) {
            //se è un paziente avrà la chiave per criptare/decriptare i dati
            account = new Document()
                    .append("username", Stringify(cipher.decrypt(info.getString("nome")), i))
                    .append("password", Cripto.SHA256(Stringify(cipher.decrypt(info.getString("nome")), i)))
                    .append("tipologia", type)
                    .append("masterkey", cipher.getEncryptionKey());
        }else if(type.equals("medico") || type.equals("amministrazione")){
            //altrimenti no, perché non sono presenti dati sensibili dei medici e del personale
            account = new Document()
                    .append("username", Stringify(cipher.decrypt(info.getString("nome")), i))
                    .append("password", Cripto.SHA256(Stringify(cipher.decrypt(info.getString("nome")), i)))
                    .append("tipologia", type);
        }
        return account;
    }

    //metodo che restiuisce il nome dell'account
    private String Stringify(String s, int i) {
        return s.toLowerCase()+i;
    }

    //metodo che genera i dati anagrafici di un paziente
    private Document getPrivateInfoPerson(){
        Random rd = new Random();
        //genero il sesso
        boolean gender = rd.nextBoolean();
        //in base al sesso carico il set di dati maschili/femminili da cui generare il nome
        String pathName = gender ? "src/main/resources/nomi_maschili": "src/main/resources/nomi_femminili.txt";
        String name = getRandomString(pathName, true);
        //genero il cognome
        String lastName = faker.name().lastName();

        //genero il documento cruiptando i dati
        Document info = new Document()
                .append("nome", name)
                .append("cognome", cipher.encrypt(lastName))
                .append("genere", gender ? cipher.encrypt("M") : cipher.encrypt("F"))
                .append("data_nascita", cipher.encrypt(getRandomDate()))
                .append("codice_fiscale", cipher.encrypt("AAABBB99C99DDDDB"));

        //genero un indirizzo
        info.put("indirizzo", getAddress());
        return info;
    }

    //metodo che genera un indirizzo casuale
    private Document getAddress(){
        Address a = faker.address();
        return new Document("via", cipher.encrypt(a.streetName()+", "+a.streetAddressNumber()))
                .append("CAP", cipher.encrypt(a.zipCode()))
                .append("citta", cipher.encrypt(a.cityName()));
    }

    //metodo che dato un path prende una linea casuale del file (che conterrà un nome, patologia, sport, etc)
    private String getRandomString(String path, boolean crypto){
        RandomAccessFile rf;
        try {
            //apro il file
            rf = new RandomAccessFile(path, "r");
            //calcolo una posizione casuale al suo interno
            long len = rf.length();
            int pos = (int) Math.floor((Math.random() * (len-10)));
            rf.seek(pos);
            //leggo la linea
            rf.readLine();
            if(crypto) return cipher.encrypt(rf.readLine());
            return rf.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "non definito";
        }
    }

    //metodo che data un'età genera un numero casuale di anni (usati nel generare le patologie)
    private int[] getRandomYears(int age){
        int n_years = (int) (Math.random() * 7) + 1;
        int[] years = new int[n_years];
        for(int i = 0; i < years.length; i++){
            years[i] = (int) (Math.random() * (age)) + (2020-age);
        }
        Arrays.sort(years);
        ArrayUtils.reverse(years);
        return years;
    }

    //metodo che genera una data tra il 1950 e il 2020
    public String getRandomDate() {
        int day = (int) (Math.random() * 31) + 1;
        int month = (int) (Math.random() * 12) + 1;
        int year = (int) (Math.random() * 70) + 1950;

        return String.format("%02d", day)+"/"+String.format("%02d", month)+"/"+year;
    }

    public void close() {
        client.close();
    }

    public MongoClient getClient(){
        return client;
    }
}
