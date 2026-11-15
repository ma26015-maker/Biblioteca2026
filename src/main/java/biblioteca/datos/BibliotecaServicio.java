package biblioteca.datos;

import biblioteca.Reglas;
import biblioteca.modelo.Libro;
import biblioteca.modelo.Material;
import biblioteca.modelo.Revista;
import biblioteca.modelo.Usuario;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class BibliotecaServicio {
    private final MongoCollection<Document> materiales;
    private final MongoCollection<Document> usuarios;
    private final MongoCollection<Document> prestamos;
    private final MongoCollection<Document> accesos;

    public BibliotecaServicio(MongoDatabase base) {
        materiales = base.getCollection("materiales");
        usuarios = base.getCollection("usuarios");
        prestamos = base.getCollection("prestamos");
        accesos = base.getCollection("accesos");
        usuarios.createIndex(Indexes.ascending("documento"), new IndexOptions().unique(true));
        prestamos.createIndex(Indexes.ascending("material", "estado"));
        prestamos.createIndex(Indexes.ascending("usuario", "estado"));
        if (accesos.countDocuments() == 0) {
            guardarClave("admin", "Admin123!");
        }
    }

    private String hash(String clave, String sal) {
        try {
            byte[] datos = Base64.getDecoder().decode(sal);
            PBEKeySpec spec = new PBEKeySpec(clave.toCharArray(), datos, 120000, 256);
            byte[] claveHash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(claveHash);
        } catch (Exception e) { throw new IllegalStateException("No fue posible validar la clave.", e); }
    }

    private void guardarClave(String nombre, String clave) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        String textoSal = Base64.getEncoder().encodeToString(sal);
        accesos.insertOne(new Document("_id", nombre).append("sal", textoSal).append("hash", hash(clave, textoSal)));
    }

    public boolean iniciarSesion(String nombre, String clave) {
        Document cuenta = accesos.find(eq("_id", nombre)).first();
        if (cuenta == null || clave == null) return false;
        return java.security.MessageDigest.isEqual(
            hash(clave, cuenta.getString("sal")).getBytes(java.nio.charset.StandardCharsets.UTF_8),
            cuenta.getString("hash").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public void cambiarClave(String actual, String nueva) {
        if (!iniciarSesion("admin", actual)) throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        if (nueva == null || nueva.length() < 8) throw new IllegalArgumentException("La contraseña nueva debe tener al menos 8 caracteres.");
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        String textoSal = Base64.getEncoder().encodeToString(sal);
        accesos.updateOne(eq("_id", "admin"), combine(set("sal", textoSal), set("hash", hash(nueva, textoSal))));
    }

    public List<Document> listaMateriales(String buscar) {
        List<Document> lista = new ArrayList<>();
        for (Document d : materiales.find().sort(new Document("titulo", 1))) {
            String texto = (d.getString("_id") + " " + d.getString("titulo") + " " + d.getString("tipo") + " " + d.getString("autor")).toLowerCase();
            if (texto.contains(buscar.trim().toLowerCase())) lista.add(d);
        }
        return lista;
    }

    public Document buscarMaterial(String codigo) { return materiales.find(eq("_id", codigo)).first(); }

    public synchronized void guardarMaterial(Material m, boolean nuevo) {
        String codigo = Reglas.obligatorio(m.getCodigo(), "el código");
        String titulo = Reglas.obligatorio(m.getTitulo(), "el título");
        String editorial = Reglas.obligatorio(m.getEditorial(), "la editorial");
        if (m.getAnio() < 1450 || m.getAnio() > LocalDate.now().getYear() + 1) throw new IllegalArgumentException("Año fuera de rango.");
        if (m.getCantidad() < 0) throw new IllegalArgumentException("Cantidad inválida.");
        Document datos = new Document("titulo", titulo).append("tipo", m.getTipo()).append("anio", m.getAnio())
            .append("editorial", editorial).append("total", m.getCantidad());
        if (m instanceof Libro libro) {
            datos.append("autor", Reglas.obligatorio(libro.getAutor(), "el autor"));
            datos.append("isbn", libro.getIsbn().trim());
            datos.append("edicion", "").append("volumen", "");
        } else if (m instanceof Revista revista) {
            datos.append("autor", "").append("isbn", "");
            datos.append("edicion", Reglas.obligatorio(revista.getEdicion(), "la edición"));
            datos.append("volumen", revista.getVolumen().trim());
        }
        if (nuevo) {
            if (buscarMaterial(codigo) != null) throw new IllegalArgumentException("Ya existe un material con ese código.");
            datos.put("_id", codigo);
            datos.put("disponibles", m.getCantidad());
            materiales.insertOne(datos);
        } else {
            if (buscarMaterial(codigo) == null) throw new IllegalArgumentException("Material inexistente.");
            long activos = prestamos.countDocuments(and(eq("material", codigo), eq("estado", "ACTIVO")));
            if (m.getCantidad() < activos) throw new IllegalArgumentException("El total no puede ser menor a los ejemplares prestados.");
            datos.put("disponibles", (int) (m.getCantidad() - activos));
            materiales.replaceOne(eq("_id", codigo), datos.append("_id", codigo));
        }
    }

    public synchronized void eliminarMaterial(String codigo) {
        if (buscarMaterial(codigo) == null) throw new IllegalArgumentException("No se encontró el material.");
        if (prestamos.countDocuments(eq("material", codigo)) > 0)
            throw new IllegalArgumentException("Este material tiene préstamos registrados y debe conservarse para el historial.");
        materiales.deleteOne(eq("_id", codigo));
    }

    public List<Document> listaUsuarios(String buscar) {
        List<Document> lista = new ArrayList<>();
        for (Document d : usuarios.find().sort(new Document("nombre", 1))) {
            String texto = (d.getString("_id") + " " + d.getString("nombre") + " " + d.getString("documento")).toLowerCase();
            if (texto.contains(buscar.trim().toLowerCase())) lista.add(d);
        }
        return lista;
    }

    public Document buscarUsuario(String codigo) { return usuarios.find(eq("_id", codigo)).first(); }

    public synchronized void guardarUsuario(Usuario u, boolean nuevo) {
        String codigo = Reglas.obligatorio(u.getCodigo(), "el código");
        Document datos = new Document("nombre", Reglas.obligatorio(u.getNombre(), "el nombre"))
            .append("documento", Reglas.obligatorio(u.getDocumento(), "el documento"))
            .append("contacto", Reglas.obligatorio(u.getContacto(), "el contacto"))
            .append("activo", u.isActivo());
        if (nuevo) {
            if (buscarUsuario(codigo) != null) throw new IllegalArgumentException("Ya existe un usuario con ese código.");
            datos.put("_id", codigo);
            usuarios.insertOne(datos);
        } else {
            if (buscarUsuario(codigo) == null) throw new IllegalArgumentException("Usuario inexistente.");
            if (!u.isActivo() && prestamos.countDocuments(and(eq("usuario", codigo), eq("estado", "ACTIVO"))) > 0)
                throw new IllegalArgumentException("No puede inactivar un usuario con préstamos activos.");
            usuarios.replaceOne(eq("_id", codigo), datos.append("_id", codigo));
        }
    }

    public synchronized String quitarUsuario(String codigo) {
        if (buscarUsuario(codigo) == null) throw new IllegalArgumentException("No existe el usuario.");
        if (prestamos.countDocuments(and(eq("usuario", codigo), eq("estado", "ACTIVO"))) > 0)
            throw new IllegalArgumentException("El usuario tiene préstamos activos.");
        if (prestamos.countDocuments(eq("usuario", codigo)) > 0) {
            usuarios.updateOne(eq("_id", codigo), set("activo", false));
            return "Usuario inactivado. Se conservó su historial.";
        }
        usuarios.deleteOne(eq("_id", codigo));
        return "Usuario eliminado.";
    }

    public synchronized String registrarPrestamo(String usuario, String material, LocalDate prevista) {
        usuario = Reglas.obligatorio(usuario, "el código de usuario");
        material = Reglas.obligatorio(material, "el código de material");
        LocalDate hoy = LocalDate.now();
        Reglas.validarFechas(hoy, prevista);
        Document persona = buscarUsuario(usuario);
        if (persona == null) throw new IllegalArgumentException("El usuario no existe.");
        if (!Boolean.TRUE.equals(persona.getBoolean("activo"))) throw new IllegalArgumentException("El usuario está inactivo.");
        if (buscarMaterial(material) == null) throw new IllegalArgumentException("El material no existe.");
        UpdateResult resultado = materiales.updateOne(and(eq("_id", material), gt("disponibles", 0)), inc("disponibles", -1));
        if (resultado.getModifiedCount() == 0) throw new IllegalArgumentException("No hay ejemplares disponibles.");
        String codigo = "P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            prestamos.insertOne(new Document("_id", codigo).append("usuario", usuario).append("material", material)
                .append("fecha", hoy.toString()).append("prevista", prevista.toString())
                .append("devolucion", "").append("estado", "ACTIVO"));
        } catch (RuntimeException e) {
            materiales.updateOne(eq("_id", material), inc("disponibles", 1));
            throw e;
        }
        return codigo;
    }

    public synchronized void devolver(String codigo) {
        Document prestamo = prestamos.find(eq("_id", codigo)).first();
        if (prestamo == null) throw new IllegalArgumentException("No existe ese préstamo.");
        if (!"ACTIVO".equals(prestamo.getString("estado"))) throw new IllegalArgumentException("El préstamo ya fue devuelto.");
        UpdateResult resultado = prestamos.updateOne(and(eq("_id", codigo), eq("estado", "ACTIVO")),
            combine(set("estado", "DEVUELTO"), set("devolucion", LocalDate.now().toString())));
        if (resultado.getModifiedCount() == 0) throw new IllegalArgumentException("El préstamo ya no está activo.");
        try {
            UpdateResult stock = materiales.updateOne(and(eq("_id", prestamo.getString("material")),
                expr(new Document("$lt", java.util.List.of("$disponibles", "$total")))), inc("disponibles", 1));
            if (stock.getModifiedCount() == 0) throw new IllegalStateException("No se pudo actualizar la disponibilidad. Revise la integridad.");
        } catch (RuntimeException e) {
            prestamos.updateOne(and(eq("_id", codigo), eq("estado", "DEVUELTO")),
                combine(set("estado", "ACTIVO"), set("devolucion", "")));
            throw e;
        }
    }

    public List<Document> listaPrestamos(String buscar, String filtro) {
        List<Document> lista = new ArrayList<>();
        for (Document d : prestamos.find().sort(new Document("fecha", -1))) {
            String estado = d.getString("estado");
            boolean vencido = "ACTIVO".equals(estado) && d.getString("prevista").compareTo(LocalDate.now().toString()) < 0;
            if ("Activos".equals(filtro) && !"ACTIVO".equals(estado)) continue;
            if ("Devueltos".equals(filtro) && !"DEVUELTO".equals(estado)) continue;
            if ("Vencidos".equals(filtro) && !vencido) continue;
            String texto = (d.getString("_id") + " " + d.getString("usuario") + " " + d.getString("material") + " " + estado).toLowerCase();
            if (texto.contains(buscar.trim().toLowerCase())) lista.add(d);
        }
        return lista;
    }

    public long contarMateriales() { return materiales.countDocuments(); }
    public long contarUsuarios() { return usuarios.countDocuments(); }
    public long contarPrestamos() { return prestamos.countDocuments(); }
    public long contarActivos() { return prestamos.countDocuments(eq("estado", "ACTIVO")); }
    public long contarVencidos() { return prestamos.countDocuments(and(eq("estado", "ACTIVO"), lt("prevista", LocalDate.now().toString()))); }

    public List<String> verificarIntegridad() {
        List<String> problemas = new ArrayList<>();
        for (Document m : materiales.find()) {
            String codigo = m.getString("_id");
            long activos = prestamos.countDocuments(and(eq("material", codigo), eq("estado", "ACTIVO")));
            int total = m.getInteger("total", 0);
            int disponibles = m.getInteger("disponibles", 0);
            if (total - activos != disponibles || disponibles < 0 || disponibles > total)
                problemas.add("Material " + codigo + ": total " + total + ", prestados " + activos + ", disponibles " + disponibles);
        }
        for (Document p : prestamos.find(eq("estado", "ACTIVO"))) {
            if (buscarMaterial(p.getString("material")) == null) problemas.add("Préstamo sin material: " + p.getString("_id"));
            if (buscarUsuario(p.getString("usuario")) == null) problemas.add("Préstamo sin usuario: " + p.getString("_id"));
        }
        return problemas;
    }
}
