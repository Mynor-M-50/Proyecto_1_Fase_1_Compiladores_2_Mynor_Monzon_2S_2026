package contacto.comun.ui;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Arbol de trabajo sobre el sistema de archivos real: abrir, crear
 * archivo/carpeta, eliminar y renombrar. Se reconstruye completo cada
 * vez que algo cambia (sencillo y suficiente para el tamano de
 * proyecto de este curso; nada de carga perezosa).
 */
public class ArbolArchivos extends JPanel {

    private final JTree arbol = new JTree();
    private Path raiz;
    private Consumer<Path> alAbrirArchivo;

    public ArbolArchivos(Path raiz) {
        super(new BorderLayout());
        this.raiz = raiz;

        arbol.setRootVisible(true);
        arbol.setShowsRootHandles(true);
        arbol.setCellRenderer(new RenderizadorNodo());
        add(new JScrollPane(arbol), BorderLayout.CENTER);

        arbol.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirSeleccionado();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mostrarMenuSiCorresponde(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mostrarMenuSiCorresponde(e);
            }
        });

        refrescar();
    }

    public void setAlAbrirArchivo(Consumer<Path> escuchador) {
        this.alAbrirArchivo = escuchador;
    }

    public void cambiarRaiz(Path nuevaRaiz) {
        this.raiz = nuevaRaiz;
        refrescar();
    }

    public Path getRaiz() {
        return raiz;
    }

    public void refrescar() {
        DefaultMutableTreeNode nodoRaiz = construirNodo(raiz);
        arbol.setModel(new DefaultTreeModel(nodoRaiz));
        arbol.expandRow(0);
    }

    private DefaultMutableTreeNode construirNodo(Path ruta) {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(ruta);
        if (Files.isDirectory(ruta)) {
            try (Stream<Path> hijos = Files.list(ruta)) {
                hijos.sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p))
                                .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                        .forEach(hijo -> nodo.add(construirNodo(hijo)));
            } catch (IOException e) {
                // carpeta sin permiso de lectura: se muestra vacia, no rompe el arbol
            }
        }
        return nodo;
    }

    private Path pathSeleccionado() {
        TreePath seleccion = arbol.getSelectionPath();
        if (seleccion == null) {
            return null;
        }
        DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) seleccion.getLastPathComponent();
        return (Path) nodo.getUserObject();
    }

    private void abrirSeleccionado() {
        Path seleccionado = pathSeleccionado();
        if (seleccionado != null && Files.isRegularFile(seleccionado) && alAbrirArchivo != null) {
            alAbrirArchivo.accept(seleccionado);
        }
    }

    private void mostrarMenuSiCorresponde(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        TreePath ruta = arbol.getPathForLocation(e.getX(), e.getY());
        if (ruta != null) {
            arbol.setSelectionPath(ruta);
        }
        Path base = pathSeleccionado();
        if (base == null) {
            base = raiz;
        }
        Path carpetaBase = Files.isDirectory(base) ? base : base.getParent();

        JPopupMenu menu = new JPopupMenu();

        JMenuItem nuevoArchivo = new JMenuItem("Nuevo archivo");
        nuevoArchivo.addActionListener(ev -> crearArchivo(carpetaBase));
        menu.add(nuevoArchivo);

        JMenuItem nuevaCarpeta = new JMenuItem("Nueva carpeta");
        nuevaCarpeta.addActionListener(ev -> crearCarpeta(carpetaBase));
        menu.add(nuevaCarpeta);

        if (!base.equals(raiz)) {
            menu.addSeparator();
            JMenuItem renombrar = new JMenuItem("Renombrar");
            Path finalBase = base;
            renombrar.addActionListener(ev -> renombrar(finalBase));
            menu.add(renombrar);

            JMenuItem eliminar = new JMenuItem("Eliminar");
            eliminar.addActionListener(ev -> eliminar(finalBase));
            menu.add(eliminar);
        }

        menu.addSeparator();
        JMenuItem refrescarItem = new JMenuItem("Refrescar");
        refrescarItem.addActionListener(ev -> refrescar());
        menu.add(refrescarItem);

        menu.show(arbol, e.getX(), e.getY());
    }

    private void crearArchivo(Path carpeta) {
        String nombre = JOptionPane.showInputDialog(this, "Nombre del archivo (con extension .y, .z o .pig):");
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        try {
            Files.createFile(carpeta.resolve(nombre));
            refrescar();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el archivo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void crearCarpeta(Path carpeta) {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la carpeta:");
        if (nombre == null || nombre.isBlank()) {
            return;
        }
        try {
            Files.createDirectory(carpeta.resolve(nombre));
            refrescar();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo crear la carpeta: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renombrar(Path ruta) {
        String nombreActual = ruta.getFileName().toString();
        String nuevoNombre = JOptionPane.showInputDialog(this, "Nuevo nombre:", nombreActual);
        if (nuevoNombre == null || nuevoNombre.isBlank() || nuevoNombre.equals(nombreActual)) {
            return;
        }
        try {
            Files.move(ruta, ruta.resolveSibling(nuevoNombre));
            refrescar();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo renombrar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar(Path ruta) {
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar '" + ruta.getFileName() + "'? Esta accion no se puede deshacer.",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            eliminarRecursivo(ruta);
            refrescar();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarRecursivo(Path ruta) throws IOException {
        if (Files.isDirectory(ruta)) {
            try (Stream<Path> hijos = Files.list(ruta)) {
                for (Path hijo : hijos.toList()) {
                    eliminarRecursivo(hijo);
                }
            }
        }
        Files.delete(ruta);
    }

    /** Muestra solo el nombre del archivo/carpeta (path.toString() daria la ruta completa). */
    private final class RenderizadorNodo extends DefaultTreeCellRenderer {
        @Override
        public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object valorUsuario = ((DefaultMutableTreeNode) value).getUserObject();
            if (!(valorUsuario instanceof Path)) {
                // new JTree() sin modelo trae un arbol de muestra con nodos de
                // texto plano; esto puede pintarse antes de que refrescar()
                // (llamado en el constructor) reemplace el modelo.
                return this;
            }
            Path ruta = (Path) valorUsuario;
            boolean esRaiz = ruta.equals(raiz);
            setText(esRaiz ? ruta.toString() : ruta.getFileName().toString());
            return this;
        }
    }
}
