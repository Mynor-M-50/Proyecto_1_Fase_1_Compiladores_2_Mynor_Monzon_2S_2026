package contacto.comun.ui;

import contacto.piglatin.PigLatinLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Coloreado de archivos .pig: los tokeniza con el mismo PigLatinLexer
 * que usa el compilador (no una libreria aparte) y clasifica cada token
 * en una categoria de color (palabra clave, tipo, cadena, numero,
 * comentario o identificador). Los comentarios llegan porque la
 * gramatica los manda al canal HIDDEN en vez de descartarlos.
 */
public class ResaltadorPigLatin implements ResaltadorSintaxis {

    // Pasa el texto por el lexer de Pig Latin y guarda la posicion y categoria de cada token
    @Override
    public List<TokenColoreado> tokenizar(String texto) {
        List<TokenColoreado> resultado = new ArrayList<>();
        PigLatinLexer lexer = new PigLatinLexer(CharStreams.fromString(texto));
        lexer.removeErrorListeners();

        Token token;
        while ((token = lexer.nextToken()).getType() != Token.EOF) {
            CategoriaToken categoria = categoriaDe(token.getType());
            if (categoria != CategoriaToken.DEFECTO) {
                resultado.add(new TokenColoreado(token.getStartIndex(), token.getStopIndex() + 1, categoria));
            }
        }
        return resultado;
    }

    // Categoria de color de cada tipo de token del lexer
    private CategoriaToken categoriaDe(int tipo) {
        if (tipo == PigLatinLexer.VARIABILES || tipo == PigLatinLexer.MAIOR || tipo == PigLatinLexer.FIN_PROGRAMA
                || tipo == PigLatinLexer.IMPORT || tipo == PigLatinLexer.ESTO || tipo == PigLatinLexer.SERIES
                || tipo == PigLatinLexer.NOVUS || tipo == PigLatinLexer.FINIS || tipo == PigLatinLexer.SI
                || tipo == PigLatinLexer.ALITER || tipo == PigLatinLexer.DUM || tipo == PigLatinLexer.FACERE
                || tipo == PigLatinLexer.PER || tipo == PigLatinLexer.PERGE || tipo == PigLatinLexer.INTERRUMPE
                || tipo == PigLatinLexer.REDDERE || tipo == PigLatinLexer.NON || tipo == PigLatinLexer.VERUM
                || tipo == PigLatinLexer.FALSUS) {
            return CategoriaToken.PALABRA_CLAVE;
        }
        if (tipo == PigLatinLexer.NUMERUS || tipo == PigLatinLexer.DECIMALIS || tipo == PigLatinLexer.TEXTUM
                || tipo == PigLatinLexer.LITTERA || tipo == PigLatinLexer.BOOL) {
            return CategoriaToken.TIPO_PRIMITIVO;
        }
        if (tipo == PigLatinLexer.CADENA || tipo == PigLatinLexer.CARACTER) {
            return CategoriaToken.CADENA;
        }
        if (tipo == PigLatinLexer.ENTERO || tipo == PigLatinLexer.DECIMAL) {
            return CategoriaToken.NUMERO;
        }
        if (tipo == PigLatinLexer.COMENTARIO_BLOQUE || tipo == PigLatinLexer.COMENTARIO_LINEA) {
            return CategoriaToken.COMENTARIO;
        }
        if (tipo == PigLatinLexer.ID) {
            return CategoriaToken.IDENTIFICADOR;
        }
        return CategoriaToken.DEFECTO;
    }
}
