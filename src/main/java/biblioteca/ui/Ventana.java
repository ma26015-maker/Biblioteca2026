package biblioteca.ui;

import biblioteca.Reglas;
import biblioteca.datos.BibliotecaServicio;
import biblioteca.modelo.Libro;
import biblioteca.modelo.Material;
import biblioteca.modelo.Revista;
import biblioteca.modelo.Usuario;
import org.bson.Document;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class Ventana extends JFrame {
    private final BibliotecaServicio servicio;
    private JTable tablaMateriales;
    private JTable tablaUsuarios;
    private JTable tablaPrestamos;
    private JTextField buscarMaterial;
    private JTextField buscarUsuario;
    private JTextField buscarPrestamo;
    private JComboBox<String> filtroPrestamo;
    private JTextArea textoReporte;
    private JComboBox<String> tipoReporte;

    public Ventana(BibliotecaServicio servicio) {
        this.servicio = servicio;
        setTitle("Sistema de Biblioteca");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 660);
        setMinimumSize(new Dimension(790, 500));
        setLocationRelativeTo(null);
        mostrarLogin();
    }

    private void mostrarLogin() {
        JPanel fondo = new JPanel(new GridBagLayout());
        JPanel datos = new JPanel(new GridLayout(0, 1, 6, 8));
        datos.setPreferredSize(new Dimension(290, 220));
        JLabel titulo = new JLabel("Sistema de Biblioteca", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 23));
        JTextField usuario = new JTextField("admin");
        JPasswordField clave = new JPasswordField();
        JButton entrar = new JButton("Iniciar sesión");
        datos.add(titulo);
        datos.add(new JLabel("Usuario"));
        datos.add(usuario);
        datos.add(new JLabel("Contraseña"));
        datos.add(clave);
        datos.add(entrar);
        entrar.addActionListener(e -> ejecutar(() -> {
            if (!servicio.iniciarSesion(usuario.getText().trim(), new String(clave.getPassword())))
                throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
            mostrarSistema();
        }));
        clave.addActionListener(e -> entrar.doClick());
        fondo.add(datos);
        setContentPane(fondo);
        revalidate();
        repaint();
    }

    private void mostrarSistema() {
        JPanel principal = new JPanel(new BorderLayout(8, 8));
        principal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel encabezado = new JPanel(new BorderLayout());
        JLabel titulo = new JLabel("Gestión de Biblioteca");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 21));
        encabezado.add(titulo, BorderLayout.WEST);
        JPanel opciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clave = new JButton("Cambiar contraseña");
        JButton salir = new JButton("Cerrar sesión");
        opciones.add(clave);
        opciones.add(salir);
        clave.addActionListener(e -> cambiarClave());
        salir.addActionListener(e -> mostrarLogin());
        encabezado.add(opciones, BorderLayout.EAST);
        principal.add(encabezado, BorderLayout.NORTH);
        JTabbedPane pestañas = new JTabbedPane();
        pestañas.addTab("Materiales", panelMateriales());
        pestañas.addTab("Usuarios", panelUsuarios());
        pestañas.addTab("Préstamos", panelPrestamos());
        pestañas.addTab("Consultas y reportes", panelReportes());
        principal.add(pestañas, BorderLayout.CENTER);
        setContentPane(principal);
        actualizarTodo();
        revalidate();
        repaint();
    }

    private JTable tabla(String... columnas) {
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
        JTable tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(25);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setAutoCreateRowSorter(true);
        return tabla;
    }

    private JPanel panelMateriales() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel arriba = new JPanel(new BorderLayout(6, 6));
        buscarMaterial = new JTextField();
        arriba.add(new JLabel("Buscar por código, título, autor o tipo"), BorderLayout.NORTH);
        arriba.add(buscarMaterial, BorderLayout.CENTER);
        escuchar(buscarMaterial, () -> ejecutar(this::actualizarMateriales));
        panel.add(arriba, BorderLayout.NORTH);
        tablaMateriales = tabla("Código", "Título", "Tipo", "Autor / Edición", "Año", "Editorial", "Total", "Disponibles");
        panel.add(new JScrollPane(tablaMateriales), BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton nuevo = new JButton("Registrar material");
        JButton editar = new JButton("Modificar");
        JButton borrar = new JButton("Eliminar");
        JButton actualizar = new JButton("Actualizar");
        botones.add(nuevo);
        botones.add(editar);
        botones.add(borrar);
        botones.add(actualizar);
        nuevo.addActionListener(e -> formularioMaterial(null));
        editar.addActionListener(e -> ejecutar(() -> formularioMaterial(servicio.buscarMaterial(seleccion(tablaMateriales)))));
        borrar.addActionListener(e -> ejecutar(() -> {
            String codigo = seleccion(tablaMateriales);
            if (confirmar("¿Eliminar el material " + codigo + "?")) {
                servicio.eliminarMaterial(codigo);
                actualizarTodo();
                aviso("Material eliminado.");
            }
        }));
        actualizar.addActionListener(e -> ejecutar(this::actualizarTodo));
        panel.add(botones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelUsuarios() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel arriba = new JPanel(new BorderLayout(6, 6));
        buscarUsuario = new JTextField();
        arriba.add(new JLabel("Buscar por código, nombre o documento"), BorderLayout.NORTH);
        arriba.add(buscarUsuario, BorderLayout.CENTER);
        escuchar(buscarUsuario, () -> ejecutar(this::actualizarUsuarios));
        panel.add(arriba, BorderLayout.NORTH);
        tablaUsuarios = tabla("Código", "Nombre", "Documento", "Contacto", "Estado");
        panel.add(new JScrollPane(tablaUsuarios), BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton nuevo = new JButton("Registrar usuario");
        JButton editar = new JButton("Modificar");
        JButton eliminar = new JButton("Eliminar / inactivar");
        JButton actualizar = new JButton("Actualizar");
        botones.add(nuevo);
        botones.add(editar);
        botones.add(eliminar);
        botones.add(actualizar);
        nuevo.addActionListener(e -> formularioUsuario(null));
        editar.addActionListener(e -> ejecutar(() -> formularioUsuario(servicio.buscarUsuario(seleccion(tablaUsuarios)))));
        eliminar.addActionListener(e -> ejecutar(() -> {
            String codigo = seleccion(tablaUsuarios);
            if (confirmar("¿Eliminar o inactivar el usuario " + codigo + "?")) {
                String resultado = servicio.quitarUsuario(codigo);
                actualizarTodo();
                aviso(resultado);
            }
        }));
        actualizar.addActionListener(e -> ejecutar(this::actualizarTodo));
        panel.add(botones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelPrestamos() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel arriba = new JPanel(new BorderLayout(6, 6));
        JPanel filtros = new JPanel(new BorderLayout(6, 6));
        buscarPrestamo = new JTextField();
        filtroPrestamo = new JComboBox<>(new String[]{"Todos", "Activos", "Devueltos", "Vencidos"});
        filtros.add(buscarPrestamo, BorderLayout.CENTER);
        filtros.add(filtroPrestamo, BorderLayout.EAST);
        arriba.add(new JLabel("Buscar por código de préstamo, usuario, material o estado"), BorderLayout.NORTH);
        arriba.add(filtros, BorderLayout.CENTER);
        escuchar(buscarPrestamo, () -> ejecutar(this::actualizarPrestamos));
        filtroPrestamo.addActionListener(e -> ejecutar(this::actualizarPrestamos));
        panel.add(arriba, BorderLayout.NORTH);
        tablaPrestamos = tabla("Código", "Usuario", "Material", "Préstamo", "Fecha prevista", "Devolución", "Estado");
        panel.add(new JScrollPane(tablaPrestamos), BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton nuevo = new JButton("Registrar préstamo");
        JButton devolver = new JButton("Registrar devolución");
        JButton todos = new JButton("Ver todos");
        JButton activos = new JButton("Solo activos");
        JButton vencidos = new JButton("Vencidos");
        botones.add(nuevo);
        botones.add(devolver);
        botones.add(todos);
        botones.add(activos);
        botones.add(vencidos);
        nuevo.addActionListener(e -> formularioPrestamo());
        devolver.addActionListener(e -> ejecutar(() -> {
            String codigo = seleccion(tablaPrestamos);
            if (confirmar("¿Registrar devolución del préstamo " + codigo + "?")) {
                servicio.devolver(codigo);
                actualizarTodo();
                aviso("Devolución registrada.");
            }
        }));
        todos.addActionListener(e -> filtroPrestamo.setSelectedItem("Todos"));
        activos.addActionListener(e -> filtroPrestamo.setSelectedItem("Activos"));
        vencidos.addActionListener(e -> filtroPrestamo.setSelectedItem("Vencidos"));
        panel.add(botones, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel panelReportes() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tipoReporte = new JComboBox<>(new String[]{"Resumen", "Materiales disponibles", "Préstamos activos", "Préstamos vencidos", "Listado de usuarios", "Historial de préstamos", "Comprobar integridad"});
        JButton generar = new JButton("Generar consulta");
        arriba.add(new JLabel("Reporte"));
        arriba.add(tipoReporte);
        arriba.add(generar);
        panel.add(arriba, BorderLayout.NORTH);
        textoReporte = new JTextArea();
        textoReporte.setEditable(false);
        textoReporte.setFont(new Font("Monospaced", Font.PLAIN, 13));
        textoReporte.setMargin(new Insets(15, 15, 15, 15));
        panel.add(new JScrollPane(textoReporte), BorderLayout.CENTER);
        generar.addActionListener(e -> ejecutar(this::actualizarReportes));
        tipoReporte.addActionListener(e -> ejecutar(this::actualizarReportes));
        return panel;
    }

    private void formularioMaterial(Document original) {
        JTextField codigo = campo(original, "_id");
        JTextField titulo = campo(original, "titulo");
        JComboBox<String> tipo = new JComboBox<>(new String[]{"Libro", "Revista"});
        if (original != null) tipo.setSelectedItem(original.getString("tipo"));
        JTextField anio = campo(original, "anio");
        if (original == null) anio.setText(String.valueOf(LocalDate.now().getYear()));
        JTextField editorial = campo(original, "editorial");
        JTextField total = campo(original, "total");
        JTextField autor = campo(original, "autor");
        JTextField isbn = campo(original, "isbn");
        JTextField edicion = campo(original, "edicion");
        JTextField volumen = campo(original, "volumen");
        codigo.setEditable(original == null);
        JPanel datos = formulario();
        agregar(datos, "Código único", codigo);
        agregar(datos, "Título", titulo);
        agregar(datos, "Tipo de material", tipo);
        agregar(datos, "Año de publicación", anio);
        agregar(datos, "Editorial", editorial);
        agregar(datos, "Cantidad total", total);
        agregar(datos, "Autor (solo libros)", autor);
        agregar(datos, "ISBN (opcional)", isbn);
        agregar(datos, "Edición (solo revistas)", edicion);
        agregar(datos, "Volumen (opcional)", volumen);
        int respuesta = JOptionPane.showConfirmDialog(this, new JScrollPane(datos), original == null ? "Nuevo material" : "Modificar material", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION) return;
        ejecutar(() -> {
            String c = Reglas.obligatorio(codigo.getText(), "el código");
            String t = Reglas.obligatorio(titulo.getText(), "el título");
            int a = Reglas.anio(anio.getText());
            String ed = Reglas.obligatorio(editorial.getText(), "la editorial");
            int n = Reglas.cantidad(total.getText());
            Material material;
            if ("Libro".equals(tipo.getSelectedItem()))
                material = new Libro(c, t, a, ed, n, autor.getText(), isbn.getText());
            else material = new Revista(c, t, a, ed, n, edicion.getText(), volumen.getText());
            servicio.guardarMaterial(material, original == null);
            actualizarTodo();
            aviso("Material guardado correctamente.");
        });
    }

    private void formularioUsuario(Document original) {
        JTextField codigo = campo(original, "_id");
        JTextField nombre = campo(original, "nombre");
        JTextField documento = campo(original, "documento");
        JTextField contacto = campo(original, "contacto");
        JCheckBox activo = new JCheckBox("Usuario habilitado", original == null || Boolean.TRUE.equals(original.getBoolean("activo")));
        codigo.setEditable(original == null);
        JPanel datos = formulario();
        agregar(datos, "Código único", codigo);
        agregar(datos, "Nombre completo", nombre);
        agregar(datos, "Documento", documento);
        agregar(datos, "Contacto", contacto);
        agregar(datos, "Estado", activo);
        if (JOptionPane.showConfirmDialog(this, datos, original == null ? "Nuevo usuario" : "Modificar usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        ejecutar(() -> {
            Usuario usuario = new Usuario(codigo.getText(), nombre.getText(), documento.getText(), contacto.getText(), activo.isSelected());
            servicio.guardarUsuario(usuario, original == null);
            actualizarTodo();
            aviso("Usuario guardado correctamente.");
        });
    }

    private void formularioPrestamo() {
        ejecutar(() -> {
            JComboBox<String> personas = new JComboBox<>();
            for (Document u : servicio.listaUsuarios(""))
                if (Boolean.TRUE.equals(u.getBoolean("activo"))) personas.addItem(u.getString("_id") + " | " + u.getString("nombre"));
            JComboBox<String> libros = new JComboBox<>();
            for (Document m : servicio.listaMateriales(""))
                if (m.getInteger("disponibles", 0) > 0) libros.addItem(m.getString("_id") + " | " + m.getString("titulo"));
            if (personas.getItemCount() == 0) throw new IllegalArgumentException("No hay usuarios habilitados.");
            if (libros.getItemCount() == 0) throw new IllegalArgumentException("No hay materiales disponibles.");
            JTextField fecha = new JTextField(LocalDate.now().toString());
            fecha.setEditable(false);
            JTextField prevista = new JTextField(LocalDate.now().plusDays(7).toString());
            JPanel datos = formulario();
            agregar(datos, "Usuario", personas);
            agregar(datos, "Material", libros);
            agregar(datos, "Fecha del préstamo", fecha);
            agregar(datos, "Fecha prevista (AAAA-MM-DD)", prevista);
            if (JOptionPane.showConfirmDialog(this, datos, "Nuevo préstamo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            String codigo = servicio.registrarPrestamo(codigoLista((String) personas.getSelectedItem()), codigoLista((String) libros.getSelectedItem()), Reglas.fecha(prevista.getText()));
            actualizarTodo();
            aviso("Préstamo registrado con código " + codigo);
        });
    }

    private String codigoLista(String texto) { return texto.substring(0, texto.indexOf(" | ")); }

    private void cambiarClave() {
        JPasswordField anterior = new JPasswordField();
        JPasswordField nueva = new JPasswordField();
        JPanel datos = formulario();
        agregar(datos, "Contraseña actual", anterior);
        agregar(datos, "Contraseña nueva", nueva);
        if (JOptionPane.showConfirmDialog(this, datos, "Cambiar contraseña", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        ejecutar(() -> {
            servicio.cambiarClave(new String(anterior.getPassword()), new String(nueva.getPassword()));
            aviso("Contraseña actualizada.");
        });
    }

    private void actualizarMateriales() {
        DefaultTableModel modelo = (DefaultTableModel) tablaMateriales.getModel();
        modelo.setRowCount(0);
        for (Document m : servicio.listaMateriales(buscarMaterial.getText())) {
            String detalle = "Libro".equals(m.getString("tipo")) ? m.getString("autor") : "Ed. " + m.getString("edicion");
            modelo.addRow(new Object[]{m.getString("_id"), m.getString("titulo"), m.getString("tipo"), detalle,
                m.getInteger("anio"), m.getString("editorial"), m.getInteger("total"), m.getInteger("disponibles")});
        }
    }

    private void actualizarUsuarios() {
        DefaultTableModel modelo = (DefaultTableModel) tablaUsuarios.getModel();
        modelo.setRowCount(0);
        for (Document u : servicio.listaUsuarios(buscarUsuario.getText()))
            modelo.addRow(new Object[]{u.getString("_id"), u.getString("nombre"), u.getString("documento"),
                u.getString("contacto"), Boolean.TRUE.equals(u.getBoolean("activo")) ? "Activo" : "Inactivo"});
    }

    private void actualizarPrestamos() {
        DefaultTableModel modelo = (DefaultTableModel) tablaPrestamos.getModel();
        modelo.setRowCount(0);
        for (Document p : servicio.listaPrestamos(buscarPrestamo.getText(), (String) filtroPrestamo.getSelectedItem())) {
            String estado = p.getString("estado");
            if ("ACTIVO".equals(estado) && p.getString("prevista").compareTo(LocalDate.now().toString()) < 0) estado = "VENCIDO";
            modelo.addRow(new Object[]{p.getString("_id"), p.getString("usuario"), p.getString("material"),
                p.getString("fecha"), p.getString("prevista"), p.getString("devolucion"), estado});
        }
    }

    private void actualizarReportes() {
        String reporte = (String) tipoReporte.getSelectedItem();
        StringBuilder texto = new StringBuilder();
        if ("Resumen".equals(reporte)) {
            texto.append("RESUMEN DE LA BIBLIOTECA\n\n")
                .append("Materiales registrados: ").append(servicio.contarMateriales()).append("\n")
                .append("Usuarios registrados: ").append(servicio.contarUsuarios()).append("\n")
                .append("Préstamos totales: ").append(servicio.contarPrestamos()).append("\n")
                .append("Préstamos activos: ").append(servicio.contarActivos()).append("\n")
                .append("Préstamos vencidos: ").append(servicio.contarVencidos()).append("\n");
        } else if ("Materiales disponibles".equals(reporte)) {
            texto.append("MATERIALES DISPONIBLES\n\n");
            for (Document m : servicio.listaMateriales(""))
                if (m.getInteger("disponibles", 0) > 0) texto.append(m.getString("_id")).append("   ").append(m.getString("titulo"))
                    .append("   (").append(m.getInteger("disponibles")).append(" disponibles)\n");
        } else if ("Listado de usuarios".equals(reporte)) {
            texto.append("LISTADO DE USUARIOS\n\n");
            for (Document u : servicio.listaUsuarios("")) texto.append(u.getString("_id")).append("   ").append(u.getString("nombre"))
                .append("   ").append(u.getString("documento")).append("   ").append(Boolean.TRUE.equals(u.getBoolean("activo")) ? "Activo" : "Inactivo").append("\n");
        } else if ("Comprobar integridad".equals(reporte)) {
            texto.append("REVISIÓN DE DISPONIBILIDAD\n\n");
            List<String> problemas = servicio.verificarIntegridad();
            if (problemas.isEmpty()) texto.append("No se encontraron diferencias entre existencias y préstamos activos.\n");
            else for (String problema : problemas) texto.append(problema).append("\n");
        } else {
            String filtro = "Préstamos activos".equals(reporte) ? "Activos" : "Préstamos vencidos".equals(reporte) ? "Vencidos" : "Todos";
            texto.append(reporte.toUpperCase()).append("\n\n");
            for (Document p : servicio.listaPrestamos("", filtro)) texto.append(p.getString("_id")).append("   Usuario: ")
                .append(p.getString("usuario")).append("   Material: ").append(p.getString("material"))
                .append("   Prevista: ").append(p.getString("prevista"))
                .append("   Estado: ").append(p.getString("estado"))
                .append("   Devolución: ").append(p.getString("devolucion")).append("\n");
        }
        if (texto.toString().split("\n").length < 3 && !"Resumen".equals(reporte)) texto.append("No se encontraron registros.\n");
        textoReporte.setText(texto.toString());
        textoReporte.setCaretPosition(0);
    }

    private void actualizarTodo() {
        actualizarMateriales();
        actualizarUsuarios();
        actualizarPrestamos();
        actualizarReportes();
    }

    private JTextField campo(Document d, String clave) {
        Object valor = d == null ? "" : d.get(clave);
        return new JTextField(valor == null ? "" : valor.toString(), 24);
    }

    private JPanel formulario() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private void agregar(JPanel panel, String nombre, JComponent valor) {
        panel.add(new JLabel(nombre));
        panel.add(valor);
    }

    private String seleccion(JTable tabla) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) throw new IllegalArgumentException("Seleccione una fila de la tabla.");
        return tabla.getValueAt(fila, 0).toString();
    }

    private void escuchar(JTextField campo, Runnable tarea) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tarea.run(); }
            public void removeUpdate(DocumentEvent e) { tarea.run(); }
            public void changedUpdate(DocumentEvent e) { tarea.run(); }
        });
    }

    private void ejecutar(Runnable tarea) {
        try { tarea.run(); }
        catch (Exception e) {
            String mensaje = e.getMessage() == null ? "No se pudo completar la operación." : e.getMessage();
            if (mensaje.contains("E11000")) mensaje = "Ya existe un registro con ese código o documento.";
            JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean confirmar(String pregunta) { return JOptionPane.showConfirmDialog(this, pregunta, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }
    private void aviso(String texto) { JOptionPane.showMessageDialog(this, texto, "Biblioteca", JOptionPane.INFORMATION_MESSAGE); }
}
