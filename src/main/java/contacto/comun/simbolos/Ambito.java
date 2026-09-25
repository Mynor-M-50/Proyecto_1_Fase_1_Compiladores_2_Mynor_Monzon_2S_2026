package contacto.comun.simbolos;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un ambito (scope) individual: global, una clase, una funcion/metodo,
 * o el cuerpo de un si/mientras/para. Encadenado a su padre para poder
 * resolver nombres declarados afuera.
 */
public class Ambito {

    private final String nombre;
    private final Ambito padre;
    private final Map<String, Simbolo> simbolos = new LinkedHashMap<>();

    public Ambito(String nombre, Ambito padre) {
        this.nombre = nombre;
        this.padre = padre;
    }

    public String getNombre() {
        return nombre;
    }

    public Ambito getPadre() {
        return padre;
    }

    /** Devuelve false si ya existia un simbolo con ese nombre EN ESTE ambito. */
    public boolean declarar(Simbolo simbolo) {
        if (simbolos.containsKey(simbolo.getNombre())) {
            return false;
        }
        simbolos.put(simbolo.getNombre(), simbolo);
        return true;
    }

    public Simbolo buscarLocal(String nombre) {
        return simbolos.get(nombre);
    }

    /** Busca en este ambito y, si no esta, sube por los padres. */
    public Simbolo buscar(String nombre) {
        Simbolo simbolo = simbolos.get(nombre);
        if (simbolo != null) {
            return simbolo;
        }
        return (padre != null) ? padre.buscar(nombre) : null;
    }
}
