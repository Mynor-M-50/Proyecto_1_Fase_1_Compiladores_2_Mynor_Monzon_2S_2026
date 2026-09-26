package contacto.comun.codegen;

import contacto.comun.tipos.Tipo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Ambitos de variables locales mientras se genera el cuerpo de UNA
 * funcion de C. En C todas las locales se declaran juntas al inicio
 * de la funcion (asi los goto de las cuartetas nunca saltan una
 * declaracion), asi que si dos bloques hermanos declaran el mismo
 * nombre con tipos distintos, al segundo se le da otro nombre en C
 * (x, x_2, ...). Tambien sirve para saber si un identificador es una
 * local/parametro o un campo de la clase (que en C es this->campo).
 */
public final class AmbitosGeneracion {

    private final ModeloPrograma.Funcion funcion;
    private final Deque<Map<String, String>> pila = new ArrayDeque<>(); // nombre fuente -> nombre en C

    public AmbitosGeneracion(ModeloPrograma.Funcion funcion) {
        this.funcion = funcion;
        entrar();
        for (ModeloPrograma.Variable parametro : funcion.getParametros()) {
            pila.peek().put(parametro.getNombre(), parametro.getNombreC());
        }
    }

    public void entrar() {
        pila.push(new HashMap<>());
    }

    public void salir() {
        if (pila.size() > 1) {
            pila.pop();
        }
    }

    /** Declara una variable local en el ambito actual; devuelve su nombre en C. */
    public String declarar(String nombre, Tipo tipo) {
        String base = ModeloPrograma.nombreSeguroC(nombre);
        String nombreC = base;
        int sufijo = 2;
        while (ocupado(nombreC, tipo)) {
            nombreC = base + "_" + sufijo++;
        }
        funcion.getLocales().putIfAbsent(nombreC, tipo);
        pila.peek().put(nombre, nombreC);
        return nombreC;
    }

    private boolean ocupado(String nombreC, Tipo tipo) {
        for (ModeloPrograma.Variable parametro : funcion.getParametros()) {
            if (parametro.getNombreC().equals(nombreC)) {
                return true;
            }
        }
        if (funcion.esMetodo() || funcion.esConstructor()) {
            if (nombreC.equals("this")) {
                return true;
            }
        }
        Tipo existente = funcion.getLocales().get(nombreC);
        return existente != null && !existente.equals(tipo);
    }

    /** Nombre en C de una local/parametro visible, o null si no es ninguno (campo, global...). */
    public String resolver(String nombre) {
        for (Map<String, String> ambito : pila) {
            String nombreC = ambito.get(nombre);
            if (nombreC != null) {
                return nombreC;
            }
        }
        return null;
    }

    /** Tipo de una local/parametro visible, o null. */
    public Tipo tipoDe(String nombre) {
        String nombreC = resolver(nombre);
        if (nombreC == null) {
            return null;
        }
        for (ModeloPrograma.Variable parametro : funcion.getParametros()) {
            if (parametro.getNombreC().equals(nombreC)) {
                return parametro.getTipo();
            }
        }
        return funcion.getLocales().get(nombreC);
    }
}
