package contacto.comun.codegen;

import contacto.comun.cuartetas.GeneradorCuartetas;
import contacto.comun.tipos.Tipo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Todo lo que el generador de C necesita saber del programa COMPLETO
 * (el .pig y cada .z/.y que importa): que estructuras/clases existen con
 * sus campos, y que funciones hay (funciones libres de Y?, metodos y
 * constructores de Zetariano, y el main del .pig) con su firma y el
 * rango de cuartetas que forma su cuerpo.
 *
 * Se llena en dos fases (ver OrquestadorPig):
 *   1. registrarFirmas() de cada generador: estructuras, campos y
 *      firmas de funciones, de TODOS los archivos, antes de generar
 *      ningun cuerpo. Asi Pila.z puede llamar a Nodo.getDato() sin
 *      importar el orden de los import.
 *   2. asignarNombresC() y luego visit() de cada generador: genera las
 *      cuartetas de cada cuerpo y marca su rango en la Funcion.
 */
public final class ModeloPrograma {

    /** Un campo, parametro o variable, con el nombre que tendra en C. */
    public static final class Variable {
        private final String nombre;
        private final String nombreC;
        private final Tipo tipo;
        private final int tamanioFijo; // > 0 solo para campos "tipo x[N]" de estructuras de Y?

        public Variable(String nombre, Tipo tipo) {
            this(nombre, tipo, 0);
        }

        public Variable(String nombre, Tipo tipo, int tamanioFijo) {
            this.nombre = nombre;
            this.nombreC = nombreSeguroC(nombre);
            this.tipo = tipo;
            this.tamanioFijo = tamanioFijo;
        }

        public String getNombre() { return nombre; }
        public String getNombreC() { return nombreC; }
        public Tipo getTipo() { return tipo; }
        public int getTamanioFijo() { return tamanioFijo; }
    }

    /** Una clase de Zetariano o una estructura de Y?: en C, un struct que vive en heap. */
    public static final class Estructura {
        private final String nombre;
        private final boolean esClase;
        private final List<Variable> campos = new ArrayList<>();

        Estructura(String nombre, boolean esClase) {
            this.nombre = nombre;
            this.esClase = esClase;
        }

        public String getNombre() { return nombre; }
        public boolean esClase() { return esClase; }
        public List<Variable> getCampos() { return campos; }

        public void agregarCampo(Variable campo) {
            campos.add(campo);
        }

        public Variable buscarCampo(String nombreCampo) {
            for (Variable campo : campos) {
                if (campo.getNombre().equals(nombreCampo)) {
                    return campo;
                }
            }
            return null;
        }
    }

    /**
     * Una funcion de C: funcion libre de Y?, metodo o constructor de una
     * clase, o el main del .pig. Los metodos reciben el objeto como
     * primer parametro ("this"); los constructores lo reservan ellos
     * mismos y lo devuelven.
     */
    public static final class Funcion {
        private final String nombreFuente;
        private final String clase;          // null si es funcion libre o main
        private final boolean esConstructor;
        private final boolean esMain;
        private final Tipo retorno;
        private final List<Variable> parametros;
        private final Object nodo;           // XxxContext de ANTLR con el cuerpo (o la clase, si es constructor implicito)
        private String nombreC;

        private GeneradorCuartetas generador;
        private int cuartetaDesde;
        private int cuartetaHasta;
        private int temporalDesde;
        private int temporalHasta;
        private final Map<String, Tipo> locales = new LinkedHashMap<>(); // nombre en C -> tipo

        public Funcion(String nombreFuente, String clase, boolean esConstructor, boolean esMain,
                       Tipo retorno, List<Variable> parametros, Object nodo) {
            this.nombreFuente = nombreFuente;
            this.clase = clase;
            this.esConstructor = esConstructor;
            this.esMain = esMain;
            this.retorno = retorno;
            this.parametros = parametros;
            this.nodo = nodo;
        }

        public String getNombreFuente() { return nombreFuente; }
        public String getClase() { return clase; }
        public boolean esConstructor() { return esConstructor; }
        public boolean esMain() { return esMain; }
        public boolean esMetodo() { return clase != null && !esConstructor; }
        public Tipo getRetorno() { return retorno; }
        public List<Variable> getParametros() { return parametros; }
        public int getAridad() { return parametros.size(); }
        public Object getNodo() { return nodo; }
        public String getNombreC() { return nombreC; }
        public Map<String, Tipo> getLocales() { return locales; }
        public GeneradorCuartetas getGenerador() { return generador; }
        public int getCuartetaDesde() { return cuartetaDesde; }
        public int getCuartetaHasta() { return cuartetaHasta; }
        public int getTemporalDesde() { return temporalDesde; }
        public int getTemporalHasta() { return temporalHasta; }

        /** "Pila.apilar", "imprimirBienvenida", ... para mensajes de error. */
        public String getNombreCompleto() {
            return (clase != null) ? clase + "." + nombreFuente : nombreFuente;
        }

        public boolean tieneCuerpo() {
            return generador != null;
        }

        /** Marca el inicio del cuerpo: las cuartetas y temporales que se emitan desde aqui son de esta funcion. */
        public void iniciarCuerpo(GeneradorCuartetas gen) {
            this.generador = gen;
            this.cuartetaDesde = gen.getCuartetas().size();
            this.temporalDesde = gen.getCantidadTemporales();
        }

        public void terminarCuerpo() {
            this.cuartetaHasta = generador.getCuartetas().size();
            this.temporalHasta = generador.getCantidadTemporales();
        }
    }

    private final Map<String, Estructura> estructuras = new LinkedHashMap<>();
    private final List<Funcion> funciones = new ArrayList<>();
    private Funcion main;

    public Estructura registrarEstructura(String nombre, boolean esClase) {
        return estructuras.computeIfAbsent(nombre, n -> new Estructura(n, esClase));
    }

    public Estructura buscarEstructura(String nombre) {
        return estructuras.get(nombre);
    }

    public Iterable<Estructura> getEstructuras() {
        return estructuras.values();
    }

    public void agregarFuncion(Funcion funcion) {
        if (funcion.esMain()) {
            main = funcion;
        } else {
            funciones.add(funcion);
        }
    }

    public List<Funcion> getFunciones() {
        return funciones;
    }

    public Funcion getMain() {
        return main;
    }

    public Funcion buscarMetodo(String clase, String nombre, int aridad) {
        for (Funcion f : funciones) {
            if (f.esMetodo() && f.getClase().equals(clase)
                    && f.getNombreFuente().equals(nombre) && f.getAridad() == aridad) {
                return f;
            }
        }
        return null;
    }

    public Funcion buscarConstructor(String clase, int aridad) {
        for (Funcion f : funciones) {
            if (f.esConstructor() && f.getClase().equals(clase) && f.getAridad() == aridad) {
                return f;
            }
        }
        return null;
    }

    public Funcion buscarFuncionLibre(String nombre, int aridad) {
        for (Funcion f : funciones) {
            if (f.getClase() == null && f.getNombreFuente().equals(nombre) && f.getAridad() == aridad) {
                return f;
            }
        }
        return null;
    }

    /** Tipo del campo "campo" de la estructura/clase "estructura", o null si no existe. */
    public Tipo tipoCampo(String estructura, String campo) {
        Estructura e = estructuras.get(estructura);
        if (e == null) {
            return null;
        }
        Variable v = e.buscarCampo(campo);
        return (v != null) ? v.getTipo() : null;
    }

    /**
     * Decide el nombre en C de cada funcion, una vez registradas TODAS
     * las firmas: Clase_metodo, new_Clase o nombreFuncion; si hay
     * sobrecarga (mismo nombre, distinta aridad) se agrega "_aridad"
     * para que no choquen en C, que no tiene sobrecarga.
     */
    public void asignarNombresC() {
        Map<String, Integer> repeticiones = new HashMap<>();
        for (Funcion f : funciones) {
            repeticiones.merge(nombreBaseC(f), 1, Integer::sum);
        }
        for (Funcion f : funciones) {
            String base = nombreBaseC(f);
            f.nombreC = (repeticiones.get(base) > 1) ? base + "_" + f.getAridad() : base;
        }
        if (main != null) {
            main.nombreC = "main";
        }
    }

    private static String nombreBaseC(Funcion f) {
        if (f.esConstructor()) {
            return "new_" + f.getClase();
        }
        if (f.esMetodo()) {
            return f.getClase() + "_" + f.getNombreFuente();
        }
        return nombreSeguroC(f.getNombreFuente());
    }

    // Palabras reservadas de C (y nombres que usa el C generado) que son
    // identificadores validos en alguno de los tres lenguajes fuente.
    private static final Set<String> RESERVADAS_C = Set.of(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
            "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
            "switch", "typedef", "union", "unsigned", "void", "volatile", "while", "main",
            "printf", "scanf", "malloc", "calloc", "free", "exit", "strlen", "strcmp", "NULL");

    /**
     * Nombre valido en C para un identificador del lenguaje fuente: si
     * choca con una palabra reservada de C, con los temporales (t0, t1..)
     * o con el runtime (zc_...), se le agrega "_v".
     */
    public static String nombreSeguroC(String nombre) {
        if (RESERVADAS_C.contains(nombre) || nombre.matches("t[0-9]+") || nombre.startsWith("zc_")) {
            return nombre + "_v";
        }
        return nombre;
    }
}
