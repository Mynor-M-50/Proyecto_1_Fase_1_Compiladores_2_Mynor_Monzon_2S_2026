package contacto.comun.cuartetas;

/** print valor */
public class ImprimirCuarteta extends Cuarteta {

    private final String valor;

    public ImprimirCuarteta(int numero, String valor) {
        super(numero);
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return getNumero() + ": print " + valor;
    }
}
