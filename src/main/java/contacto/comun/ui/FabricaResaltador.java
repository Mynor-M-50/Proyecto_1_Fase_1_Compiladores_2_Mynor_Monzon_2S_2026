package contacto.comun.ui;

/** Elige el ResaltadorSintaxis correcto segun la extension del archivo. */
public final class FabricaResaltador {

    private FabricaResaltador() {
    }

    public static ResaltadorSintaxis paraArchivo(String nombreArchivo) {
        String nombre = nombreArchivo.toLowerCase();
        if (nombre.endsWith(".z")) {
            return new ResaltadorZetariano();
        }
        if (nombre.endsWith(".y")) {
            return new ResaltadorYLang();
        }
        if (nombre.endsWith(".pig")) {
            return new ResaltadorPigLatin();
        }
        return null; // extension desconocida: se edita sin colorear
    }

    // true si es .z, .y o .pig
    public static boolean esArchivoDelProyecto(String nombreArchivo) {
        return paraArchivo(nombreArchivo) != null;
    }
}
