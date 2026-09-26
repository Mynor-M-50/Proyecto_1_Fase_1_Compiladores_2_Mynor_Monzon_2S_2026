package contacto.comun.cuartetas;

import contacto.comun.codegen.RuntimeC;
import contacto.comun.cuartetas.acceso.Lugar;
import contacto.comun.tipos.Tipo;

/**
 * destino = zc_leer_xxx();  -- lee una LINEA completa y la convierte al
 * tipo del destino. Con scanf("%d") una entrada que no es numero se
 * queda en el buffer y el siguiente scanf vuelve a fallar para siempre;
 * leyendo por linea eso no pasa (una entrada invalida vale 0).
 */
public class LeerCuarteta extends Cuarteta {

    private final Lugar destino;
    private final Tipo tipo;

    public LeerCuarteta(int numero, Lugar destino, Tipo tipo) {
        super(numero);
        this.destino = destino;
        this.tipo = tipo;
    }

    public Lugar getDestino() {
        return destino;
    }

    public Tipo getTipo() {
        return tipo;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        destino.generarC(codigo);
        codigo.append(" = ").append(RuntimeC.funcionLectura(tipo)).append("();\n");
    }

    @Override
    public String toString() {
        return getNumero() + ": read " + destino;
    }
}
