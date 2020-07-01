public class Main {
    public static void main(String[] args) {
        LoginGUI.main();
        //Main s = new Main();
        //s.loadDatabase();
    }

    //metodo usato per caricare i dati nel database
    private void loadDatabase() {
        try {
            DBUtils conn = new DBUtils("localhost",
                    27017,
                    "root",
                    "root",
                    "admin"
            );
            conn.getPatient(100);
            conn.getDoctorsAndAmministrative(5, "medico");
            conn.getDoctorsAndAmministrative(10, "amministrazione");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
