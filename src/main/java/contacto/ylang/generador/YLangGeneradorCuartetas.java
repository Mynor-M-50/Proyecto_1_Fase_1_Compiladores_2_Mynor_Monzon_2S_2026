package contacto.ylang.generador;

import contacto.comun.codegen.AmbitosGeneracion;
import contacto.comun.codegen.ModeloPrograma;
import contacto.comun.codegen.RuntimeC;
import contacto.comun.cuartetas.GeneradorCuartetas;
import contacto.comun.cuartetas.acceso.CampoLugar;
import contacto.comun.cuartetas.acceso.IndiceLugar;
import contacto.comun.cuartetas.acceso.LiteralLugar;
import contacto.comun.cuartetas.acceso.Lugar;
import contacto.comun.cuartetas.acceso.NombreLugar;
import contacto.comun.errores.RecolectorErrores;
import contacto.comun.errores.TipoError;
import contacto.comun.tipos.Operador;
import contacto.comun.tipos.TablaTipos;
import contacto.comun.tipos.Tipo;
import contacto.ylang.YLangParserBaseVisitor;
import contacto.ylang.YLangParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Genera cuartetas (C3D) para un archivo .y YA validado por
 * YLangSemanticoListener. Visitor por la misma razon que en Zetariano
 * (control de orden de saltos/etiquetas). Devuelve Lugar en vez de
 * String -- ver comun.cuartetas.acceso.Lugar.
 *
 * Igual que en Zetariano, se usa en dos pasos (ver OrquestadorPig):
 * registrarFirmas() registra las estructuras y las funciones en el
 * ModeloPrograma, y visit() genera el cuerpo de cada funcion.
 *
 * Las estructuras viven en heap (en C son punteros): una variable de
 * tipo estructura sin valor inicial ya queda reservada (con sus campos
 * en 0), y un literal {a, b, c} se asigna campo por campo (o elemento
 * por elemento si es un arreglo), segun el tipo que se espera en ese
 * lugar.
 */
public class YLangGeneradorCuartetas extends YLangParserBaseVisitor<Lugar> {

    private final GeneradorCuartetas gen = new GeneradorCuartetas();
    private final ParseTreeProperty<Tipo> tiposSemantico;
    private final ParseTreeProperty<Tipo> tiposCalculados = new ParseTreeProperty<>();
    private final ModeloPrograma modelo;
    private final RecolectorErrores errores;
    private final String nombreArchivo;

    private final ParseTreeProperty<ModeloPrograma.Funcion> funcionDe = new ParseTreeProperty<>();
    private ModeloPrograma.Funcion funcionActual;
    private AmbitosGeneracion ambitos;

    private final Deque<String[]> pilaControlFlujo = new ArrayDeque<>();

    public YLangGeneradorCuartetas(ParseTreeProperty<Tipo> tipos, ModeloPrograma modelo,
                                   RecolectorErrores errores, String nombreArchivo) {
        this.tiposSemantico = tipos;
        this.modelo = modelo;
        this.errores = errores;
        this.nombreArchivo = nombreArchivo;
    }

    public GeneradorCuartetas getGenerador() {
        return gen;
    }

    // =====================================================================
    // Paso 1: firmas
    // =====================================================================

    public void registrarFirmas(ProgramaContext programa) {
        registrarEstructuras(programa);
        for (FuncionDefContext funcionCtx : programa.seccionFunciones().funcionDef()) {
            List<ModeloPrograma.Variable> parametros = new ArrayList<>();
            if (funcionCtx.parametros() != null) {
                for (ParametroContext parametro : funcionCtx.parametros().parametro()) {
                    Tipo tipo = resolverTipo(parametro.tipo(), parametro.LBRACKET() != null ? 1 : 0);
                    parametros.add(new ModeloPrograma.Variable(parametro.ID().getText(), tipo));
                }
            }
            Tipo retorno = (funcionCtx.tipo() != null) ? resolverTipo(funcionCtx.tipo(), 0) : Tipo.vacio();
            ModeloPrograma.Funcion funcion = new ModeloPrograma.Funcion(funcionCtx.ID().getText(), null,
                    false, false, retorno, parametros, funcionCtx);
            modelo.agregarFuncion(funcion);
            funcionDe.put(funcionCtx, funcion);
        }
    }

    /** Estructuras de la seccion %estructuras y tambien las locales a una funcion: en C todas son globales. */
    private void registrarEstructuras(ParseTree nodo) {
        if (nodo instanceof EstructuraContext) {
            EstructuraContext estructuraCtx = (EstructuraContext) nodo;
            ModeloPrograma.Estructura estructura = modelo.registrarEstructura(estructuraCtx.ID().getText(), false);
            for (CampoEstructuraContext campo : estructuraCtx.campoEstructura()) {
                Tipo tipo = resolverTipo(campo.tipo(), 0);
                int tamanio = 0;
                if (campo.ENTERO_LITERAL() != null) {
                    tipo = Tipo.arregloDe(tipo);
                    tamanio = Integer.parseInt(campo.ENTERO_LITERAL().getText());
                }
                estructura.agregarCampo(new ModeloPrograma.Variable(campo.ID().getText(), tipo, tamanio));
            }
        }
        for (int i = 0; i < nodo.getChildCount(); i++) {
            registrarEstructuras(nodo.getChild(i));
        }
    }

    // =====================================================================
    // Paso 2: cuerpos
    // =====================================================================

    @Override
    public Lugar visitPrograma(ProgramaContext ctx) {
        visit(ctx.seccionFunciones());
        return null;
    }

    @Override
    public Lugar visitSeccionFunciones(SeccionFuncionesContext ctx) {
        for (FuncionDefContext funcion : ctx.funcionDef()) {
            visit(funcion);
        }
        return null;
    }

    @Override
    public Lugar visitFuncionDef(FuncionDefContext ctx) {
        funcionActual = funcionDe.get(ctx);
        ambitos = new AmbitosGeneracion(funcionActual);
        funcionActual.iniciarCuerpo(gen);
        gen.emitirEtiqueta("inicio_" + ctx.ID().getText());
        for (SentenciaContext sentencia : ctx.sentencia()) {
            visit(sentencia);
        }
        funcionActual.terminarCuerpo();
        funcionActual = null;
        ambitos = null;
        return null;
    }

    @Override
    public Lugar visitEstructura(EstructuraContext ctx) {
        return null; // ya registrada como struct global en registrarFirmas
    }

    // =====================================================================
    // Sentencias
    // =====================================================================

    @Override
    public Lugar visitDeclaracionVariable(DeclaracionVariableContext ctx) {
        // tipo[] x[3][4]: los corchetes van en el tipo y/o despues del nombre
        int corchetes = ctx.LBRACKET().size();
        Tipo tipo = resolverTipo(ctx.tipo(), corchetes);
        List<Lugar> tamanios = new ArrayList<>();
        for (var tamanio : ctx.ENTERO_LITERAL()) {
            tamanios.add(new LiteralLugar(tamanio.getText()));
        }

        Lugar inicial = (ctx.expresion() != null) ? valorEsperado(ctx.expresion(), tipo) : null;
        String nombreC = ambitos.declarar(ctx.ID().getText(), tipo);
        Lugar variable = new NombreLugar(nombreC);

        if (inicial != null) {
            gen.emitirAsignacion(variable, inicial);
        } else if (!tamanios.isEmpty()) {
            // entero a[5]: el arreglo ya existe (en 0), como en C
            Tipo elemento = tipo;
            for (int i = 0; i < tamanios.size(); i++) {
                elemento = elemento.tipoElemento();
            }
            List<Lugar> argumentos = new ArrayList<>();
            argumentos.add(new LiteralLugar(String.valueOf(tamanios.size())));
            argumentos.add(new LiteralLugar("sizeof(" + RuntimeC.tipoC(elemento) + ")"));
            argumentos.addAll(tamanios);
            gen.emitirLlamada(variable, null, "new " + tipo, "zc_nuevo_arreglo", argumentos);
        } else if (tipo.esEstructura() && !tipo.esArreglo()) {
            // Persona p: la estructura ya existe (campos en 0), como en C
            reservarEstructura(variable, tipo.getNombreEstructura());
        }
        return null;
    }

    @Override
    public Lugar visitDeclaracionParaInit(DeclaracionParaInitContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo(), 0);
        Lugar inicial = (ctx.expresion() != null) ? valorEsperado(ctx.expresion(), tipo) : null;
        String nombreC = ambitos.declarar(ctx.ID().getText(), tipo);
        if (inicial != null) {
            gen.emitirAsignacion(new NombreLugar(nombreC), inicial);
        }
        return null;
    }

    @Override
    public Lugar visitSentenciaExpresion(SentenciaExpresionContext ctx) {
        List<ExpresionContext> expresiones = ctx.expresion();
        if (expresiones.size() < 2) {
            visitarSinValor(expresiones.get(0));
            return null;
        }
        Lugar destino = valor(expresiones.get(0));
        ModeloPrograma.Variable campoFijo = campoDeTamanioFijo(expresiones.get(0));
        if (campoFijo != null && expresiones.get(1) instanceof ExpLiteralCompuestoContext) {
            // p.notas = {1, 2, 3} con "entero notas[3]" en la estructura:
            // en C es un arreglo dentro del struct, se copia elemento por elemento
            asignarArregloFijo(destino, (ExpLiteralCompuestoContext) expresiones.get(1), campoFijo);
            return null;
        }
        Lugar origen = valorEsperado(expresiones.get(1), tipo(expresiones.get(0)));
        gen.emitirAsignacion(destino, origen);
        return null;
    }

    /**
     * Una expresion usada como sentencia ("i++", el update de un para):
     * su valor no se usa, asi que "i++" no necesita guardar el valor
     * anterior en un temporal.
     */
    private void visitarSinValor(ExpresionContext ctx) {
        if (ctx instanceof ExpIncDecSufijoContext) {
            ExpIncDecSufijoContext incDec = (ExpIncDecSufijoContext) ctx;
            Lugar lugar = valor(incDec.expresion());
            gen.emitirOperacionBinaria(lugar, lugar, (incDec.PLUSPLUS() != null) ? "+" : "-", new LiteralLugar("1"));
        } else {
            visit(ctx);
        }
    }

    @Override
    public Lugar visitSentenciaSi(SentenciaSiContext ctx) {
        List<ExpresionContext> condiciones = ctx.expresion();
        List<BloqueIndentadoContext> cuerpos = ctx.bloqueIndentado();
        String etiquetaFin = gen.nuevaEtiqueta();

        for (int i = 0; i < condiciones.size(); i++) {
            Lugar condicion = valor(condiciones.get(i));
            String etiquetaSiguiente = gen.nuevaEtiqueta();
            gen.emitirSaltoSiFalso(condicion, etiquetaSiguiente);
            visit(cuerpos.get(i));
            gen.emitirSalto(etiquetaFin);
            gen.emitirEtiqueta(etiquetaSiguiente);
        }

        if (cuerpos.size() > condiciones.size()) {
            visit(cuerpos.get(cuerpos.size() - 1));
        }

        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public Lugar visitBloqueIndentado(BloqueIndentadoContext ctx) {
        ambitos.entrar();
        for (SentenciaContext sentencia : ctx.sentencia()) {
            visit(sentencia);
        }
        ambitos.salir();
        return null;
    }

    @Override
    public Lugar visitSentenciaMientras(SentenciaMientrasContext ctx) {
        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);
        Lugar condicion = valor(ctx.expresion());
        gen.emitirSaltoSiFalso(condicion, etiquetaFin);

        pilaControlFlujo.push(new String[]{etiquetaInicio, etiquetaFin});
        visit(ctx.bloqueIndentado());
        pilaControlFlujo.pop();

        gen.emitirSalto(etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public Lugar visitSentenciaHacerMientras(SentenciaHacerMientrasContext ctx) {
        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaContinua = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);

        pilaControlFlujo.push(new String[]{etiquetaContinua, etiquetaFin});
        visit(ctx.bloqueIndentado());
        pilaControlFlujo.pop();

        gen.emitirEtiqueta(etiquetaContinua);
        Lugar condicion = valor(ctx.expresion());
        gen.emitirSaltoSiVerdadero(condicion, etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public Lugar visitSentenciaPara(SentenciaParaContext ctx) {
        ambitos.entrar(); // la variable del para solo existe dentro del para
        visit(ctx.declaracionParaInit());

        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaContinua = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);
        Lugar condicion = valor(ctx.expresion(0));
        gen.emitirSaltoSiFalso(condicion, etiquetaFin);

        pilaControlFlujo.push(new String[]{etiquetaContinua, etiquetaFin});
        visit(ctx.bloqueIndentado());
        pilaControlFlujo.pop();

        gen.emitirEtiqueta(etiquetaContinua);
        visitarSinValor(ctx.expresion(1));
        gen.emitirSalto(etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        ambitos.salir();
        return null;
    }

    @Override
    public Lugar visitSentenciaElegir(SentenciaElegirContext ctx) {
        Lugar selector = valor(ctx.expresion());
        Tipo tipoSelector = tipo(ctx.expresion());
        String etiquetaFin = gen.nuevaEtiqueta();

        // Como el switch de C: se prueban los casos en orden y cada uno
        // salta a su cuerpo; sin "romper" se sigue con el siguiente cuerpo.
        List<CasoElegirContext> casos = ctx.casoElegir();
        List<String> etiquetasCuerpo = new ArrayList<>();
        for (CasoElegirContext caso : casos) {
            String etiquetaCuerpo = gen.nuevaEtiqueta();
            etiquetasCuerpo.add(etiquetaCuerpo);
            Lugar comparacion = gen.nuevoTemporal(Tipo.booleano());
            gen.emitirOperacionBinaria(comparacion, selector, "==",
                    new LiteralLugar(caso.literalCaso().getText()), tipoSelector, tipoLiteralCaso(caso.literalCaso()));
            gen.emitirSaltoSiVerdadero(comparacion, etiquetaCuerpo);
        }
        String etiquetaSiempre = (ctx.casoSiempre() != null) ? gen.nuevaEtiqueta() : etiquetaFin;
        gen.emitirSalto(etiquetaSiempre);

        pilaControlFlujo.push(new String[]{etiquetaFin, etiquetaFin});
        for (int i = 0; i < casos.size(); i++) {
            gen.emitirEtiqueta(etiquetasCuerpo.get(i));
            visit(casos.get(i).bloqueIndentado());
        }
        if (ctx.casoSiempre() != null) {
            gen.emitirEtiqueta(etiquetaSiempre);
            visit(ctx.casoSiempre().bloqueIndentado());
        }
        pilaControlFlujo.pop();
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    private Tipo tipoLiteralCaso(LiteralCasoContext ctx) {
        if (ctx.CADENA_LITERAL() != null) return Tipo.cadena();
        if (ctx.CARACTER_LITERAL() != null) return Tipo.caracter();
        return Tipo.entero();
    }

    @Override
    public Lugar visitSentenciaRomper(SentenciaRomperContext ctx) {
        if (!pilaControlFlujo.isEmpty()) {
            gen.emitirSalto(pilaControlFlujo.peek()[1]);
        }
        return null;
    }

    @Override
    public Lugar visitSentenciaContinuar(SentenciaContinuarContext ctx) {
        // "continuar" salta al ciclo mas interno, aunque haya un elegir en medio
        for (String[] destino : pilaControlFlujo) {
            if (!destino[0].equals(destino[1])) { // en un elegir ambos son la etiqueta de fin
                gen.emitirSalto(destino[0]);
                break;
            }
        }
        return null;
    }

    @Override
    public Lugar visitSentenciaRetornar(SentenciaRetornarContext ctx) {
        Lugar valor = (ctx.expresion() != null) ? valorEsperado(ctx.expresion(), funcionActual.getRetorno()) : null;
        gen.emitirRetorno(valor);
        return null;
    }

    // =====================================================================
    // Expresiones
    // =====================================================================

    @Override
    public Lugar visitExpEntero(ExpEnteroContext ctx) {
        return new LiteralLugar(ctx.getText());
    }

    @Override
    public Lugar visitExpFlotante(ExpFlotanteContext ctx) {
        return new LiteralLugar(ctx.getText());
    }

    @Override
    public Lugar visitExpCaracter(ExpCaracterContext ctx) {
        return new LiteralLugar(ctx.getText());
    }

    @Override
    public Lugar visitExpCadena(ExpCadenaContext ctx) {
        return new LiteralLugar(ctx.getText());
    }

    @Override
    public Lugar visitExpVerdadero(ExpVerdaderoContext ctx) {
        return new LiteralLugar("1");
    }

    @Override
    public Lugar visitExpFalso(ExpFalsoContext ctx) {
        return new LiteralLugar("0");
    }

    @Override
    public Lugar visitExpId(ExpIdContext ctx) {
        String nombre = ctx.ID().getText();
        String local = ambitos.resolver(nombre);
        if (local != null) {
            calcularTipo(ctx, ambitos.tipoDe(nombre));
            return new NombreLugar(local);
        }
        return new NombreLugar(ModeloPrograma.nombreSeguroC(nombre));
    }

    @Override
    public Lugar visitExpParentesis(ExpParentesisContext ctx) {
        Lugar lugar = valor(ctx.expresion());
        calcularTipo(ctx, tipo(ctx.expresion()));
        return lugar;
    }

    @Override
    public Lugar visitExpUnario(ExpUnarioContext ctx) {
        Lugar operando = valor(ctx.expresion());
        boolean negacion = ctx.NOT() != null;
        calcularTipo(ctx, TablaTipos.resultadoUnario(
                negacion ? Operador.NEGACION_LOGICA : Operador.MENOS_UNARIO, tipo(ctx.expresion())));
        Lugar temp = gen.nuevoTemporal(tipo(ctx));
        gen.emitirOperacionUnaria(temp, negacion ? "!" : "-", operando);
        return temp;
    }

    @Override
    public Lugar visitExpIncDecPrefijo(ExpIncDecPrefijoContext ctx) {
        Lugar lugar = valor(ctx.expresion());
        calcularTipo(ctx, tipo(ctx.expresion()));
        String operador = (ctx.PLUSPLUS() != null) ? "+" : "-";
        gen.emitirOperacionBinaria(lugar, lugar, operador, new LiteralLugar("1"));
        return lugar;
    }

    @Override
    public Lugar visitExpIncDecSufijo(ExpIncDecSufijoContext ctx) {
        Lugar lugar = valor(ctx.expresion());
        calcularTipo(ctx, tipo(ctx.expresion()));
        Lugar temp = gen.nuevoTemporal(tipo(ctx));
        gen.emitirAsignacion(temp, lugar);
        String operador = (ctx.PLUSPLUS() != null) ? "+" : "-";
        gen.emitirOperacionBinaria(lugar, lugar, operador, new LiteralLugar("1"));
        return temp;
    }

    @Override
    public Lugar visitExpMultiplicativa(ExpMultiplicativaContext ctx) {
        return (ctx.STAR() != null)
                ? emitirBinaria(ctx, "*", Operador.MULTIPLICACION)
                : emitirBinaria(ctx, "/", Operador.DIVISION);
    }

    @Override
    public Lugar visitExpAditiva(ExpAditivaContext ctx) {
        return (ctx.PLUS() != null)
                ? emitirBinaria(ctx, "+", Operador.SUMA)
                : emitirBinaria(ctx, "-", Operador.RESTA);
    }

    @Override
    public Lugar visitExpRelacional(ExpRelacionalContext ctx) {
        return (ctx.LT() != null)
                ? emitirBinaria(ctx, "<", Operador.MENOR)
                : emitirBinaria(ctx, ">", Operador.MAYOR);
    }

    @Override
    public Lugar visitExpIgualdad(ExpIgualdadContext ctx) {
        return (ctx.EQ() != null)
                ? emitirBinaria(ctx, "==", Operador.IGUAL)
                : emitirBinaria(ctx, "!=", Operador.DIFERENTE);
    }

    @Override
    public Lugar visitExpAnd(ExpAndContext ctx) {
        return emitirBinaria(ctx, "&&", Operador.AND);
    }

    @Override
    public Lugar visitExpOr(ExpOrContext ctx) {
        return emitirBinaria(ctx, "||", Operador.OR);
    }

    /** Todas las binarias tienen la forma "expresion OP expresion": hijos 0 y 1. */
    private Lugar emitirBinaria(ParserRuleContext ctx, String operador, Operador tipoOperador) {
        ExpresionContext izq = ctx.getRuleContext(ExpresionContext.class, 0);
        ExpresionContext der = ctx.getRuleContext(ExpresionContext.class, 1);
        Lugar lugarIzq = valor(izq);
        Lugar lugarDer = valor(der);
        calcularTipo(ctx, TablaTipos.resultadoBinario(tipo(izq), tipoOperador, tipo(der)));
        Lugar temp = gen.nuevoTemporal(tipo(ctx));
        gen.emitirOperacionBinaria(temp, lugarIzq, operador, lugarDer, tipo(izq), tipo(der));
        return temp;
    }

    @Override
    public Lugar visitExpIndice(ExpIndiceContext ctx) {
        Lugar base = valor(ctx.expresion(0));
        Lugar indice = valor(ctx.expresion(1));
        calcularTipo(ctx, tipo(ctx.expresion(0)).tipoElemento());
        return new IndiceLugar(base, indice);
    }

    @Override
    public Lugar visitExpAcceso(ExpAccesoContext ctx) {
        Lugar base = valor(ctx.expresion());
        Tipo tipoBase = tipo(ctx.expresion());
        String nombreCampo = ctx.ID().getText();
        if (tipoBase.esEstructura() && !tipoBase.esArreglo()) {
            ModeloPrograma.Estructura estructura = modelo.buscarEstructura(tipoBase.getNombreEstructura());
            ModeloPrograma.Variable campo = (estructura != null) ? estructura.buscarCampo(nombreCampo) : null;
            if (campo != null) {
                calcularTipo(ctx, campo.getTipo());
                return new CampoLugar(base, campo.getNombreC());
            }
        }
        return new CampoLugar(base, nombreCampo);
    }

    @Override
    public Lugar visitExpLlamadaFuncion(ExpLlamadaFuncionContext ctx) {
        String nombre = ctx.ID().getText();
        List<ExpresionContext> argumentosCtx = (ctx.argumentos() != null)
                ? ctx.argumentos().expresion() : List.of();
        ModeloPrograma.Funcion funcion = modelo.buscarFuncionLibre(nombre, argumentosCtx.size());
        if (funcion == null) {
            error("No existe la funcion '" + nombre + "' con " + argumentosCtx.size() + " argumento(s)", ctx);
            return new LiteralLugar("0");
        }
        List<Lugar> argumentos = new ArrayList<>();
        for (int i = 0; i < argumentosCtx.size(); i++) {
            argumentos.add(valorEsperado(argumentosCtx.get(i), funcion.getParametros().get(i).getTipo()));
        }
        calcularTipo(ctx, funcion.getRetorno());
        Lugar destino = funcion.getRetorno().esVacio() ? null : gen.nuevoTemporal(funcion.getRetorno());
        gen.emitirLlamada(destino, null, nombre, funcion.getNombreC(), argumentos);
        return destino;
    }

    @Override
    public Lugar visitExpLiteralCompuesto(ExpLiteralCompuestoContext ctx) {
        // Sin un tipo esperado (p.ej. suelto como argumento de imprimir):
        // se usa el que haya inferido el semantico.
        return generarLiteral(ctx, tipo(ctx));
    }

    @Override
    public Lugar visitExpImprimir(ExpImprimirContext ctx) {
        if (ctx.expresion() != null) {
            Lugar valor = valor(ctx.expresion());
            gen.emitirImprimir(valor, RuntimeC.formatoImpresion(tipo(ctx.expresion())), true);
        } else {
            gen.emitirImprimir(new LiteralLugar("\"\""), "%s", true);
        }
        return null;
    }

    @Override
    public Lugar visitExpLeer(ExpLeerContext ctx) {
        Tipo tipo = tipo(ctx).esError() ? Tipo.cadena() : tipo(ctx);
        Lugar temp = gen.nuevoTemporal(tipo);
        gen.emitirLeer(temp, tipo);
        return temp;
    }

    // =====================================================================
    // Literales compuestos {a, b, c}
    // =====================================================================

    /** Como valor(), pero un literal {..} se arma segun el tipo que se espera ahi. */
    private Lugar valorEsperado(ExpresionContext ctx, Tipo esperado) {
        if (ctx instanceof ExpLiteralCompuestoContext && esperado != null && !esperado.esError()) {
            return generarLiteral((ExpLiteralCompuestoContext) ctx, esperado);
        }
        return valor(ctx);
    }

    private Lugar generarLiteral(ExpLiteralCompuestoContext ctx, Tipo esperado) {
        List<ExpresionContext> elementos = ctx.expresion();
        if (esperado != null && esperado.esEstructura() && !esperado.esArreglo()) {
            // {v1, v2, ...} para una estructura: campo por campo, en orden
            ModeloPrograma.Estructura estructura = modelo.buscarEstructura(esperado.getNombreEstructura());
            Lugar temp = gen.nuevoTemporal(esperado);
            reservarEstructura(temp, esperado.getNombreEstructura());
            if (estructura != null) {
                int cantidad = Math.min(elementos.size(), estructura.getCampos().size());
                for (int i = 0; i < cantidad; i++) {
                    ModeloPrograma.Variable campo = estructura.getCampos().get(i);
                    Lugar destino = new CampoLugar(temp, campo.getNombreC());
                    if (campo.getTamanioFijo() > 0 && elementos.get(i) instanceof ExpLiteralCompuestoContext) {
                        asignarArregloFijo(destino, (ExpLiteralCompuestoContext) elementos.get(i), campo);
                    } else {
                        gen.emitirAsignacion(destino, valorEsperado(elementos.get(i), campo.getTipo()));
                    }
                }
            }
            return temp;
        }

        // {v1, v2, ...} para un arreglo: se reserva y se asigna elemento por elemento
        Tipo tipoArreglo = (esperado != null && esperado.esArreglo()) ? esperado : null;
        if (tipoArreglo == null) {
            Tipo elemento = elementos.isEmpty() ? Tipo.entero() : tipo(elementos.get(0));
            tipoArreglo = Tipo.arregloDe(elemento.esError() ? Tipo.entero() : elemento);
        }
        Tipo elemento = tipoArreglo.tipoElemento();
        List<Lugar> valores = new ArrayList<>();
        for (ExpresionContext expresion : elementos) {
            valores.add(valorEsperado(expresion, elemento));
        }
        Lugar temp = gen.nuevoTemporal(tipoArreglo);
        gen.emitirLlamada(temp, null, "new " + elemento + "[]", "zc_nuevo_arreglo", List.of(
                new LiteralLugar("1"),
                new LiteralLugar("sizeof(" + RuntimeC.tipoC(elemento) + ")"),
                new LiteralLugar(String.valueOf(valores.size()))));
        for (int i = 0; i < valores.size(); i++) {
            gen.emitirAsignacion(new IndiceLugar(temp, new LiteralLugar(String.valueOf(i))), valores.get(i));
        }
        return temp;
    }

    private void asignarArregloFijo(Lugar destino, ExpLiteralCompuestoContext literal, ModeloPrograma.Variable campo) {
        List<ExpresionContext> elementos = literal.expresion();
        int cantidad = Math.min(elementos.size(), campo.getTamanioFijo());
        for (int i = 0; i < cantidad; i++) {
            Lugar valor = valorEsperado(elementos.get(i), campo.getTipo().tipoElemento());
            gen.emitirAsignacion(new IndiceLugar(destino, new LiteralLugar(String.valueOf(i))), valor);
        }
    }

    /** Si la expresion es "x.campo" y ese campo es "tipo campo[N]" (arreglo dentro del struct). */
    private ModeloPrograma.Variable campoDeTamanioFijo(ExpresionContext ctx) {
        if (!(ctx instanceof ExpAccesoContext)) {
            return null;
        }
        ExpAccesoContext acceso = (ExpAccesoContext) ctx;
        Tipo base = tipo(acceso.expresion());
        if (!base.esEstructura() || base.esArreglo()) {
            return null;
        }
        ModeloPrograma.Estructura estructura = modelo.buscarEstructura(base.getNombreEstructura());
        ModeloPrograma.Variable campo = (estructura != null) ? estructura.buscarCampo(acceso.ID().getText()) : null;
        return (campo != null && campo.getTamanioFijo() > 0) ? campo : null;
    }

    private void reservarEstructura(Lugar destino, String nombreEstructura) {
        gen.emitirLlamada(destino, null, "new " + nombreEstructura, "zc_reservar",
                List.of(new LiteralLugar("sizeof(" + nombreEstructura + ")")));
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    /** Visita una expresion que DEBE producir valor (nunca devuelve null). */
    private Lugar valor(ExpresionContext ctx) {
        Lugar lugar = visit(ctx);
        return (lugar != null) ? lugar : new LiteralLugar("0");
    }

    private Tipo tipo(ParseTree ctx) {
        Tipo calculado = tiposCalculados.get(ctx);
        if (calculado != null) {
            return calculado;
        }
        Tipo semantico = tiposSemantico.get(ctx);
        return (semantico != null) ? semantico : Tipo.error();
    }

    private void calcularTipo(ParseTree ctx, Tipo calculado) {
        Tipo semantico = tiposSemantico.get(ctx);
        if ((semantico == null || semantico.esError()) && calculado != null) {
            tiposCalculados.put(ctx, calculado);
        }
    }

    private Tipo resolverTipo(TipoContext ctx, int corchetesExtra) {
        Tipo base = (ctx.tipoPrimitivo() != null)
                ? resolverTipoPrimitivo(ctx.tipoPrimitivo())
                : Tipo.estructura(ctx.ID().getText());
        int total = ctx.LBRACKET().size() + corchetesExtra;
        return total > 0 ? Tipo.arregloDe(base, total) : base;
    }

    private Tipo resolverTipoPrimitivo(TipoPrimitivoContext ctx) {
        if (ctx.KW_ENTERO() != null) return Tipo.entero();
        if (ctx.KW_FLOTANTE() != null) return Tipo.decimal();
        if (ctx.KW_CARACTER() != null) return Tipo.caracter();
        if (ctx.KW_CADENA() != null) return Tipo.cadena();
        return Tipo.booleano(); // KW_BOOL
    }

    private void error(String mensaje, ParserRuleContext ctx) {
        errores.agregar(TipoError.SEMANTICO, mensaje, ctx.getStart().getLine(),
                ctx.getStart().getCharPositionInLine(), nombreArchivo);
    }
}
