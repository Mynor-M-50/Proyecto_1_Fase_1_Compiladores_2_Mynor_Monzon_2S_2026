package contacto.comun.orquestador;

import contacto.comun.cuartetas.Cuarteta;
import contacto.comun.errores.ErrorCompilacion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Prueba manual de punta a punta para el orquestador. Uso:
 *
 *   java contacto.comun.orquestador.PruebaOrquestador ruta/al/main.pig
 *
 * Si no se le pasa argumento, usa test-programs/pila/main.pig por defecto
 * (que importa Nodo.z, Pila.z y utils/utils.y).
 */
public class PruebaOrquestador {

    public static void main(String[] args) throws IOException {
        String ruta = (args.length > 0) ? args[0] : "test-programs/pila/main.pig";

        OrquestadorPig.Resultado resultado = OrquestadorPig.compilar(Path.of(ruta));

        if (resultado.errores.tieneErrores()) {
            System.out.println("Se encontraron errores:");
            for (ErrorCompilacion error : resultado.errores.getErrores()) {
                System.out.println("  " + error);
            }
            System.exit(1);
        }

        System.out.println("Compilacion OK (pig + imports), sin errores.\n");

        for (var importado : resultado.cuartetasImportadas.entrySet()) {
            System.out.println("--- Cuartetas de " + importado.getKey() + " ---");
            for (Cuarteta cuarteta : importado.getValue().getCuartetas()) {
                System.out.println(cuarteta);
            }
            System.out.println();
        }

        System.out.println("--- Cuartetas del .pig ---");
        for (Cuarteta cuarteta : resultado.cuartetasPig.getCuartetas()) {
            System.out.println(cuarteta);
        }

        Path salida = Path.of("salida.c");
        Files.writeString(salida, resultado.codigoC);
        System.out.println("\n--- Codigo C escrito en " + salida.toAbsolutePath() + " ---");
    }
}
