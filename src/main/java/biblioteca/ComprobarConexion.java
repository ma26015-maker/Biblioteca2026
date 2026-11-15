package biblioteca;

import biblioteca.datos.Conexion;

public class ComprobarConexion {
    public static void main(String[] args) {
        try (Conexion conexion = new Conexion()) {
            System.out.println("Conexion correcta con MongoDB Atlas. Base: " + conexion.getBase().getName());
        } catch (Exception e) {
            System.err.println("No fue posible conectar con MongoDB Atlas.");
            System.exit(1);
        }
    }
}
