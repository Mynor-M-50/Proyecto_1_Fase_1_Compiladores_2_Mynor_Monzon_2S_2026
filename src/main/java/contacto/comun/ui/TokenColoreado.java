package contacto.comun.ui;

/** Un token del texto con su rango [inicio, fin) y su categoria visual. */
public final class TokenColoreado {

    private final int inicio;
    private final int fin;
    private final CategoriaToken categoria;

    public TokenColoreado(int inicio, int fin, CategoriaToken categoria) {
        this.inicio = inicio;
        this.fin = fin;
        this.categoria = categoria;
    }

    public int getInicio() {
        return inicio;
    }

    public int getFin() {
        return fin;
    }

    public CategoriaToken getCategoria() {
        return categoria;
    }
}
