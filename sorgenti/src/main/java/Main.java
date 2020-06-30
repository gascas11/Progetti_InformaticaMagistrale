public class Main {
    public static void main(String[] args) {
        LoginGUI.main();
        //Main s = new Main();
        //s.fillDatabase();
    }

    //metodo usato per caricare i dati nel database
    private void fillDatabase() {
        try {
            DBUtils conn = new DBUtils("localhost",
                    27017,
                    "root",
                    "root",
                    "admin"
            );
            conn.fillData(100);
            conn.fillDoctorsAndAmministrative(5, "medico");
            conn.fillDoctorsAndAmministrative(10, "amministrazione");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
