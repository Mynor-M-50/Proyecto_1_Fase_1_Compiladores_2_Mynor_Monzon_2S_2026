package contacto.ylang;

import contacto.comun.cuartetas.Cuarteta;
import contacto.comun.codegen.GeneradorCodigoC;
import contacto.comun.codegen.ModeloPrograma;
import contacto.comun.errores.ErrorCompilacion;
import contacto.comun.errores.EscuchaErrores;
import contacto.comun.errores.RecolectorErrores;
import contacto.comun.errores.TipoError;
import contacto.ylang.generador.YLangGeneradorCuartetas;
import contacto.ylang.semantico.YLangSemanticoListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Prueba manual de punta a punta para un archivo .y. Uso:
 *
 *   java contacto.ylang.PruebaYLang ruta/al/archivo.y
 *
 * Si no se le pasa argumento, usa test-programs/pila/utils/utils.y por defecto.
 */
public class PruebaYLang {

    public static void main(String[] args) throws IOException {
        String ruta = (args.length > 0) ? args[0] : "test-programs/pila/utils/utils.y";
        RecolectorErrores errores = new RecolectorErrores();

        CharStream entrada = CharStreams.fromPath(Path.of(ruta));

        YLangLexer lexer = new YLangLexer(entrada);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new EscuchaErrores(errores, TipoError.LEXICO, ruta));

        YLangParser parser = new YLangParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new EscuchaErrores(errores, TipoError.SINTACTICO, ruta));

        ParseTree arbol = parser.programa();

        if (errores.tieneErrores()) {
            imprimirErrores(errores);
            return;
        }

        YLangSemanticoListener semantico = new YLangSemanticoListener(errores, ruta);
        ParseTreeWalker.DEFAULT.walk(semantico, arbol);

        if (errores.tieneErrores()) {
            imprimirErrores(errores);
            return;
        }

        System.out.println("Semantico OK, sin errores.\n");

        ModeloPrograma modelo = new ModeloPrograma();
        YLangGeneradorCuartetas generador = new YLangGeneradorCuartetas(semantico.getTipos(), modelo, errores, ruta);
        generador.registrarFirmas((YLangParser.ProgramaContext) arbol);
        modelo.asignarNombresC();
        generador.visit(arbol);
        if (errores.tieneErrores()) {
            imprimirErrores(errores);
            return;
        }

        System.out.println("--- Cuartetas generadas ---");
        for (Cuarteta cuarteta : generador.getGenerador().getCuartetas()) {
            System.out.println(cuarteta);
        }

        String codigoC = GeneradorCodigoC.generar(ruta, modelo);
        java.nio.file.Path salida = Path.of("salida.c");
        java.nio.file.Files.writeString(salida, codigoC);
        System.out.println("\n--- Codigo C escrito en " + salida.toAbsolutePath() + " ---");
    }

    private static void imprimirErrores(RecolectorErrores errores) {
        System.out.println("Se encontraron errores:");
        for (ErrorCompilacion error : errores.getErrores()) {
            System.out.println("  " + error);
        }
        System.exit(1);
    }
}
