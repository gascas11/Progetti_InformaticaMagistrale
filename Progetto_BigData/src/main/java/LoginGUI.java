import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.json.JSONObject;

import javax.swing.*;

public class LoginGUI extends JFrame {
    private JButton btnLogin;
    private JPasswordField txtPassword;
    private JTextField txtUsername;
    private String username;
    private String id;
    private String type;
    private String masterKey;

    public LoginGUI() {
        this.username = null;
        this.id = null;
        this.type = null;
        this.masterKey = null;

        //creo i componenti grafici
        initComponents();

        //lister del bottone di login che al click prova a fare l'accesso
        btnLogin.addActionListener(e -> {
            if(performSignIn(txtUsername.getText(), txtPassword.getText()) && this.username != null && this.id != null && this.type != null) {
                dispose();
                HomeGUI maingui = new HomeGUI(this.username, this.id, this.type, this.masterKey);
                maingui.start();
            }
            else{
                JOptionPane.showMessageDialog(LoginGUI.this, "Login non eseguito");
            }

        });

    }

    //metodo che disegna la UI
    private void initComponents() {
        setTitle("Login Form");
        JPanel panel = new JPanel();
        JLabel lblTitle = new JLabel();
        JLabel lblUsername = new JLabel();
        JLabel lblPassword = new JLabel();
        btnLogin = new JButton();
        txtUsername = new JTextField();
        txtPassword = new JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTitle.setText("Login");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(panel);
        panel.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(171, 171, 171)
                                .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(180, 180, 180))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(34, 34, 34)
                                .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(34, 34, 34))
        );

        lblUsername.setText("Username");
        lblPassword.setText("Password");

        btnLogin.setText("login");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                                .addGap(43, 43, 43)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblUsername)
                                                        .addComponent(lblPassword))
                                                .addGap(18, 18, 18)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtUsername)
                                                        .addComponent(txtPassword)))
                                        .addGroup(layout.createSequentialGroup()
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(54, 54, 54))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblUsername)
                                        .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblPassword)
                                        .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(btnLogin)
                                .addContainerGap(66, Short.MAX_VALUE))
        );
        pack();
        setLocationRelativeTo(null);
    }

    //metodo che dati username e password, restituisce true se il login va a buon fine
    private boolean performSignIn(String usr, String pwd) {
        try {
            DBUtils conn = new DBUtils("localhost",
                    27017,
                    "signin",
                    "signin",
                    "myproject"
            );

            
            MongoDatabase db = conn.getClient().getDatabase("myproject");
            MongoCollection<Document> pazienti = db.getCollection("utenti");

            //cerco se ne esiste uno con lo stesso username richiesto
            FindIterable<Document> fi = pazienti.find(Filters.eq("username",usr));
            MongoCursor<Document> cursor = fi.iterator();
            try {
                //se c'è
                while(cursor.hasNext()) {
                    JSONObject obj = new JSONObject(cursor.next().toJson());
                    String psw_from_db = obj.getString("password");
                    //controllo che anche la password inserita sia uguale
                    if(Cripto.SHA256(pwd).equals(psw_from_db)){
                        //setto le variabili globali dell'account e restituisco true
                        this.username = usr;
                        this.type = obj.getString("tipologia");
                        if(this.type.equals("paziente")){
                            this.masterKey = obj.getString("masterkey");
                        }
                        JSONObject oid = obj.getJSONObject("_id");
                        this.id = oid.getString("$oid");
                        conn.close();
                        return true;
                    }
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void main() {
        java.awt.EventQueue.invokeLater(() -> new LoginGUI().setVisible(true));
    }
}