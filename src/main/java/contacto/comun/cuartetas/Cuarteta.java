package contacto.comun.cuartetas;

/**
 * Una cuarteta de codigo de tres direcciones (C3D). Cada subtipo
 * concreto sabe imprimirse a si mismo en el formato de depuracion
 * (toString) que se muestra en pantalla; la traduccion a C vive aparte
 * en comun.codegen, para no mezclar "que es una cuarteta" con "como
 * se ve en C" (eso lo arma un visitor/switch sobre estas clases).
 */
public abstract class Cuarteta {

    private final int numero;

    protected Cuarteta(int numero) {
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }
}
