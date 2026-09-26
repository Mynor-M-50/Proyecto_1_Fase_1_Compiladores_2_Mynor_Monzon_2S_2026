package contacto.piglatin.generador;

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
import contacto.piglatin.PigLatinBaseVisitor;
import contacto.piglatin.PigLatinParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Genera cuartetas (C3D) para un archivo .pig YA validado por
 * PigLatinSemanticoListener. Visitor por la misma razon que en los
 * otros dos lenguajes (control de orden de saltos/etiquetas).
 *
 * Todo el .pig es el main() del programa C. Las clases (.z) y funciones
 * (.y) que usa ya estan registradas en el ModeloPrograma cuando se
 * genera este archivo (ver OrquestadorPig), asi que cada llamada sale
 * con su nombre real en C (pila.apilar(x) -> Pila_apilar(pila, x)).
 *
 * Diferencia real de forma: "objetivo" (id.campo[i].metodo()) es una
 * lista plana de sufijos, no recursiva -- se resuelve iterando, igual
 * que en el semantico (ver PigLatinSemanticoListener.exitObjetivo).
 *
 * perge = continue (salta a la actualizacion/inicio del ciclo)
 * interrumpe = break (sale del ciclo)
 */
public class PigLatinGeneradorCuartetas extends PigLatinBaseVisitor<Lugar> {

    private final GeneradorCuartetas gen = new GeneradorCuartetas();
    private final ParseTreeProperty<Tipo> tiposSemantico;
    private final ParseTreeProperty<Tipo> tiposCalculados = new ParseTreeProperty<>();
    private final ModeloPrograma modelo;
    private final RecolectorErrores errores;
    private final String nombreArchivo;

    private ModeloPrograma.Funcion main;
    private AmbitosGeneracion ambitos;

    // {etiquetaContinua, etiquetaFin} del ciclo mas interno activo
    private final Deque<String[]> pilaControlFlujo = new ArrayDeque<>();

    public PigLatinGeneradorCuartetas(ParseTreeProperty<Tipo> tipos, ModeloPrograma modelo,
                                      RecolectorErrores errores, String nombreArchivo) {
        this.tiposSemantico = tipos;
        this.modelo = modelo;
        this.errores = errores;
        this.nombreArchivo = nombreArchivo;
    }

    public GeneradorCuartetas getGenerador() {
        return gen;
    }

    public void registrarFirmas(ProgramaContext programa) {
        main = new ModeloPrograma.Funcion("main", null, false, true, Tipo.entero(), new ArrayList<>(), programa);
        modelo.agregarFuncion(main);
    }

    // =====================================================================
    // Programa / secciones
    // =====================================================================

    @Override
    public Lugar visitPrograma(ProgramaContext ctx) {
        // Pig Latin no tiene bloques con ambito propio (el semantico usa
        // una sola tabla), asi que todo el programa es un solo ambito.
        ambitos = new AmbitosGeneracion(main);
        main.iniciarCuerpo(gen);
        if (ctx.seccionVariables() != null) {
            visit(ctx.seccionVariables());
        }
        visit(ctx.seccionMaior());
        main.terminarCuerpo();
        return null;
    }

    @Override
    public Lugar visitSeccionVariables(SeccionVariablesContext ctx) {
        for (DeclaracionContext declaracion : ctx.declaracion()) {
            visit(declaracion);
        }
        return null;
    }

    @Override
    public Lugar visitSeccionMaior(SeccionMaiorContext ctx) {
        gen.emitirEtiqueta("inicio_programa");
        for (InstruccionContext instruccion : ctx.instruccion()) {
            visit(instruccion);
        }
        return null;
    }

    // =====================================================================
    // Declaraciones
    // =====================================================================

    @Override
    public Lugar visitDeclObjeto(DeclObjetoContext ctx) {
        String clase = ctx.ID(1).getText();
        List<Lugar> argumentos = evaluarArgumentos(ctx.listaArgumentos());
        Lugar variable = new NombreLugar(ambitos.declarar(ctx.ID(0).getText(), Tipo.estructura(clase)));
        ModeloPrograma.Funcion constructor = modelo.buscarConstructor(clase, argumentos.size());
        if (constructor == null) {
            error("No existe un constructor de '" + clase + "' con " + argumentos.size() + " argumento(s)", ctx);
            return null;
        }
        gen.emitirLlamada(variable, null, "new " + clase, constructor.getNombreC(), argumentos);
        return null;
    }

    @Override
    public Lugar visitDeclEstructura(DeclEstructuraContext ctx) {
        Tipo tipo = Tipo.estructura(ctx.ID(1).getText());
        Lugar valor = generarEstructura(ctx.literalEstructura(), tipo);
        String nombreC = ambitos.declarar(ctx.ID(0).getText(), tipo);
        gen.emitirAsignacion(new NombreLugar(nombreC), valor);
        return null;
    }

    @Override
    public Lugar visitDeclBooleana(DeclBooleanaContext ctx) {
        Lugar valor = (ctx.expresion() != null)
                ? valor(ctx.expresion())
                : new LiteralLugar(ctx.valorBooleano().VERUM() != null ? "1" : "0");
        String nombreC = ambitos.declarar(ctx.ID().getText(), Tipo.booleano());
        gen.emitirAsignacion(new NombreLugar(nombreC), valor);
        return null;
    }

    @Override
    public Lugar visitDeclConValor(DeclConValorContext ctx) {
        Lugar valor = valor(ctx.expresion());
        String nombreC = ambitos.declarar(ctx.ID().getText(), resolverTipo(ctx.tipo()));
        gen.emitirAsignacion(new NombreLugar(nombreC), valor);
        return null;
    }

    @Override
    public Lugar visitDeclSinValor(DeclSinValorContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo());
        String nombreC = ambitos.declarar(ctx.ID().getText(), tipo);
        if (tipo.esEstructura() && modelo.buscarEstructura(tipo.getNombreEstructura()) != null) {
            reservarEstructura(new NombreLugar(nombreC), tipo.getNombreEstructura());
        }
        return null;
    }

    @Override
    public Lugar visitArregloTipado(ArregloTipadoContext ctx) {
        Tipo tipo = Tipo.arregloDe(resolverTipo(ctx.tipo()));
        declararArreglo(ctx.ID().getText(), tipo, ctx.dimension(), ctx.listaValores());
        return null;
    }

    @Override
    public Lugar visitArregloInferido(ArregloInferidoContext ctx) {
        Tipo elemento = Tipo.entero();
        List<ValorListaContext> valores = ctx.listaValores().valorLista();
        if (!valores.isEmpty() && valores.get(0).expresion() != null) {
            Tipo primero = tipo(valores.get(0).expresion());
            if (!primero.esError()) {
                elemento = primero;
            }
        }
        declararArreglo(ctx.ID().getText(), Tipo.arregloDe(elemento), ctx.dimension(), ctx.listaValores());
        return null;
    }

    private void declararArreglo(String nombre, Tipo tipo, DimensionContext dimension, ListaValoresContext valores) {
        Lugar tamanio = (dimension != null) ? valor(dimension.expresion()) : null;
        Lugar arreglo = null;
        if (valores != null) {
            arreglo = generarLista(valores, tipo, tamanio);
        } else if (tamanio != null) {
            arreglo = gen.nuevoTemporal(tipo);
            reservarArreglo(arreglo, tipo.tipoElemento(), tamanio);
        }
        String nombreC = ambitos.declarar(nombre, tipo);
        if (arreglo != null) {
            gen.emitirAsignacion(new NombreLugar(nombreC), arreglo);
        }
    }

    // =====================================================================
    // Literales {campo: valor; ...} y {v1, v2, ...}
    // =====================================================================

    /** Reserva una estructura nueva y le asigna cada "campo: valor" del literal. */
    private Lugar generarEstructura(LiteralEstructuraContext ctx, Tipo tipo) {
        String nombreEstructura = tipo.getNombreEstructura();
        ModeloPrograma.Estructura estructura = modelo.buscarEstructura(nombreEstructura);
        if (estructura == null) {
            error("No existe la estructura '" + nombreEstructura + "' (revisa el import)", ctx);
            return new LiteralLugar("NULL");
        }
        Lugar temp = gen.nuevoTemporal(tipo);
        reservarEstructura(temp, nombreEstructura);
        for (AsignacionAtributoContext atributo : ctx.asignacionAtributo()) {
            ModeloPrograma.Variable campo = estructura.buscarCampo(atributo.ID().getText());
            if (campo == null) {
                error("La estructura '" + nombreEstructura + "' no tiene el campo '"
                        + atributo.ID().getText() + "'", atributo);
                continue;
            }
            asignarCampo(new CampoLugar(temp, campo.getNombreC()), campo, atributo.valorAtributo());
        }
        return temp;
    }

    private void asignarCampo(Lugar destino, ModeloPrograma.Variable campo, ValorAtributoContext valor) {
        if (valor.literalEstructura() != null) {
            gen.emitirAsignacion(destino, generarEstructura(valor.literalEstructura(), campo.getTipo()));
        } else if (valor.listaValores() != null) {
            if (campo.getTamanioFijo() > 0) {
                // arreglo dentro del struct (entero notas[3] en el .y): elemento por elemento
                asignarElementos(destino, valor.listaValores(), campo.getTipo().tipoElemento(), campo.getTamanioFijo());
            } else {
                gen.emitirAsignacion(destino, generarLista(valor.listaValores(), campo.getTipo(), null));
            }
        } else if (valor.dimensionPrimitiva() != null) {
            // numerus[5]: fija el tamanio de un arreglo del struct
            if (campo.getTamanioFijo() == 0) {
                Lugar tamanio = valor(valor.dimensionPrimitiva().expresion());
                Lugar arreglo = gen.nuevoTemporal(campo.getTipo());
                reservarArreglo(arreglo, campo.getTipo().tipoElemento(), tamanio);
                gen.emitirAsignacion(destino, arreglo);
            }
        } else {
            gen.emitirAsignacion(destino, valor(valor.expresion()));
        }
    }

    /** Reserva un arreglo con los valores de la lista (de tamanio "tamanio" si viene, o el de la lista). */
    private Lugar generarLista(ListaValoresContext ctx, Tipo tipoArreglo, Lugar tamanio) {
        Tipo elemento = tipoArreglo.tipoElemento();
        List<Lugar> valores = new ArrayList<>();
        for (ValorListaContext valor : ctx.valorLista()) {
            valores.add(valorLista(valor, elemento));
        }
        Lugar arreglo = gen.nuevoTemporal(tipoArreglo);
        reservarArreglo(arreglo, elemento,
                (tamanio != null) ? tamanio : new LiteralLugar(String.valueOf(valores.size())));
        for (int i = 0; i < valores.size(); i++) {
            gen.emitirAsignacion(new IndiceLugar(arreglo, new LiteralLugar(String.valueOf(i))), valores.get(i));
        }
        return arreglo;
    }

    private void asignarElementos(Lugar destino, ListaValoresContext ctx, Tipo elemento, int maximo) {
        List<ValorListaContext> valores = ctx.valorLista();
        for (int i = 0; i < valores.size() && i < maximo; i++) {
            Lugar valor = valorLista(valores.get(i), elemento);
            gen.emitirAsignacion(new IndiceLugar(destino, new LiteralLugar(String.valueOf(i))), valor);
        }
    }

    private Lugar valorLista(ValorListaContext valor, Tipo elemento) {
        if (valor.literalEstructura() != null) {
            return generarEstructura(valor.literalEstructura(), elemento);
        }
        return valor(valor.expresion());
    }

    private void reservarEstructura(Lugar destino, String nombreEstructura) {
        gen.emitirLlamada(destino, null, "new " + nombreEstructura, "zc_reservar",
                List.of(new LiteralLugar("sizeof(" + nombreEstructura + ")")));
    }

    private void reservarArreglo(Lugar destino, Tipo elemento, Lugar tamanio) {
        gen.emitirLlamada(destino, null, "new " + elemento + "[]", "zc_nuevo_arreglo", List.of(
                new LiteralLugar("1"),
                new LiteralLugar("sizeof(" + RuntimeC.tipoC(elemento) + ")"),
                tamanio));
    }

    // =====================================================================
    // Instrucciones
    // =====================================================================

    @Override
    public Lugar visitAsignacionSimple(AsignacionSimpleContext ctx) {
        Lugar destino = valorObjetivo(ctx.objetivo());
        Lugar origen = valor(ctx.expresion());
        gen.emitirAsignacion(destino, origen);
        return null;
    }

    @Override
    public Lugar visitAsignacionEstructura(AsignacionEstructuraContext ctx) {
        Lugar destino = valorObjetivo(ctx.objetivo());
        Tipo tipo = tipo(ctx.objetivo());
        if (!tipo.esEstructura() || tipo.esArreglo()) {
            error("Solo se puede asignar {campo: valor} a una estructura, no a " + tipo, ctx);
            return null;
        }
        gen.emitirAsignacion(destino, generarEstructura(ctx.literalEstructura(), tipo));
        return null;
    }

    @Override
    public Lugar visitAsignacionLista(AsignacionListaContext ctx) {
        Lugar destino = valorObjetivo(ctx.objetivo());
        Tipo tipo = tipo(ctx.objetivo());
        ModeloPrograma.Variable campoFijo = campoFinal(ctx.objetivo());
        if (campoFijo != null && campoFijo.getTamanioFijo() > 0) {
            asignarElementos(destino, ctx.listaValores(), campoFijo.getTipo().tipoElemento(), campoFijo.getTamanioFijo());
            return null;
        }
        if (!tipo.esArreglo()) {
            error("Solo se puede asignar {v1, v2, ...} a un arreglo, no a " + tipo, ctx);
            return null;
        }
        gen.emitirAsignacion(destino, generarLista(ctx.listaValores(), tipo, null));
        return null;
    }

    @Override
    public Lugar visitIncremento(IncrementoContext ctx) {
        Lugar lugar = valorObjetivo(ctx.objetivo());
        String operador = (ctx.MASMAS() != null) ? "+" : "-";
        gen.emitirOperacionBinaria(lugar, lugar, operador, new LiteralLugar("1"));
        return null;
    }

    @Override
    public Lugar visitLlamadaFuncionInstruccion(LlamadaFuncionInstruccionContext ctx) {
        visit(ctx.llamadaFuncion());
        return null;
    }

    @Override
    public Lugar visitLlamadaMetodoInstruccion(LlamadaMetodoInstruccionContext ctx) {
        visit(ctx.objetivo());
        return null;
    }

    @Override
    public Lugar visitCondicional(CondicionalContext ctx) {
        String etiquetaFin = gen.nuevaEtiqueta();
        String etiquetaSiguiente = gen.nuevaEtiqueta();

        Lugar condicion = valor(ctx.expresion());
        gen.emitirSaltoSiFalso(condicion, etiquetaSiguiente);
        visit(ctx.bloque());
        gen.emitirSalto(etiquetaFin);
        gen.emitirEtiqueta(etiquetaSiguiente);

        for (RamaAliterSiContext rama : ctx.ramaAliterSi()) {
            etiquetaSiguiente = gen.nuevaEtiqueta();
            Lugar cond = valor(rama.expresion());
            gen.emitirSaltoSiFalso(cond, etiquetaSiguiente);
            visit(rama.bloque());
            gen.emitirSalto(etiquetaFin);
            gen.emitirEtiqueta(etiquetaSiguiente);
        }

        if (ctx.ramaAliter() != null) {
            visit(ctx.ramaAliter().bloque());
        }

        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public Lugar visitBloque(BloqueContext ctx) {
        for (InstruccionContext instruccion : ctx.instruccion()) {
            visit(instruccion);
        }
        return null;
    }

    @Override
    public Lugar visitCicloDum(CicloDumContext ctx) {
        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);
        Lugar condicion = valor(ctx.expresion());
        gen.emitirSaltoSiFalso(condicion, etiquetaFin);

        pilaControlFlujo.push(new String[]{etiquetaInicio, etiquetaFin});
        visit(ctx.bloque());
        pilaControlFlujo.pop();

        gen.emitirSalto(etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public Lugar visitCicloFacere(CicloFacereContext ctx) {
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
    public Lugar visitCicloPer(CicloPerContext ctx) {
        visit(ctx.inicializacionPer());

        String etiquetaInicio = gen.nuevaEtiqueta();
        String etiquetaContinua = gen.nuevaEtiqueta();
        String etiquetaFin = gen.nuevaEtiqueta();

        gen.emitirEtiqueta(etiquetaInicio);
        Lugar condicion = valor(ctx.expresion());
        gen.emitirSaltoSiFalso(condicion, etiquetaFin);

        pilaControlFlujo.push(new String[]{etiquetaContinua, etiquetaFin});
        visit(ctx.bloque());
        pilaControlFlujo.pop();

        gen.emitirEtiqueta(etiquetaContinua);
        visit(ctx.actualizacionPer());
        gen.emitirSalto(etiquetaInicio);
        gen.emitirEtiqueta(etiquetaFin);
        return null;
    }

    @Override
    public Lugar visitPerDeclara(PerDeclaraContext ctx) {
        Lugar valor = valor(ctx.expresion());
        String nombreC = ambitos.declarar(ctx.ID().getText(), resolverTipo(ctx.tipo()));
        gen.emitirAsignacion(new NombreLugar(nombreC), valor);
        return null;
    }

    @Override
    public Lugar visitPerAsigna(PerAsignaContext ctx) {
        Lugar destino = valorObjetivo(ctx.objetivo());
        Lugar valor = valor(ctx.expresion());
        gen.emitirAsignacion(destino, valor);
        return null;
    }

    @Override
    public Lugar visitPerIncremento(PerIncrementoContext ctx) {
        Lugar lugar = valorObjetivo(ctx.objetivo());
        String operador = (ctx.MASMAS() != null) ? "+" : "-";
        gen.emitirOperacionBinaria(lugar, lugar, operador, new LiteralLugar("1"));
        return null;
    }

    @Override
    public Lugar visitPerAsignacion(PerAsignacionContext ctx) {
        Lugar destino = valorObjetivo(ctx.objetivo());
        Lugar valor = valor(ctx.expresion());
        gen.emitirAsignacion(destino, valor);
        return null;
    }

    @Override
    public Lugar visitPerge(PergeContext ctx) {
        // perge = continue
        if (!pilaControlFlujo.isEmpty()) {
            gen.emitirSalto(pilaControlFlujo.peek()[0]);
        }
        return null;
    }

    @Override
    public Lugar visitInterrumpe(InterrumpeContext ctx) {
        // interrumpe = break
        if (!pilaControlFlujo.isEmpty()) {
            gen.emitirSalto(pilaControlFlujo.peek()[1]);
        }
        return null;
    }

    @Override
    public Lugar visitReddere(ReddereContext ctx) {
        // En el .pig, reddere termina el programa (return del main de C)
        Lugar valor = (ctx.expresion() != null) ? valor(ctx.expresion()) : new LiteralLugar("0");
        gen.emitirRetorno(valor);
        return null;
    }

    @Override
    public Lugar visitImprimir(ImprimirContext ctx) {
        for (ExpresionContext expresion : ctx.expresion()) {
            Lugar valor = valor(expresion);
            gen.emitirImprimir(valor, RuntimeC.formatoImpresion(tipo(expresion)), false); // Pig Latin nunca agrega \n solo
        }
        return null;
    }

    @Override
    public Lugar visitLeerEnVariable(LeerEnVariableContext ctx) {
        Lugar destino = valorObjetivo(ctx.objetivo());
        gen.emitirLeer(destino, tipo(ctx.objetivo()));
        return null;
    }

    @Override
    public Lugar visitLeerDescartado(LeerDescartadoContext ctx) {
        Lugar temp = gen.nuevoTemporal(Tipo.cadena());
        gen.emitirLeer(temp, Tipo.cadena());
        return null;
    }

    // =====================================================================
    // objetivo: id.campo[i].metodo() -- lista plana de sufijos
    // =====================================================================

    @Override
    public Lugar visitObjetivo(ObjetivoContext ctx) {
        String nombre = ctx.ID().getText();
        String local = ambitos.resolver(nombre);
        Lugar actual = new NombreLugar(local != null ? local : ModeloPrograma.nombreSeguroC(nombre));
        Tipo tipoActual = (local != null) ? ambitos.tipoDe(nombre) : Tipo.error();

        for (SufijoAccesoContext sufijo : ctx.sufijoAcceso()) {
            if (sufijo instanceof SufijoAtributoContext) {
                String nombreCampo = ((SufijoAtributoContext) sufijo).ID().getText();
                ModeloPrograma.Variable campo = campoDe(tipoActual, nombreCampo);
                if (campo != null) {
                    actual = new CampoLugar(actual, campo.getNombreC());
                    tipoActual = campo.getTipo();
                } else {
                    actual = new CampoLugar(actual, nombreCampo);
                    tipoActual = Tipo.error();
                }
            } else if (sufijo instanceof SufijoIndiceContext) {
                Lugar indice = valor(((SufijoIndiceContext) sufijo).expresion());
                actual = new IndiceLugar(actual, indice);
                tipoActual = tipoActual.tipoElemento();
            } else if (sufijo instanceof SufijoMetodoContext) {
                SufijoMetodoContext metodoCtx = (SufijoMetodoContext) sufijo;
                List<Lugar> argumentos = evaluarArgumentos(metodoCtx.listaArgumentos());
                if (!tipoActual.esEstructura() || tipoActual.esArreglo()) {
                    error("Solo se puede llamar un metodo sobre un objeto", metodoCtx);
                    return new LiteralLugar("0");
                }
                String clase = tipoActual.getNombreEstructura();
                String nombreMetodo = metodoCtx.ID().getText();
                ModeloPrograma.Funcion metodo = modelo.buscarMetodo(clase, nombreMetodo, argumentos.size());
                if (metodo == null) {
                    error("No existe el metodo '" + nombreMetodo + "' con " + argumentos.size()
                            + " argumento(s) en la clase '" + clase + "'", metodoCtx);
                    return new LiteralLugar("0");
                }
                Lugar destino = metodo.getRetorno().esVacio() ? null : gen.nuevoTemporal(metodo.getRetorno());
                gen.emitirLlamada(destino, actual, nombreMetodo, metodo.getNombreC(), argumentos);
                actual = destino;
                tipoActual = metodo.getRetorno();
            }
        }
        calcularTipo(ctx, tipoActual);
        return actual;
    }

    /** Campo "nombreCampo" de la estructura/clase de tipo "tipo", o null. */
    private ModeloPrograma.Variable campoDe(Tipo tipo, String nombreCampo) {
        if (tipo == null || !tipo.esEstructura() || tipo.esArreglo()) {
            return null;
        }
        ModeloPrograma.Estructura estructura = modelo.buscarEstructura(tipo.getNombreEstructura());
        return (estructura != null) ? estructura.buscarCampo(nombreCampo) : null;
    }

    /** Si el objetivo termina en ".campo", ese campo (para saber si es un arreglo de tamanio fijo). */
    private ModeloPrograma.Variable campoFinal(ObjetivoContext ctx) {
        List<SufijoAccesoContext> sufijos = ctx.sufijoAcceso();
        if (sufijos.isEmpty() || !(sufijos.get(sufijos.size() - 1) instanceof SufijoAtributoContext)) {
            return null;
        }
        String nombre = ctx.ID().getText();
        Tipo tipo = ambitos.tipoDe(nombre);
        ModeloPrograma.Variable campo = null;
        for (SufijoAccesoContext sufijo : sufijos) {
            if (tipo == null) {
                return null;
            }
            if (sufijo instanceof SufijoAtributoContext) {
                campo = campoDe(tipo, ((SufijoAtributoContext) sufijo).ID().getText());
                tipo = (campo != null) ? campo.getTipo() : null;
            } else if (sufijo instanceof SufijoIndiceContext) {
                tipo = tipo.tipoElemento();
                campo = null;
            } else {
                return null;
            }
        }
        return campo;
    }

    /** Un objetivo usado como valor o destino (nunca null). */
    private Lugar valorObjetivo(ObjetivoContext ctx) {
        Lugar lugar = visit(ctx);
        return (lugar != null) ? lugar : new LiteralLugar("0");
    }

    private List<Lugar> evaluarArgumentos(ListaArgumentosContext ctx) {
        List<Lugar> lugares = new ArrayList<>();
        if (ctx != null) {
            for (ExpresionContext argumento : ctx.expresion()) {
                lugares.add(valor(argumento));
            }
        }
        return lugares;
    }

    // =====================================================================
    // Expresiones
    // =====================================================================

    @Override
    public Lugar visitExprAcceso(ExprAccesoContext ctx) {
        Lugar lugar = visit(ctx.objetivo());
        calcularTipo(ctx, tipo(ctx.objetivo()));
        return lugar;
    }

    @Override
    public Lugar visitExprLiteral(ExprLiteralContext ctx) {
        return visit(ctx.literal());
    }

    @Override
    public Lugar visitLitEntero(LitEnteroContext ctx) {
        return new LiteralLugar(ctx.getText());
    }

    @Override
    public Lugar visitLitDecimal(LitDecimalContext ctx) {
        return new LiteralLugar(ctx.getText());
    }

    @Override
    public Lugar visitLitCadena(LitCadenaContext ctx) {
        return new LiteralLugar(ctx.getText());
    }

    @Override
    public Lugar visitLitCaracter(LitCaracterContext ctx) {
        return new LiteralLugar(ctx.getText());
    }

    @Override
    public Lugar visitLitVerum(LitVerumContext ctx) {
        return new LiteralLugar("1");
    }

    @Override
    public Lugar visitLitFalsus(LitFalsusContext ctx) {
        return new LiteralLugar("0");
    }

    @Override
    public Lugar visitExprAgrupada(ExprAgrupadaContext ctx) {
        Lugar lugar = valor(ctx.expresion());
        calcularTipo(ctx, tipo(ctx.expresion()));
        return lugar;
    }

    @Override
    public Lugar visitExprUnaria(ExprUnariaContext ctx) {
        Lugar operando = valor(ctx.expresion());
        boolean negacion = ctx.NON() != null;
        calcularTipo(ctx, TablaTipos.resultadoUnario(
                negacion ? Operador.NEGACION_LOGICA : Operador.MENOS_UNARIO, tipo(ctx.expresion())));
        Lugar temp = gen.nuevoTemporal(tipo(ctx));
        gen.emitirOperacionUnaria(temp, negacion ? "!" : "-", operando);
        return temp;
    }

    @Override
    public Lugar visitExprMulDiv(ExprMulDivContext ctx) {
        return (ctx.POR() != null)
                ? emitirBinaria(ctx, "*", Operador.MULTIPLICACION)
                : emitirBinaria(ctx, "/", Operador.DIVISION);
    }

    @Override
    public Lugar visitExprSumaResta(ExprSumaRestaContext ctx) {
        return (ctx.MAS() != null)
                ? emitirBinaria(ctx, "+", Operador.SUMA)
                : emitirBinaria(ctx, "-", Operador.RESTA);
    }

    @Override
    public Lugar visitExprRelacional(ExprRelacionalContext ctx) {
        if (ctx.MENOR() != null) return emitirBinaria(ctx, "<", Operador.MENOR);
        if (ctx.MAYOR() != null) return emitirBinaria(ctx, ">", Operador.MAYOR);
        if (ctx.MENORIGUAL() != null) return emitirBinaria(ctx, "<=", Operador.MENOR_IGUAL);
        return emitirBinaria(ctx, ">=", Operador.MAYOR_IGUAL);
    }

    @Override
    public Lugar visitExprIgualdad(ExprIgualdadContext ctx) {
        return (ctx.IGUALIGUAL() != null)
                ? emitirBinaria(ctx, "==", Operador.IGUAL)
                : emitirBinaria(ctx, "!=", Operador.DIFERENTE);
    }

    @Override
    public Lugar visitExprAnd(ExprAndContext ctx) {
        return emitirBinaria(ctx, "&&", Operador.AND);
    }

    @Override
    public Lugar visitExprOr(ExprOrContext ctx) {
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
    public Lugar visitExprNuevoObjeto(ExprNuevoObjetoContext ctx) {
        String clase = ctx.ID().getText();
        List<Lugar> argumentos = evaluarArgumentos(ctx.listaArgumentos());
        ModeloPrograma.Funcion constructor = modelo.buscarConstructor(clase, argumentos.size());
        if (constructor == null) {
            error("No existe un constructor de '" + clase + "' con " + argumentos.size() + " argumento(s)", ctx);
            return new LiteralLugar("NULL");
        }
        Lugar temp = gen.nuevoTemporal(Tipo.estructura(clase));
        gen.emitirLlamada(temp, null, "new " + clase, constructor.getNombreC(), argumentos);
        return temp;
    }

    @Override
    public Lugar visitExprLlamada(ExprLlamadaContext ctx) {
        return visit(ctx.llamadaFuncion());
    }

    @Override
    public Lugar visitLlamadaFuncion(LlamadaFuncionContext ctx) {
        String nombre = ctx.ID().getText();
        List<Lugar> argumentos = evaluarArgumentos(ctx.listaArgumentos());
        ModeloPrograma.Funcion funcion = modelo.buscarFuncionLibre(nombre, argumentos.size());
        if (funcion == null) {
            error("No existe la funcion '" + nombre + "' con " + argumentos.size() + " argumento(s) (revisa el import)", ctx);
            return new LiteralLugar("0");
        }
        Lugar destino = funcion.getRetorno().esVacio() ? null : gen.nuevoTemporal(funcion.getRetorno());
        gen.emitirLlamada(destino, null, nombre, funcion.getNombreC(), argumentos);
        return destino;
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

    /**
     * El semantico de Pig Latin busca metodos y campos por nombre en una
     * tabla plana (sin saber de que clase son); aqui ya se conoce la
     * clase exacta, asi que el tipo calculado tiene prioridad.
     */
    private void calcularTipo(ParseTree ctx, Tipo calculado) {
        if (calculado != null && !calculado.esError()) {
            tiposCalculados.put(ctx, calculado);
        }
    }

    private Tipo resolverTipo(TipoContext ctx) {
        if (ctx instanceof TipoNumerusContext) return Tipo.entero();
        if (ctx instanceof TipoDecimalisContext) return Tipo.decimal();
        if (ctx instanceof TipoTextumContext) return Tipo.cadena();
        if (ctx instanceof TipoLitteraContext) return Tipo.caracter();
        if (ctx instanceof TipoBoolContext) return Tipo.booleano();
        return Tipo.estructura(ctx.getText()); // TipoEstructura: nombre de una clase/estructura importada
    }

    private void error(String mensaje, ParserRuleContext ctx) {
        errores.agregar(TipoError.SEMANTICO, mensaje, ctx.getStart().getLine(),
                ctx.getStart().getCharPositionInLine(), nombreArchivo);
    }
}
