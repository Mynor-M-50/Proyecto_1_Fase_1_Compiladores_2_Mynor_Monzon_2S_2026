package contacto.comun.cuartetas.acceso;

import contacto.comun.cuartetas.CodigoTransformable;

/**
 * Representa DONDE vive un valor dentro de una cuarteta: una variable o
 * temporal (un identificador), un literal, un campo de estructura
 * (base.campo) o un elemento de arreglo (base[indice]).
 *
 * Adaptado del "MemoryAccess" del repo de referencia de la auxiliar.
 * Simplificado para hoy: junto variable y temporal en una sola clase
 * (NombreLugar) porque en el C generado terminan siendo exactamente lo
 * mismo, un identificador; y no incluyo un acceso "a la pila" (su
 * propio StackAccess estaba sin implementar tambien).
 */
public abstract class Lugar implements CodigoTransformable {

    @Override
    public abstract String toString();
}
