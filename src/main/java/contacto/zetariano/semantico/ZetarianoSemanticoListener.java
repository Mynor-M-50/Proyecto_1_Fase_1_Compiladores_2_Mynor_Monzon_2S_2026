package contacto.zetariano.semantico;

import contacto.comun.errores.RecolectorErrores;
import contacto.comun.errores.TipoError;
import contacto.comun.simbolos.Ambito;
import contacto.comun.simbolos.RolSimbolo;
import contacto.comun.simbolos.Simbolo;
import contacto.comun.simbolos.TablaSimbolos;
import contacto.comun.tipos.Operador;
import contacto.comun.tipos.TablaTipos;
import contacto.comun.tipos.Tipo;
import contacto.comun.tipos.TipoPrimitivo;
import contacto.zetariano.ZetarianoBaseListener;
import contacto.zetariano.ZetarianoParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Analiza semanticamente un archivo .z: declara clase/campos/metodos/
 * constructor en la tabla de simbolos, valida tipos de cada expresion y
 * los deja anotados en {@link #getTipos()} para que el generador de
 * cuartetas (otro Listener, en zetariano.generador) no tenga que
 * recalcularlos.
 *
 * Por que Listener y no una jerarquia de AST con instanceof: ANTLR ya
 * reparte cada construccion gramatical a su propio metodo
 * (exitDeclaracionVariable, exitExpAditiva, etc.) - eso ES el despacho
 * polimorfico que antes se simulaba a mano con cadenas de instanceof.
 * Cada metodo de aqui abajo es chico y se encarga de UNA sola regla.
 *
 * Limitacion conocida (a proposito, por tiempo): esto analiza UN SOLO
 * archivo .z de forma aislada. Si el archivo usa otra clase (p.ej.
 * Pila.z usando Nodo), no podemos verificar los campos/metodos exactos
 * de esa otra clase todavia -- se acepta de forma permisiva (se le
 * asigna Tipo.error(), que actua como "no lo se, no valides mas sobre
 * esto"). La verificacion cruzada entre clases se resuelve cuando el
 * orquestador de Pig Latin junte las tablas de simbolos de todos los
 * .z/.y importados (fase de manana).
 */
public class ZetarianoSemanticoListener extends ZetarianoBaseListener {

    private final TablaSimbolos tabla = new TablaSimbolos();
    private final RecolectorErrores errores;
    private final ParseTreeProperty<Tipo> tipos = new ParseTreeProperty<>();

    private String nombreClaseActual;
    private Tipo tipoRetornoActual;
    private int profundidadCiclo;

    public ZetarianoSemanticoListener(RecolectorErrores errores) {
        this.errores = errores;
    }

    public TablaSimbolos getTabla() {
        return tabla;
    }

    /** El generador de cuartetas lee de aqui el tipo ya resuelto de cada expresion. */
    public ParseTreeProperty<Tipo> getTipos() {
        return tipos;
    }

    // =====================================================================
    // Clase, campos, constructor, metodo
    // =====================================================================

    @Override
    public void enterClase(ClaseContext ctx) {
        nombreClaseActual = ctx.ID().getText();
    }

    @Override
    public void exitCampo(CampoContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo(), contarCorchetes(ctx.LBRACKET()));

        if (ctx.expresion() != null) {
            validarInicializador(ctx.expresion(), tipo, ctx);
        }

        Simbolo simbolo = new Simbolo(ctx.ID().getText(), tipo, RolSimbolo.VARIABLE,
                linea(ctx), columna(ctx));
        if (!tabla.declarar(simbolo)) {
            error("Ya existe un miembro llamado '" + ctx.ID().getText()
                    + "' en la clase '" + nombreClaseActual + "'", ctx);
        }
    }

    @Override
    public void enterConstructor(ConstructorContext ctx) {
        String nombre = ctx.ID().getText();
        if (!nombre.equals(nombreClaseActual)) {
            error("El nombre del constructor ('" + nombre
                    + "') debe coincidir con el nombre de la clase ('"
                    + nombreClaseActual + "')", ctx);
        }

        int aridad = (ctx.parametros() != null) ? ctx.parametros().parametro().size() : 0;
        Simbolo simbolo = new Simbolo(claveSobrecarga(nombre, aridad),
                Tipo.estructura(nombreClaseActual), RolSimbolo.CONSTRUCTOR,
                linea(ctx), columna(ctx));
        if (!tabla.declarar(simbolo)) {
            error("Ya existe un constructor de '" + nombreClaseActual
                    + "' con " + aridad + " parametro(s)", ctx);
        }

        tabla.entrarAmbito("constructor");
        declararParametros(ctx.parametros());
        tipoRetornoActual = Tipo.vacio();
    }

    @Override
    public void exitConstructor(ConstructorContext ctx) {
        tabla.salirAmbito();
        tipoRetornoActual = null;
    }

    @Override
    public void enterMetodo(MetodoContext ctx) {
        String nombre = ctx.ID().getText();
        Tipo tipoRetorno = (ctx.VOID() != null)
                ? Tipo.vacio()
                : resolverTipo(ctx.tipo(), contarCorchetes(ctx.LBRACKET()));

        int aridad = (ctx.parametros() != null) ? ctx.parametros().parametro().size() : 0;
        Simbolo simbolo = new Simbolo(claveSobrecarga(nombre, aridad), tipoRetorno,
                RolSimbolo.METODO, linea(ctx), columna(ctx));
        if (!tabla.declarar(simbolo)) {
            error("Ya existe un metodo '" + nombre + "' con " + aridad + " parametro(s)", ctx);
        }

        tabla.entrarAmbito("metodo " + nombre);
        declararParametros(ctx.parametros());
        tipoRetornoActual = tipoRetorno;
    }

    @Override
    public void exitMetodo(MetodoContext ctx) {
        // TODO (pendiente, no hoy): verificar que un metodo no-void
        // retorne un valor en TODOS sus caminos posibles, como hacia
        // el AnalizadorSemantico viejo con todosLosCaminosRetornan().
        tabla.salirAmbito();
        tipoRetornoActual = null;
    }

    private void declararParametros(ParametrosContext ctx) {
        if (ctx == null) {
            return;
        }
        for (ParametroContext parametro : ctx.parametro()) {
            Tipo tipo = resolverTipo(parametro.tipo(), contarCorchetes(parametro.LBRACKET()));
            Simbolo simbolo = new Simbolo(parametro.ID().getText(), tipo, RolSimbolo.PARAMETRO,
                    linea(parametro), columna(parametro));
            if (!tabla.declarar(simbolo)) {
                error("El parametro '" + parametro.ID().getText() + "' esta repetido", parametro);
            }
        }
    }

    // =====================================================================
    // Sentencias
    // =====================================================================

    @Override
    public void exitDeclaracionVariable(DeclaracionVariableContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo(), contarCorchetes(ctx.LBRACKET()));
        declararVariable(ctx.ID().getText(), tipo, ctx.expresion(), ctx);
    }

    // Regla aparte que usa "for(...)" para su inicializacion (misma forma
    // que declaracionVariable pero sin el ';' final). Sin este manejador,
    // la variable del for nunca quedaba declarada en la tabla.
    @Override
    public void exitDeclaracionVariableSinPuntoYComa(DeclaracionVariableSinPuntoYComaContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo(), contarCorchetes(ctx.LBRACKET()));
        declararVariable(ctx.ID().getText(), tipo, ctx.expresion(), ctx);
    }

    private void declararVariable(String nombre, Tipo tipo, ExpresionContext inicializador,
                                   ParserRuleContext nodo) {
        if (inicializador != null) {
            validarInicializador(inicializador, tipo, nodo);
        }
        Simbolo simbolo = new Simbolo(nombre, tipo, RolSimbolo.VARIABLE,
                linea(nodo), columna(nodo));
        if (!tabla.declarar(simbolo)) {
            error("La variable '" + nombre + "' ya fue declarada en este ambito", nodo);
        }
    }

    @Override
    public void exitSentenciaExpresion(SentenciaExpresionContext ctx) {
        List<ExpresionContext> expresiones = ctx.expresion();
        if (expresiones.size() < 2) {
            return; // era solo una llamada/expresion suelta, nada que validar aqui
        }

        ExpresionContext destino = expresiones.get(0);
        ExpresionContext valor = expresiones.get(1);

        if (!esDestinoValido(destino)) {
            error("El lado izquierdo de una asignacion debe ser una variable, "
                    + "this.campo, un elemento de arreglo o un atributo", ctx);
            return;
        }

        Tipo tipoDestino = tipoDe(destino);
        Tipo tipoValor = tipoDe(valor);
        OperadorAsignacionContext op = ctx.operadorAsignacion();

        if (op.ASSIGN() != null) {
            if (!TablaTipos.esAsignable(tipoDestino, tipoValor)) {
                error(TablaTipos.mensajeAsignacion(tipoDestino, tipoValor), ctx);
            }
        } else {
            // += se acepta si es una suma valida (numerica o concatenacion);
            // -= y *= solo entre numericos.
            Operador operador = (op.PLUS_ASSIGN() != null) ? Operador.SUMA
                    : (op.MINUS_ASSIGN() != null) ? Operador.RESTA
                    : Operador.MULTIPLICACION;
            Tipo resultado = TablaTipos.resultadoBinario(tipoDestino, operador, tipoValor);
            if (resultado.esError() && !tipoDestino.esError() && !tipoValor.esError()) {
                error(TablaTipos.mensajeBinario(tipoDestino, operador, tipoValor), ctx);
            }
        }
    }

    private boolean esDestinoValido(ExpresionContext ctx) {
        return ctx instanceof ExpIdContext
                || ctx instanceof ExpAccesoContext
                || ctx instanceof ExpIndiceContext;
    }

    // Nota sobre for/while/doWhile y profundidadCiclo: como el Listener
    // recorre los HIJOS antes de disparar el exit del padre, para que
    // "break"/"continue" vean la profundidad correcta hay que subirla
    // en el enter del ciclo, y bajarla en su exit -- no al reves.
    @Override
    public void enterSentenciaFor(SentenciaForContext ctx) {
        tabla.entrarAmbito("for");
        profundidadCiclo++;
    }

    @Override
    public void exitSentenciaFor(SentenciaForContext ctx) {
        profundidadCiclo--;
        tabla.salirAmbito();
    }

    @Override
    public void enterSentenciaWhile(SentenciaWhileContext ctx) {
        profundidadCiclo++;
    }

    @Override
    public void exitSentenciaWhile(SentenciaWhileContext ctx) {
        profundidadCiclo--;
    }

    @Override
    public void enterSentenciaDoWhile(SentenciaDoWhileContext ctx) {
        profundidadCiclo++;
    }

    @Override
    public void exitSentenciaDoWhile(SentenciaDoWhileContext ctx) {
        profundidadCiclo--;
    }

    @Override
    public void exitSentenciaBreak(SentenciaBreakContext ctx) {
        if (profundidadCiclo == 0) {
            error("'break' solo se puede usar dentro de un ciclo o un switch", ctx);
        }
    }

    @Override
    public void exitSentenciaContinue(SentenciaContinueContext ctx) {
        if (profundidadCiclo == 0) {
            error("'continue' solo se puede usar dentro de un ciclo", ctx);
        }
    }

    @Override
    public void exitSentenciaReturn(SentenciaReturnContext ctx) {
        if (tipoRetornoActual == null) {
            return; // fuera de un metodo/constructor; no deberia pasar segun la gramatica
        }
        boolean tieneValor = ctx.expresion() != null;

        if (tipoRetornoActual.esVacio()) {
            if (tieneValor) {
                error("Este metodo es 'void' y no debe retornar ningun valor", ctx);
            }
            return;
        }

        if (!tieneValor) {
            error("Este metodo debe retornar un valor de tipo " + tipoRetornoActual, ctx);
            return;
        }

        Tipo tipoValor = tipoDe(ctx.expresion());
        if (!TablaTipos.esAsignable(tipoRetornoActual, tipoValor)) {
            error("El metodo declara retornar " + tipoRetornoActual
                    + " pero retorna " + tipoValor, ctx);
        }
    }

    @Override
    public void exitSentenciaIf(SentenciaIfContext ctx) {
        for (ExpresionContext condicion : ctx.expresion()) {
            verificarCondicion(condicion);
        }
    }

    @Override
    public void exitSentenciaSwitch(SentenciaSwitchContext ctx) {
        // El selector de un switch NO tiene que ser booleano (a diferencia
        // de si/mientras): solo pedimos que sea un valor simple, no un
        // arreglo ni un objeto.
        Tipo tipo = tipoDe(ctx.expresion());
        if (!tipo.esError() && (tipo.esArreglo() || tipo.esEstructura() || tipo.esVacio())) {
            error("El valor de un switch debe ser un tipo simple (numero, "
                    + "caracter o cadena), no " + tipo, ctx);
        }
    }

    private void verificarCondicion(ExpresionContext ctx) {
        Tipo tipo = tipoDe(ctx);
        if (!TablaTipos.esCondicionValida(tipo)) {
            error("La condicion debe ser de tipo booleano, no " + tipo, ctx);
        }
    }

    // =====================================================================
    // Expresiones: cada alternativa etiquetada calcula su propio tipo y
    // lo guarda en "tipos" para que su nodo padre (y el generador de
    // cuartetas) lo puedan leer sin recalcularlo.
    // =====================================================================

    @Override
    public void exitExpEntero(ExpEnteroContext ctx) {
        tipos.put(ctx, Tipo.entero());
    }

    @Override
    public void exitExpDecimal(ExpDecimalContext ctx) {
        tipos.put(ctx, Tipo.decimal());
    }

    @Override
    public void exitExpCaracter(ExpCaracterContext ctx) {
        tipos.put(ctx, Tipo.caracter());
    }

    @Override
    public void exitExpCadena(ExpCadenaContext ctx) {
        tipos.put(ctx, Tipo.cadena());
    }

    @Override
    public void exitExpVerdadero(ExpVerdaderoContext ctx) {
        tipos.put(ctx, Tipo.booleano());
    }

    @Override
    public void exitExpFalso(ExpFalsoContext ctx) {
        tipos.put(ctx, Tipo.booleano());
    }

    @Override
    public void exitExpNulo(ExpNuloContext ctx) {
        tipos.put(ctx, Tipo.nulo());
    }

    @Override
    public void exitExpThis(ExpThisContext ctx) {
        tipos.put(ctx, Tipo.estructura(nombreClaseActual));
    }

    @Override
    public void exitExpId(ExpIdContext ctx) {
        String nombre = ctx.ID().getText();
        Simbolo simbolo = tabla.buscar(nombre);
        Tipo resultado;
        if (simbolo == null) {
            error("La variable '" + nombre + "' no ha sido declarada", ctx);
            resultado = Tipo.error();
        } else if (simbolo.esInvocable()) {
            error("'" + nombre + "' es un metodo, debe llamarse con parentesis", ctx);
            resultado = Tipo.error();
        } else {
            resultado = simbolo.getTipo();
        }
        tipos.put(ctx, resultado);
    }

    @Override
    public void exitExpParentesis(ExpParentesisContext ctx) {
        tipos.put(ctx, tipoDe(ctx.expresion()));
    }

    @Override
    public void exitExpUnario(ExpUnarioContext ctx) {
        Tipo operando = tipoDe(ctx.expresion());
        Operador operador = (ctx.NOT() != null) ? Operador.NEGACION_LOGICA : Operador.MENOS_UNARIO;
        Tipo resultado = TablaTipos.resultadoUnario(operador, operando);
        if (resultado.esError() && !operando.esError()) {
            error(TablaTipos.mensajeUnario(operador, operando), ctx);
        }
        tipos.put(ctx, resultado);
    }

    @Override
    public void exitExpIncDecPrefijo(ExpIncDecPrefijoContext ctx) {
        validarIncDec(ctx.expresion(), ctx);
        tipos.put(ctx, tipoDe(ctx.expresion()));
    }

    @Override
    public void exitExpIncDecSufijo(ExpIncDecSufijoContext ctx) {
        validarIncDec(ctx.expresion(), ctx);
        tipos.put(ctx, tipoDe(ctx.expresion()));
    }

    private void validarIncDec(ExpresionContext operando, ParserRuleContext nodo) {
        if (!esDestinoValido(operando)) {
            error("++ / -- solo se puede aplicar a una variable, this.campo "
                    + "o un elemento de arreglo", nodo);
            return;
        }
        Tipo tipo = tipoDe(operando);
        if (!tipo.esError() && !tipo.esNumerico()) {
            error("++ / -- solo se puede aplicar a valores numericos, no a " + tipo, nodo);
        }
    }

    @Override
    public void exitExpMultiplicativa(ExpMultiplicativaContext ctx) {
        Operador operador = (ctx.STAR() != null) ? Operador.MULTIPLICACION
                : (ctx.SLASH() != null) ? Operador.DIVISION
                : Operador.MODULO;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExpAditiva(ExpAditivaContext ctx) {
        Operador operador = (ctx.PLUS() != null) ? Operador.SUMA : Operador.RESTA;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExpRelacional(ExpRelacionalContext ctx) {
        Operador operador = (ctx.LT() != null) ? Operador.MENOR
                : (ctx.GT() != null) ? Operador.MAYOR
                : (ctx.LE() != null) ? Operador.MENOR_IGUAL
                : Operador.MAYOR_IGUAL;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExpIgualdad(ExpIgualdadContext ctx) {
        Operador operador = (ctx.EQ() != null) ? Operador.IGUAL : Operador.DIFERENTE;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExpAnd(ExpAndContext ctx) {
        resolverBinaria(ctx, ctx.expresion(0), Operador.AND, ctx.expresion(1));
    }

    @Override
    public void exitExpOr(ExpOrContext ctx) {
        resolverBinaria(ctx, ctx.expresion(0), Operador.OR, ctx.expresion(1));
    }

    private void resolverBinaria(ParserRuleContext nodo, ExpresionContext izq,
                                  Operador operador, ExpresionContext der) {
        Tipo tipoIzq = tipoDe(izq);
        Tipo tipoDer = tipoDe(der);
        Tipo resultado = TablaTipos.resultadoBinario(tipoIzq, operador, tipoDer);
        if (resultado.esError() && !tipoIzq.esError() && !tipoDer.esError()) {
            error(TablaTipos.mensajeBinario(tipoIzq, operador, tipoDer), nodo);
        }
        tipos.put(nodo, resultado);
    }

    @Override
    public void exitExpTernario(ExpTernarioContext ctx) {
        Tipo condicion = tipoDe(ctx.expresion(0));
        if (!TablaTipos.esCondicionValida(condicion)) {
            error("La condicion del operador ternario debe ser booleana, no " + condicion, ctx);
        }

        Tipo siVerdadero = tipoDe(ctx.expresion(1));
        Tipo siFalso = tipoDe(ctx.expresion(2));
        Tipo resultado;
        if (siVerdadero.esError() || siFalso.esError()) {
            resultado = Tipo.error();
        } else if (siVerdadero.mismoTipoQue(siFalso)) {
            resultado = siVerdadero;
        } else if (TablaTipos.esAsignable(siVerdadero, siFalso)) {
            resultado = siVerdadero;
        } else if (TablaTipos.esAsignable(siFalso, siVerdadero)) {
            resultado = siFalso;
        } else {
            error("Las dos ramas del operador ternario deben ser del mismo tipo ("
                    + siVerdadero + " vs " + siFalso + ")", ctx);
            resultado = Tipo.error();
        }
        tipos.put(ctx, resultado);
    }

    @Override
    public void exitExpIndice(ExpIndiceContext ctx) {
        Tipo base = tipoDe(ctx.expresion(0));
        Tipo indice = tipoDe(ctx.expresion(1));

        if (!indice.esError() && !indice.esNumerico()) {
            error("El indice de un arreglo debe ser numerico, no " + indice, ctx.expresion(1));
        }

        Tipo resultado;
        if (base.esError()) {
            resultado = Tipo.error();
        } else if (!base.esArreglo()) {
            error("No es un arreglo, no se puede indexar con corchetes", ctx);
            resultado = Tipo.error();
        } else {
            resultado = base.tipoElemento();
        }
        tipos.put(ctx, resultado);
    }

    @Override
    public void exitExpAcceso(ExpAccesoContext ctx) {
        Tipo base = tipoDe(ctx.expresion());
        String nombreCampo = ctx.ID().getText();
        tipos.put(ctx, tipoDeMiembro(base, nombreCampo, RolSimbolo.VARIABLE, ctx));
    }

    @Override
    public void exitExpLlamadaMetodo(ExpLlamadaMetodoContext ctx) {
        Tipo base = tipoDe(ctx.expresion());
        String nombreMetodo = ctx.ID().getText();
        int aridad = (ctx.argumentos() != null) ? ctx.argumentos().expresion().size() : 0;

        if (base.esError()) {
            tipos.put(ctx, Tipo.error());
            return;
        }
        if (!base.esEstructura()) {
            error("Solo se puede llamar un metodo sobre un objeto", ctx);
            tipos.put(ctx, Tipo.error());
            return;
        }
        if (!base.getNombreEstructura().equals(nombreClaseActual)) {
            // Metodo de OTRA clase (p.ej. Nodo desde Pila): ver limitacion
            // de analisis de un solo archivo en el comentario de la clase.
            tipos.put(ctx, Tipo.error());
            return;
        }

        Simbolo simbolo = tabla.buscarTipoDefinido(claveSobrecarga(nombreMetodo, aridad));
        if (simbolo == null) {
            error("No existe el metodo '" + nombreMetodo + "' con " + aridad
                    + " argumento(s) en la clase '" + nombreClaseActual + "'", ctx);
            tipos.put(ctx, Tipo.error());
            return;
        }
        tipos.put(ctx, simbolo.getTipo());
    }

    @Override
    public void exitExpNuevoObjeto(ExpNuevoObjetoContext ctx) {
        String nombreClase = ctx.ID().getText();
        int aridad = (ctx.argumentos() != null) ? ctx.argumentos().expresion().size() : 0;

        if (nombreClase.equals(nombreClaseActual)) {
            Simbolo simbolo = tabla.buscarTipoDefinido(claveSobrecarga(nombreClase, aridad));
            if (simbolo == null) {
                error("No existe un constructor de '" + nombreClase + "' con " + aridad
                        + " argumento(s)", ctx);
            }
        }
        // Si es OTRA clase (new Nodo(...) dentro de Pila.z), se acepta sin
        // verificar el constructor -- misma limitacion de un solo archivo.
        tipos.put(ctx, Tipo.estructura(nombreClase));
    }

    @Override
    public void exitExpNuevoArreglo(ExpNuevoArregloContext ctx) {
        Tipo base = resolverTipoPrimitivo(ctx.tipoPrimitivo());
        for (ExpresionContext dimension : ctx.expresion()) {
            Tipo tipoDim = tipoDe(dimension);
            if (!tipoDim.esError() && !tipoDim.esNumerico()) {
                error("El tamanio de un arreglo debe ser numerico, no " + tipoDim, dimension);
            }
        }
        tipos.put(ctx, Tipo.arregloDe(base, ctx.expresion().size()));
    }

    @Override
    public void exitExpArregloLiteral(ExpArregloLiteralContext ctx) {
        List<ExpresionContext> elementos = ctx.expresion();
        if (elementos.isEmpty()) {
            tipos.put(ctx, Tipo.error());
            return;
        }
        Tipo primero = tipoDe(elementos.get(0));
        for (int i = 1; i < elementos.size(); i++) {
            Tipo actual = tipoDe(elementos.get(i));
            if (!actual.esError() && !primero.esError()
                    && !TablaTipos.esAsignable(primero, actual)
                    && !TablaTipos.esAsignable(actual, primero)) {
                error("Los elementos del arreglo no son todos del mismo tipo ("
                        + primero + " vs " + actual + ")", elementos.get(i));
            }
        }
        tipos.put(ctx, primero.esError() ? Tipo.error() : Tipo.arregloDe(primero));
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private Tipo tipoDeMiembro(Tipo base, String nombre, RolSimbolo rolEsperado, ParserRuleContext nodo) {
        if (base.esError()) {
            return Tipo.error();
        }
        if (!base.esEstructura()) {
            error("Solo se puede acceder con '.' a los atributos de un objeto", nodo);
            return Tipo.error();
        }
        if (!base.getNombreEstructura().equals(nombreClaseActual)) {
            // Atributo de OTRA clase: ver limitacion de analisis de un solo archivo.
            return Tipo.error();
        }
        Simbolo simbolo = tabla.buscarTipoDefinido(nombre);
        if (simbolo == null || simbolo.getRol() != rolEsperado) {
            error("La clase '" + nombreClaseActual + "' no tiene el atributo '" + nombre + "'", nodo);
            return Tipo.error();
        }
        return simbolo.getTipo();
    }

    private void validarInicializador(ExpresionContext valor, Tipo tipoEsperado, ParserRuleContext nodo) {
        Tipo tipoValor = tipoDe(valor);
        if (valor instanceof ExpArregloLiteralContext && tipoEsperado.esArreglo()) {
            // Ya se valido elemento por elemento en exitExpArregloLiteral;
            // aqui solo comparamos profundidad de arreglo.
            if (tipoValor.esError()) {
                return; // el error puntual ya se reporto adentro del literal
            }
        }
        if (!TablaTipos.esAsignable(tipoEsperado, tipoValor)) {
            error(TablaTipos.mensajeAsignacion(tipoEsperado, tipoValor), nodo);
        }
    }

    /** Resuelve una regla "tipo" (primitivo o nombre de clase) a un Tipo, con arreglo opcional. */
    private Tipo resolverTipo(TipoContext ctx, int corchetes) {
        Tipo base;
        if (ctx.tipoPrimitivo() != null) {
            base = resolverTipoPrimitivo(ctx.tipoPrimitivo());
        } else {
            base = Tipo.estructura(ctx.ID().getText());
        }
        return corchetes > 0 ? Tipo.arregloDe(base, corchetes) : base;
    }

    private Tipo resolverTipoPrimitivo(TipoPrimitivoContext ctx) {
        if (ctx.KW_INT() != null) return Tipo.entero();
        if (ctx.KW_DOUBLE() != null) return Tipo.decimal();
        if (ctx.KW_CHAR() != null) return Tipo.caracter();
        if (ctx.KW_BOOLEAN() != null) return Tipo.booleano();
        return Tipo.cadena(); // KW_STRING
    }

    private int contarCorchetes(List<TerminalNode> corchetes) {
        return corchetes.size();
    }

    /** Clave usada en la tabla de simbolos para permitir sobrecarga por aridad (cantidad de parametros). */
    private String claveSobrecarga(String nombre, int aridad) {
        return nombre + "#" + aridad;
    }

    private Tipo tipoDe(ParseTree nodo) {
        Tipo tipo = tipos.get(nodo);
        return (tipo != null) ? tipo : Tipo.error();
    }

    private int linea(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    private int columna(ParserRuleContext ctx) {
        return ctx.getStart().getCharPositionInLine();
    }

    private void error(String mensaje, ParserRuleContext ctx) {
        errores.agregar(TipoError.SEMANTICO, mensaje, linea(ctx), columna(ctx));
    }
}
