package contacto.comun.codegen;

import contacto.comun.cuartetas.Cuarteta;
import contacto.comun.cuartetas.EtiquetaCuarteta;
import contacto.comun.cuartetas.GeneradorCuartetas;
import contacto.comun.cuartetas.SaltoCondicionalCuarteta;
import contacto.comun.cuartetas.SaltoCuarteta;
import contacto.comun.tipos.Tipo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Arma el archivo .c completo a partir del ModeloPrograma (el .pig y
 * todo lo que importa):
 *
 *   1. el runtime (RuntimeC): includes y funciones auxiliares;
 *   2. un "typedef struct" + "struct" por cada clase (.z) o
 *      estructura (.y), con sus campos;
 *   3. el prototipo de cada funcion (asi el orden de definicion no
 *      importa, igual que en los lenguajes fuente);
 *   4. cada funcion libre de Y?, cada constructor y metodo de
 *      Zetariano, y al final el main del .pig.
 *
 * Cada funcion declara al inicio TODAS sus locales y temporales (con su
 * tipo real, inicializadas en 0): asi ningun goto de las cuartetas salta
 * una declaracion, y ninguna variable se lee sin valor.
 *
 * Objetos y estructuras viven en heap: en C son punteros (Nodo*), los
 * constructores reservan el objeto y lo devuelven, y los metodos lo
 * reciben como primer parametro "this".
 */
public final class GeneradorCodigoC {

    private GeneradorCodigoC() {
    }

    public static String generar(String nombrePrograma, ModeloPrograma modelo) {
        StringBuilder codigo = new StringBuilder();
        codigo.append("// Generado automaticamente a partir de ").append(nombrePrograma).append("\n");
        codigo.append(RuntimeC.CODIGO).append('\n');

        declararEstructuras(codigo, modelo);

        List<ModeloPrograma.Funcion> funciones = modelo.getFunciones();
        if (!funciones.isEmpty()) {
            for (ModeloPrograma.Funcion funcion : funciones) {
                codigo.append(firma(funcion)).append(";\n");
            }
            codigo.append('\n');
        }

        for (ModeloPrograma.Funcion funcion : funciones) {
            definirFuncion(codigo, funcion);
        }
        if (modelo.getMain() != null) {
            definirFuncion(codigo, modelo.getMain());
        } else {
            codigo.append("int main(void) {\n    return 0;\n}\n");
        }
        return codigo.toString();
    }

    private static void declararEstructuras(StringBuilder codigo, ModeloPrograma modelo) {
        boolean hayEstructuras = false;
        for (ModeloPrograma.Estructura estructura : modelo.getEstructuras()) {
            codigo.append("typedef struct ").append(estructura.getNombre()).append(' ')
                    .append(estructura.getNombre()).append(";\n");
            hayEstructuras = true;
        }
        if (!hayEstructuras) {
            return;
        }
        codigo.append('\n');
        for (ModeloPrograma.Estructura estructura : modelo.getEstructuras()) {
            codigo.append("struct ").append(estructura.getNombre()).append(" {\n");
            if (estructura.getCampos().isEmpty()) {
                codigo.append("    char _vacio; // C no permite un struct sin campos\n");
            }
            for (ModeloPrograma.Variable campo : estructura.getCampos()) {
                codigo.append("    ");
                if (campo.getTamanioFijo() > 0) {
                    codigo.append(RuntimeC.tipoC(campo.getTipo().tipoElemento())).append(' ')
                            .append(campo.getNombreC()).append('[').append(campo.getTamanioFijo()).append(']');
                } else {
                    codigo.append(RuntimeC.tipoC(campo.getTipo())).append(' ').append(campo.getNombreC());
                }
                codigo.append(";\n");
            }
            codigo.append("};\n\n");
        }
    }

    private static String firma(ModeloPrograma.Funcion funcion) {
        StringBuilder sb = new StringBuilder();
        if (funcion.esMain()) {
            sb.append("int");
        } else if (funcion.esConstructor()) {
            sb.append(funcion.getClase()).append('*');
        } else {
            sb.append(RuntimeC.tipoC(funcion.getRetorno()));
        }
        sb.append(' ').append(funcion.getNombreC()).append('(');

        boolean primero = true;
        if (funcion.esMetodo()) {
            sb.append(funcion.getClase()).append("* this");
            primero = false;
        }
        for (ModeloPrograma.Variable parametro : funcion.getParametros()) {
            if (!primero) {
                sb.append(", ");
            }
            sb.append(RuntimeC.tipoC(parametro.getTipo())).append(' ').append(parametro.getNombreC());
            primero = false;
        }
        if (primero) {
            sb.append("void");
        }
        return sb.append(')').toString();
    }

    private static void definirFuncion(StringBuilder codigo, ModeloPrograma.Funcion funcion) {
        codigo.append(firma(funcion)).append(" {\n");

        if (funcion.esConstructor()) {
            codigo.append("    ").append(funcion.getClase()).append("* this = zc_reservar(sizeof(")
                    .append(funcion.getClase()).append("));\n");
        } else if (funcion.esMetodo()) {
            codigo.append("    zc_verificar_objeto(this, \"").append(funcion.getNombreCompleto()).append("\");\n");
        }

        for (Map.Entry<String, Tipo> local : funcion.getLocales().entrySet()) {
            codigo.append("    ").append(RuntimeC.tipoC(local.getValue())).append(' ')
                    .append(local.getKey()).append(" = 0;\n");
        }

        GeneradorCuartetas gen = funcion.getGenerador();
        if (gen != null) {
            for (int i = funcion.getTemporalDesde(); i < funcion.getTemporalHasta(); i++) {
                Tipo tipo = gen.getTipoTemporal(i);
                if (tipo.esVacio()) {
                    continue;
                }
                codigo.append("    ").append(RuntimeC.tipoC(tipo)).append(' ')
                        .append(GeneradorCuartetas.nombreTemporal(i)).append(" = 0;\n");
            }

            List<Cuarteta> cuerpo = gen.getCuartetas().subList(funcion.getCuartetaDesde(), funcion.getCuartetaHasta());
            Set<String> etiquetasUsadas = etiquetasDestino(cuerpo);
            codigo.append('\n');
            for (Cuarteta cuarteta : cuerpo) {
                codigo.append("    ");
                if (cuarteta instanceof EtiquetaCuarteta
                        && !etiquetasUsadas.contains(((EtiquetaCuarteta) cuarteta).getNombre())) {
                    // Etiqueta que ningun goto usa (p.ej. inicio_apilar): solo
                    // como comentario, para que gcc -Wall no avise.
                    codigo.append("// ").append(((EtiquetaCuarteta) cuarteta).getNombre()).append(":\n");
                    continue;
                }
                cuarteta.generarC(codigo);
            }
        }

        if (funcion.esConstructor()) {
            codigo.append("    return this;\n");
        } else if (funcion.esMain()) {
            codigo.append("    return 0;\n");
        }
        codigo.append("}\n\n");
    }

    private static Set<String> etiquetasDestino(List<Cuarteta> cuerpo) {
        Set<String> destinos = new HashSet<>();
        for (Cuarteta cuarteta : cuerpo) {
            if (cuarteta instanceof SaltoCuarteta) {
                destinos.add(((SaltoCuarteta) cuarteta).getEtiquetaDestino());
            } else if (cuarteta instanceof SaltoCondicionalCuarteta) {
                destinos.add(((SaltoCondicionalCuarteta) cuarteta).getEtiquetaDestino());
            }
        }
        return destinos;
    }
}
