package biblioteca.modelo;

public abstract class Material {
    private String codigo;
    private String titulo;
    private int anio;
    private String editorial;
    private int cantidad;

    public Material(String codigo, String titulo, int anio, String editorial, int cantidad) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.anio = anio;
        this.editorial = editorial;
        this.cantidad = cantidad;
    }

    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public int getAnio() { return anio; }
    public String getEditorial() { return editorial; }
    public int getCantidad() { return cantidad; }
    public abstract String getTipo();
}
