package contacto.comun.cuartetas.acceso;

/** Acceso a un elemento de arreglo: base[indice], igual en C que en el lenguaje fuente. */
public class IndiceLugar extends Lugar {

    private final Lugar base;
    private final Lugar indice;

    public IndiceLugar(Lugar base, Lugar indice) {
        this.base = base;
        this.indice = indice;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        base.generarC(codigo);
        codigo.append('[');
        indice.generarC(codigo);
        codigo.append(']');
    }

    @Override
    public String toString() {
        return base + "[" + indice + "]";
    }
}
