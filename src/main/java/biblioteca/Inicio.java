package biblioteca;

import biblioteca.datos.BibliotecaServicio;
import biblioteca.datos.Conexion;
import biblioteca.ui.Ventana;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Inicio {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception e) { }
        try {
            Conexion conexion = new Conexion();
            BibliotecaServicio servicio = new BibliotecaServicio(conexion.getBase());
            Runtime.getRuntime().addShutdownHook(new Thread(conexion::close));
            SwingUtilities.invokeLater(() -> new Ventana(servicio).setVisible(true));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "No se pudo conectar a MongoDB.\nCompruebe que el servidor esté encendido y revise MONGO_URI.\n" + e.getMessage(),
                "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }
}
