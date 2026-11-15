package biblioteca;

import java.time.LocalDate;

public class Reglas {
    public static String obligatorio(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) throw new IllegalArgumentException("Debe ingresar " + campo + ".");
        return texto.trim();
    }

    public static int anio(String texto) {
        int anio;
        try { anio = Integer.parseInt(texto.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("El año debe ser un número entero."); }
        if (anio < 1450 || anio > LocalDate.now().getYear() + 1) throw new IllegalArgumentException("El año de publicación no es válido.");
        return anio;
    }

    public static int cantidad(String texto) {
        int numero;
        try { numero = Integer.parseInt(texto.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("La cantidad debe ser un número entero."); }
        if (numero < 0) throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        return numero;
    }

    public static LocalDate fecha(String texto) {
        try { return LocalDate.parse(obligatorio(texto, "la fecha")); }
        catch (Exception e) { throw new IllegalArgumentException("La fecha debe tener formato AAAA-MM-DD."); }
    }

    public static void validarFechas(LocalDate fecha, LocalDate prevista) {
        if (prevista.isBefore(fecha)) throw new IllegalArgumentException("La fecha prevista no puede ser anterior a la del préstamo.");
    }
}
