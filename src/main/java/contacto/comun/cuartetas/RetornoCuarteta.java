package contacto.comun.cuartetas;

/** return valor   (valor es null para un retorno sin valor). */
public class RetornoCuarteta extends Cuarteta {

    private final String valor;

    public RetornoCuarteta(int numero, String valor) {
        super(numero);
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return getNumero() + ": return " + (valor != null ? valor : "");
    }
}
