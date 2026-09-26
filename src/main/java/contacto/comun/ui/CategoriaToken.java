package contacto.comun.ui;

/**
 * Categorias visuales para el coloreado de codigo. Deliberadamente
 * pocas: palabras clave, tipos, cadenas/caracteres, numeros,
 * comentarios e identificadores. Los operadores y signos de puntuacion
 * quedan en DEFECTO (color normal del texto) -- casi ningun editor los
 * colorea aparte, y enumerar cada uno de los ~30 simbolos de cada
 * lenguaje no le agrega valor real al usuario.
 */
public enum CategoriaToken {
    PALABRA_CLAVE,
    TIPO_PRIMITIVO,
    CADENA,
    NUMERO,
    COMENTARIO,
    IDENTIFICADOR,
    DEFECTO
}
