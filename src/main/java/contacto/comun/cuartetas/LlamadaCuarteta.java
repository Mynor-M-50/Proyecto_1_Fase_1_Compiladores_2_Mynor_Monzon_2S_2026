package contacto.comun.cuartetas;

import contacto.comun.cuartetas.acceso.Lugar;

import java.util.List;

/**
 * [destino =] nombre(objetivo?, arg1, arg2, ...)
 *
 * "objetivo" es null para una llamada a funcion libre (Y?) o a un
 * constructor (new Clase(...)); "destino" es null cuando la funcion es
 * void o no interesa el valor de retorno.
 *
 * "nombre" es el del lenguaje fuente (se ve asi en la cuarteta:
 * pila.apilar(x)) y "nombreC" el de la funcion en el C generado, con el
 * nombre de la clase incluido (Pila_apilar(pila, x)) para que dos
 * clases con un metodo del mismo nombre no choquen en C. Ver
 * ModeloPrograma.asignarNombresC().
 */
public class LlamadaCuarteta extends Cuarteta {

    private final Lugar destino;
    private final Lugar objetivo;
    private final String nombre;
    private final String nombreC;
    private final List<Lugar> argumentos;

    public LlamadaCuarteta(int numero, Lugar destino, Lugar objetivo, String nombre, String nombreC,
                           List<Lugar> argumentos) {
        super(numero);
        this.destino = destino;
        this.objetivo = objetivo;
        this.nombre = nombre;
        this.nombreC = nombreC;
        this.argumentos = argumentos;
    }

    public Lugar getDestino() { return destino; }
    public Lugar getObjetivo() { return objetivo; }
    public String getNombre() { return nombre; }
    public String getNombreC() { return nombreC; }
    public List<Lugar> getArgumentos() { return argumentos; }

    @Override
    public void generarC(StringBuilder codigo) {
        if (destino != null) {
            destino.generarC(codigo);
            codigo.append(" = ");
        }
        codigo.append(nombreC).append('(');
        boolean primero = true;
        if (objetivo != null) {
            objetivo.generarC(codigo);
            primero = false;
        }
        for (Lugar argumento : argumentos) {
            if (!primero) {
                codigo.append(", ");
            }
            argumento.generarC(codigo);
            primero = false;
        }
        codigo.append(");\n");
    }

    @Override
    public String toString() {
        String llamada = (objetivo != null ? objetivo + "." : "") + nombre
                + "(" + argumentosTexto() + ")";
        return getNumero() + ": " + (destino != null ? destino + " = call " : "call ") + llamada;
    }

    private String argumentosTexto() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < argumentos.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(argumentos.get(i));
        }
        return sb.toString();
    }
}
