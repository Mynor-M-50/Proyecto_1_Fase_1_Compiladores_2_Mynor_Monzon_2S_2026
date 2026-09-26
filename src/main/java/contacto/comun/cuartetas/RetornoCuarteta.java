package contacto.comun.cuartetas;

import contacto.comun.cuartetas.acceso.Lugar;

/** return valor;   (valor es null para un retorno sin valor). */
public class RetornoCuarteta extends Cuarteta {

    private final Lugar valor;

    public RetornoCuarteta(int numero, Lugar valor) {
        super(numero);
        this.valor = valor;
    }

    public Lugar getValor() {
        return valor;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        codigo.append("return");
        if (valor != null) {
            codigo.append(' ');
            valor.generarC(codigo);
        }
        codigo.append(";\n");
    }

    @Override
    public String toString() {
        return getNumero() + ": return " + (valor != null ? valor.toString() : "");
    }
}
