package contacto.comun.simbolos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Pila de ambitos (scopes). Un mismo objeto se comparte entre el
 * analizador semantico y el generador de cuartetas de un mismo
 * lenguaje: el generador reutiliza la resolucion de simbolos que ya
 * hizo semantico en vez de repetirla.
 *
 * Ojo: para reutilizarla asi hay que recorrer el arbol dos veces con
 * la MISMA tabla, pero volviendo a entrar/salir de los ambitos en el
 * mismo orden (semantico y generador comparten los mismos puntos de
 * entrarAmbito/salirAmbito). Si un dia hace falta, la alternativa es
 * que semantico guarde el Ambito resuelto de cada nodo en un
 * ParseTreeProperty en vez de que el generador la reconstruya.
 */
public class TablaSimbolos {

    private final Deque<Ambito> pila = new ArrayDeque<>();

    // Historial de TODO lo declarado alguna vez, sin importar si su
    // ambito ya se cerro. El generador de codigo C lo usa para saber
    // que variables declarar al inicio del programa (los ambitos en si
    // se pierden apenas se hace salirAmbito()).
    private final List<Simbolo> historial = new ArrayList<>();

    // Arranca con el ambito global ya abierto
    public TablaSimbolos() {
        pila.push(new Ambito("global", null));
    }

    // Abre un ambito hijo del actual
    public void entrarAmbito(String nombre) {
        pila.push(new Ambito(nombre, pila.peek()));
    }

    // Cierra el ambito actual; el global nunca se cierra
    public void salirAmbito() {
        if (pila.size() > 1) {
            pila.pop();
        }
    }

    public Ambito ambitoActual() {
        return pila.peek();
    }

    // Declara en el ambito actual; false si el nombre ya existia ahi (redeclaracion)
    public boolean declarar(Simbolo simbolo) {
        boolean exito = pila.peek().declarar(simbolo);
        if (exito) {
            historial.add(simbolo);
        }
        return exito;
    }

    public List<Simbolo> getHistorial() {
        return historial;
    }

    // Busca desde el ambito actual hacia afuera, hasta el global
    public Simbolo buscar(String nombre) {
        return pila.peek().buscar(nombre);
    }

    // Busca solo en el ambito actual
    public Simbolo buscarEnAmbitoActual(String nombre) {
        return pila.peek().buscarLocal(nombre);
    }

    /**
     * Busca un tipo definido (clase/estructura) por nombre. Estas se
     * declaran siempre en el ambito global, asi que buscamos ahi
     * directamente en vez de depender de en que ambito estemos parados.
     */
    public Simbolo buscarTipoDefinido(String nombre) {
        Ambito global = pila.peekLast();
        return (global != null) ? global.buscarLocal(nombre) : null;
    }
}

