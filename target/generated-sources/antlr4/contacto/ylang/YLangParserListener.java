// Generated from contacto/ylang/YLangParser.g4 by ANTLR 4.13.2
package contacto.ylang;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link YLangParser}.
 */
public interface YLangParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link YLangParser#programa}.
	 * @param ctx the parse tree
	 */
	void enterPrograma(YLangParser.ProgramaContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#programa}.
	 * @param ctx the parse tree
	 */
	void exitPrograma(YLangParser.ProgramaContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#seccionEstructuras}.
	 * @param ctx the parse tree
	 */
	void enterSeccionEstructuras(YLangParser.SeccionEstructurasContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#seccionEstructuras}.
	 * @param ctx the parse tree
	 */
	void exitSeccionEstructuras(YLangParser.SeccionEstructurasContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#seccionFunciones}.
	 * @param ctx the parse tree
	 */
	void enterSeccionFunciones(YLangParser.SeccionFuncionesContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#seccionFunciones}.
	 * @param ctx the parse tree
	 */
	void exitSeccionFunciones(YLangParser.SeccionFuncionesContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#estructura}.
	 * @param ctx the parse tree
	 */
	void enterEstructura(YLangParser.EstructuraContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#estructura}.
	 * @param ctx the parse tree
	 */
	void exitEstructura(YLangParser.EstructuraContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#campoEstructura}.
	 * @param ctx the parse tree
	 */
	void enterCampoEstructura(YLangParser.CampoEstructuraContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#campoEstructura}.
	 * @param ctx the parse tree
	 */
	void exitCampoEstructura(YLangParser.CampoEstructuraContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#funcionDef}.
	 * @param ctx the parse tree
	 */
	void enterFuncionDef(YLangParser.FuncionDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#funcionDef}.
	 * @param ctx the parse tree
	 */
	void exitFuncionDef(YLangParser.FuncionDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#parametros}.
	 * @param ctx the parse tree
	 */
	void enterParametros(YLangParser.ParametrosContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#parametros}.
	 * @param ctx the parse tree
	 */
	void exitParametros(YLangParser.ParametrosContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#parametro}.
	 * @param ctx the parse tree
	 */
	void enterParametro(YLangParser.ParametroContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#parametro}.
	 * @param ctx the parse tree
	 */
	void exitParametro(YLangParser.ParametroContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#tipo}.
	 * @param ctx the parse tree
	 */
	void enterTipo(YLangParser.TipoContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#tipo}.
	 * @param ctx the parse tree
	 */
	void exitTipo(YLangParser.TipoContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#tipoPrimitivo}.
	 * @param ctx the parse tree
	 */
	void enterTipoPrimitivo(YLangParser.TipoPrimitivoContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#tipoPrimitivo}.
	 * @param ctx the parse tree
	 */
	void exitTipoPrimitivo(YLangParser.TipoPrimitivoContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentencia}.
	 * @param ctx the parse tree
	 */
	void enterSentencia(YLangParser.SentenciaContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentencia}.
	 * @param ctx the parse tree
	 */
	void exitSentencia(YLangParser.SentenciaContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#declaracionVariable}.
	 * @param ctx the parse tree
	 */
	void enterDeclaracionVariable(YLangParser.DeclaracionVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#declaracionVariable}.
	 * @param ctx the parse tree
	 */
	void exitDeclaracionVariable(YLangParser.DeclaracionVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentenciaExpresion}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaExpresion(YLangParser.SentenciaExpresionContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentenciaExpresion}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaExpresion(YLangParser.SentenciaExpresionContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentenciaSi}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaSi(YLangParser.SentenciaSiContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentenciaSi}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaSi(YLangParser.SentenciaSiContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#bloqueIndentado}.
	 * @param ctx the parse tree
	 */
	void enterBloqueIndentado(YLangParser.BloqueIndentadoContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#bloqueIndentado}.
	 * @param ctx the parse tree
	 */
	void exitBloqueIndentado(YLangParser.BloqueIndentadoContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentenciaElegir}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaElegir(YLangParser.SentenciaElegirContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentenciaElegir}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaElegir(YLangParser.SentenciaElegirContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#casoElegir}.
	 * @param ctx the parse tree
	 */
	void enterCasoElegir(YLangParser.CasoElegirContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#casoElegir}.
	 * @param ctx the parse tree
	 */
	void exitCasoElegir(YLangParser.CasoElegirContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#casoSiempre}.
	 * @param ctx the parse tree
	 */
	void enterCasoSiempre(YLangParser.CasoSiempreContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#casoSiempre}.
	 * @param ctx the parse tree
	 */
	void exitCasoSiempre(YLangParser.CasoSiempreContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#literalCaso}.
	 * @param ctx the parse tree
	 */
	void enterLiteralCaso(YLangParser.LiteralCasoContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#literalCaso}.
	 * @param ctx the parse tree
	 */
	void exitLiteralCaso(YLangParser.LiteralCasoContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentenciaPara}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaPara(YLangParser.SentenciaParaContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentenciaPara}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaPara(YLangParser.SentenciaParaContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#declaracionParaInit}.
	 * @param ctx the parse tree
	 */
	void enterDeclaracionParaInit(YLangParser.DeclaracionParaInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#declaracionParaInit}.
	 * @param ctx the parse tree
	 */
	void exitDeclaracionParaInit(YLangParser.DeclaracionParaInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentenciaMientras}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaMientras(YLangParser.SentenciaMientrasContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentenciaMientras}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaMientras(YLangParser.SentenciaMientrasContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentenciaHacerMientras}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaHacerMientras(YLangParser.SentenciaHacerMientrasContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentenciaHacerMientras}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaHacerMientras(YLangParser.SentenciaHacerMientrasContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentenciaRetornar}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaRetornar(YLangParser.SentenciaRetornarContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentenciaRetornar}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaRetornar(YLangParser.SentenciaRetornarContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentenciaRomper}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaRomper(YLangParser.SentenciaRomperContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentenciaRomper}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaRomper(YLangParser.SentenciaRomperContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#sentenciaContinuar}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaContinuar(YLangParser.SentenciaContinuarContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#sentenciaContinuar}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaContinuar(YLangParser.SentenciaContinuarContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expAditiva}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpAditiva(YLangParser.ExpAditivaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expAditiva}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpAditiva(YLangParser.ExpAditivaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expIgualdad}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpIgualdad(YLangParser.ExpIgualdadContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expIgualdad}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpIgualdad(YLangParser.ExpIgualdadContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expUnario}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpUnario(YLangParser.ExpUnarioContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expUnario}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpUnario(YLangParser.ExpUnarioContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expFlotante}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpFlotante(YLangParser.ExpFlotanteContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expFlotante}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpFlotante(YLangParser.ExpFlotanteContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expLlamadaFuncion}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpLlamadaFuncion(YLangParser.ExpLlamadaFuncionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expLlamadaFuncion}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpLlamadaFuncion(YLangParser.ExpLlamadaFuncionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expLiteralCompuesto}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpLiteralCompuesto(YLangParser.ExpLiteralCompuestoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expLiteralCompuesto}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpLiteralCompuesto(YLangParser.ExpLiteralCompuestoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expRelacional}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpRelacional(YLangParser.ExpRelacionalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expRelacional}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpRelacional(YLangParser.ExpRelacionalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expOr}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpOr(YLangParser.ExpOrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expOr}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpOr(YLangParser.ExpOrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expMultiplicativa}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpMultiplicativa(YLangParser.ExpMultiplicativaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expMultiplicativa}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpMultiplicativa(YLangParser.ExpMultiplicativaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expLeer}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpLeer(YLangParser.ExpLeerContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expLeer}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpLeer(YLangParser.ExpLeerContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expIncDecPrefijo}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpIncDecPrefijo(YLangParser.ExpIncDecPrefijoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expIncDecPrefijo}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpIncDecPrefijo(YLangParser.ExpIncDecPrefijoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expCadena}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpCadena(YLangParser.ExpCadenaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expCadena}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpCadena(YLangParser.ExpCadenaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expAcceso}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpAcceso(YLangParser.ExpAccesoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expAcceso}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpAcceso(YLangParser.ExpAccesoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expVerdadero}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpVerdadero(YLangParser.ExpVerdaderoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expVerdadero}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpVerdadero(YLangParser.ExpVerdaderoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expAnd}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpAnd(YLangParser.ExpAndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expAnd}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpAnd(YLangParser.ExpAndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expIncDecSufijo}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpIncDecSufijo(YLangParser.ExpIncDecSufijoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expIncDecSufijo}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpIncDecSufijo(YLangParser.ExpIncDecSufijoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expParentesis}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpParentesis(YLangParser.ExpParentesisContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expParentesis}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpParentesis(YLangParser.ExpParentesisContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expCaracter}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpCaracter(YLangParser.ExpCaracterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expCaracter}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpCaracter(YLangParser.ExpCaracterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expFalso}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpFalso(YLangParser.ExpFalsoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expFalso}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpFalso(YLangParser.ExpFalsoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expImprimir}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpImprimir(YLangParser.ExpImprimirContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expImprimir}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpImprimir(YLangParser.ExpImprimirContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expIndice}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpIndice(YLangParser.ExpIndiceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expIndice}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpIndice(YLangParser.ExpIndiceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expEntero}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpEntero(YLangParser.ExpEnteroContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expEntero}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpEntero(YLangParser.ExpEnteroContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expId}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpId(YLangParser.ExpIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expId}
	 * labeled alternative in {@link YLangParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpId(YLangParser.ExpIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link YLangParser#argumentos}.
	 * @param ctx the parse tree
	 */
	void enterArgumentos(YLangParser.ArgumentosContext ctx);
	/**
	 * Exit a parse tree produced by {@link YLangParser#argumentos}.
	 * @param ctx the parse tree
	 */
	void exitArgumentos(YLangParser.ArgumentosContext ctx);
}