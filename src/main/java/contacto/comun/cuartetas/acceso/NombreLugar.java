package contacto.comun.cuartetas.acceso;

/** Una variable o un temporal (t0, t1, ...) -- ambos son, en C, un identificador. */
public class NombreLugar extends Lugar {

    private final String nombre;

    public NombreLugar(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        codigo.append(nombre);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
