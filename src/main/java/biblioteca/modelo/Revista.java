package biblioteca.modelo;

public class Revista extends Material {
    private String edicion;
    private String volumen;

    public Revista(String codigo, String titulo, int anio, String editorial, int cantidad, String edicion, String volumen) {
        super(codigo, titulo, anio, editorial, cantidad);
        this.edicion = edicion;
        this.volumen = volumen;
    }

    public String getEdicion() { return edicion; }
    public String getVolumen() { return volumen; }
    @Override public String getTipo() { return "Revista"; }
}
