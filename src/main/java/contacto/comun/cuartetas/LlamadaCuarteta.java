package contacto.comun.cuartetas;

import java.util.List;

/**
 * destino = call objetivo.nombre(arg1, arg2, ...)
 * "objetivo" es null para una llamada a funcion libre (Y?) o a un
 * constructor (new Clase(...)); "destino" es null cuando no interesa
 * el valor de retorno (una llamada usada como sentencia suelta).
 */
public class LlamadaCuarteta extends Cuarteta {

    private final String destino;
    private final String objetivo;
    private final String nombre;
    private final List<String> argumentos;

    public LlamadaCuarteta(int numero, String destino, String objetivo, String nombre, List<String> argumentos) {
        super(numero);
        this.destino = destino;
        this.objetivo = objetivo;
        this.nombre = nombre;
        this.argumentos = argumentos;
    }

    public String getDestino() { return destino; }
    public String getObjetivo() { return objetivo; }
    public String getNombre() { return nombre; }
    public List<String> getArgumentos() { return argumentos; }

    @Override
    public String toString() {
        String llamada = (objetivo != null ? objetivo + "." : "") + nombre
                + "(" + String.join(", ", argumentos) + ")";
        return getNumero() + ": " + (destino != null ? destino + " = call " : "call ") + llamada;
    }
}
