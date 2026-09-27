package contacto.comun.ui;

import contacto.comun.cuartetas.Cuarteta;
import contacto.comun.errores.ErrorCompilacion;
import contacto.comun.errores.EscuchaErrores;
import contacto.comun.errores.RecolectorErrores;
import contacto.comun.errores.TipoError;
import contacto.comun.orquestador.OrquestadorPig;
import contacto.ylang.YLangLexer;
import contacto.ylang.YLangParser;
import contacto.zetariano.ZetarianoLexer;
import contacto.zetariano.ZetarianoParser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Conecta el boton "Compilar" de la ventana con el backend real.
 *
 * - Un .pig se compila COMPLETO con OrquestadorPig (el .pig junto con
 *   todo lo que importa): esa es la unidad real de compilacion del
 *   proyecto, la que produce codigo C ejecutable.
 * - Un .z o .y suelto no se puede generar a C por si solo (son
 *   "librerias" pensadas para que un .pig las importe: sus tipos y
 *   funciones se resuelven en conjunto, no aisladas). Para esos solo
 *   se corre lexer+parser y se muestran errores lexicos/sintacticos --
 *   util mientras se escribe, sin fingir una compilacion completa que
 *   no tiene sentido para un archivo aislado.
 */
public final class ControladorCompilacion {

    private ControladorCompilacion() {
    }

    // Segun la extension: .pig compila todo; .z/.y solo revisan sintaxis
    public static void compilar(Path archivo, PanelSalida salida) {
        salida.limpiar();
        String nombre = archivo.getFileName().toString().toLowerCase();
        try {
            if (nombre.endsWith(".pig")) {
                compilarPig(archivo, salida);
            } else if (nombre.endsWith(".z")) {
                verificarSintaxis(archivo, salida, "Zetariano", ControladorCompilacion::parsearZetariano);
            } else if (nombre.endsWith(".y")) {
                verificarSintaxis(archivo, salida, "Y?", ControladorCompilacion::parsearYLang);
            } else {
                salida.mostrarErrores("Este archivo no es .pig, .z ni .y -- no hay nada que compilar.");
            }
        } catch (IOException ex) {
            salida.mostrarErrores("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    // Compila con OrquestadorPig, muestra errores/cuartetas/C y guarda el .c junto al .pig
    private static void compilarPig(Path archivo, PanelSalida salida) throws IOException {
        OrquestadorPig.Resultado resultado = OrquestadorPig.compilar(archivo);

        if (resultado.errores.tieneErrores()) {
            salida.mostrarErrores(formatearErrores(resultado.errores));
            return;
        }

        StringBuilder cuartetas = new StringBuilder();
        for (Map.Entry<String, contacto.comun.cuartetas.GeneradorCuartetas> importado
                : resultado.cuartetasImportadas.entrySet()) {
            cuartetas.append("--- ").append(importado.getKey()).append(" ---\n");
            for (Cuarteta cuarteta : importado.getValue().getCuartetas()) {
                cuartetas.append(cuarteta).append('\n');
            }
            cuartetas.append('\n');
        }
        cuartetas.append("--- ").append(archivo).append(" ---\n");
        for (Cuarteta cuarteta : resultado.cuartetasPig.getCuartetas()) {
            cuartetas.append(cuarteta).append('\n');
        }

        salida.mostrarErrores("Compilacion OK, sin errores.");
        salida.mostrarCuartetas(cuartetas.toString());
        salida.mostrarCodigoC(resultado.codigoC);

        Path archivoC = rutaConExtension(archivo, ".c");
        try {
            java.nio.file.Files.writeString(archivoC, resultado.codigoC);
            salida.mostrarErrores("Compilacion OK, sin errores.\nCodigo C guardado en " + archivoC);
        } catch (IOException ex) {
            salida.mostrarErrores("Compilacion OK, pero no se pudo guardar el .c en disco: " + ex.getMessage());
        }
    }

    /** "main.pig" -> "main.c" (misma carpeta, mismo nombre, otra extension). */
    private static Path rutaConExtension(Path archivo, String extension) {
        String nombre = archivo.getFileName().toString();
        int punto = nombre.lastIndexOf('.');
        String base = (punto >= 0) ? nombre.substring(0, punto) : nombre;
        return archivo.resolveSibling(base + extension);
    }

    /** Corre lexer+parser de un lenguaje y deja los errores en el recolector (ver parsearZetariano/parsearYLang). */
    private interface Parseador {
        void parsear(CharStream entrada, RecolectorErrores errores, String nombreArchivo);
    }

    // Corre solo lexer+parser de un .z/.y y muestra el resultado
    private static void verificarSintaxis(Path archivo, PanelSalida salida, String lenguaje, Parseador parseador)
            throws IOException {
        RecolectorErrores errores = new RecolectorErrores();
        CharStream entrada = CharStreams.fromPath(archivo);
        parseador.parsear(entrada, errores, archivo.toString());

        if (errores.tieneErrores()) {
            salida.mostrarErrores(formatearErrores(errores));
        } else {
            salida.mostrarErrores("Sintaxis de " + lenguaje + " OK. (" + archivo.getFileName()
                    + " es una libreria: para generar C completo, compila el .pig que la importa.)");
        }
    }

    // Lexer + parser de Zetariano con los errores al recolector
    private static void parsearZetariano(CharStream entrada, RecolectorErrores errores, String nombreArchivo) {
        ZetarianoLexer lexer = new ZetarianoLexer(entrada);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new EscuchaErrores(errores, TipoError.LEXICO, nombreArchivo));
        ZetarianoParser parser = new ZetarianoParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new EscuchaErrores(errores, TipoError.SINTACTICO, nombreArchivo));
        parser.programa();
    }

    // Lexer + parser de Y? con los errores al recolector
    private static void parsearYLang(CharStream entrada, RecolectorErrores errores, String nombreArchivo) {
        YLangLexer lexer = new YLangLexer(entrada);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new EscuchaErrores(errores, TipoError.LEXICO, nombreArchivo));
        YLangParser parser = new YLangParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new EscuchaErrores(errores, TipoError.SINTACTICO, nombreArchivo));
        parser.programa();
    }

    // Un error por linea
    private static String formatearErrores(RecolectorErrores errores) {
        StringBuilder sb = new StringBuilder();
        for (ErrorCompilacion error : errores.getErrores()) {
            sb.append(error).append('\n');
        }
        return sb.toString();
    }
}
