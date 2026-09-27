package contacto.comun.ui;

import java.awt.Color;

/** Paleta de colores del editor. Un solo lugar para ajustar el tema. */
public final class TemaEditor {

    public static final Color FONDO = new Color(0x1E1E1E);
    public static final Color TEXTO_NORMAL = new Color(0xD4D4D4);
    public static final Color FONDO_PANEL = new Color(0x252526);
    public static final Color BORDE = new Color(0x3C3C3C);
    public static final Color SELECCION = new Color(0x264F78);
    public static final Color LINEA_ACTUAL = new Color(0x2A2A2A);

    private static final Color PALABRA_CLAVE = new Color(0x569CD6);
    private static final Color TIPO_PRIMITIVO = new Color(0x4EC9B0);
    private static final Color CADENA = new Color(0xCE9178);
    private static final Color NUMERO = new Color(0xB5CEA8);
    private static final Color COMENTARIO = new Color(0x6A9955);
    private static final Color IDENTIFICADOR = TEXTO_NORMAL;

    private TemaEditor() {
    }

    // Color de cada categoria de token
    public static Color colorDe(CategoriaToken categoria) {
        switch (categoria) {
            case PALABRA_CLAVE: return PALABRA_CLAVE;
            case TIPO_PRIMITIVO: return TIPO_PRIMITIVO;
            case CADENA: return CADENA;
            case NUMERO: return NUMERO;
            case COMENTARIO: return COMENTARIO;
            case IDENTIFICADOR: return IDENTIFICADOR;
            default: return TEXTO_NORMAL;
        }
    }
}
