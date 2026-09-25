package contacto.comun.cuartetas;

/**
 * destino = izquierdo OPERADOR derecho   (binaria)
 * destino = OPERADOR izquierdo           (unaria, derecho == null)
 */
public class OperacionCuarteta extends Cuarteta {

    private final String destino;
    private final String izquierdo;
    private final String operador;
    private final String derecho;

    public OperacionCuarteta(int numero, String destino, String izquierdo, String operador, String derecho) {
        super(numero);
        this.destino = destino;
        this.izquierdo = izquierdo;
        this.operador = operador;
        this.derecho = derecho;
    }

    public String getDestino() {
        return destino;
    }

    public String getIzquierdo() {
        return izquierdo;
    }

    public String getOperador() {
        return operador;
    }

    public String getDerecho() {
        return derecho;
    }

    public boolean esUnaria() {
        return derecho == null;
    }

    @Override
    public String toString() {
        if (esUnaria()) {
            return getNumero() + ": " + destino + " = " + operador + izquierdo;
        }
        return getNumero() + ": " + destino + " = " + izquierdo + " " + operador + " " + derecho;
    }
}
