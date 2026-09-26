package contacto.comun.orquestador;

import contacto.comun.codegen.GeneradorCodigoC;
import contacto.comun.codegen.ModeloPrograma;
import contacto.comun.cuartetas.GeneradorCuartetas;
import contacto.comun.errores.EscuchaErrores;
import contacto.comun.errores.RecolectorErrores;
import contacto.comun.errores.TipoError;
import contacto.comun.simbolos.TablaSimbolos;
import contacto.comun.tipos.Tipo;
import contacto.piglatin.PigLatinLexer;
import contacto.piglatin.PigLatinParser;
import contacto.piglatin.generador.PigLatinGeneradorCuartetas;
import contacto.piglatin.semantico.PigLatinSemanticoListener;
import contacto.ylang.YLangLexer;
import contacto.ylang.YLangParser;
import contacto.ylang.generador.YLangGeneradorCuartetas;
import contacto.ylang.semantico.YLangSemanticoListener;
import contacto.zetariano.ZetarianoLexer;
import contacto.zetariano.ZetarianoParser;
import contacto.zetariano.generador.ZetarianoGeneradorCuartetas;
import contacto.zetariano.semantico.ZetarianoSemanticoListener;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compila un archivo .pig JUNTO CON los .z/.y que importa y produce UN
 * solo programa C con todo:
 *
 *   1. Analisis de cada .z/.y con el lexer/parser/semantico de su
 *      propio lenguaje.
 *   2. Firmas: cada generador registra en el ModeloPrograma sus
 *      clases/estructuras, campos y funciones -- de TODOS los archivos
 *      antes de generar ningun cuerpo, para que una clase pueda usar
 *      otra sin importar el orden de los import. Con ese modelo se
 *      analiza semanticamente el .pig (pila.apilar(x) se busca en la
 *      clase de "pila").
 *   3. Cuerpos: cuartetas de cada funcion/metodo/constructor y del main.
 *      Aqui tambien se detectan llamadas y campos de otra clase que no
 *      existen (el semantico de cada .z no ve las otras clases).
 *   4. GeneradorCodigoC arma el .c con el ModeloPrograma completo.
 *
 * Cada error trae el nombre del archivo de origen (ver
 * ErrorCompilacion.getArchivo()), necesario porque aqui hay varios
 * archivos compilandose en una sola pasada.
 *
 * Limitaciones:
 *  - Resuelve rutas de import literal: "utils.utils.y" -> carpeta del
 *    .pig + "utils/utils.y". No hay classpath ni busqueda en varias
 *    carpetas.
 *  - Si un archivo tiene errores lexicos/sintacticos/semanticos se
 *    detiene ahi, sin seguir con el resto para juntar mas errores.
 */
public final class OrquestadorPig {

    public static final class Resultado {
        public final RecolectorErrores errores = new RecolectorErrores();
        public TablaSimbolos tablaPig;
        public GeneradorCuartetas cuartetasPig;
        /** Cuartetas de cada .z/.y importado, por nombre de archivo, en orden de import. */
        public final Map<String, GeneradorCuartetas> cuartetasImportadas = new LinkedHashMap<>();
        public String codigoC;
    }

    /** Un .z o .y ya analizado, esperando a que se genere su codigo. */
    private static final class Importado {
        final String nombreArchivo;
        final ZetarianoParser.ProgramaContext arbolZetariano;
        final YLangParser.ProgramaContext arbolYLang;
        final ParseTreeProperty<Tipo> tipos;
        ZetarianoGeneradorCuartetas generadorZetariano;
        YLangGeneradorCuartetas generadorYLang;

        Importado(String nombreArchivo, ZetarianoParser.ProgramaContext arbolZetariano,
                  YLangParser.ProgramaContext arbolYLang, ParseTreeProperty<Tipo> tipos) {
            this.nombreArchivo = nombreArchivo;
            this.arbolZetariano = arbolZetariano;
            this.arbolYLang = arbolYLang;
            this.tipos = tipos;
        }
    }

    private OrquestadorPig() {
    }

    public static Resultado compilar(Path archivoPig) throws IOException {
        Resultado resultado = new Resultado();
        RecolectorErrores errores = resultado.errores;
        String nombrePig = archivoPig.toString();

        // ---- 1. Analisis ----
        PigLatinLexer lexerPig = new PigLatinLexer(CharStreams.fromPath(archivoPig));
        lexerPig.removeErrorListeners();
        lexerPig.addErrorListener(new EscuchaErrores(errores, TipoError.LEXICO, nombrePig));
        PigLatinParser parserPig = new PigLatinParser(new CommonTokenStream(lexerPig));
        parserPig.removeErrorListeners();
        parserPig.addErrorListener(new EscuchaErrores(errores, TipoError.SINTACTICO, nombrePig));

        PigLatinParser.ProgramaContext arbolPig = parserPig.programa();
        if (errores.tieneErrores()) {
            return resultado;
        }

        List<Importado> importados = new ArrayList<>();
        Set<Path> yaImportados = new HashSet<>();
        if (arbolPig.seccionImports() != null) {
            for (PigLatinParser.RutaImportContext ruta : arbolPig.seccionImports().rutaImport()) {
                Path archivoImportado = resolverRuta(archivoPig.getParent(), ruta.getText());
                if (!yaImportados.add(archivoImportado.toAbsolutePath().normalize())) {
                    continue; // el mismo archivo importado dos veces
                }
                if (!Files.exists(archivoImportado)) {
                    errores.agregar(TipoError.SEMANTICO, "No existe el archivo importado '" + ruta.getText()
                                    + "' (se busco en " + archivoImportado + ")",
                            ruta.getStart().getLine(), ruta.getStart().getCharPositionInLine(), nombrePig);
                    return resultado;
                }
                Importado importado = analizarImportado(archivoImportado, errores);
                if (errores.tieneErrores()) {
                    return resultado;
                }
                importados.add(importado);
            }
        }

        // ---- 2. Firmas de todos los archivos, y semantico del .pig con ellas ----
        ModeloPrograma modelo = new ModeloPrograma();
        for (Importado importado : importados) {
            if (importado.arbolZetariano != null) {
                importado.generadorZetariano = new ZetarianoGeneradorCuartetas(importado.tipos, modelo,
                        errores, importado.nombreArchivo);
                importado.generadorZetariano.registrarFirmas(importado.arbolZetariano);
            } else {
                importado.generadorYLang = new YLangGeneradorCuartetas(importado.tipos, modelo,
                        errores, importado.nombreArchivo);
                importado.generadorYLang.registrarFirmas(importado.arbolYLang);
            }
        }
        PigLatinSemanticoListener semantico = new PigLatinSemanticoListener(errores, modelo, nombrePig);
        ParseTreeWalker.DEFAULT.walk(semantico, arbolPig);
        if (errores.tieneErrores()) {
            return resultado;
        }

        PigLatinGeneradorCuartetas generadorPig = new PigLatinGeneradorCuartetas(semantico.getTipos(), modelo,
                errores, nombrePig);
        generadorPig.registrarFirmas(arbolPig);
        modelo.asignarNombresC();

        // ---- 3. Cuerpos ----
        for (Importado importado : importados) {
            GeneradorCuartetas cuartetas;
            if (importado.generadorZetariano != null) {
                importado.generadorZetariano.visit(importado.arbolZetariano);
                cuartetas = importado.generadorZetariano.getGenerador();
            } else {
                importado.generadorYLang.visit(importado.arbolYLang);
                cuartetas = importado.generadorYLang.getGenerador();
            }
            resultado.cuartetasImportadas.put(importado.nombreArchivo, cuartetas);
        }
        generadorPig.visit(arbolPig);
        if (errores.tieneErrores()) {
            return resultado;
        }

        // ---- 4. Codigo C ----
        resultado.tablaPig = semantico.getTabla();
        resultado.cuartetasPig = generadorPig.getGenerador();
        resultado.codigoC = GeneradorCodigoC.generar(nombrePig, modelo);
        return resultado;
    }

    /** "utils.utils.y" -> carpetaBase/utils/utils.y ; "Nodo.z" -> carpetaBase/Nodo.z */
    private static Path resolverRuta(Path carpetaBase, String rutaImport) {
        int ultimoPunto = rutaImport.lastIndexOf('.');
        String sinExtension = rutaImport.substring(0, ultimoPunto).replace('.', '/');
        String extension = rutaImport.substring(ultimoPunto); // incluye el punto: ".z" o ".y"
        Path base = (carpetaBase != null) ? carpetaBase : Path.of(".");
        return base.resolve(sinExtension + extension);
    }

    private static Importado analizarImportado(Path archivo, RecolectorErrores errores)
            throws IOException {
        String nombre = archivo.getFileName().toString();
        if (nombre.endsWith(".z")) {
            return analizarZetariano(archivo, errores);
        }
        if (nombre.endsWith(".y")) {
            return analizarYLang(archivo, errores);
        }
        errores.agregar(TipoError.SEMANTICO, "No se reconoce la extension del import: " + archivo, 0, 0, archivo.toString());
        return null;
    }

    private static Importado analizarZetariano(Path archivo, RecolectorErrores errores)
            throws IOException {
        String nombreArchivo = archivo.toString();
        CharStream entrada = CharStreams.fromPath(archivo);
        ZetarianoLexer lexer = new ZetarianoLexer(entrada);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new EscuchaErrores(errores, TipoError.LEXICO, nombreArchivo));
        ZetarianoParser parser = new ZetarianoParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new EscuchaErrores(errores, TipoError.SINTACTICO, nombreArchivo));

        ZetarianoParser.ProgramaContext arbol = parser.programa();
        if (errores.tieneErrores()) {
            return null;
        }

        ZetarianoSemanticoListener semantico = new ZetarianoSemanticoListener(errores, nombreArchivo);
        ParseTreeWalker.DEFAULT.walk(semantico, arbol);
        return new Importado(nombreArchivo, arbol, null, semantico.getTipos());
    }

    private static Importado analizarYLang(Path archivo, RecolectorErrores errores)
            throws IOException {
        String nombreArchivo = archivo.toString();
        CharStream entrada = CharStreams.fromPath(archivo);
        YLangLexer lexer = new YLangLexer(entrada);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new EscuchaErrores(errores, TipoError.LEXICO, nombreArchivo));
        YLangParser parser = new YLangParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(new EscuchaErrores(errores, TipoError.SINTACTICO, nombreArchivo));

        YLangParser.ProgramaContext arbol = parser.programa();
        if (errores.tieneErrores()) {
            return null;
        }

        YLangSemanticoListener semantico = new YLangSemanticoListener(errores, nombreArchivo);
        ParseTreeWalker.DEFAULT.walk(semantico, arbol);
        return new Importado(nombreArchivo, null, arbol, semantico.getTipos());
    }
}
