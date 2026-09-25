package contacto.comun.errores;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Conecta los errores que ANTLR detecta por su cuenta (lexicos y
 * sintacticos) con el mismo RecolectorErrores que usan los
 * analizadores semanticos, para que la UI muestre una sola lista.
 *
 * Uso tipico al armar el lexer/parser de cualquiera de los 3 lenguajes:
 *
 *   lexer.removeErrorListeners();
 *   lexer.addErrorListener(new EscuchaErrores(errores, TipoError.LEXICO));
 *   parser.removeErrorListeners();
 *   parser.addErrorListener(new EscuchaErrores(errores, TipoError.SINTACTICO));
 */
public final class EscuchaErrores extends BaseErrorListener {

    private final RecolectorErrores errores;
    private final TipoError tipo;

    public EscuchaErrores(RecolectorErrores errores, TipoError tipo) {
        this.errores = errores;
        this.tipo = tipo;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                             int linea, int columna, String mensaje,
                             RecognitionException excepcion) {
        errores.agregar(tipo, mensaje, linea, columna);
    }
}
