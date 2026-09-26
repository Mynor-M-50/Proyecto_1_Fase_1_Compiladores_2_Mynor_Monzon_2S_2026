package contacto.comun.ui;

import java.util.List;

/**
 * Tokeniza un texto completo y devuelve cada token ya clasificado para
 * pintarlo. Cada lenguaje (Zetariano, Y?, Pig Latin) implementa esto
 * usando SU PROPIO lexer de ANTLR -- el mismo que ya genera el
 * compilador, no una libreria de coloreado aparte. Eso es justo lo que
 * pide el proyecto: coloreado sin librerias, en tiempo real.
 */
public interface ResaltadorSintaxis {
    List<TokenColoreado> tokenizar(String texto);
}
