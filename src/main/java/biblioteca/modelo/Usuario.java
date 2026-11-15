package biblioteca.modelo;

public class Usuario {
    private String codigo;
    private String nombre;
    private String documento;
    private String contacto;
    private boolean activo;

    public Usuario(String codigo, String nombre, String documento, String contacto, boolean activo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.documento = documento;
        this.contacto = contacto;
        this.activo = activo;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDocumento() { return documento; }
    public String getContacto() { return contacto; }
    public boolean isActivo() { return activo; }
}
