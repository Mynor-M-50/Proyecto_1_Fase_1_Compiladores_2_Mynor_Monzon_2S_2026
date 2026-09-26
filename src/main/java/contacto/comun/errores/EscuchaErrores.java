package contacto.comun.errores;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Conecta los errores que ANTLR detecta por su cuenta (lexicos y
 * sintacticos) con el mismo RecolectorErrores que usan los
 * analizadores semanticos, para que la UI muestre una sola lista.
 * Lleva el nombre del archivo que esta escuchando, para que el error
 * final diga de donde vino (importante con el orquestador, que
 * escucha varios archivos en una sola compilacion).
 */
public final class EscuchaErrores extends BaseErrorListener {

    private final RecolectorErrores errores;
    private final TipoError tipo;
    private final String archivo;

    public EscuchaErrores(RecolectorErrores errores, TipoError tipo, String archivo) {
        this.errores = errores;
        this.tipo = tipo;
        this.archivo = archivo;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                             int linea, int columna, String mensaje,
                             RecognitionException excepcion) {
        errores.agregar(tipo, mensaje, linea, columna, archivo);
    }
}
