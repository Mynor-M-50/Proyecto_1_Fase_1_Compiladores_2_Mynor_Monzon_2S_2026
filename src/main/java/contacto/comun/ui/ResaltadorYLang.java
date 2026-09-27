package contacto.comun.ui;

import contacto.ylang.YLangLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * OJO: YLangLexer tiene indentacion sensible al contexto
 * (YLangLexerBase inyecta NEWLINE/INDENT/DEDENT). Esos tokens
 * sinteticos no coinciden con ninguna categoria de coloreado y quedan
 * en DEFECTO (se ignoran), asi que no hace falta tratarlos aparte.
 */
public class ResaltadorYLang implements ResaltadorSintaxis {

    // Pasa el texto por el lexer de Y? y guarda la posicion y categoria de cada token
    @Override
    public List<TokenColoreado> tokenizar(String texto) {
        List<TokenColoreado> resultado = new ArrayList<>();
        YLangLexer lexer = new YLangLexer(CharStreams.fromString(texto));
        lexer.removeErrorListeners();

        Token token;
        while ((token = lexer.nextToken()).getType() != Token.EOF) {
            // Los tokens sinteticos (NEWLINE/INDENT/DEDENT) no coinciden
            // con ninguna categoria de abajo y quedan en DEFECTO, que se
            // ignora igual -- no hace falta filtrarlos aparte.
            CategoriaToken categoria = categoriaDe(token.getType());
            if (categoria != CategoriaToken.DEFECTO) {
                resultado.add(new TokenColoreado(token.getStartIndex(), token.getStopIndex() + 1, categoria));
            }
        }
        return resultado;
    }

    // Categoria de color de cada tipo de token del lexer
    private CategoriaToken categoriaDe(int tipo) {
        if (tipo == YLangLexer.SI || tipo == YLangLexer.ENTONCES || tipo == YLangLexer.SINO
                || tipo == YLangLexer.CONTRARIO || tipo == YLangLexer.ELEGIR || tipo == YLangLexer.CASO
                || tipo == YLangLexer.SIEMPRE || tipo == YLangLexer.ROMPER || tipo == YLangLexer.PARA
                || tipo == YLangLexer.MIENTRAS || tipo == YLangLexer.HACER || tipo == YLangLexer.CONTINUAR
                || tipo == YLangLexer.RETORNAR || tipo == YLangLexer.IMPRIMIR || tipo == YLangLexer.LEER
                || tipo == YLangLexer.VERDADERO || tipo == YLangLexer.FALSO || tipo == YLangLexer.DEFINIR
                || tipo == YLangLexer.ESTRUCTURA || tipo == YLangLexer.SECCION_ESTRUCTURAS
                || tipo == YLangLexer.SECCION_FUNCIONES) {
            return CategoriaToken.PALABRA_CLAVE;
        }
        if (tipo == YLangLexer.KW_ENTERO || tipo == YLangLexer.KW_FLOTANTE || tipo == YLangLexer.KW_CARACTER
                || tipo == YLangLexer.KW_CADENA || tipo == YLangLexer.KW_BOOL) {
            return CategoriaToken.TIPO_PRIMITIVO;
        }
        if (tipo == YLangLexer.CADENA_LITERAL || tipo == YLangLexer.CARACTER_LITERAL) {
            return CategoriaToken.CADENA;
        }
        if (tipo == YLangLexer.ENTERO_LITERAL || tipo == YLangLexer.FLOTANTE_LITERAL) {
            return CategoriaToken.NUMERO;
        }
        if (tipo == YLangLexer.LINE_COMMENT || tipo == YLangLexer.BLOCK_COMMENT) {
            return CategoriaToken.COMENTARIO;
        }
        if (tipo == YLangLexer.ID) {
            return CategoriaToken.IDENTIFICADOR;
        }
        return CategoriaToken.DEFECTO;
    }
}
