package contacto.comun.cuartetas.acceso;

/** Acceso a un campo de un objeto/estructura: base->campo en C (los objetos viven en heap). */
public class CampoLugar extends Lugar {

    private final Lugar base;
    private final String nombreCampo;

    public CampoLugar(Lugar base, String nombreCampo) {
        this.base = base;
        this.nombreCampo = nombreCampo;
    }

    @Override
    public void generarC(StringBuilder codigo) {
        base.generarC(codigo);
        codigo.append("->").append(nombreCampo);
    }

    @Override
    public String toString() {
        return base + "." + nombreCampo;
    }
}
