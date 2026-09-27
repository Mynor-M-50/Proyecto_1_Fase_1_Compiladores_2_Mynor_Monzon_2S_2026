package contacto.comun.cuartetas.acceso;

/** Un literal tal cual va a aparecer en el C generado (ya en su forma de texto C). */
public class LiteralLugar extends Lugar {

    private final String textoC;

    public LiteralLugar(String textoC) {
        this.textoC = textoC;
    }

    // El literal se copia tal cual (ya viene escrito como en C)
    @Override
    public void generarC(StringBuilder codigo) {
        codigo.append(textoC);
    }

    @Override
    public String toString() {
        return textoC;
    }
}
