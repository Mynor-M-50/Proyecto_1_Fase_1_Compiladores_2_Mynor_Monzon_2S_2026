package contacto.zetariano.generador;

import contacto.comun.cuartetas.GeneradorCuartetas;
import contacto.comun.tipos.Tipo;
import contacto.zetariano.ZetarianoBaseVisitor;
import contacto.zetariano.ZetarianoParser.*;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Genera cuartetas (C3D) para un archivo .z YA validado por
 * {@link contacto.zetariano.semantico.ZetarianoSemanticoListener}. Se le
 * pasa el mapa de tipos que ese listener resolvio, por si hace falta
 * mas adelante (p.ej. el generador de C necesitara saber si un "+" es
 * concatenacion de cadenas o suma numerica); hoy no se usa a fondo,
 * queda disponible.
 *
 * Por que Visitor y no Listener (a diferencia del semantico): generar
 * saltos para if/while/for requiere controlar el ORDEN exacto en que
 * se emite cada cosa (evaluar condicion -> emitir salto -> AHI SI
 * visitar el cuerpo -> poner la etiqueta de salida). Un Listener
 * recorre todos los hijos automaticamente antes de que el padre pueda
 * reaccionar; un Visitor deja que este codigo decida cuando llamar
 * visit(...) sobre cada hijo, que es justo lo que hace falta aca.
 *
 * Cada visitExpXxx devuelve el "lugar" (nombre de variable, temporal
 * generado, o literal como texto) donde queda el valor de esa
 * expresion, para que su nodo padre lo use directamente.
 *
 * Limitaciones conocidas de hoy (pendientes para manana):
 *   - Los literales de arreglo ({1,2,3}) no generan todavia las
 *     asignaciones elemento por elemento.
 *   - "new Tipo[n]" no reserva memoria real, solo deja una cuarteta
 *     simbolica -- la reserva real se resuelve en comun.codegen.
 *   - Los nombres de variable se usan tal cual (sin distinguir por
 *     ambito), asi que dos variables con el mismo nombre en scopes
 *     distintos podrian chocar en las cuartetas. Bien para probar el
 *     flujo hoy; hay que revisitar antes de generar C de verdad.
 */
public class ZetarianoGeneradorCuartetas extends ZetarianoBaseVisitor<String> {

    private final GeneradorCuartetas gen = new GeneradorCuartetas();
    private final ParseTreeProperty<Tipo> tipos;

    // pila de {etiquetaContinua, etiquetaFin} del ciclo/switch mas interno activo
    private final Deque<String[]> pilaControlFlujo = new ArrayDeque<>();

    public ZetarianoGeneradorCuartetas(ParseTreeProperty<Tipo> tipos) {
        this.tipos = tipos;
    }

    public GeneradorCuartetas getGenerador() {
        return gen;
    }

    // =====================================================================
    // Programa / clase / miembros
    // =====================================================================

    @Override
    public String visitPrograma(ProgramaContext ctx) {
        visit(ctx.clase());
        return null;
    }

    @Override
    public String visitClase(ClaseContext ctx) {
        for (MiembroContext miembro : ctx.miembro()) {
            visit(miembro);
        }
        return null;
    }

    @Override
    public String visitCampo(CampoContext ctx) {
        // Los campos no generan cuartetas por si solos; su inicializacion
        // (si la hay) se resuelve al generar el constructor en C, en
        // comun.codegen (manana), no aqui.
        return null;
    }

    @Override
    public String visitConstructor(ConstructorContext ctx) {
        gen.emitirEtiqueta("inicio_" + ctx.ID().getText() + "_constructor");
        for (SentenciaContext sentencia : ctx.bloque().sentencia()) {
            visit(sentencia);
        }
        return null;
    }

    @Override
    public String visitMetodo(MetodoContext ctx) {
        gen.emitirEtiqueta("inicio_" + ctx.ID().getText());
        for (SentenciaContext sentencia : ctx.bloque().sentencia()) {
            visit(sentencia);
        }
        return null;
    }

    // =====================================================================
    // Sentencias
    // =====================================================================

    @Override
    public String visitBloque(BloqueContext ctx) {
        for (SentenciaContext sentencia : ctx.sentencia()) {
            visit(sentencia);
        }
        return null;
    }

    @Override
    public String visitDeclaracionVariable(DeclaracionVariableContext ctx) {
        if (ctx.expresion() != null) {
            String origen = visit(ctx.expresion());
            gen.emitirAsignacion(ctx.ID().getText(), origen);
        }
        return null;
    }

    @Override
    public String visitSentenciaExpresion(SentenciaExpresionContext ctx) {
        List<ExpresionContext> expresiones = ctx.expresion();
        if (expresiones.size() < 2) {
            visit(expresiones.get(0)); // llamada/expresion suelta, por su efecto
            return null;
        }

        String destino = visit(expresiones.get(0)); // lugar, no un valor a leer
        String origen = visit(expresiones.get(1));
        OperadorAsignacionContext op = ctx.operadorAsignacion();

        if (op.ASSIGN() != null) {
            gen.emitirAsignacion(destino, origen);
        } else {
            String operador = (op.PLUS_ASSIGN() != null) ? "+"
                    : (op.MINUS_ASSIGN() != null) ? "-" : "*";
            gen.emitirOperacionBinaria(destino, destino, operador, origen);
        }
        return null;
    }

    @Override
    public String visitSentenciaIf(SentenciaIfContext ctx) {
        List<ExpresionContext> condiciones = ctx.expresion();
        List<SentenciaOBloqueContext> cuerpos = ctx.sentenciaOBloque();
        String etiquetaFin = gen.nuevaEtiqueta();

        for (int i = 0; i < condiciones.size(); i++) {
            String condicion = visit(condiciones.get(i));
            String etiquetaSiguiente = gen.nuevaEtiqueta();
            gen.emitirSaltoSiFalso(condicion, etiquetaSiguiente);
            visit(cuerpos.get(i));
            gen.emitirSalto(etiquetaFin);
            gen.emitirEtiqueta(etiquetaSiguiente);
        }

        if (cuerpos.size() > condiciones.size()) {
            visit(cuerpos.get(cuerpos.size() - 1)); // rama "else" final, si existe
        }

        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public String visitSentenciaOBloque(SentenciaOBloqueContext ctx) {
        if (ctx.bloque() != null) {
            visit(ctx.bloque());
        } else {
            visit(ctx.sentencia());
        }
        return null;
    }

    @Override
    public String visitSentenciaWhile(SentenciaWhileContext ctx) {
        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);
        String condicion = visit(ctx.expresion());
        gen.emitirSaltoSiFalso(condicion, etiquetaFin);

        pilaControlFlujo.push(new String[]{etiquetaInicio, etiquetaFin});
        visit(ctx.sentenciaOBloque());
        pilaControlFlujo.pop();

        gen.emitirSalto(etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public String visitSentenciaDoWhile(SentenciaDoWhileContext ctx) {
        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaContinua = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);

        pilaControlFlujo.push(new String[]{etiquetaContinua, etiquetaFin});
        visit(ctx.bloque());
        pilaControlFlujo.pop();

        gen.emitirEtiqueta(etiquetaContinua);
        String condicion = visit(ctx.expresion());
        gen.emitirSaltoSiVerdadero(condicion, etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public String visitSentenciaFor(SentenciaForContext ctx) {
        if (ctx.forInit() != null) {
            visit(ctx.forInit());
        }

        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaContinua = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);
        if (ctx.expresion() != null) {
            String condicion = visit(ctx.expresion());
            gen.emitirSaltoSiFalso(condicion, etiquetaFin);
        }

        pilaControlFlujo.push(new String[]{etiquetaContinua, etiquetaFin});
        visit(ctx.sentenciaOBloque());
        pilaControlFlujo.pop();

        gen.emitirEtiqueta(etiquetaContinua);
        if (ctx.forUpdate() != null) {
            visit(ctx.forUpdate());
        }
        gen.emitirSalto(etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public String visitForInit(ForInitContext ctx) {
        if (ctx.declaracionVariableSinPuntoYComa() != null) {
            visit(ctx.declaracionVariableSinPuntoYComa());
        } else {
            visit(ctx.expresionLista());
        }
        return null;
    }

    @Override
    public String visitDeclaracionVariableSinPuntoYComa(DeclaracionVariableSinPuntoYComaContext ctx) {
        if (ctx.expresion() != null) {
            String origen = visit(ctx.expresion());
            gen.emitirAsignacion(ctx.ID().getText(), origen);
        }
        return null;
    }

    @Override
    public String visitForUpdate(ForUpdateContext ctx) {
        visit(ctx.expresionLista());
        return null;
    }

    @Override
    public String visitExpresionLista(ExpresionListaContext ctx) {
        for (ExpresionContext expresion : ctx.expresion()) {
            visit(expresion);
        }
        return null;
    }

    @Override
    public String visitSentenciaSwitch(SentenciaSwitchContext ctx) {
        String selector = visit(ctx.expresion());
        String etiquetaFin = gen.nuevaEtiqueta();

        pilaControlFlujo.push(new String[]{etiquetaFin, etiquetaFin}); // "break" salta a fin
        for (CasoSwitchContext caso : ctx.casoSwitch()) {
            String etiquetaSiguiente = gen.nuevaEtiqueta();
            String temp = gen.nuevoTemporal();
            gen.emitirOperacionBinaria(temp, selector, "==", caso.literalCaso().getText());
            gen.emitirSaltoSiFalso(temp, etiquetaSiguiente);
            for (SentenciaContext sentencia : caso.sentencia()) {
                visit(sentencia);
            }
            gen.emitirEtiqueta(etiquetaSiguiente);
        }
        if (ctx.casoDefault() != null) {
            for (SentenciaContext sentencia : ctx.casoDefault().sentencia()) {
                visit(sentencia);
            }
        }
        pilaControlFlujo.pop();
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public String visitSentenciaBreak(SentenciaBreakContext ctx) {
        if (!pilaControlFlujo.isEmpty()) {
            gen.emitirSalto(pilaControlFlujo.peek()[1]);
        }
        return null;
    }

    @Override
    public String visitSentenciaContinue(SentenciaContinueContext ctx) {
        if (!pilaControlFlujo.isEmpty()) {
            gen.emitirSalto(pilaControlFlujo.peek()[0]);
        }
        return null;
    }

    @Override
    public String visitSentenciaReturn(SentenciaReturnContext ctx) {
        String valor = (ctx.expresion() != null) ? visit(ctx.expresion()) : null;
        gen.emitirRetorno(valor);
        return null;
    }

    // =====================================================================
    // Expresiones
    // =====================================================================

    @Override
    public String visitExpEntero(ExpEnteroContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitExpDecimal(ExpDecimalContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitExpCaracter(ExpCaracterContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitExpCadena(ExpCadenaContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitExpVerdadero(ExpVerdaderoContext ctx) {
        return "1";
    }

    @Override
    public String visitExpFalso(ExpFalsoContext ctx) {
        return "0";
    }

    @Override
    public String visitExpNulo(ExpNuloContext ctx) {
        return "NULL";
    }

    @Override
    public String visitExpThis(ExpThisContext ctx) {
        return "this";
    }

    @Override
    public String visitExpId(ExpIdContext ctx) {
        return ctx.ID().getText();
    }

    @Override
    public String visitExpParentesis(ExpParentesisContext ctx) {
        return visit(ctx.expresion());
    }

    @Override
    public String visitExpUnario(ExpUnarioContext ctx) {
        String operando = visit(ctx.expresion());
        String operador = (ctx.NOT() != null) ? "!" : "-";
        String temp = gen.nuevoTemporal();
        gen.emitirOperacionUnaria(temp, operador, operando);
        return temp;
    }

    @Override
    public String visitExpIncDecPrefijo(ExpIncDecPrefijoContext ctx) {
        String lugar = visit(ctx.expresion());
        String operador = (ctx.INC() != null) ? "+" : "-";
        gen.emitirOperacionBinaria(lugar, lugar, operador, "1");
        return lugar; // prefijo: el valor resultante es el YA incrementado
    }

    @Override
    public String visitExpIncDecSufijo(ExpIncDecSufijoContext ctx) {
        String lugar = visit(ctx.expresion());
        String temp = gen.nuevoTemporal();
        gen.emitirAsignacion(temp, lugar); // guarda el valor viejo
        String operador = (ctx.INC() != null) ? "+" : "-";
        gen.emitirOperacionBinaria(lugar, lugar, operador, "1");
        return temp; // sufijo: el valor resultante es el ANTERIOR al incremento
    }

    @Override
    public String visitExpMultiplicativa(ExpMultiplicativaContext ctx) {
        String operador = (ctx.STAR() != null) ? "*" : (ctx.SLASH() != null) ? "/" : "%";
        return emitirBinaria(ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public String visitExpAditiva(ExpAditivaContext ctx) {
        String operador = (ctx.PLUS() != null) ? "+" : "-";
        return emitirBinaria(ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public String visitExpRelacional(ExpRelacionalContext ctx) {
        String operador = (ctx.LT() != null) ? "<" : (ctx.GT() != null) ? ">"
                : (ctx.LE() != null) ? "<=" : ">=";
        return emitirBinaria(ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public String visitExpIgualdad(ExpIgualdadContext ctx) {
        String operador = (ctx.EQ() != null) ? "==" : "!=";
        return emitirBinaria(ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public String visitExpAnd(ExpAndContext ctx) {
        return emitirBinaria(ctx.expresion(0), "&&", ctx.expresion(1));
    }

    @Override
    public String visitExpOr(ExpOrContext ctx) {
        return emitirBinaria(ctx.expresion(0), "||", ctx.expresion(1));
    }

    private String emitirBinaria(ExpresionContext izq, String operador, ExpresionContext der) {
        String lugarIzq = visit(izq);
        String lugarDer = visit(der);
        String temp = gen.nuevoTemporal();
        gen.emitirOperacionBinaria(temp, lugarIzq, operador, lugarDer);
        return temp;
    }

    @Override
    public String visitExpTernario(ExpTernarioContext ctx) {
        String condicion = visit(ctx.expresion(0));
        String etiquetaFalso = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();
        String temp = gen.nuevoTemporal();

        gen.emitirSaltoSiFalso(condicion, etiquetaFalso);
        gen.emitirAsignacion(temp, visit(ctx.expresion(1)));
        gen.emitirSalto(etiquetaFin);
        gen.emitirEtiqueta(etiquetaFalso);
        gen.emitirAsignacion(temp, visit(ctx.expresion(2)));
        gen.emitirEtiqueta(etiquetaFin);

        return temp;
    }

    @Override
    public String visitExpIndice(ExpIndiceContext ctx) {
        String base = visit(ctx.expresion(0));
        String indice = visit(ctx.expresion(1));
        return base + "[" + indice + "]";
    }

    @Override
    public String visitExpAcceso(ExpAccesoContext ctx) {
        String base = visit(ctx.expresion());
        return base + "." + ctx.ID().getText();
    }

    @Override
    public String visitExpLlamadaMetodo(ExpLlamadaMetodoContext ctx) {
        String objetivo = visit(ctx.expresion());
        List<String> argumentos = evaluarArgumentos(ctx.argumentos());
        String temp = gen.nuevoTemporal();
        gen.emitirLlamada(temp, objetivo, ctx.ID().getText(), argumentos);
        return temp;
    }

    @Override
    public String visitExpNuevoObjeto(ExpNuevoObjetoContext ctx) {
        List<String> argumentos = evaluarArgumentos(ctx.argumentos());
        String temp = gen.nuevoTemporal();
        gen.emitirLlamada(temp, null, "new_" + ctx.ID().getText(), argumentos);
        return temp;
    }

    @Override
    public String visitExpNuevoArreglo(ExpNuevoArregloContext ctx) {
        // Simplificacion de hoy: solo se deja una cuarteta simbolica con
        // las dimensiones evaluadas; la reserva de memoria real en C se
        // resuelve en comun.codegen (manana).
        List<String> dimensiones = new ArrayList<>();
        for (ExpresionContext dimension : ctx.expresion()) {
            dimensiones.add(visit(dimension));
        }
        String temp = gen.nuevoTemporal();
        gen.emitirLlamada(temp, null, "new_arreglo_" + ctx.tipoPrimitivo().getText(), dimensiones);
        return temp;
    }

    @Override
    public String visitExpArregloLiteral(ExpArregloLiteralContext ctx) {
        // TODO (pendiente, no hoy): emitir las asignaciones elemento por
        // elemento (t[0] = ..., t[1] = ..., etc, recursivo para literales
        // anidados). Por ahora solo evaluamos los elementos, para que sus
        // propias cuartetas (si tienen efectos, como una llamada) queden
        // emitidas, y devolvemos un temporal simbolico.
        for (ExpresionContext elemento : ctx.expresion()) {
            visit(elemento);
        }
        return gen.nuevoTemporal();
    }

    @Override
    public String visitExpLlamadaPrintln(ExpLlamadaPrintlnContext ctx) {
        if (ctx.expresion() != null) {
            gen.emitirImprimir(visit(ctx.expresion()));
        }
        return null;
    }

    @Override
    public String visitExpLlamadaPrint(ExpLlamadaPrintContext ctx) {
        if (ctx.expresion() != null) {
            gen.emitirImprimir(visit(ctx.expresion()));
        }
        return null;
    }

    @Override
    public String visitExpLlamadaReadln(ExpLlamadaReadlnContext ctx) {
        String temp = gen.nuevoTemporal();
        gen.emitirLeer(temp);
        return temp;
    }
}
