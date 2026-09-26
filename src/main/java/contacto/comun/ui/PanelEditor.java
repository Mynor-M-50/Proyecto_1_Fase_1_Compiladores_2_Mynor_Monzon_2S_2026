package contacto.comun.ui;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Una pestaña del editor: un JTextPane con StyledDocument, recoloreado
 * cada vez que el texto cambia. El recoloreo se demora 150ms desde la
 * ultima tecla (Timer) para no re-tokenizar el archivo completo en
 * cada pulsacion si la persona escribe rapido.
 */
public class PanelEditor extends JPanel {

    private final JTextPane textPane = new JTextPane();
    private final Timer temporizadorResaltado;
    private final ResaltadorSintaxis resaltador;

    private Path archivo; // null si es un archivo nuevo, todavia no guardado
    private boolean modificado = false;
    private Runnable alModificar; // avisa a VentanaPrincipal para refrescar el "*" de la pestaña en vivo

    public PanelEditor(Path archivo, String contenido) {
        super(new BorderLayout());
        this.archivo = archivo;
        this.resaltador = (archivo != null) ? FabricaResaltador.paraArchivo(archivo.getFileName().toString()) : null;

        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textPane.setBackground(TemaEditor.FONDO);
        textPane.setForeground(TemaEditor.TEXTO_NORMAL);
        textPane.setCaretColor(TemaEditor.TEXTO_NORMAL);
        textPane.setSelectionColor(TemaEditor.SELECCION);
        textPane.setText(contenido);

        JScrollPane scroll = new JScrollPane(textPane);
        scroll.getViewport().setBackground(TemaEditor.FONDO);
        add(scroll, BorderLayout.CENTER);

        temporizadorResaltado = new Timer(150, e -> resaltar());
        temporizadorResaltado.setRepeats(false);

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                marcarModificado();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                marcarModificado();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // cambios de solo estilo (el propio resaltado); no cuenta como modificacion del contenido
            }
        });

        resaltar();
    }

    private void marcarModificado() {
        modificado = true;
        if (alModificar != null) {
            alModificar.run();
        }
        temporizadorResaltado.restart();
    }

    public void setAlModificar(Runnable alModificar) {
        this.alModificar = alModificar;
    }

    private void resaltar() {
        if (resaltador == null) {
            return; // extension sin resaltador (p.ej. .txt): se edita en texto plano
        }
        SwingUtilities.invokeLater(() -> {
            StyledDocument documento = textPane.getStyledDocument();
            String texto;
            try {
                // OJO: textPane.getText() devuelve el texto con los \r\n
                // originales del archivo; el Document de Swing normaliza a
                // un solo \n por linea. Si el archivo viene de Windows
                // (CRLF, como los de test-programs/pila), tokenizar con
                // getText() desfasa las posiciones una cada linea y el
                // coloreado se ve "corrido". Por eso se lee del documento.
                texto = documento.getText(0, documento.getLength());
            } catch (javax.swing.text.BadLocationException ex) {
                return;
            }

            List<TokenColoreado> tokens = resaltador.tokenizar(texto);

            SimpleAttributeSet normal = new SimpleAttributeSet();
            StyleConstants.setForeground(normal, TemaEditor.TEXTO_NORMAL);
            documento.setCharacterAttributes(0, texto.length(), normal, true);

            for (TokenColoreado token : tokens) {
                SimpleAttributeSet estilo = new SimpleAttributeSet();
                StyleConstants.setForeground(estilo, TemaEditor.colorDe(token.getCategoria()));
                if (token.getCategoria() == CategoriaToken.PALABRA_CLAVE) {
                    StyleConstants.setBold(estilo, true);
                }
                if (token.getCategoria() == CategoriaToken.COMENTARIO) {
                    StyleConstants.setItalic(estilo, true);
                }
                int longitud = token.getFin() - token.getInicio();
                if (token.getInicio() >= 0 && longitud > 0 && token.getFin() <= texto.length()) {
                    documento.setCharacterAttributes(token.getInicio(), longitud, estilo, false);
                }
            }
        });
    }

    public String getContenido() {
        return textPane.getText();
    }

    public Path getArchivo() {
        return archivo;
    }

    public void setArchivo(Path archivo) {
        this.archivo = archivo;
    }

    public boolean isModificado() {
        return modificado;
    }

    public void guardar() throws java.io.IOException {
        if (archivo == null) {
            throw new IllegalStateException("Este archivo todavia no tiene una ruta (usar guardar como)");
        }
        Files.writeString(archivo, getContenido());
        modificado = false;
    }

    public String getNombrePestana() {
        String base = (archivo != null) ? archivo.getFileName().toString() : "Sin titulo";
        return modificado ? base + " *" : base;
    }
}
