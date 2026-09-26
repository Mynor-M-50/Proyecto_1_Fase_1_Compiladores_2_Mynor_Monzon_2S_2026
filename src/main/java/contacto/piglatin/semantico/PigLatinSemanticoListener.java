package contacto.piglatin.semantico;

import contacto.comun.codegen.ModeloPrograma;
import contacto.comun.errores.RecolectorErrores;
import contacto.comun.errores.TipoError;
import contacto.comun.simbolos.RolSimbolo;
import contacto.comun.simbolos.Simbolo;
import contacto.comun.simbolos.TablaSimbolos;
import contacto.comun.tipos.Operador;
import contacto.comun.tipos.TablaTipos;
import contacto.comun.tipos.Tipo;
import contacto.piglatin.PigLatinBaseListener;
import contacto.piglatin.PigLatinParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.List;

/**
 * Analiza semanticamente un archivo .pig. A diferencia de Y? y
 * Zetariano, Pig Latin YA NO define sus propias estructuras ni
 * funciones -- todo viene de archivos .z/.y importados. Por eso el
 * constructor recibe el ModeloPrograma que el orquestador ya lleno con
 * las firmas de esos archivos: cada clase/estructura con sus campos,
 * cada metodo/constructor en SU clase y cada funcion libre de Y?.
 *
 * Asi pila.apilar(x) se busca en la clase de "pila" (no por nombre
 * suelto en una tabla plana), y las variables locales de los metodos
 * importados no se mezclan con las del .pig.
 */
public class PigLatinSemanticoListener extends PigLatinBaseListener {

    private final TablaSimbolos tabla = new TablaSimbolos();
    private final RecolectorErrores errores;
    private final ParseTreeProperty<Tipo> tipos = new ParseTreeProperty<>();

    private final ModeloPrograma modelo;
    private final String nombreArchivo;
    private int profundidadCiclo;

    public PigLatinSemanticoListener(RecolectorErrores errores, ModeloPrograma modelo, String nombreArchivo) {
        this.errores = errores;
        this.modelo = modelo;
        this.nombreArchivo = nombreArchivo;
    }

    public TablaSimbolos getTabla() {
        return tabla;
    }

    public ParseTreeProperty<Tipo> getTipos() {
        return tipos;
    }

    // =====================================================================
    // Declaraciones (seccion VARIABILES)
    // =====================================================================

    @Override
    public void exitDeclObjeto(DeclObjetoContext ctx) {
        String nombreClase = ctx.ID(1).getText(); // ID(0) es el nombre de la variable, ID(1) la clase
        verificarConstructor(nombreClase, ctx.listaArgumentos(), ctx);
        declarar(ctx.ID(0).getText(), Tipo.estructura(nombreClase), ctx);
    }

    @Override
    public void exitDeclEstructura(DeclEstructuraContext ctx) {
        declarar(ctx.ID(0).getText(), Tipo.estructura(ctx.ID(1).getText()), ctx);
    }

    @Override
    public void exitDeclBooleana(DeclBooleanaContext ctx) {
        declarar(ctx.ID().getText(), Tipo.booleano(), ctx);
    }

    @Override
    public void exitDeclConValor(DeclConValorContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo());
        Tipo tipoValor = tipoDe(ctx.expresion());
        if (!TablaTipos.esAsignable(tipo, tipoValor)) {
            error(TablaTipos.mensajeAsignacion(tipo, tipoValor), ctx);
        }
        declarar(ctx.ID().getText(), tipo, ctx);
    }

    @Override
    public void exitDeclSinValor(DeclSinValorContext ctx) {
        declarar(ctx.ID().getText(), resolverTipo(ctx.tipo()), ctx);
    }

    private void declarar(String nombre, Tipo tipo, ParserRuleContext nodo) {
        Simbolo simbolo = new Simbolo(nombre, tipo, RolSimbolo.VARIABLE, linea(nodo), columna(nodo));
        if (!tabla.declarar(simbolo)) {
            error("'" + nombre + "' ya fue declarado", nodo);
        }
    }

    @Override
    public void exitArregloTipado(ArregloTipadoContext ctx) {
        Tipo tipoElemento = resolverTipo(ctx.tipo());
        declarar(ctx.ID().getText(), Tipo.arregloDe(tipoElemento), ctx);
    }

    @Override
    public void exitArregloInferido(ArregloInferidoContext ctx) {
        // Sin tipo explicito: se infiere de los valores, si hay; si no,
        // se marca error (no se puede saber el tipo de un arreglo vacio
        // sin tipo declarado).
        Tipo tipoElemento = Tipo.error();
        List<ValorListaContext> valores = ctx.listaValores().valorLista();
        if (!valores.isEmpty() && valores.get(0).expresion() != null) {
            tipoElemento = tipoDe(valores.get(0).expresion());
        }
        declarar(ctx.ID().getText(), Tipo.arregloDe(tipoElemento), ctx);
    }

    // =====================================================================
    // Instrucciones
    // =====================================================================

    @Override
    public void exitAsignacionSimple(AsignacionSimpleContext ctx) {
        Tipo tipoDestino = tipoDe(ctx.objetivo());
        Tipo tipoValor = tipoDe(ctx.expresion());
        if (!TablaTipos.esAsignable(tipoDestino, tipoValor)) {
            error(TablaTipos.mensajeAsignacion(tipoDestino, tipoValor), ctx);
        }
    }

    @Override
    public void exitIncremento(IncrementoContext ctx) {
        Tipo tipo = tipoDe(ctx.objetivo());
        if (!tipo.esError() && !tipo.esNumerico()) {
            error("++ / -- solo se aplica a valores numericos, no a " + tipo, ctx);
        }
    }

    @Override
    public void exitCondicional(CondicionalContext ctx) {
        verificarCondicion(ctx.expresion());
    }

    @Override
    public void exitRamaAliterSi(RamaAliterSiContext ctx) {
        verificarCondicion(ctx.expresion());
    }

    @Override
    public void enterCicloDum(CicloDumContext ctx) {
        profundidadCiclo++;
    }

    @Override
    public void exitCicloDum(CicloDumContext ctx) {
        profundidadCiclo--;
        verificarCondicion(ctx.expresion());
    }

    @Override
    public void enterCicloFacere(CicloFacereContext ctx) {
        profundidadCiclo++;
    }

    @Override
    public void exitCicloFacere(CicloFacereContext ctx) {
        profundidadCiclo--;
        verificarCondicion(ctx.expresion());
    }

    @Override
    public void enterCicloPer(CicloPerContext ctx) {
        tabla.entrarAmbito("per");
        profundidadCiclo++;
    }

    @Override
    public void exitCicloPer(CicloPerContext ctx) {
        profundidadCiclo--;
        tabla.salirAmbito();
        verificarCondicion(ctx.expresion());
    }

    @Override
    public void exitPerDeclara(PerDeclaraContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo());
        Tipo tipoValor = tipoDe(ctx.expresion());
        if (!TablaTipos.esAsignable(tipo, tipoValor)) {
            error(TablaTipos.mensajeAsignacion(tipo, tipoValor), ctx);
        }
        declarar(ctx.ID().getText(), tipo, ctx);
    }

    @Override
    public void exitPerAsigna(PerAsignaContext ctx) {
        Tipo tipoDestino = tipoDe(ctx.objetivo());
        Tipo tipoValor = tipoDe(ctx.expresion());
        if (!TablaTipos.esAsignable(tipoDestino, tipoValor)) {
            error(TablaTipos.mensajeAsignacion(tipoDestino, tipoValor), ctx);
        }
    }

    private void verificarCondicion(ExpresionContext ctx) {
        Tipo tipo = tipoDe(ctx);
        if (!TablaTipos.esCondicionValida(tipo)) {
            error("La condicion debe ser de tipo booleano, no " + tipo, ctx);
        }
    }

    @Override
    public void exitPerge(PergeContext ctx) {
        if (profundidadCiclo == 0) {
            error("'perge' solo se puede usar dentro de un ciclo", ctx);
        }
    }

    @Override
    public void exitInterrumpe(InterrumpeContext ctx) {
        if (profundidadCiclo == 0) {
            error("'interrumpe' solo se puede usar dentro de un ciclo", ctx);
        }
    }

    @Override
    public void exitImprimir(ImprimirContext ctx) {
        for (ExpresionContext valor : ctx.expresion()) {
            Tipo tipo = tipoDe(valor);
            if (tipo.esArreglo() || tipo.esEstructura()) {
                error("No se puede imprimir un arreglo o una estructura/objeto completo", valor);
            }
        }
    }

    @Override
    public void exitLeerEnVariable(LeerEnVariableContext ctx) {
        Tipo tipo = tipoDe(ctx.objetivo());
        if (!tipo.esError() && (tipo.esArreglo() || tipo.esEstructura())) {
            error("No se puede leer directamente sobre un arreglo o una estructura/objeto", ctx);
        }
    }

    // =====================================================================
    // objetivo: id.campo[indice].metodo(...) -- regla plana, no recursiva
    // (a diferencia de "expresion" en Y?/Zetariano), asi que su tipo se
    // resuelve iterando sufijoAcceso en orden, no con el patron bottom-up
    // de costumbre.
    // =====================================================================

    @Override
    public void exitObjetivo(ObjetivoContext ctx) {
        String nombreBase = ctx.ID().getText();
        Simbolo simbolo = tabla.buscar(nombreBase);
        Tipo actual;
        if (simbolo == null) {
            error("'" + nombreBase + "' no ha sido declarado ni importado", ctx);
            actual = Tipo.error();
        } else {
            actual = simbolo.getTipo();
        }

        for (SufijoAccesoContext sufijo : ctx.sufijoAcceso()) {
            if (sufijo instanceof SufijoAtributoContext) {
                actual = resolverAtributo(actual, ((SufijoAtributoContext) sufijo).ID().getText(), sufijo);
            } else if (sufijo instanceof SufijoIndiceContext) {
                actual = resolverIndice(actual, ((SufijoIndiceContext) sufijo).expresion(), sufijo);
            } else if (sufijo instanceof SufijoMetodoContext) {
                actual = resolverMetodo(actual, (SufijoMetodoContext) sufijo);
            }
        }

        tipos.put(ctx, actual);
    }

    private Tipo resolverAtributo(Tipo base, String nombre, ParserRuleContext nodo) {
        if (base.esError()) {
            return Tipo.error();
        }
        if (!base.esEstructura()) {
            error("Solo se puede acceder con '.' a un campo de un objeto/estructura", nodo);
            return Tipo.error();
        }
        Tipo campo = modelo.tipoCampo(base.getNombreEstructura(), nombre);
        if (campo == null) {
            error("No existe el campo/atributo '" + nombre + "' en " + base, nodo);
            return Tipo.error();
        }
        return campo;
    }

    private Tipo resolverIndice(Tipo base, ExpresionContext indiceCtx, ParserRuleContext nodo) {
        Tipo indice = tipoDe(indiceCtx);
        if (!indice.esError() && !indice.esNumerico()) {
            error("El indice de un arreglo debe ser numerico, no " + indice, indiceCtx);
        }
        if (base.esError()) {
            return Tipo.error();
        }
        if (!base.esArreglo()) {
            error("No es un arreglo, no se puede indexar", nodo);
            return Tipo.error();
        }
        return base.tipoElemento();
    }

    private Tipo resolverMetodo(Tipo base, SufijoMetodoContext ctx) {
        String nombre = ctx.ID().getText();
        int aridad = (ctx.listaArgumentos() != null) ? ctx.listaArgumentos().expresion().size() : 0;
        if (base.esError()) {
            return Tipo.error();
        }
        if (!base.esEstructura()) {
            error("Solo se puede llamar un metodo sobre un objeto", ctx);
            return Tipo.error();
        }
        ModeloPrograma.Funcion metodo = modelo.buscarMetodo(base.getNombreEstructura(), nombre, aridad);
        if (metodo == null) {
            error("No existe el metodo '" + nombre + "' con " + aridad + " argumento(s) en " + base, ctx);
            return Tipo.error();
        }
        verificarArgumentos(metodo, ctx.listaArgumentos());
        return metodo.getRetorno();
    }

    @Override
    public void exitExprAcceso(ExprAccesoContext ctx) {
        tipos.put(ctx, tipoDe(ctx.objetivo()));
    }

    // =====================================================================
    // Expresiones
    // =====================================================================

    @Override
    public void exitLitEntero(LitEnteroContext ctx) {
        tipos.put(ctx, Tipo.entero());
    }

    @Override
    public void exitLitDecimal(LitDecimalContext ctx) {
        tipos.put(ctx, Tipo.decimal());
    }

    @Override
    public void exitLitCadena(LitCadenaContext ctx) {
        tipos.put(ctx, Tipo.cadena());
    }

    @Override
    public void exitLitCaracter(LitCaracterContext ctx) {
        tipos.put(ctx, Tipo.caracter());
    }

    @Override
    public void exitLitVerum(LitVerumContext ctx) {
        tipos.put(ctx, Tipo.booleano());
    }

    @Override
    public void exitLitFalsus(LitFalsusContext ctx) {
        tipos.put(ctx, Tipo.booleano());
    }

    @Override
    public void exitExprLiteral(ExprLiteralContext ctx) {
        tipos.put(ctx, tipoDe(ctx.literal()));
    }

    @Override
    public void exitExprAgrupada(ExprAgrupadaContext ctx) {
        tipos.put(ctx, tipoDe(ctx.expresion()));
    }

    @Override
    public void exitExprUnaria(ExprUnariaContext ctx) {
        Tipo operando = tipoDe(ctx.expresion());
        Operador operador = (ctx.NON() != null) ? Operador.NEGACION_LOGICA : Operador.MENOS_UNARIO;
        Tipo resultado = TablaTipos.resultadoUnario(operador, operando);
        if (resultado.esError() && !operando.esError()) {
            error(TablaTipos.mensajeUnario(operador, operando), ctx);
        }
        tipos.put(ctx, resultado);
    }

    @Override
    public void exitExprMulDiv(ExprMulDivContext ctx) {
        Operador operador = (ctx.POR() != null) ? Operador.MULTIPLICACION : Operador.DIVISION;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExprSumaResta(ExprSumaRestaContext ctx) {
        Operador operador = (ctx.MAS() != null) ? Operador.SUMA : Operador.RESTA;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExprRelacional(ExprRelacionalContext ctx) {
        Operador operador = (ctx.MENOR() != null) ? Operador.MENOR
                : (ctx.MAYOR() != null) ? Operador.MAYOR
                : (ctx.MENORIGUAL() != null) ? Operador.MENOR_IGUAL : Operador.MAYOR_IGUAL;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExprIgualdad(ExprIgualdadContext ctx) {
        Operador operador = (ctx.IGUALIGUAL() != null) ? Operador.IGUAL : Operador.DIFERENTE;
        resolverBinaria(ctx, ctx.expresion(0), operador, ctx.expresion(1));
    }

    @Override
    public void exitExprAnd(ExprAndContext ctx) {
        resolverBinaria(ctx, ctx.expresion(0), Operador.AND, ctx.expresion(1));
    }

    @Override
    public void exitExprOr(ExprOrContext ctx) {
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
    public void exitExprNuevoObjeto(ExprNuevoObjetoContext ctx) {
        verificarConstructor(ctx.ID().getText(), ctx.listaArgumentos(), ctx);
        tipos.put(ctx, Tipo.estructura(ctx.ID().getText()));
    }

    /** Llamada a funcion libre (de un .y), como expresion o como instruccion suelta. */
    @Override
    public void exitLlamadaFuncion(LlamadaFuncionContext ctx) {
        String nombre = ctx.ID().getText();
        int aridad = (ctx.listaArgumentos() != null) ? ctx.listaArgumentos().expresion().size() : 0;
        ModeloPrograma.Funcion funcion = modelo.buscarFuncionLibre(nombre, aridad);
        if (funcion == null) {
            error("No existe la funcion '" + nombre + "' con " + aridad + " argumento(s) (revisa el import)", ctx);
            tipos.put(ctx, Tipo.error());
            return;
        }
        verificarArgumentos(funcion, ctx.listaArgumentos());
        tipos.put(ctx, funcion.getRetorno());
    }

    @Override
    public void exitExprLlamada(ExprLlamadaContext ctx) {
        Tipo tipo = tipoDe(ctx.llamadaFuncion());
        if (tipo.esVacio()) {
            error("La funcion '" + ctx.llamadaFuncion().ID().getText()
                    + "' no retorna ningun valor, no se puede usar como expresion", ctx);
            tipo = Tipo.error();
        }
        tipos.put(ctx, tipo);
    }

    private void verificarConstructor(String clase, ListaArgumentosContext argumentos, ParserRuleContext ctx) {
        ModeloPrograma.Estructura estructura = modelo.buscarEstructura(clase);
        if (estructura == null || !estructura.esClase()) {
            error("No existe la clase '" + clase + "' (revisa el import)", ctx);
            return;
        }
        int aridad = (argumentos != null) ? argumentos.expresion().size() : 0;
        ModeloPrograma.Funcion constructor = modelo.buscarConstructor(clase, aridad);
        if (constructor == null) {
            error("No existe un constructor de '" + clase + "' con " + aridad + " argumento(s)", ctx);
            return;
        }
        verificarArgumentos(constructor, argumentos);
    }

    /** Cada argumento debe ser asignable al tipo de su parametro. */
    private void verificarArgumentos(ModeloPrograma.Funcion funcion, ListaArgumentosContext argumentos) {
        if (argumentos == null) {
            return;
        }
        List<ExpresionContext> valores = argumentos.expresion();
        for (int i = 0; i < valores.size() && i < funcion.getAridad(); i++) {
            Tipo esperado = funcion.getParametros().get(i).getTipo();
            Tipo recibido = tipoDe(valores.get(i));
            if (!TablaTipos.esAsignable(esperado, recibido)) {
                error("El argumento " + (i + 1) + " de '" + funcion.getNombreCompleto() + "' debe ser "
                        + esperado + ", no " + recibido, valores.get(i));
            }
        }
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private Tipo resolverTipo(TipoContext ctx) {
        if (ctx instanceof TipoNumerusContext) return Tipo.entero();
        if (ctx instanceof TipoDecimalisContext) return Tipo.decimal();
        if (ctx instanceof TipoTextumContext) return Tipo.cadena();
        if (ctx instanceof TipoLitteraContext) return Tipo.caracter();
        if (ctx instanceof TipoBoolContext) return Tipo.booleano();
        // TipoEstructura: el nombre de una estructura/clase importada
        return Tipo.estructura(ctx.getText());
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
