package contacto.comun.cuartetas;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper compartido por el generador de cuartetas de cualquiera de los
 * 3 lenguajes: lleva los contadores de temporales, etiquetas y numero
 * de cuarteta, y ofrece un metodo emitirXxx por cada tipo de cuarteta
 * para no repetir "new XxxCuarteta(contador++, ...)" en cada lenguaje.
 */
public class GeneradorCuartetas {

    private final List<Cuarteta> cuartetas = new ArrayList<>();
    private int contadorCuartetas = 0;
    private int contadorTemporales = 0;
    private int contadorEtiquetas = 0;

    public String nuevoTemporal() {
        return "t" + (contadorTemporales++);
    }

    public String nuevaEtiqueta() {
        return "L" + (contadorEtiquetas++);
    }

    public void emitirAsignacion(String destino, String origen) {
        cuartetas.add(new AsignacionCuarteta(contadorCuartetas++, destino, origen));
    }

    public void emitirOperacionBinaria(String destino, String izquierdo, String operador, String derecho) {
        cuartetas.add(new OperacionCuarteta(contadorCuartetas++, destino, izquierdo, operador, derecho));
    }

    public void emitirOperacionUnaria(String destino, String operador, String operando) {
        cuartetas.add(new OperacionCuarteta(contadorCuartetas++, destino, operando, operador, null));
    }

    public void emitirEtiqueta(String nombre) {
        cuartetas.add(new EtiquetaCuarteta(contadorCuartetas++, nombre));
    }

    public void emitirSalto(String etiquetaDestino) {
        cuartetas.add(new SaltoCuarteta(contadorCuartetas++, etiquetaDestino));
    }

    public void emitirSaltoSiFalso(String condicion, String etiquetaDestino) {
        cuartetas.add(new SaltoCondicionalCuarteta(contadorCuartetas++, condicion, true, etiquetaDestino));
    }

    public void emitirSaltoSiVerdadero(String condicion, String etiquetaDestino) {
        cuartetas.add(new SaltoCondicionalCuarteta(contadorCuartetas++, condicion, false, etiquetaDestino));
    }

    public void emitirImprimir(String valor) {
        cuartetas.add(new ImprimirCuarteta(contadorCuartetas++, valor));
    }

    public void emitirLeer(String destino) {
        cuartetas.add(new LeerCuarteta(contadorCuartetas++, destino));
    }

    public void emitirLlamada(String destino, String objetivo, String nombre, List<String> argumentos) {
        cuartetas.add(new LlamadaCuarteta(contadorCuartetas++, destino, objetivo, nombre, argumentos));
    }

    public void emitirRetorno(String valor) {
        cuartetas.add(new RetornoCuarteta(contadorCuartetas++, valor));
    }

    public List<Cuarteta> getCuartetas() {
        return cuartetas;
    }
}
