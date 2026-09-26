package contacto.comun.cuartetas;

import contacto.comun.cuartetas.acceso.Lugar;
import contacto.comun.cuartetas.acceso.NombreLugar;
import contacto.comun.tipos.Tipo;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper compartido por el generador de cuartetas de cualquiera de los
 * 3 lenguajes: lleva los contadores de temporales, etiquetas y numero
 * de cuarteta, y ofrece un metodo emitirXxx por cada tipo de cuarteta.
 *
 * Cada temporal guarda su tipo: el generador de C lo declara con su
 * tipo real (int, double, char*, Nodo*...), no todos como double.
 */
public class GeneradorCuartetas {

    private final List<Cuarteta> cuartetas = new ArrayList<>();
    private final List<Tipo> tiposTemporales = new ArrayList<>();
    private int contadorCuartetas = 0;
    private int contadorEtiquetas = 0;

    /** Nuevo temporal del tipo dado, listo para usar como destino/operando. */
    public NombreLugar nuevoTemporal(Tipo tipo) {
        tiposTemporales.add(tipo != null ? tipo : Tipo.error());
        return new NombreLugar(nombreTemporal(tiposTemporales.size() - 1));
    }

    public static String nombreTemporal(int indice) {
        return "t" + indice;
    }

    public String nuevaEtiqueta() {
        return "L" + (contadorEtiquetas++);
    }

    public void emitirAsignacion(Lugar destino, Lugar origen) {
        cuartetas.add(new AsignacionCuarteta(contadorCuartetas++, destino, origen));
    }

    public void emitirOperacionBinaria(Lugar destino, Lugar izquierdo, String operador, Lugar derecho) {
        emitirOperacionBinaria(destino, izquierdo, operador, derecho, null, null);
    }

    /**
     * Con los tipos de los operandos: el C de "+" entre cadenas es una
     * concatenacion y el de "=="/"!=" entre cadenas compara contenido.
     */
    public void emitirOperacionBinaria(Lugar destino, Lugar izquierdo, String operador, Lugar derecho,
                                       Tipo tipoIzquierdo, Tipo tipoDerecho) {
        cuartetas.add(new OperacionCuarteta(contadorCuartetas++, destino, izquierdo, operador, derecho,
                tipoIzquierdo, tipoDerecho));
    }

    public void emitirOperacionUnaria(Lugar destino, String operador, Lugar operando) {
        cuartetas.add(new OperacionCuarteta(contadorCuartetas++, destino, operando, operador, null, null, null));
    }

    public void emitirEtiqueta(String nombre) {
        cuartetas.add(new EtiquetaCuarteta(contadorCuartetas++, nombre));
    }

    public void emitirSalto(String etiquetaDestino) {
        cuartetas.add(new SaltoCuarteta(contadorCuartetas++, etiquetaDestino));
    }

    public void emitirSaltoSiFalso(Lugar condicion, String etiquetaDestino) {
        cuartetas.add(new SaltoCondicionalCuarteta(contadorCuartetas++, condicion, true, etiquetaDestino));
    }

    public void emitirSaltoSiVerdadero(Lugar condicion, String etiquetaDestino) {
        cuartetas.add(new SaltoCondicionalCuarteta(contadorCuartetas++, condicion, false, etiquetaDestino));
    }

    public void emitirImprimir(Lugar valor, String formatoC, boolean saltoLinea) {
        cuartetas.add(new ImprimirCuarteta(contadorCuartetas++, valor, formatoC, saltoLinea));
    }

    /** Lee una linea de la entrada y la convierte al tipo de "destino". */
    public void emitirLeer(Lugar destino, Tipo tipo) {
        cuartetas.add(new LeerCuarteta(contadorCuartetas++, destino, tipo));
    }

    /**
     * "nombre" es el del lenguaje fuente (lo que se ve en la cuarteta:
     * pila.apilar(x)); "nombreC" el de la funcion en el C generado
     * (Pila_apilar(pila, x)). destino == null para llamadas void.
     */
    public void emitirLlamada(Lugar destino, Lugar objetivo, String nombre, String nombreC, List<Lugar> argumentos) {
        cuartetas.add(new LlamadaCuarteta(contadorCuartetas++, destino, objetivo, nombre, nombreC, argumentos));
    }

    public void emitirRetorno(Lugar valor) {
        cuartetas.add(new RetornoCuarteta(contadorCuartetas++, valor));
    }

    public List<Cuarteta> getCuartetas() {
        return cuartetas;
    }

    public int getCantidadTemporales() {
        return tiposTemporales.size();
    }

    public Tipo getTipoTemporal(int indice) {
        return tiposTemporales.get(indice);
    }
}
