package contacto.zetariano.generador;

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
import contacto.zetariano.ZetarianoBaseVisitor;
import contacto.zetariano.ZetarianoParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Genera cuartetas (C3D) para un archivo .z YA validado por
 * ZetarianoSemanticoListener. Visitor (no Listener) para controlar el
 * orden de saltos y etiquetas. Cada visitExpXxx devuelve el Lugar
 * donde queda el valor (variable/temporal/literal/campo/indice), o
 * null si la expresion no produce valor (llamada a un metodo void).
 *
 * Se usa en dos pasos (ver OrquestadorPig):
 *   1. registrarFirmas(): registra en el ModeloPrograma la clase, sus
 *      campos, constructores y metodos -- antes de generar el cuerpo de
 *      NINGUN archivo, para que Pila.z pueda llamar a Nodo.getDato().
 *   2. visit(): genera el cuerpo de cada constructor/metodo como una
 *      funcion de C aparte.
 *
 * El semantico analiza cada .z por separado, asi que para llamadas y
 * campos de OTRA clase (cima.getDato() dentro de Pila) su tipo queda en
 * "error". Aqui ya se conocen todas las clases (ModeloPrograma): esos
 * tipos se calculan de nuevo, y si el metodo o campo no existe se
 * reporta como error semantico.
 */
public class ZetarianoGeneradorCuartetas extends ZetarianoBaseVisitor<Lugar> {

    private final GeneradorCuartetas gen = new GeneradorCuartetas();
    private final ParseTreeProperty<Tipo> tiposSemantico;
    private final ParseTreeProperty<Tipo> tiposCalculados = new ParseTreeProperty<>();
    private final ModeloPrograma modelo;
    private final RecolectorErrores errores;
    private final String nombreArchivo;

    private final ParseTreeProperty<ModeloPrograma.Funcion> funcionDe = new ParseTreeProperty<>();
    private ModeloPrograma.Funcion constructorImplicito;
    private String claseActual;
    private ModeloPrograma.Funcion funcionActual;
    private AmbitosGeneracion ambitos;

    // {etiquetaContinua, etiquetaFin} del ciclo/switch mas interno activo
    private final Deque<String[]> pilaControlFlujo = new ArrayDeque<>();

    public ZetarianoGeneradorCuartetas(ParseTreeProperty<Tipo> tipos, ModeloPrograma modelo,
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
        ClaseContext clase = programa.clase();
        String nombreClase = clase.ID().getText();
        ModeloPrograma.Estructura estructura = modelo.registrarEstructura(nombreClase, true);

        boolean tieneConstructor = false;
        for (MiembroContext miembro : clase.miembro()) {
            if (miembro.campo() != null) {
                CampoContext campo = miembro.campo();
                Tipo tipo = resolverTipo(campo.tipo(), campo.LBRACKET().size());
                estructura.agregarCampo(new ModeloPrograma.Variable(campo.ID().getText(), tipo));
            } else if (miembro.constructor() != null) {
                ConstructorContext ctor = miembro.constructor();
                ModeloPrograma.Funcion funcion = new ModeloPrograma.Funcion(ctor.ID().getText(), nombreClase,
                        true, false, Tipo.estructura(nombreClase), parametros(ctor.parametros()), ctor);
                modelo.agregarFuncion(funcion);
                funcionDe.put(ctor, funcion);
                tieneConstructor = true;
            } else if (miembro.metodo() != null) {
                MetodoContext metodo = miembro.metodo();
                Tipo retorno = (metodo.VOID() != null)
                        ? Tipo.vacio()
                        : resolverTipo(metodo.tipo(), metodo.LBRACKET().size());
                ModeloPrograma.Funcion funcion = new ModeloPrograma.Funcion(metodo.ID().getText(), nombreClase,
                        false, false, retorno, parametros(metodo.parametros()), metodo);
                modelo.agregarFuncion(funcion);
                funcionDe.put(metodo, funcion);
            }
        }

        if (!tieneConstructor) {
            // Igual que en Java: sin constructor explicito hay uno vacio
            // (que igual inicializa los campos con valor por defecto).
            constructorImplicito = new ModeloPrograma.Funcion(nombreClase, nombreClase, true, false,
                    Tipo.estructura(nombreClase), new ArrayList<>(), clase);
            modelo.agregarFuncion(constructorImplicito);
        }
    }

    private List<ModeloPrograma.Variable> parametros(ParametrosContext ctx) {
        List<ModeloPrograma.Variable> lista = new ArrayList<>();
        if (ctx != null) {
            for (ParametroContext parametro : ctx.parametro()) {
                Tipo tipo = resolverTipo(parametro.tipo(), parametro.LBRACKET().size());
                lista.add(new ModeloPrograma.Variable(parametro.ID().getText(), tipo));
            }
        }
        return lista;
    }

    // =====================================================================
    // Paso 2: cuerpos
    // =====================================================================

    @Override
    public Lugar visitPrograma(ProgramaContext ctx) {
        visit(ctx.clase());
        return null;
    }

    @Override
    public Lugar visitClase(ClaseContext ctx) {
        claseActual = ctx.ID().getText();
        if (constructorImplicito != null) {
            iniciarFuncion(constructorImplicito, "inicio_" + claseActual + "_constructor");
            inicializarCampos(ctx);
            terminarFuncion();
        }
        for (MiembroContext miembro : ctx.miembro()) {
            visit(miembro);
        }
        return null;
    }

    @Override
    public Lugar visitCampo(CampoContext ctx) {
        return null; // su valor inicial se asigna dentro de cada constructor (inicializarCampos)
    }

    @Override
    public Lugar visitConstructor(ConstructorContext ctx) {
        iniciarFuncion(funcionDe.get(ctx), "inicio_" + ctx.ID().getText() + "_constructor");
        inicializarCampos((ClaseContext) ctx.getParent().getParent());
        for (SentenciaContext sentencia : ctx.bloque().sentencia()) {
            visit(sentencia);
        }
        terminarFuncion();
        return null;
    }

    @Override
    public Lugar visitMetodo(MetodoContext ctx) {
        iniciarFuncion(funcionDe.get(ctx), "inicio_" + ctx.ID().getText());
        for (SentenciaContext sentencia : ctx.bloque().sentencia()) {
            visit(sentencia);
        }
        terminarFuncion();
        return null;
    }

    private void iniciarFuncion(ModeloPrograma.Funcion funcion, String etiqueta) {
        funcionActual = funcion;
        ambitos = new AmbitosGeneracion(funcion);
        funcion.iniciarCuerpo(gen);
        gen.emitirEtiqueta(etiqueta);
    }

    private void terminarFuncion() {
        funcionActual.terminarCuerpo();
        funcionActual = null;
        ambitos = null;
    }

    /** "int x = 5;" en la clase: se asigna al inicio de cada constructor, como en Java. */
    private void inicializarCampos(ClaseContext clase) {
        for (MiembroContext miembro : clase.miembro()) {
            CampoContext campo = miembro.campo();
            if (campo != null && campo.expresion() != null) {
                Lugar valor = valor(campo.expresion());
                gen.emitirAsignacion(lugarCampo(new NombreLugar("this"), claseActual, campo.ID().getText()), valor);
            }
        }
    }

    // =====================================================================
    // Sentencias
    // =====================================================================

    @Override
    public Lugar visitBloque(BloqueContext ctx) {
        ambitos.entrar();
        for (SentenciaContext sentencia : ctx.sentencia()) {
            visit(sentencia);
        }
        ambitos.salir();
        return null;
    }

    @Override
    public Lugar visitDeclaracionVariable(DeclaracionVariableContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo(), ctx.LBRACKET().size());
        declararLocal(ctx.ID().getText(), tipo, ctx.expresion());
        return null;
    }

    @Override
    public Lugar visitDeclaracionVariableSinPuntoYComa(DeclaracionVariableSinPuntoYComaContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo(), ctx.LBRACKET().size());
        declararLocal(ctx.ID().getText(), tipo, ctx.expresion());
        return null;
    }

    private void declararLocal(String nombre, Tipo tipo, ExpresionContext inicial) {
        // El valor inicial se evalua ANTES de declarar: en "int x = x + 1"
        // la x de la derecha no puede ser la nueva.
        Lugar origen = (inicial != null) ? valor(inicial) : null;
        String nombreC = ambitos.declarar(nombre, tipo);
        if (origen != null) {
            gen.emitirAsignacion(new NombreLugar(nombreC), origen);
        }
    }

    @Override
    public Lugar visitSentenciaExpresion(SentenciaExpresionContext ctx) {
        List<ExpresionContext> expresiones = ctx.expresion();
        if (expresiones.size() < 2) {
            visitarSinValor(expresiones.get(0));
            return null;
        }

        Lugar destino = valor(expresiones.get(0));
        Lugar origen = valor(expresiones.get(1));
        OperadorAsignacionContext op = ctx.operadorAsignacion();

        if (op.ASSIGN() != null) {
            gen.emitirAsignacion(destino, origen);
        } else {
            String operador = (op.PLUS_ASSIGN() != null) ? "+"
                    : (op.MINUS_ASSIGN() != null) ? "-" : "*";
            gen.emitirOperacionBinaria(destino, destino, operador, origen,
                    tipo(expresiones.get(0)), tipo(expresiones.get(1)));
        }
        return null;
    }

    @Override
    public Lugar visitSentenciaIf(SentenciaIfContext ctx) {
        List<ExpresionContext> condiciones = ctx.expresion();
        List<SentenciaOBloqueContext> cuerpos = ctx.sentenciaOBloque();
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
    public Lugar visitSentenciaOBloque(SentenciaOBloqueContext ctx) {
        if (ctx.bloque() != null) {
            visit(ctx.bloque());
        } else {
            ambitos.entrar();
            visit(ctx.sentencia());
            ambitos.salir();
        }
        return null;
    }

    @Override
    public Lugar visitSentenciaWhile(SentenciaWhileContext ctx) {
        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);
        Lugar condicion = valor(ctx.expresion());
        gen.emitirSaltoSiFalso(condicion, etiquetaFin);

        pilaControlFlujo.push(new String[]{etiquetaInicio, etiquetaFin});
        visit(ctx.sentenciaOBloque());
        pilaControlFlujo.pop();

        gen.emitirSalto(etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public Lugar visitSentenciaDoWhile(SentenciaDoWhileContext ctx) {
        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaContinua = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);

        pilaControlFlujo.push(new String[]{etiquetaContinua, etiquetaFin});
        visit(ctx.bloque());
        pilaControlFlujo.pop();

        gen.emitirEtiqueta(etiquetaContinua);
        Lugar condicion = valor(ctx.expresion());
        gen.emitirSaltoSiVerdadero(condicion, etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public Lugar visitSentenciaFor(SentenciaForContext ctx) {
        ambitos.entrar(); // la variable del for solo existe dentro del for
        if (ctx.forInit() != null) {
            visit(ctx.forInit());
        }

        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaContinua = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);
        if (ctx.expresion() != null) {
            Lugar condicion = valor(ctx.expresion());
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
        ambitos.salir();
        return null;
    }

    @Override
    public Lugar visitForInit(ForInitContext ctx) {
        if (ctx.declaracionVariableSinPuntoYComa() != null) {
            visit(ctx.declaracionVariableSinPuntoYComa());
        } else {
            visit(ctx.expresionLista());
        }
        return null;
    }

    @Override
    public Lugar visitForUpdate(ForUpdateContext ctx) {
        visit(ctx.expresionLista());
        return null;
    }

    @Override
    public Lugar visitExpresionLista(ExpresionListaContext ctx) {
        for (ExpresionContext expresion : ctx.expresion()) {
            visitarSinValor(expresion);
        }
        return null;
    }

    /**
     * Una expresion usada como sentencia ("i++;", el update de un for):
     * su valor no se usa, asi que "i++" no necesita guardar el valor
     * anterior en un temporal.
     */
    private void visitarSinValor(ExpresionContext ctx) {
        if (ctx instanceof ExpIncDecSufijoContext) {
            ExpIncDecSufijoContext incDec = (ExpIncDecSufijoContext) ctx;
            Lugar lugar = valor(incDec.expresion());
            gen.emitirOperacionBinaria(lugar, lugar, (incDec.INC() != null) ? "+" : "-", new LiteralLugar("1"));
        } else {
            visit(ctx);
        }
    }

    @Override
    public Lugar visitSentenciaSwitch(SentenciaSwitchContext ctx) {
        Lugar selector = valor(ctx.expresion());
        Tipo tipoSelector = tipo(ctx.expresion());
        String etiquetaFin = gen.nuevaEtiqueta();

        // Como en Java: un caso sin "break" sigue con el cuerpo del
        // siguiente (fall-through). Primero se prueban las comparaciones
        // en orden; cada una salta a la etiqueta de su cuerpo.
        List<CasoSwitchContext> casos = ctx.casoSwitch();
        List<String> etiquetasCuerpo = new ArrayList<>();
        for (CasoSwitchContext caso : casos) {
            String etiquetaCuerpo = gen.nuevaEtiqueta();
            etiquetasCuerpo.add(etiquetaCuerpo);
            Lugar comparacion = gen.nuevoTemporal(Tipo.booleano());
            gen.emitirOperacionBinaria(comparacion, selector, "==",
                    new LiteralLugar(caso.literalCaso().getText()), tipoSelector, tipoLiteralCaso(caso.literalCaso()));
            gen.emitirSaltoSiVerdadero(comparacion, etiquetaCuerpo);
        }
        String etiquetaDefault = (ctx.casoDefault() != null) ? gen.nuevaEtiqueta() : etiquetaFin;
        gen.emitirSalto(etiquetaDefault);

        pilaControlFlujo.push(new String[]{etiquetaFin, etiquetaFin});
        ambitos.entrar();
        for (int i = 0; i < casos.size(); i++) {
            gen.emitirEtiqueta(etiquetasCuerpo.get(i));
            for (SentenciaContext sentencia : casos.get(i).sentencia()) {
                visit(sentencia);
            }
        }
        if (ctx.casoDefault() != null) {
            gen.emitirEtiqueta(etiquetaDefault);
            for (SentenciaContext sentencia : ctx.casoDefault().sentencia()) {
                visit(sentencia);
            }
        }
        ambitos.salir();
        pilaControlFlujo.pop();
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    private Tipo tipoLiteralCaso(LiteralCasoContext ctx) {
        if (ctx.STRING_LITERAL() != null) return Tipo.cadena();
        if (ctx.CHAR_LITERAL() != null) return Tipo.caracter();
        return Tipo.entero();
    }

    @Override
    public Lugar visitSentenciaBreak(SentenciaBreakContext ctx) {
        if (!pilaControlFlujo.isEmpty()) {
            gen.emitirSalto(pilaControlFlujo.peek()[1]);
        }
        return null;
    }

    @Override
    public Lugar visitSentenciaContinue(SentenciaContinueContext ctx) {
        // "continue" salta al ciclo mas interno, aunque haya un switch en medio
        for (String[] destino : pilaControlFlujo) {
            if (!destino[0].equals(destino[1])) { // en un switch ambos son la etiqueta de fin
                gen.emitirSalto(destino[0]);
                break;
            }
        }
        return null;
    }

    @Override
    public Lugar visitSentenciaReturn(SentenciaReturnContext ctx) {
        if (funcionActual.esConstructor()) {
            gen.emitirRetorno(new NombreLugar("this")); // en C el constructor devuelve el objeto creado
            return null;
        }
        Lugar valor = (ctx.expresion() != null) ? valor(ctx.expresion()) : null;
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
    public Lugar visitExpDecimal(ExpDecimalContext ctx) {
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
    public Lugar visitExpNulo(ExpNuloContext ctx) {
        return new LiteralLugar("NULL");
    }

    @Override
    public Lugar visitExpThis(ExpThisContext ctx) {
        return new NombreLugar("this");
    }

    @Override
    public Lugar visitExpId(ExpIdContext ctx) {
        String nombre = ctx.ID().getText();
        String local = ambitos.resolver(nombre);
        if (local != null) {
            calcularTipo(ctx, ambitos.tipoDe(nombre));
            return new NombreLugar(local);
        }
        // No es local ni parametro: es un campo de esta clase (this->campo)
        ModeloPrograma.Estructura clase = modelo.buscarEstructura(claseActual);
        ModeloPrograma.Variable campo = (clase != null) ? clase.buscarCampo(nombre) : null;
        if (campo != null) {
            calcularTipo(ctx, campo.getTipo());
            return new CampoLugar(new NombreLugar("this"), campo.getNombreC());
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
        String operador = (ctx.INC() != null) ? "+" : "-";
        gen.emitirOperacionBinaria(lugar, lugar, operador, new LiteralLugar("1"));
        return lugar;
    }

    @Override
    public Lugar visitExpIncDecSufijo(ExpIncDecSufijoContext ctx) {
        Lugar lugar = valor(ctx.expresion());
        calcularTipo(ctx, tipo(ctx.expresion()));
        Lugar temp = gen.nuevoTemporal(tipo(ctx));
        gen.emitirAsignacion(temp, lugar);
        String operador = (ctx.INC() != null) ? "+" : "-";
        gen.emitirOperacionBinaria(lugar, lugar, operador, new LiteralLugar("1"));
        return temp;
    }

    @Override
    public Lugar visitExpMultiplicativa(ExpMultiplicativaContext ctx) {
        if (ctx.STAR() != null) return emitirBinaria(ctx, "*", Operador.MULTIPLICACION);
        if (ctx.SLASH() != null) return emitirBinaria(ctx, "/", Operador.DIVISION);
        return emitirBinaria(ctx, "%", Operador.MODULO);
    }

    @Override
    public Lugar visitExpAditiva(ExpAditivaContext ctx) {
        return (ctx.PLUS() != null)
                ? emitirBinaria(ctx, "+", Operador.SUMA)
                : emitirBinaria(ctx, "-", Operador.RESTA);
    }

    @Override
    public Lugar visitExpRelacional(ExpRelacionalContext ctx) {
        if (ctx.LT() != null) return emitirBinaria(ctx, "<", Operador.MENOR);
        if (ctx.GT() != null) return emitirBinaria(ctx, ">", Operador.MAYOR);
        if (ctx.LE() != null) return emitirBinaria(ctx, "<=", Operador.MENOR_IGUAL);
        return emitirBinaria(ctx, ">=", Operador.MAYOR_IGUAL);
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
    public Lugar visitExpTernario(ExpTernarioContext ctx) {
        Lugar condicion = valor(ctx.expresion(0));
        String etiquetaFalso = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirSaltoSiFalso(condicion, etiquetaFalso);
        Lugar siVerdadero = valor(ctx.expresion(1));
        calcularTipo(ctx, tipo(ctx.expresion(1)));
        Lugar temp = gen.nuevoTemporal(tipo(ctx));
        gen.emitirAsignacion(temp, siVerdadero);
        gen.emitirSalto(etiquetaFin);
        gen.emitirEtiqueta(etiquetaFalso);
        gen.emitirAsignacion(temp, valor(ctx.expresion(2)));
        gen.emitirEtiqueta(etiquetaFin);

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
            return lugarCampo(base, tipoBase.getNombreEstructura(), nombreCampo, ctx);
        }
        return new CampoLugar(base, nombreCampo);
    }

    private Lugar lugarCampo(Lugar base, String clase, String nombreCampo) {
        return lugarCampo(base, clase, nombreCampo, null);
    }

    private Lugar lugarCampo(Lugar base, String clase, String nombreCampo, ParserRuleContext ctx) {
        ModeloPrograma.Estructura estructura = modelo.buscarEstructura(clase);
        ModeloPrograma.Variable campo = (estructura != null) ? estructura.buscarCampo(nombreCampo) : null;
        if (campo == null) {
            if (ctx != null) {
                error("La clase '" + clase + "' no tiene el atributo '" + nombreCampo + "'", ctx);
            }
            return new CampoLugar(base, nombreCampo);
        }
        if (ctx != null) {
            calcularTipo(ctx, campo.getTipo());
        }
        return new CampoLugar(base, campo.getNombreC());
    }

    @Override
    public Lugar visitExpLlamadaLocal(ExpLlamadaLocalContext ctx) {
        // metodo() dentro de la clase = this.metodo()
        List<Lugar> argumentos = evaluarArgumentos(ctx.argumentos());
        return emitirLlamadaMetodo(ctx, new NombreLugar("this"), claseActual, ctx.ID().getText(), argumentos);
    }

    @Override
    public Lugar visitExpLlamadaMetodo(ExpLlamadaMetodoContext ctx) {
        Lugar objetivo = valor(ctx.expresion());
        Tipo tipoObjetivo = tipo(ctx.expresion());
        List<Lugar> argumentos = evaluarArgumentos(ctx.argumentos());
        if (!tipoObjetivo.esEstructura() || tipoObjetivo.esArreglo()) {
            error("Solo se puede llamar un metodo sobre un objeto, no sobre " + tipoObjetivo, ctx);
            return new LiteralLugar("0");
        }
        return emitirLlamadaMetodo(ctx, objetivo, tipoObjetivo.getNombreEstructura(), ctx.ID().getText(), argumentos);
    }

    private Lugar emitirLlamadaMetodo(ParserRuleContext ctx, Lugar objetivo, String clase, String nombre,
                                      List<Lugar> argumentos) {
        ModeloPrograma.Funcion metodo = modelo.buscarMetodo(clase, nombre, argumentos.size());
        if (metodo == null) {
            error("No existe el metodo '" + nombre + "' con " + argumentos.size()
                    + " argumento(s) en la clase '" + clase + "'", ctx);
            return new LiteralLugar("0");
        }
        calcularTipo(ctx, metodo.getRetorno());
        Lugar destino = metodo.getRetorno().esVacio() ? null : gen.nuevoTemporal(metodo.getRetorno());
        gen.emitirLlamada(destino, objetivo, nombre, metodo.getNombreC(), argumentos);
        return destino;
    }

    @Override
    public Lugar visitExpNuevoObjeto(ExpNuevoObjetoContext ctx) {
        String clase = ctx.ID().getText();
        List<Lugar> argumentos = evaluarArgumentos(ctx.argumentos());
        ModeloPrograma.Funcion constructor = modelo.buscarConstructor(clase, argumentos.size());
        if (constructor == null) {
            error("No existe un constructor de '" + clase + "' con " + argumentos.size() + " argumento(s)", ctx);
            return new LiteralLugar("NULL");
        }
        calcularTipo(ctx, Tipo.estructura(clase));
        Lugar temp = gen.nuevoTemporal(Tipo.estructura(clase));
        gen.emitirLlamada(temp, null, "new " + clase, constructor.getNombreC(), argumentos);
        return temp;
    }

    private List<Lugar> evaluarArgumentos(ArgumentosContext ctx) {
        List<Lugar> lugares = new ArrayList<>();
        if (ctx != null) {
            for (ExpresionContext argumento : ctx.expresion()) {
                lugares.add(valor(argumento));
            }
        }
        return lugares;
    }

    @Override
    public Lugar visitExpNuevoArreglo(ExpNuevoArregloContext ctx) {
        List<Lugar> argumentos = new ArrayList<>();
        Tipo base = resolverTipoPrimitivo(ctx.tipoPrimitivo());
        argumentos.add(new LiteralLugar(String.valueOf(ctx.expresion().size())));
        argumentos.add(new LiteralLugar("sizeof(" + RuntimeC.tipoC(base) + ")"));
        for (ExpresionContext dimension : ctx.expresion()) {
            argumentos.add(valor(dimension));
        }
        Tipo tipoArreglo = Tipo.arregloDe(base, ctx.expresion().size());
        calcularTipo(ctx, tipoArreglo);
        Lugar temp = gen.nuevoTemporal(tipoArreglo);
        gen.emitirLlamada(temp, null, "new " + base + "[]", "zc_nuevo_arreglo", argumentos);
        return temp;
    }

    @Override
    public Lugar visitExpArregloLiteral(ExpArregloLiteralContext ctx) {
        // {a, b, c}: se reserva un arreglo de 3 y se asigna elemento por elemento
        Tipo tipoArreglo = tipo(ctx);
        if (!tipoArreglo.esArreglo()) {
            Tipo elemento = ctx.expresion().isEmpty() ? Tipo.entero() : tipo(ctx.expresion(0));
            tipoArreglo = Tipo.arregloDe(elemento.esError() ? Tipo.entero() : elemento);
        }
        Tipo elemento = tipoArreglo.tipoElemento();
        List<Lugar> valores = new ArrayList<>();
        for (ExpresionContext expresion : ctx.expresion()) {
            valores.add(valor(expresion));
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

    @Override
    public Lugar visitExpLlamadaPrintln(ExpLlamadaPrintlnContext ctx) {
        if (ctx.expresion() != null) {
            Lugar valor = valor(ctx.expresion());
            gen.emitirImprimir(valor, RuntimeC.formatoImpresion(tipo(ctx.expresion())), true);
        } else {
            gen.emitirImprimir(new LiteralLugar("\"\""), "%s", true);
        }
        return null;
    }

    @Override
    public Lugar visitExpLlamadaPrint(ExpLlamadaPrintContext ctx) {
        if (ctx.expresion() != null) {
            Lugar valor = valor(ctx.expresion());
            gen.emitirImprimir(valor, RuntimeC.formatoImpresion(tipo(ctx.expresion())), false);
        }
        return null;
    }

    @Override
    public Lugar visitExpLlamadaReadln(ExpLlamadaReadlnContext ctx) {
        calcularTipo(ctx, Tipo.cadena());
        Lugar temp = gen.nuevoTemporal(Tipo.cadena());
        gen.emitirLeer(temp, Tipo.cadena());
        return temp;
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    /** Visita una expresion que DEBE producir valor (nunca devuelve null). */
    private Lugar valor(ExpresionContext ctx) {
        Lugar lugar = visit(ctx);
        return (lugar != null) ? lugar : new LiteralLugar("0");
    }

    /**
     * Tipo de una expresion: el que calculo este generador (con todas
     * las clases a la vista) o, si no, el del semantico.
     */
    private Tipo tipo(ParseTree ctx) {
        Tipo calculado = tiposCalculados.get(ctx);
        if (calculado != null) {
            return calculado;
        }
        Tipo semantico = tiposSemantico.get(ctx);
        return (semantico != null) ? semantico : Tipo.error();
    }

    /** Guarda el tipo calculado aqui solo si el semantico no pudo resolverlo. */
    private void calcularTipo(ParseTree ctx, Tipo calculado) {
        Tipo semantico = tiposSemantico.get(ctx);
        if ((semantico == null || semantico.esError()) && calculado != null) {
            tiposCalculados.put(ctx, calculado);
        }
    }

    private Tipo resolverTipo(TipoContext ctx, int corchetes) {
        Tipo base = (ctx.tipoPrimitivo() != null)
                ? resolverTipoPrimitivo(ctx.tipoPrimitivo())
                : Tipo.estructura(ctx.ID().getText());
        return corchetes > 0 ? Tipo.arregloDe(base, corchetes) : base;
    }

    private Tipo resolverTipoPrimitivo(TipoPrimitivoContext ctx) {
        if (ctx.KW_INT() != null) return Tipo.entero();
        if (ctx.KW_DOUBLE() != null) return Tipo.decimal();
        if (ctx.KW_CHAR() != null) return Tipo.caracter();
        if (ctx.KW_BOOLEAN() != null) return Tipo.booleano();
        return Tipo.cadena(); // KW_STRING
    }

    private void error(String mensaje, ParserRuleContext ctx) {
        errores.agregar(TipoError.SEMANTICO, mensaje, ctx.getStart().getLine(),
                ctx.getStart().getCharPositionInLine(), nombreArchivo);
    }
}
