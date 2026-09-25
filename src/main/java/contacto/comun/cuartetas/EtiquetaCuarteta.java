package contacto.comun.cuartetas;

/** Marca un punto del programa al que se puede saltar (nombre:). */
public class EtiquetaCuarteta extends Cuarteta {

    private final String nombre;

    public EtiquetaCuarteta(int numero, String nombre) {
        super(numero);
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return getNumero() + ": " + nombre + ":";
    }
}
