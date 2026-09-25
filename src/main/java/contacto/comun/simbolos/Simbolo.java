package contacto.comun.simbolos;

import contacto.comun.tipos.Tipo;

/**
 * Una entrada en la tabla de simbolos: variable, parametro, funcion,
 * metodo, constructor, clase o estructura.
 */
public class Simbolo {

    private final String nombre;
    private final Tipo tipo;
    private final RolSimbolo rol;
    private final int linea;
    private final int columna;

    // Referencia opcional al contexto de ANTLR (XxxContext) donde se
    // definio (una funcion, metodo, clase o estructura), para que el
    // generador de cuartetas pueda volver a el sin tener que buscarlo
    // de nuevo en el arbol. Se deja como Object para no atar esta
    // clase compartida a la gramatica concreta de ningun lenguaje.
    private Object nodoDefinicion;

    public Simbolo(String nombre, Tipo tipo, RolSimbolo rol, int linea, int columna) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.rol = rol;
        this.linea = linea;
        this.columna = columna;
    }

    public String getNombre() {
        return nombre;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public RolSimbolo getRol() {
        return rol;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    public Object getNodoDefinicion() {
        return nodoDefinicion;
    }

    public void setNodoDefinicion(Object nodoDefinicion) {
        this.nodoDefinicion = nodoDefinicion;
    }

    public boolean esInvocable() {
        return rol == RolSimbolo.FUNCION || rol == RolSimbolo.METODO || rol == RolSimbolo.CONSTRUCTOR;
    }

    public boolean esTipoDefinido() {
        return rol == RolSimbolo.ESTRUCTURA || rol == RolSimbolo.CLASE;
    }

    @Override
    public String toString() {
        return rol + " " + nombre + " : " + tipo;
    }
}
