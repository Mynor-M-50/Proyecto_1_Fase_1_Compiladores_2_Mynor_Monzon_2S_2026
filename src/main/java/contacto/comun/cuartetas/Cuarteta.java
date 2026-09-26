package contacto.comun.cuartetas;

/**
 * Una cuarteta de codigo de tres direcciones (C3D). Cada subtipo sabe
 * imprimirse en el formato de depuracion (toString) Y generar su
 * propio fragmento de C (generarC) -- adoptado del patron
 * CodeTransformable del repo de referencia de la auxiliar, en vez de
 * un generador de C aparte con un switch gigante sobre el tipo de cada
 * cuarteta.
 */
public abstract class Cuarteta implements CodigoTransformable {

    private final int numero;

    protected Cuarteta(int numero) {
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }
}
