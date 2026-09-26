package contacto.ylang.semantico;

import contacto.comun.errores.RecolectorErrores;
import contacto.comun.errores.TipoError;
import contacto.comun.simbolos.RolSimbolo;
import contacto.comun.simbolos.Simbolo;
import contacto.comun.simbolos.TablaSimbolos;
import contacto.comun.tipos.Operador;
import contacto.comun.tipos.TablaTipos;
import contacto.comun.tipos.Tipo;
import contacto.ylang.YLangParserBaseListener;
import contacto.ylang.YLangParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analiza semanticamente un archivo .y: registra estructuras y
 * funciones, valida tipos y los deja anotados para el generador de
 * cuartetas. Mismo enfoque que ZetarianoSemanticoListener (Listener
 * sobre el arbol de ANTLR, sin AST propio, sin instanceof) -- ver los
 * comentarios de esa clase para el porque.
 *
 * Diferencia real de diseno frente a Zetariano: alla los campos de la
 * (unica) clase vivian todos en el mismo ambito global, asi que
 * buscarlos era una busqueda plana. Aca puede haber VARIAS estructuras,
 * cada una con sus propios campos (que hasta pueden repetir nombre
 * entre estructuras distintas), asi que un campo se resuelve buscando
 * la DEFINICION de su estructura (guardada en Simbolo.nodoDefinicion)
 * y recorriendo sus campoEstructura, no con una busqueda plana en la
 * tabla de simbolos.
 *
 * Limitacion conocida (igual que en Zetariano, por tiempo): no hay
 * ambito propio por cada bloque {} / bloqueIndentado -- solo la
 * funcion y el "para" tienen su propio ambito. Una variable declarada
 * dentro de un "si" queda visible en el resto de la funcion. Server
 * para probar el flujo hoy; ajustar si hace falta.
 */
public class YLangSemanticoListener extends YLangParserBaseListener {

    private final TablaSimbolos tabla = new TablaSimbolos();
    private final RecolectorErrores errores;
    private final ParseTreeProperty<Tipo> tipos = new ParseTreeProperty<>();

    private final String nombreArchivo;
    private Tipo tipoRetornoActual;
    private int profundidadCiclo;
    private int profundidadElegir; // "romper" vale dentro de un elegir; "continuar" no

    public YLangSemanticoListener(RecolectorErrores errores, String nombreArchivo) {
        this.errores = errores;
        this.nombreArchivo = nombreArchivo;
    }

    public TablaSimbolos getTabla() {
        return tabla;
    }

    public ParseTreeProperty<Tipo> getTipos() {
        return tipos;
    }

    // =====================================================================
    // Pre-registro: estructuras y funciones se registran ANTES de
    // recorrer sus cuerpos, para permitir referencias sin importar el
    // orden de declaracion (una funcion puede llamar a otra declarada
    // mas abajo; una estructura puede tener un campo de otra estructura
    // declarada mas abajo).
    // =====================================================================

    @Override
    public void enterSeccionFunciones(SeccionFuncionesContext ctx) {
        for (FuncionDefContext funcion : ctx.funcionDef()) {
            String nombre = funcion.ID().getText();
            Tipo tipoRetorno = (funcion.ARROW() != null) ? resolverTipo(funcion.tipo(), 0) : Tipo.vacio();
            int aridad = (funcion.parametros() != null) ? funcion.parametros().parametro().size() : 0;

            Simbolo simbolo = new Simbolo(claveSobrecarga(nombre, aridad), tipoRetorno, RolSimbolo.FUNCION,
                    linea(funcion), columna(funcion));
            simbolo.setNodoDefinicion(funcion);
            if (!tabla.declarar(simbolo)) {
                error("Ya existe una funcion '" + nombre + "' con " + aridad + " parametro(s)", funcion);
            }
        }
    }

    @Override
    public void enterSeccionEstructuras(SeccionEstructurasContext ctx) {
        for (EstructuraContext estructura : ctx.estructura()) {
            String nombre = estructura.ID().getText();
            Simbolo simbolo = new Simbolo(nombre, Tipo.estructura(nombre), RolSimbolo.ESTRUCTURA,
                    linea(estructura), columna(estructura));
            simbolo.setNodoDefinicion(estructura);
            if (!tabla.declarar(simbolo)) {
                error("Ya existe una estructura o simbolo llamado '" + nombre + "'", estructura);
            }
        }
    }

    @Override
    public void exitEstructura(EstructuraContext ctx) {
        String nombre = ctx.ID().getText();

        // Si ya esta declarada (caso normal: estructura global, pre-
        // registrada arriba), no la volvemos a declarar. Si no esta
        // (estructura local dentro de una funcion), se declara ahora,
        // en el ambito actual.
        if (tabla.buscarEnAmbitoActual(nombre) == null) {
            Simbolo simbolo = new Simbolo(nombre, Tipo.estructura(nombre), RolSimbolo.ESTRUCTURA,
                    linea(ctx), columna(ctx));
            simbolo.setNodoDefinicion(ctx);
            if (!tabla.declarar(simbolo)) {
                error("Ya existe una estructura o simbolo llamado '" + nombre + "'", ctx);
                return;
            }
        }

        Set<String> vistos = new HashSet<>();
        for (CampoEstructuraContext campo : ctx.campoEstructura()) {
            String nombreCampo = campo.ID().getText();
            if (!vistos.add(nombreCampo)) {
                error("El campo '" + nombreCampo + "' esta repetido en la estructura '" + nombre + "'", campo);
            }
        }
    }

    // =====================================================================
    // Funciones
    // =====================================================================

    @Override
    public void enterFuncionDef(FuncionDefContext ctx) {
        Tipo tipoRetorno = (ctx.ARROW() != null) ? resolverTipo(ctx.tipo(), 0) : Tipo.vacio();
        tabla.entrarAmbito("funcion " + ctx.ID().getText());

        if (ctx.parametros() != null) {
            for (ParametroContext parametro : ctx.parametros().parametro()) {
                Tipo tipoParametro = resolverTipo(parametro.tipo(), corchetesOpcional(parametro.LBRACKET()));
                Simbolo simbolo = new Simbolo(parametro.ID().getText(), tipoParametro, RolSimbolo.PARAMETRO,
                        linea(parametro), columna(parametro));
                if (!tabla.declarar(simbolo)) {
                    error("El parametro '" + parametro.ID().getText() + "' esta repetido", parametro);
                }
            }
        }

        tipoRetornoActual = tipoRetorno;
    }

    @Override
    public void exitFuncionDef(FuncionDefContext ctx) {
        tabla.salirAmbito();
        tipoRetornoActual = null;
    }

    // =====================================================================
    // Sentencias
    // =====================================================================

    @Override
    public void exitDeclaracionVariable(DeclaracionVariableContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo(), corchetesLista(ctx.LBRACKET()));
        declararVariable(ctx.ID().getText(), tipo, ctx.expresion(), ctx);
    }

    @Override
    public void exitDeclaracionParaInit(DeclaracionParaInitContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo(), 0);
        declararVariable(ctx.ID().getText(), tipo, ctx.expresion(), ctx);
    }

    private void declararVariable(String nombre, Tipo tipo, ExpresionContext inicializador, ParserRuleContext nodo) {
        if (inicializador != null) {
            validarInicializador(inicializador, tipo, nodo);
        }
        Simbolo simbolo = new Simbolo(nombre, tipo, RolSimbolo.VARIABLE, linea(nodo), columna(nodo));
        if (!tabla.declarar(simbolo)) {
            error("La variable '" + nombre + "' ya fue declarada en este ambito", nodo);
        }
    }

    @Override
    public void exitSentenciaExpresion(SentenciaExpresionContext ctx) {
        List<ExpresionContext> expresiones = ctx.expresion();
        if (expresiones.size() < 2) {
            return; // era una llamada/expresion suelta
        }
        ExpresionContext destino = expresiones.get(0);
        ExpresionContext valor = expresiones.get(1);

        if (!esDestinoValido(destino)) {
            error("El lado izquierdo de una asignacion debe ser una variable, "
                    + "un elemento de arreglo o un campo", ctx);
            return;
        }

        Tipo tipoDestino = tipoDe(destino);
        if (!TablaTipos.esAsignable(tipoDestino, tipoDe(valor))) {
            if (valor instanceof ExpLiteralCompuestoContext) {
                validarLiteralCompuesto((ExpLiteralCompuestoContext) valor, tipoDestino, ctx);
            } else {
                error(TablaTipos.mensajeAsignacion(tipoDestino, tipoDe(valor)), ctx);
            }
        }
    }

    private boolean esDestinoValido(ExpresionContext ctx) {
        return ctx instanceof ExpIdContext
                || ctx instanceof ExpAccesoContext
                || ctx instanceof ExpIndiceContext;
    }

    @Override
    public void enterSentenciaPara(SentenciaParaContext ctx) {
        tabla.entrarAmbito("para");
        profundidadCiclo++;
    }

    @Override
    public void exitSentenciaPara(SentenciaParaContext ctx) {
        profundidadCiclo--;
        tabla.salirAmbito();
    }

    @Override
    public void enterSentenciaMientras(SentenciaMientrasContext ctx) {
        profundidadCiclo++;
    }

    @Override
    public void exitSentenciaMientras(SentenciaMientrasContext ctx) {
        profundidadCiclo--;
    }

    @Override
    public void enterSentenciaHacerMientras(SentenciaHacerMientrasContext ctx) {
        profundidadCiclo++;
    }

    @Override
    public void exitSentenciaHacerMientras(SentenciaHacerMientrasContext ctx) {
        profundidadCiclo--;
    }

    @Override
    public void exitSentenciaRomper(SentenciaRomperContext ctx) {
        if (profundidadCiclo == 0 && profundidadElegir == 0) {
            error("'romper' solo se puede usar dentro de un ciclo o un elegir", ctx);
        }
    }

    @Override
    public void exitSentenciaContinuar(SentenciaContinuarContext ctx) {
        if (profundidadCiclo == 0) {
            error("'continuar' solo se puede usar dentro de un ciclo", ctx);
        }
    }

    @Override
    public void exitSentenciaRetornar(SentenciaRetornarContext ctx) {
        if (tipoRetornoActual == null) {
            return;
        }
        boolean tieneValor = ctx.expresion() != null;

        if (tipoRetornoActual.esVacio()) {
            if (tieneValor) {
                error("Esta funcion no declara tipo de retorno (falta '-> tipo') "
                        + "y no deberia retornar un valor", ctx);
            }
            return;
        }
        if (!tieneValor) {
            error("Esta funcion debe retornar un valor de tipo " + tipoRetornoActual, ctx);
            return;
        }
        Tipo tipoValor = tipoDe(ctx.expresion());
        if (!TablaTipos.esAsignable(tipoRetornoActual, tipoValor)) {
            error("La funcion declara retornar " + tipoRetornoActual
                    + " pero retorna " + tipoValor, ctx);
        }
    }

    @Override
    public void exitSentenciaSi(SentenciaSiContext ctx) {
        for (ExpresionContext condicion : ctx.expresion()) {
            verificarCondicion(condicion);
        }
    }

    @Override
    public void enterSentenciaElegir(SentenciaElegirContext ctx) {
        profundidadElegir++;
    }

    @Override
    public void exitSentenciaElegir(SentenciaElegirContext ctx) {
        profundidadElegir--;
        Tipo tipo = tipoDe(ctx.expresion());
        if (!tipo.esError() && (tipo.esArreglo() || tipo.esEstructura() || tipo.esVacio())) {
            error("El valor de un elegir debe ser un tipo simple, no " + tipo, ctx);
        }
    }

    private void verificarCondicion(ExpresionContext ctx) {
        Tipo tipo = tipoDe(ctx);
        if (!TablaTipos.esCondicionValida(tipo)) {
            error("La condicion debe ser de tipo booleano, no " + tipo, ctx);
        }
    }

    // =====================================================================
    // Expresiones
    // =====================================================================

    @Override
    public void exitExpEntero(ExpEnteroContext ctx) {
        tipos.put(ctx, Tipo.entero());
    }

    @Override
    public void exitExpFlotante(ExpFlotanteContext ctx) {
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
    public void exitExpId(ExpIdContext ctx) {
        String nombre = ctx.ID().getText();
        Simbolo simbolo = tabla.buscar(nombre);
        Tipo resultado;
        if (simbolo == null) {
            error("La variable '" + nombre + "' no ha sido declarada", ctx);
            resultado = Tipo.error();
        } else if (simbolo.esInvocable()) {
            error("'" + nombre + "' es una funcion, debe llamarse con parentesis", ctx);
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
            error("++ / -- solo se puede aplicar a una variable o un elemento de arreglo", nodo);
            return;
        }
        Tipo tipo = tipoDe(operando);
        if (!tipo.esError() && !tipo.esNumerico()) {
            error("++ / -- solo se puede aplicar a valores numericos, no a " + tipo, nodo);
        }
    }

    @Override
    public void exitExpMultiplicativa(ExpMultiplicativaContext ctx) {
        Operador operador = (ctx.STAR() != null) ? Operador.MULTIPLICACION : Operador.DIVISION;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExpAditiva(ExpAditivaContext ctx) {
        Operador operador = (ctx.PLUS() != null) ? Operador.SUMA : Operador.RESTA;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExpRelacional(ExpRelacionalContext ctx) {
        Operador operador = (ctx.LT() != null) ? Operador.MENOR : Operador.MAYOR;
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

    private void resolverBinaria(ParserRuleContext nodo, ExpresionContext izq, Operador operador, ExpresionContext der) {
        Tipo tipoIzq = tipoDe(izq);
        Tipo tipoDer = tipoDe(der);
        Tipo resultado = TablaTipos.resultadoBinario(tipoIzq, operador, tipoDer);
        if (resultado.esError() && !tipoIzq.esError() && !tipoDer.esError()) {
            error(TablaTipos.mensajeBinario(tipoIzq, operador, tipoDer), nodo);
        }
        tipos.put(nodo, resultado);
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
            error("No es un arreglo, no se puede indexar", ctx);
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
        Tipo resultado;
        if (base.esError()) {
            resultado = Tipo.error();
        } else if (!base.esEstructura()) {
            error("Solo se puede acceder con '.' a los campos de una estructura", ctx);
            resultado = Tipo.error();
        } else {
            resultado = tipoDeCampo(base.getNombreEstructura(), nombreCampo, ctx);
        }
        tipos.put(ctx, resultado);
    }

    private Tipo tipoDeCampo(String nombreEstructura, String nombreCampo, ParserRuleContext nodo) {
        Simbolo estructura = tabla.buscarTipoDefinido(nombreEstructura);
        if (estructura == null || !(estructura.getNodoDefinicion() instanceof EstructuraContext)) {
            error("No existe la estructura '" + nombreEstructura + "'", nodo);
            return Tipo.error();
        }
        EstructuraContext definicion = (EstructuraContext) estructura.getNodoDefinicion();
        for (CampoEstructuraContext campo : definicion.campoEstructura()) {
            if (campo.ID().getText().equals(nombreCampo)) {
                return resolverTipo(campo.tipo(), corchetesOpcional(campo.LBRACKET()));
            }
        }
        error("La estructura '" + nombreEstructura + "' no tiene el campo '" + nombreCampo + "'", nodo);
        return Tipo.error();
    }

    @Override
    public void exitExpLlamadaFuncion(ExpLlamadaFuncionContext ctx) {
        String nombre = ctx.ID().getText();
        int aridad = (ctx.argumentos() != null) ? ctx.argumentos().expresion().size() : 0;
        Simbolo simbolo = tabla.buscarTipoDefinido(claveSobrecarga(nombre, aridad));
        Tipo resultado;
        if (simbolo == null) {
            error("No existe la funcion '" + nombre + "' con " + aridad + " argumento(s)", ctx);
            resultado = Tipo.error();
        } else {
            resultado = simbolo.getTipo();
            if (ctx.argumentos() != null && simbolo.getNodoDefinicion() instanceof FuncionDefContext) {
                validarArgumentos(ctx.argumentos(), (FuncionDefContext) simbolo.getNodoDefinicion(), ctx);
            }
        }
        tipos.put(ctx, resultado);
    }

    private void validarArgumentos(ArgumentosContext argumentos, FuncionDefContext definicion, ParserRuleContext nodo) {
        if (definicion.parametros() == null) {
            return;
        }
        List<ParametroContext> parametros = definicion.parametros().parametro();
        List<ExpresionContext> valores = argumentos.expresion();
        for (int i = 0; i < Math.min(parametros.size(), valores.size()); i++) {
            Tipo tipoParametro = resolverTipo(parametros.get(i).tipo(), corchetesOpcional(parametros.get(i).LBRACKET()));
            Tipo tipoArgumento = tipoDe(valores.get(i));
            if (!TablaTipos.esAsignable(tipoParametro, tipoArgumento)) {
                error("El argumento " + (i + 1) + " debe ser de tipo " + tipoParametro
                        + " pero es de tipo " + tipoArgumento, valores.get(i));
            }
        }
    }

    @Override
    public void exitExpImprimir(ExpImprimirContext ctx) {
        if (ctx.expresion() != null) {
            Tipo tipo = tipoDe(ctx.expresion());
            if (tipo.esArreglo() || tipo.esEstructura()) {
                error("No se puede imprimir un arreglo o una estructura completa", ctx);
            }
        }
        tipos.put(ctx, Tipo.vacio());
    }

    @Override
    public void exitExpLeer(ExpLeerContext ctx) {
        tipos.put(ctx, Tipo.cadena());
    }

    @Override
    public void exitExpLiteralCompuesto(ExpLiteralCompuestoContext ctx) {
        // El tipo real de un {...} solo se sabe por contexto (a que se
        // esta asignando): se valida en declararVariable/campo, no aqui.
        tipos.put(ctx, Tipo.error());
    }

    // =====================================================================
    // Literales compuestos: arreglo ({1,2,3}) o instancia de estructura
    // posicional ({10, 20, 85.5}, en el orden en que se declararon los campos)
    // =====================================================================

    private void validarInicializador(ExpresionContext valor, Tipo tipoEsperado, ParserRuleContext nodo) {
        if (valor instanceof ExpLiteralCompuestoContext) {
            validarLiteralCompuesto((ExpLiteralCompuestoContext) valor, tipoEsperado, nodo);
            return;
        }
        Tipo tipoValor = tipoDe(valor);
        if (!TablaTipos.esAsignable(tipoEsperado, tipoValor)) {
            error(TablaTipos.mensajeAsignacion(tipoEsperado, tipoValor), nodo);
        }
    }

    private void validarLiteralCompuesto(ExpLiteralCompuestoContext literal, Tipo tipoEsperado, ParserRuleContext nodo) {
        List<ExpresionContext> elementos = literal.expresion();

        if (tipoEsperado.esEstructura()) {
            Simbolo estructura = tabla.buscarTipoDefinido(tipoEsperado.getNombreEstructura());
            if (estructura == null || !(estructura.getNodoDefinicion() instanceof EstructuraContext)) {
                error("No existe la estructura '" + tipoEsperado.getNombreEstructura() + "'", nodo);
                return;
            }
            List<CampoEstructuraContext> campos = ((EstructuraContext) estructura.getNodoDefinicion()).campoEstructura();
            if (elementos.size() != campos.size()) {
                error("La estructura '" + tipoEsperado.getNombreEstructura() + "' tiene " + campos.size()
                        + " campo(s) pero se le dan " + elementos.size() + " valor(es)", nodo);
                return;
            }
            for (int i = 0; i < elementos.size(); i++) {
                Tipo tipoCampo = resolverTipo(campos.get(i).tipo(), corchetesOpcional(campos.get(i).LBRACKET()));
                validarInicializador(elementos.get(i), tipoCampo, elementos.get(i));
            }
            return;
        }

        if (tipoEsperado.esArreglo()) {
            Tipo tipoElemento = tipoEsperado.tipoElemento();
            for (ExpresionContext elemento : elementos) {
                validarInicializador(elemento, tipoElemento, elemento);
            }
            return;
        }

        error("No se puede usar '{}' para inicializar un valor de tipo " + tipoEsperado, nodo);
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private Tipo resolverTipo(TipoContext ctx, int corchetesExtra) {
        Tipo base;
        int corchetesPropios = ctx.LBRACKET().size();
        if (ctx.tipoPrimitivo() != null) {
            base = resolverTipoPrimitivo(ctx.tipoPrimitivo());
        } else {
            base = Tipo.estructura(ctx.ID().getText());
        }
        int total = corchetesPropios + corchetesExtra;
        return total > 0 ? Tipo.arregloDe(base, total) : base;
    }

    private Tipo resolverTipoPrimitivo(TipoPrimitivoContext ctx) {
        if (ctx.KW_ENTERO() != null) return Tipo.entero();
        if (ctx.KW_FLOTANTE() != null) return Tipo.decimal();
        if (ctx.KW_CARACTER() != null) return Tipo.caracter();
        if (ctx.KW_CADENA() != null) return Tipo.cadena();
        return Tipo.booleano(); // KW_BOOL
    }

    private int corchetesLista(List<TerminalNode> corchetes) {
        return corchetes.size();
    }

    private int corchetesOpcional(TerminalNode corchete) {
        return corchete != null ? 1 : 0;
    }

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
        errores.agregar(TipoError.SEMANTICO, mensaje, linea(ctx), columna(ctx), nombreArchivo);
    }
}
