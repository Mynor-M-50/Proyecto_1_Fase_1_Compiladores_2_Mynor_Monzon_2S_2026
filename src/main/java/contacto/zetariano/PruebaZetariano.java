package contacto.zetariano;

import contacto.comun.codegen.GeneradorCodigoC;
import contacto.comun.codegen.ModeloPrograma;
import contacto.comun.cuartetas.Cuarteta;
import contacto.comun.errores.ErrorCompilacion;
import contacto.comun.errores.EscuchaErrores;
import contacto.comun.errores.RecolectorErrores;
import contacto.comun.errores.TipoError;
import contacto.zetariano.generador.ZetarianoGeneradorCuartetas;
import contacto.zetariano.semantico.ZetarianoSemanticoListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Prueba manual de punta a punta para uno o varios archivos .z que se
 * usan entre si: lexer -> parser -> analisis semantico -> generador de
 * cuartetas -> C. Uso:
 *
 *   java contacto.zetariano.PruebaZetariano Nodo.z Pila.z ...
 *
 * Si no se le pasa argumento, usa test-programs/pila/Nodo.z y Pila.z
 * (Pila usa Nodo, asi que Pila.z solo no se puede generar).
 */
public class PruebaZetariano {

    public static void main(String[] args) throws IOException {
        List<String> rutas = (args.length > 0)
                ? List.of(args)
                : List.of("test-programs/pila/Nodo.z", "test-programs/pila/Pila.z");
        RecolectorErrores errores = new RecolectorErrores();

        List<ZetarianoParser.ProgramaContext> arboles = new ArrayList<>();
        List<ZetarianoSemanticoListener> semanticos = new ArrayList<>();
        for (String ruta : rutas) {
            CharStream entrada = CharStreams.fromPath(Path.of(ruta));

            ZetarianoLexer lexer = new ZetarianoLexer(entrada);
            lexer.removeErrorListeners();
            lexer.addErrorListener(new EscuchaErrores(errores, TipoError.LEXICO, ruta));

            ZetarianoParser parser = new ZetarianoParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(new EscuchaErrores(errores, TipoError.SINTACTICO, ruta));

            ZetarianoParser.ProgramaContext arbol = parser.programa();
            if (errores.tieneErrores()) {
                imprimirErrores(errores);
                return;
            }

            ZetarianoSemanticoListener semantico = new ZetarianoSemanticoListener(errores, ruta);
            ParseTreeWalker.DEFAULT.walk(semantico, arbol);
            if (errores.tieneErrores()) {
                imprimirErrores(errores);
                return;
            }
            arboles.add(arbol);
            semanticos.add(semantico);
        }

        System.out.println("Semantico OK, sin errores.\n");

        ModeloPrograma modelo = new ModeloPrograma();
        List<ZetarianoGeneradorCuartetas> generadores = new ArrayList<>();
        for (int i = 0; i < rutas.size(); i++) {
            ZetarianoGeneradorCuartetas generador = new ZetarianoGeneradorCuartetas(
                    semanticos.get(i).getTipos(), modelo, errores, rutas.get(i));
            generador.registrarFirmas(arboles.get(i));
            generadores.add(generador);
        }
        modelo.asignarNombresC();
        for (int i = 0; i < rutas.size(); i++) {
            generadores.get(i).visit(arboles.get(i));
        }
        if (errores.tieneErrores()) {
            imprimirErrores(errores);
            return;
        }

        for (int i = 0; i < rutas.size(); i++) {
            System.out.println("--- Cuartetas de " + rutas.get(i) + " ---");
            for (Cuarteta cuarteta : generadores.get(i).getGenerador().getCuartetas()) {
                System.out.println(cuarteta);
            }
            System.out.println();
        }

        String codigoC = GeneradorCodigoC.generar(String.join(", ", rutas), modelo);
        Path salida = Path.of("salida.c");
        Files.writeString(salida, codigoC);
        System.out.println("--- Codigo C escrito en " + salida.toAbsolutePath() + " ---");
    }

    private static void imprimirErrores(RecolectorErrores errores) {
        System.out.println("Se encontraron errores:");
        for (ErrorCompilacion error : errores.getErrores()) {
            System.out.println("  " + error);
        }
        System.exit(1);
    }
}
