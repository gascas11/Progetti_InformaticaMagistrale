import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

public class HomeGUI extends JFrame{
    private String username;
    private String id;
    private String type;
    private String masterkey;
    private JTextArea txtAreaInfo;
    private JComboBox<ComboPatient> cmbPazienti;
    private ArrayList<ComboPatient> pazienti;
    private AnalizzatoreSpark sa;

    public HomeGUI(String username, String id, String type, String masterkey) {
        this.username = username;
        this.id = id;
        this.type = type;
        this.masterkey = Objects.requireNonNullElse(masterkey, "");

        //disegno il layout
        createGuiComponents();
    }

    private void createGuiComponents() {
        setTitle("Area riservata");
        //setto la dimensione dinamicamente in base alla risoluzione del desktop
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenSize.setSize(screenSize.getWidth()*0.4, screenSize.getHeight()*0.6);

        //creo il pannello principale
        JPanel panel = new JPanel();
        BorderLayout layout = new BorderLayout();
        panel.setLayout(layout);
        add(panel);

        //al centro inserisco una textarea dove visualizzo i dati
        txtAreaInfo = new JTextArea();
        txtAreaInfo.setEditable(false);
        JScrollPane infoPanel = new JScrollPane(txtAreaInfo, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel titlePanel = new JPanel();

        //in base al tipo di account visualizzo cose diverse nella barra in alto
        JLabel lbltitle;
        if(this.type.equals("paziente")){

            //se è un paziente visualizzo il suo account e i dati relativi alla sua scheda medica
            lbltitle = new JLabel("Homepage di "+this.username+" ["+this.id+"]");
            titlePanel.add(lbltitle);

            //tramite uno script spark ricavo, filtro e decripto i dati
            sa = new AnalizzatoreSpark(this.id, this.type, this.masterkey);
            txtAreaInfo.setText(sa.analize());
        }else if(this.type.equals("medico") || this.type.equals("amministrazione")) {
            //se è un medico o un amministratore visualizzo il suo account e una selectbox (con ricerca) per scegliere
            //il paziente di cui visualizzare i dati
            lbltitle = new JLabel("Homepage di "+this.username+" ["+this.id+"]");
            titlePanel.add(lbltitle);

            //combobox con ricerca
            //ricevo tutti i paziente presenti nel database e li inserisco come elementi della combobox
            this.pazienti = selectAllPatients();
            cmbPazienti = new JComboBox<>();
            for (ComboPatient paziente : pazienti) {
                cmbPazienti.addItem(paziente);
            }
            //visualizzo il primo
            this.id = cmbPazienti.getItemAt(0).getId();
            //se si aggiorna la combobox aggiorno anche lo script sparl
            cmbPazienti.addItemListener(itemEvent -> {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    ComboPatient item = (ComboPatient) itemEvent.getItem();
                    HomeGUI.this.id = item.getId();
                    sa = new AnalizzatoreSpark(id, type, item.getMasterkey());
                    txtAreaInfo.setText(sa.analize());
                }
            });
            //permetto di poter scrivere nella combobox
            cmbPazienti.setEditable(true);
            //ricavo la nuova stringa inserita
            final JTextField textfield = (JTextField) cmbPazienti.getEditor().getEditorComponent();
            textfield.setColumns(15);
            //aggiorno l'array di elementi mostrati tramite il motodo comboFilter (sotto)
            textfield.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent ke) {
                    SwingUtilities.invokeLater(() -> comboFilter(textfield.getText()));
                }
            });
            titlePanel.add(cmbPazienti);

            //tramite uno script spark ricavo, filtro e decripto i dati
            sa = new AnalizzatoreSpark(id, type, cmbPazienti.getItemAt(0).getMasterkey());
            txtAreaInfo.setText(sa.analize());
        }

        
        JPanel buttonPanel = new JPanel();
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setSize(screenSize);
        setLocationRelativeTo(null);
    }

    private void comboFilter(String enteredText) {
       
        if (!cmbPazienti.isPopupVisible()) {
            cmbPazienti.showPopup();
        }

        //lista di item filtrata, inizialmente vuota
        ArrayList<ComboPatient> filterArray= new ArrayList<>();
        //per ogni item riferito al cliente nela lista, se il testo contiene la stringa da ricercare, lo aggiunge all'array filtrato
        for (ComboPatient comboItem : pazienti) {
            if (comboItem.getName().toLowerCase().contains(enteredText.toLowerCase())) {
                filterArray.add(comboItem);
            }
        }

        //se nell'array filtrato è presente almeno un elemento
        if (filterArray.size() > 0) {
            //rimuovo dalla combobox tutti gli elementi, e riaggiungo quelli presenti nell'array filtrato
            DefaultComboBoxModel model = (DefaultComboBoxModel) cmbPazienti.getModel();
            model.removeAllElements();
            for (ComboPatient s: filterArray){
                model.addElement(s);
            }

            //setto l'editor della combobox con il valore della stringa da ricercare
            JTextField textfield = (JTextField) cmbPazienti.getEditor().getEditorComponent();
            textfield.setText(enteredText);
        }
    }

    //metodo che prende dal database tutti i pazienti presenti
    private ArrayList<ComboPatient> selectAllPatients() {
        ArrayList<ComboPatient> allPatients = new ArrayList<>();
        DBUtils conn = null;
        try {
            //mi connetto al database
            conn = new DBUtils("localhost",
                    27017,
                    this.type,
                    this.type,
                    "myproject"
            );
            MongoDatabase db = conn.getClient().getDatabase("myproject");
            MongoCollection<Document> pazienti = db.getCollection("anagrafiche");

            //eseguo una query per ricavare tutti i pazienti
            FindIterable<Document> fi = pazienti.find();
            MongoCursor<Document> cursor = fi.iterator();
            try {
                //per ogni paziente
                while(cursor.hasNext()) {
                    //creo un oggetto json con i dati necessari (nome,cognome,accountId)
                    JSONObject obj = new JSONObject(cursor.next().toJson());
                    JSONObject patientid = obj.getJSONObject("_id");
                    JSONObject accountId = obj.getJSONObject("account");

                    //creo l'item da inserire nella combobox relativo al paziente
                    ComboPatient item = new ComboPatient();
                    item.setMasterkey(getMasterKey(db, new ObjectId(accountId.getString("$oid"))));
                    Cripto chiper = new Cripto(item.getMasterkey());
                    item.setName(chiper.decrypt(obj.getString("nome"))+" "+chiper.decrypt(obj.getString("cognome")));
                    item.setId(patientid.getString("$oid"));

                    //aggiungo l'item alla lista di tutti i pazienti
                    allPatients.add(item);
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allPatients;
    }

    //metodo che dato un account id restituisce la sua chiave per la decriptazione dei dati
    private String getMasterKey(MongoDatabase db, ObjectId accountID) {
        String masterKey = null;
        MongoCollection<Document> account = db.getCollection("utenti");
        FindIterable<Document> fi = account.find(Filters.eq("_id", accountID));
        MongoCursor<Document> cursor = fi.iterator();
        try {
            while(cursor.hasNext()) {
                JSONObject accountObj = new JSONObject(cursor.next().toJson());
                masterKey = accountObj.getString("masterkey");
            }
        } finally {
            cursor.close();
        }
        return masterKey;
    }

    public void start() {
        this.setVisible(true);
    }
}
