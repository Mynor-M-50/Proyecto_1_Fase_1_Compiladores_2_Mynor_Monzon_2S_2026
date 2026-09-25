package contacto.comun.errores;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un solo lugar donde se acumulan TODOS los errores de una compilacion
 * (lexicos, sintacticos y semanticos), sin importar el lenguaje de
 * origen. El EscuchaErrores (para lexico/sintactico) y cada analizador
 * semantico de cada lenguaje escriben aqui; la UI solo necesita leer
 * de aqui al final para mostrar la lista completa.
 */
public final class RecolectorErrores {

    private final List<ErrorCompilacion> errores = new ArrayList<>();

    public void agregar(TipoError tipo, String mensaje, int linea, int columna) {
        errores.add(new ErrorCompilacion(tipo, mensaje, linea, columna));
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    public boolean tieneErroresDeTipo(TipoError tipo) {
        for (ErrorCompilacion error : errores) {
            if (error.getTipo() == tipo) {
                return true;
            }
        }
        return false;
    }

    public List<ErrorCompilacion> getErrores() {
        return Collections.unmodifiableList(errores);
    }

    public void limpiar() {
        errores.clear();
    }
}
