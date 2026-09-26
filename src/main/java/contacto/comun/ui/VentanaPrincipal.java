package contacto.comun.ui;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Ventana principal del IDE. Une las piezas: ArbolArchivos a la
 * izquierda, pestañas de PanelEditor al centro, PanelSalida abajo.
 */
public class VentanaPrincipal extends JFrame {

    private final ArbolArchivos arbolArchivos;
    private final JTabbedPane pestanasEditor = new JTabbedPane();
    private final PanelSalida panelSalida = new PanelSalida();
    private final Map<Path, PanelEditor> editoresAbiertos = new HashMap<>();
    private final Map<PanelEditor, javax.swing.JLabel> etiquetasPestana = new HashMap<>();

    public VentanaPrincipal(Path carpetaProyecto) {
        super("Proyecto 1 - Compiladores 2 (Y? / Zetariano / Pig Latin)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        arbolArchivos = new ArbolArchivos(carpetaProyecto);
        arbolArchivos.setAlAbrirArchivo(this::abrirArchivo);
        arbolArchivos.setPreferredSize(new Dimension(260, 0));

        JSplitPane divisionVertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pestanasEditor, panelSalida);
        divisionVertical.setResizeWeight(0.7);

        JSplitPane divisionHorizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, arbolArchivos, divisionVertical);
        divisionHorizontal.setResizeWeight(0.0);

        setJMenuBar(construirMenu());
        add(construirBarraHerramientas(), BorderLayout.NORTH);
        add(divisionHorizontal, BorderLayout.CENTER);
    }

    // =====================================================================
    // Menu y barra de herramientas
    // =====================================================================

    private JMenuBar construirMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.add(accion("Abrir carpeta...", null, e -> abrirCarpeta()));
        archivo.addSeparator();
        archivo.add(accion("Guardar", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), e -> guardarActivo()));
        archivo.add(accion("Guardar como...", null, e -> guardarComoActivo()));
        archivo.add(accion("Descargar (guardar como)...", null, e -> guardarComoActivo()));
        archivo.addSeparator();
        archivo.add(accion("Salir", null, e -> System.exit(0)));
        barra.add(archivo);

        return barra;
    }

    private JToolBar construirBarraHerramientas() {
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);

        JButton compilar = new JButton("Compilar");
        compilar.addActionListener(e -> compilarActivo());
        barra.add(compilar);

        return barra;
    }

    private JMenuItem accion(String texto, KeyStroke atajo, java.awt.event.ActionListener accion) {
        JMenuItem item = new JMenuItem(texto);
        if (atajo != null) {
            item.setAccelerator(atajo);
        }
        item.addActionListener(accion);
        return item;
    }

    // =====================================================================
    // Archivos
    // =====================================================================

    private void abrirCarpeta() {
        JFileChooser selector = new JFileChooser(arbolArchivos.getRaiz().toFile());
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            arbolArchivos.cambiarRaiz(selector.getSelectedFile().toPath());
        }
    }

    private void abrirArchivo(Path archivo) {
        if (editoresAbiertos.containsKey(archivo)) {
            pestanasEditor.setSelectedComponent(editoresAbiertos.get(archivo));
            return;
        }
        try {
            String contenido = Files.readString(archivo);
            PanelEditor editor = new PanelEditor(archivo, contenido);
            editor.setAlModificar(() -> actualizarTituloPestana(editor));
            editoresAbiertos.put(archivo, editor);
            int indice = pestanasEditor.getTabCount();
            pestanasEditor.addTab(null, editor);
            pestanasEditor.setTabComponentAt(indice, crearComponentePestana(editor));
            pestanasEditor.setSelectedComponent(editor);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Pestaña con titulo + boton "x" para cerrarla (JTabbedPane no trae esto de fabrica). */
    private JPanel crearComponentePestana(PanelEditor editor) {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        panel.setOpaque(false);

        javax.swing.JLabel etiqueta = new javax.swing.JLabel(editor.getNombrePestana());
        etiquetasPestana.put(editor, etiqueta);
        panel.add(etiqueta);

        JButton cerrar = new JButton("x");
        cerrar.setMargin(new java.awt.Insets(0, 4, 0, 4));
        cerrar.setContentAreaFilled(false);
        cerrar.setBorderPainted(false);
        cerrar.setFocusable(false);
        cerrar.addActionListener(e -> cerrarPestana(editor));
        panel.add(cerrar);

        return panel;
    }

    private void cerrarPestana(PanelEditor editor) {
        if (editor.isModificado()) {
            int opcion = JOptionPane.showConfirmDialog(this,
                    "'" + editor.getNombrePestana().replace(" *", "") + "' tiene cambios sin guardar. "
                            + "¿Cerrar de todos modos?",
                    "Cambios sin guardar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opcion != JOptionPane.YES_OPTION) {
                return;
            }
        }
        pestanasEditor.remove(editor);
        etiquetasPestana.remove(editor);
        if (editor.getArchivo() != null) {
            editoresAbiertos.remove(editor.getArchivo());
        } else {
            editoresAbiertos.values().remove(editor);
        }
    }

    private PanelEditor editorActivo() {
        return (PanelEditor) pestanasEditor.getSelectedComponent();
    }

    private void guardarActivo() {
        PanelEditor editor = editorActivo();
        if (editor == null) {
            return;
        }
        if (editor.getArchivo() == null) {
            guardarComoActivo();
            return;
        }
        try {
            editor.guardar();
            actualizarTituloPestana(editor);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarComoActivo() {
        PanelEditor editor = editorActivo();
        if (editor == null) {
            return;
        }
        JFileChooser selector = new JFileChooser(arbolArchivos.getRaiz().toFile());
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = selector.getSelectedFile().toPath();
        try {
            Files.writeString(destino, editor.getContenido());
            editoresAbiertos.remove(editor.getArchivo());
            editor.setArchivo(destino);
            editoresAbiertos.put(destino, editor);
            actualizarTituloPestana(editor);
            arbolArchivos.refrescar();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarTituloPestana(PanelEditor editor) {
        javax.swing.JLabel etiqueta = etiquetasPestana.get(editor);
        if (etiqueta != null) {
            etiqueta.setText(editor.getNombrePestana());
        }
    }

    // =====================================================================
    // Compilar
    // =====================================================================

    private void compilarActivo() {
        PanelEditor editor = editorActivo();
        if (editor == null) {
            return;
        }
        // Se guardan TODAS las pestañas modificadas, no solo la activa: si
        // Pila.z esta abierto y modificado pero el foco esta en main.pig,
        // compilar debe reflejar lo que se ve en el editor de Pila.z
        // tambien, no la version vieja que quedo en disco.
        guardarTodosLosModificados();
        if (editor.getArchivo() == null) {
            JOptionPane.showMessageDialog(this, "Guarda el archivo antes de compilar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ControladorCompilacion.compilar(editor.getArchivo(), panelSalida);
        arbolArchivos.refrescar(); // por si se genero un nuevo .c junto al .pig
    }

    private void guardarTodosLosModificados() {
        for (PanelEditor editor : editoresAbiertos.values()) {
            if (editor.isModificado() && editor.getArchivo() != null) {
                try {
                    editor.guardar();
                    actualizarTituloPestana(editor);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "No se pudo guardar "
                            + editor.getArchivo().getFileName() + ": " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // =====================================================================
    public static void main(String[] args) {
        Path carpetaInicial = (args.length > 0) ? Path.of(args[0]) : Path.of(".");
        SwingUtilities.invokeLater(() -> new VentanaPrincipal(carpetaInicial).setVisible(true));
    }
}
