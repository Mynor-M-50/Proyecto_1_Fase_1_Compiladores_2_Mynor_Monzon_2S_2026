package contacto.comun.ui;

import contacto.zetariano.ZetarianoLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public class ResaltadorZetariano implements ResaltadorSintaxis {

    @Override
    public List<TokenColoreado> tokenizar(String texto) {
        List<TokenColoreado> resultado = new ArrayList<>();
        ZetarianoLexer lexer = new ZetarianoLexer(CharStreams.fromString(texto));
        lexer.removeErrorListeners(); // un caracter invalido mientras se escribe no debe imprimir en consola

        Token token;
        while ((token = lexer.nextToken()).getType() != Token.EOF) {
            CategoriaToken categoria = categoriaDe(token.getType());
            if (categoria != CategoriaToken.DEFECTO) {
                resultado.add(new TokenColoreado(token.getStartIndex(), token.getStopIndex() + 1, categoria));
            }
        }
        return resultado;
    }

    private CategoriaToken categoriaDe(int tipo) {
        if (tipo == ZetarianoLexer.PUBLIC || tipo == ZetarianoLexer.PRIVATE || tipo == ZetarianoLexer.THIS
                || tipo == ZetarianoLexer.CLASS || tipo == ZetarianoLexer.VOID || tipo == ZetarianoLexer.NEW
                || tipo == ZetarianoLexer.NULL || tipo == ZetarianoLexer.TRUE || tipo == ZetarianoLexer.FALSE
                || tipo == ZetarianoLexer.IF || tipo == ZetarianoLexer.ELSE || tipo == ZetarianoLexer.SWITCH
                || tipo == ZetarianoLexer.CASE || tipo == ZetarianoLexer.DEFAULT || tipo == ZetarianoLexer.BREAK
                || tipo == ZetarianoLexer.FOR || tipo == ZetarianoLexer.WHILE || tipo == ZetarianoLexer.DO
                || tipo == ZetarianoLexer.CONTINUE || tipo == ZetarianoLexer.RETURN || tipo == ZetarianoLexer.PRINTLN
                || tipo == ZetarianoLexer.PRINT || tipo == ZetarianoLexer.READLN) {
            return CategoriaToken.PALABRA_CLAVE;
        }
        if (tipo == ZetarianoLexer.KW_INT || tipo == ZetarianoLexer.KW_DOUBLE || tipo == ZetarianoLexer.KW_CHAR
                || tipo == ZetarianoLexer.KW_BOOLEAN || tipo == ZetarianoLexer.KW_STRING) {
            return CategoriaToken.TIPO_PRIMITIVO;
        }
        if (tipo == ZetarianoLexer.STRING_LITERAL || tipo == ZetarianoLexer.CHAR_LITERAL) {
            return CategoriaToken.CADENA;
        }
        if (tipo == ZetarianoLexer.INT_LITERAL || tipo == ZetarianoLexer.DOUBLE_LITERAL) {
            return CategoriaToken.NUMERO;
        }
        if (tipo == ZetarianoLexer.LINE_COMMENT || tipo == ZetarianoLexer.BLOCK_COMMENT) {
            return CategoriaToken.COMENTARIO;
        }
        if (tipo == ZetarianoLexer.ID) {
            return CategoriaToken.IDENTIFICADOR;
        }
        return CategoriaToken.DEFECTO;
    }
}
