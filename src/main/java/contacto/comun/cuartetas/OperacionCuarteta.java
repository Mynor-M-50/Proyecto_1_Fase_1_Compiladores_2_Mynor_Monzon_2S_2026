package contacto.comun.cuartetas;

import contacto.comun.codegen.RuntimeC;
import contacto.comun.cuartetas.acceso.Lugar;
import contacto.comun.tipos.Tipo;
import contacto.comun.tipos.TipoPrimitivo;

/**
 * destino = izquierdo OPERADOR derecho   (binaria)
 * destino = OPERADOR izquierdo           (unaria, derecho == null)
 *
 * Si se conocen los tipos de los operandos, dos casos cambian en C:
 *  - "+" con alguna cadena es concatenacion: zc_concatenar(a, b),
 *    convirtiendo antes a cadena el lado que no lo sea.
 *  - "==" / "!=" entre dos cadenas compara el contenido (strcmp), no
 *    la direccion de memoria.
 */
public class OperacionCuarteta extends Cuarteta {

    private final Lugar destino;
    private final Lugar izquierdo;
    private final String operador;
    private final Lugar derecho;
    private final Tipo tipoIzquierdo;
    private final Tipo tipoDerecho;

    public OperacionCuarteta(int numero, Lugar destino, Lugar izquierdo, String operador, Lugar derecho) {
        this(numero, destino, izquierdo, operador, derecho, null, null);
    }

    public OperacionCuarteta(int numero, Lugar destino, Lugar izquierdo, String operador, Lugar derecho,
                             Tipo tipoIzquierdo, Tipo tipoDerecho) {
        super(numero);
        this.destino = destino;
        this.izquierdo = izquierdo;
        this.operador = operador;
        this.derecho = derecho;
        this.tipoIzquierdo = tipoIzquierdo;
        this.tipoDerecho = tipoDerecho;
    }

    public Lugar getDestino() {
        return destino;
    }

    public Lugar getIzquierdo() {
        return izquierdo;
    }

    public String getOperador() {
        return operador;
    }

    public Lugar getDerecho() {
        return derecho;
    }

    public boolean esUnaria() {
        return derecho == null;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        destino.generarC(codigo);
        codigo.append(" = ");
        if (esUnaria()) {
            codigo.append(operador);
            izquierdo.generarC(codigo);
        } else if (operador.equals("+") && (esCadena(tipoIzquierdo) || esCadena(tipoDerecho))) {
            codigo.append("zc_concatenar(");
            generarComoCadena(codigo, izquierdo, tipoIzquierdo);
            codigo.append(", ");
            generarComoCadena(codigo, derecho, tipoDerecho);
            codigo.append(')');
        } else if ((operador.equals("==") || operador.equals("!=")) && esCadena(tipoIzquierdo) && esCadena(tipoDerecho)) {
            if (operador.equals("!=")) {
                codigo.append('!');
            }
            codigo.append("zc_cadenas_iguales(");
            izquierdo.generarC(codigo);
            codigo.append(", ");
            derecho.generarC(codigo);
            codigo.append(')');
        } else {
            izquierdo.generarC(codigo);
            codigo.append(' ').append(operador).append(' ');
            derecho.generarC(codigo);
        }
        codigo.append(";\n");
    }

    private static boolean esCadena(Tipo tipo) {
        return tipo != null && !tipo.esArreglo() && tipo.getPrimitivo() == TipoPrimitivo.CADENA;
    }

    private static void generarComoCadena(StringBuilder codigo, Lugar valor, Tipo tipo) {
        String conversion = RuntimeC.conversionACadena(tipo);
        if (conversion == null) {
            valor.generarC(codigo);
            return;
        }
        codigo.append(conversion).append('(');
        valor.generarC(codigo);
        codigo.append(')');
    }

    @Override
    public String toString() {
        if (esUnaria()) {
            return getNumero() + ": " + destino + " = " + operador + izquierdo;
        }
        return getNumero() + ": " + destino + " = " + izquierdo + " " + operador + " " + derecho;
    }
}
