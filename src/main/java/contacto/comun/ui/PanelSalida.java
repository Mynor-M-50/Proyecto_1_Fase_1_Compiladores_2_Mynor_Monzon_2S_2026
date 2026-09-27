package contacto.comun.ui;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;

/** Consola de salida: errores, cuartetas generadas y el .c final, cada uno en su pestaña. */
public class PanelSalida extends JPanel {

    private final JTextArea areaErrores = crearArea();
    private final JTextArea areaCuartetas = crearArea();
    private final JTextArea areaCodigoC = crearArea();
    private final JTabbedPane pestanas = new JTabbedPane();

    // Tres pestanas: Errores, Cuartetas y Codigo C
    public PanelSalida() {
        super(new BorderLayout());
        pestanas.addTab("Errores", new JScrollPane(areaErrores));
        pestanas.addTab("Cuartetas", new JScrollPane(areaCuartetas));
        pestanas.addTab("Codigo C", new JScrollPane(areaCodigoC));
        add(pestanas, BorderLayout.CENTER);
    }

    // Area de texto de solo lectura con la fuente y colores del tema
    private JTextArea crearArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(TemaEditor.FONDO_PANEL);
        area.setForeground(TemaEditor.TEXTO_NORMAL);
        return area;
    }

    // Vacia las tres pestanas antes de cada compilacion
    public void limpiar() {
        areaErrores.setText("");
        areaCuartetas.setText("");
        areaCodigoC.setText("");
    }

    // Muestra el texto en Errores y cambia a esa pestana
    public void mostrarErrores(String texto) {
        areaErrores.setText(texto);
        areaErrores.setCaretPosition(0); // sin esto, un mensaje largo (ruta de archivo) deja el inicio fuera de vista
        pestanas.setSelectedIndex(0);
    }

    // Muestra las cuartetas (sin cambiar de pestana)
    public void mostrarCuartetas(String texto) {
        areaCuartetas.setText(texto);
        areaCuartetas.setCaretPosition(0);
    }

    // Muestra el codigo C y cambia a esa pestana
    public void mostrarCodigoC(String texto) {
        areaCodigoC.setText(texto);
        areaCodigoC.setCaretPosition(0);
        pestanas.setSelectedIndex(2);
    }
}
