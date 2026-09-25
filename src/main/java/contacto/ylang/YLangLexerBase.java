package contacto.ylang;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Y? delimita bloques por indentacion, no por llaves. ANTLR4 no soporta
 * esto de forma nativa (es un lenguaje libre de contexto, la indentacion
 * es sensible al contexto), asi que lo resolvemos "a mano" aqui, con la
 * misma tecnica que usan las gramaticas de Python en grammars-v4:
 *
 *   1. El lexer generado (YLangLexer.g4) reconoce un token crudo
 *      NEWLINE_RAW = salto(s) de linea + la indentacion de la siguiente
 *      linea, todo pegado.
 *   2. Esta clase intercepta ese token en nextToken() y, comparando el
 *      ancho de esa indentacion contra una pila (indentStack), decide
 *      si hay que emitir NEWLINE solo, NEWLINE+INDENT (subio el nivel)
 *      o NEWLINE+DEDENT... (bajo uno o mas niveles).
 *
 * OJO / pendiente de probar:
 *   - anchoIndentacion() cuenta caracteres tal cual (un tab = 1). Si el
 *     profesor mezcla tabs y espacios esto puede dar niveles raros;
 *     ajustar si hace falta expandiendo tabs a N espacios.
 *   - Si una linea queda con una indentacion que no calza EXACTO con
 *     ningun nivel de la pila (p.ej. entre dos niveles ya usados), hoy
 *     se hace DEDENT hasta el nivel mas cercano por abajo sin marcar
 *     error. Si el profesor pide error de indentacion explicito, este
 *     es el lugar donde agregarlo.
 */
public abstract class YLangLexerBase extends Lexer {

    private final Deque<Token> pendingTokens = new ArrayDeque<>();
    private final Deque<Integer> indentStack = new ArrayDeque<>();
    private int nestingLevel = 0;
    private boolean vioPrimerTokenReal = false;

    protected YLangLexerBase(CharStream input) {
        super(input);
        indentStack.push(0);
    }

    @Override
    public Token nextToken() {
        if (!pendingTokens.isEmpty()) {
            return pendingTokens.poll();
        }

        Token raw = super.nextToken();

        if (raw.getType() == YLangLexer.LPAREN || raw.getType() == YLangLexer.LBRACKET) {
            nestingLevel++;
            vioPrimerTokenReal = true;
            return raw;
        }
        if (raw.getType() == YLangLexer.RPAREN || raw.getType() == YLangLexer.RBRACKET) {
            if (nestingLevel > 0) {
                nestingLevel--;
            }
            vioPrimerTokenReal = true;
            return raw;
        }

        if (raw.getType() == YLangLexer.NEWLINE_RAW) {
            if (nestingLevel > 0) {
                // Dentro de (...) o [...] los saltos de linea no cuentan
                // para la indentacion (permite partir expresiones largas
                // en varias lineas).
                return nextToken();
            }
            return procesarNewline(raw);
        }

        if (raw.getType() == Token.EOF) {
            return procesarEOF(raw);
        }

        vioPrimerTokenReal = true;
        return raw;
    }

    private Token procesarNewline(Token raw) {
        if (_input.LA(1) == IntStream.EOF || esInicioDeComentario()) {
            // Linea en blanco o de puro comentario: no cambia la
            // indentacion, seguimos como si este salto no existiera.
            return nextToken();
        }
        if (!vioPrimerTokenReal) {
            // No emitimos NEWLINE antes del primer token real del archivo.
            return nextToken();
        }

        int nivel = anchoIndentacion(espaciosFinales(raw.getText()));
        Token newline = fabricarToken(YLangLexer.NEWLINE, raw, "<NEWLINE>");

        int actual = indentStack.peek();
        if (nivel > actual) {
            indentStack.push(nivel);
            pendingTokens.add(fabricarToken(YLangLexer.INDENT, raw, "<INDENT>"));
        } else if (nivel < actual) {
            while (indentStack.peek() > nivel) {
                indentStack.pop();
                pendingTokens.add(fabricarToken(YLangLexer.DEDENT, raw, "<DEDENT>"));
            }
        }

        return newline;
    }

    private Token procesarEOF(Token raw) {
        while (indentStack.size() > 1) {
            indentStack.pop();
            pendingTokens.add(fabricarToken(YLangLexer.DEDENT, raw, "<DEDENT>"));
        }
        pendingTokens.add(raw);
        return pendingTokens.poll();
    }

    private boolean esInicioDeComentario() {
        int c1 = _input.LA(1);
        int c2 = _input.LA(2);
        return c1 == '/' && (c2 == '/' || c2 == '*');
    }

    private String espaciosFinales(String textoNewlineRaw) {
        int ultimoSalto = Math.max(textoNewlineRaw.lastIndexOf('\n'), textoNewlineRaw.lastIndexOf('\r'));
        return ultimoSalto >= 0 ? textoNewlineRaw.substring(ultimoSalto + 1) : "";
    }

    private int anchoIndentacion(String espacios) {
        return espacios.length();
    }

    private CommonToken fabricarToken(int tipo, Token modelo, String texto) {
        CommonToken t = new CommonToken(modelo);
        t.setType(tipo);
        t.setText(texto);
        return t;
    }
}