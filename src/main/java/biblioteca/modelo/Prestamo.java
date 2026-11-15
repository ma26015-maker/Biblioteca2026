package biblioteca.modelo;

import java.time.LocalDate;

public class Prestamo {
    private String codigo;
    private String usuario;
    private String material;
    private LocalDate fecha;
    private LocalDate prevista;
    private LocalDate devolucion;
    private String estado;

    public Prestamo(String codigo, String usuario, String material, LocalDate fecha, LocalDate prevista, LocalDate devolucion, String estado) {
        this.codigo = codigo;
        this.usuario = usuario;
        this.material = material;
        this.fecha = fecha;
        this.prevista = prevista;
        this.devolucion = devolucion;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getUsuario() { return usuario; }
    public String getMaterial() { return material; }
    public LocalDate getFecha() { return fecha; }
    public LocalDate getPrevista() { return prevista; }
    public LocalDate getDevolucion() { return devolucion; }
    public String getEstado() { return estado; }
}
