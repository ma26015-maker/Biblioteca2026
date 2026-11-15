package biblioteca;

import biblioteca.modelo.Libro;
import biblioteca.modelo.Revista;
import java.time.LocalDate;

public class Pruebas {
    private static int correctas = 0;

    private static void verificar(boolean condicion) {
        if (!condicion) throw new AssertionError("Prueba fallida");
        correctas++;
    }

    private static void error(Runnable operacion) {
        boolean fallo = false;
        try { operacion.run(); }
        catch (IllegalArgumentException e) { fallo = true; }
        verificar(fallo);
    }

    public static void main(String[] args) {
        verificar(Reglas.anio("2020") == 2020);
        error(() -> Reglas.anio("1400"));
        error(() -> Reglas.anio("abc"));
        verificar(Reglas.cantidad("0") == 0);
        error(() -> Reglas.cantidad("-1"));
        error(() -> Reglas.cantidad("5.5"));
        verificar(Reglas.obligatorio(" ABC ", "código").equals("ABC"));
        error(() -> Reglas.obligatorio(" ", "nombre"));
        verificar(Reglas.fecha("2026-11-13").equals(LocalDate.of(2026, 11, 13)));
        error(() -> Reglas.fecha("13/11/2026"));
        error(() -> Reglas.validarFechas(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 12)));
        verificar(new Libro("L1", "Java", 2020, "Editora", 1, "Ana", "123").getTipo().equals("Libro"));
        verificar(new Revista("R1", "Ciencia", 2020, "Editora", 2, "4", "2").getTipo().equals("Revista"));
        System.out.println("Pruebas correctas: " + correctas);
    }
}
