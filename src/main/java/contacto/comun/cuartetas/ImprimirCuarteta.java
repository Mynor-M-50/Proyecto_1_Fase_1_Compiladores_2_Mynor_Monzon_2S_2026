package contacto.comun.cuartetas;

import contacto.comun.codegen.RuntimeC;
import contacto.comun.cuartetas.acceso.Lugar;

/** printf(formatoC, valor);  (+ un "\n" si saltoLinea). */
public class ImprimirCuarteta extends Cuarteta {

    private final Lugar valor;
    private final String formatoC; // "%d", "%f", "%c", "%s"...
    private final boolean saltoLinea;

    public ImprimirCuarteta(int numero, Lugar valor, String formatoC, boolean saltoLinea) {
        super(numero);
        this.valor = valor;
        this.formatoC = formatoC;
        this.saltoLinea = saltoLinea;
    }

    public Lugar getValor() {
        return valor;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        boolean esDecimal = RuntimeC.FORMATO_DECIMAL.equals(formatoC);
        codigo.append("printf(\"").append(esDecimal ? "%s" : formatoC);
        if (saltoLinea) {
            codigo.append("\\n");
        }
        codigo.append("\", ");
        if (esDecimal) {
            // 2.5 y no 2.500000, igual que en el lenguaje fuente
            codigo.append("zc_decimal_a_cadena(");
            valor.generarC(codigo);
            codigo.append(')');
        } else {
            valor.generarC(codigo);
        }
        codigo.append(");\n");
    }

    @Override
    public String toString() {
        return getNumero() + ": print " + valor + (saltoLinea ? " (con salto de linea)" : "");
    }
}
