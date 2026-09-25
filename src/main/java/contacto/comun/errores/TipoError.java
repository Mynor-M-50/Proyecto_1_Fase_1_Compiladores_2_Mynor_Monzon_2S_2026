package contacto.comun.errores;

/**
 * Categoria de un error de compilacion. Los tres lenguajes (Y?,
 * Zetariano, Pig Latin) reportan sus errores usando estas mismas
 * categorias, para que la UI pueda listarlos todos juntos.
 */
public enum TipoError {
    LEXICO,
    SINTACTICO,
    SEMANTICO
}
