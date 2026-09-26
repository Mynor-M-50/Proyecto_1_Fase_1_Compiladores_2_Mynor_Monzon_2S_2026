package contacto.comun.cuartetas;

/**
 * Implementado por cualquier cuarteta u operando capaz de generar su
 * propio fragmento de codigo C. Adaptado de la idea de
 * "CodeTransformable" del repo de referencia de la auxiliar: en vez de
 * un generador externo con un switch gigante sobre "que tipo de cosa es
 * esto", cada clase sabe traducirse a si misma.
 */
public interface CodigoTransformable {
    void generarC(StringBuilder codigo);
}
