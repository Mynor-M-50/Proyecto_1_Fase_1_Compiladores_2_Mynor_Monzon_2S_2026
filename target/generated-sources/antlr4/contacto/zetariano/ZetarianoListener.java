// Generated from contacto/zetariano/Zetariano.g4 by ANTLR 4.13.2
package contacto.zetariano;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ZetarianoParser}.
 */
public interface ZetarianoListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#programa}.
	 * @param ctx the parse tree
	 */
	void enterPrograma(ZetarianoParser.ProgramaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#programa}.
	 * @param ctx the parse tree
	 */
	void exitPrograma(ZetarianoParser.ProgramaContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#clase}.
	 * @param ctx the parse tree
	 */
	void enterClase(ZetarianoParser.ClaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#clase}.
	 * @param ctx the parse tree
	 */
	void exitClase(ZetarianoParser.ClaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#miembro}.
	 * @param ctx the parse tree
	 */
	void enterMiembro(ZetarianoParser.MiembroContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#miembro}.
	 * @param ctx the parse tree
	 */
	void exitMiembro(ZetarianoParser.MiembroContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#campo}.
	 * @param ctx the parse tree
	 */
	void enterCampo(ZetarianoParser.CampoContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#campo}.
	 * @param ctx the parse tree
	 */
	void exitCampo(ZetarianoParser.CampoContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#modificador}.
	 * @param ctx the parse tree
	 */
	void enterModificador(ZetarianoParser.ModificadorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#modificador}.
	 * @param ctx the parse tree
	 */
	void exitModificador(ZetarianoParser.ModificadorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#constructor}.
	 * @param ctx the parse tree
	 */
	void enterConstructor(ZetarianoParser.ConstructorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#constructor}.
	 * @param ctx the parse tree
	 */
	void exitConstructor(ZetarianoParser.ConstructorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#metodo}.
	 * @param ctx the parse tree
	 */
	void enterMetodo(ZetarianoParser.MetodoContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#metodo}.
	 * @param ctx the parse tree
	 */
	void exitMetodo(ZetarianoParser.MetodoContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#parametros}.
	 * @param ctx the parse tree
	 */
	void enterParametros(ZetarianoParser.ParametrosContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#parametros}.
	 * @param ctx the parse tree
	 */
	void exitParametros(ZetarianoParser.ParametrosContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#parametro}.
	 * @param ctx the parse tree
	 */
	void enterParametro(ZetarianoParser.ParametroContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#parametro}.
	 * @param ctx the parse tree
	 */
	void exitParametro(ZetarianoParser.ParametroContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#tipo}.
	 * @param ctx the parse tree
	 */
	void enterTipo(ZetarianoParser.TipoContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#tipo}.
	 * @param ctx the parse tree
	 */
	void exitTipo(ZetarianoParser.TipoContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#tipoPrimitivo}.
	 * @param ctx the parse tree
	 */
	void enterTipoPrimitivo(ZetarianoParser.TipoPrimitivoContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#tipoPrimitivo}.
	 * @param ctx the parse tree
	 */
	void exitTipoPrimitivo(ZetarianoParser.TipoPrimitivoContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#bloque}.
	 * @param ctx the parse tree
	 */
	void enterBloque(ZetarianoParser.BloqueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#bloque}.
	 * @param ctx the parse tree
	 */
	void exitBloque(ZetarianoParser.BloqueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentencia}.
	 * @param ctx the parse tree
	 */
	void enterSentencia(ZetarianoParser.SentenciaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentencia}.
	 * @param ctx the parse tree
	 */
	void exitSentencia(ZetarianoParser.SentenciaContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#declaracionVariable}.
	 * @param ctx the parse tree
	 */
	void enterDeclaracionVariable(ZetarianoParser.DeclaracionVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#declaracionVariable}.
	 * @param ctx the parse tree
	 */
	void exitDeclaracionVariable(ZetarianoParser.DeclaracionVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaExpresion}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaExpresion(ZetarianoParser.SentenciaExpresionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaExpresion}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaExpresion(ZetarianoParser.SentenciaExpresionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#operadorAsignacion}.
	 * @param ctx the parse tree
	 */
	void enterOperadorAsignacion(ZetarianoParser.OperadorAsignacionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#operadorAsignacion}.
	 * @param ctx the parse tree
	 */
	void exitOperadorAsignacion(ZetarianoParser.OperadorAsignacionContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaIf}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaIf(ZetarianoParser.SentenciaIfContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaIf}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaIf(ZetarianoParser.SentenciaIfContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaOBloque}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaOBloque(ZetarianoParser.SentenciaOBloqueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaOBloque}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaOBloque(ZetarianoParser.SentenciaOBloqueContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaSwitch}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaSwitch(ZetarianoParser.SentenciaSwitchContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaSwitch}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaSwitch(ZetarianoParser.SentenciaSwitchContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#casoSwitch}.
	 * @param ctx the parse tree
	 */
	void enterCasoSwitch(ZetarianoParser.CasoSwitchContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#casoSwitch}.
	 * @param ctx the parse tree
	 */
	void exitCasoSwitch(ZetarianoParser.CasoSwitchContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#casoDefault}.
	 * @param ctx the parse tree
	 */
	void enterCasoDefault(ZetarianoParser.CasoDefaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#casoDefault}.
	 * @param ctx the parse tree
	 */
	void exitCasoDefault(ZetarianoParser.CasoDefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#literalCaso}.
	 * @param ctx the parse tree
	 */
	void enterLiteralCaso(ZetarianoParser.LiteralCasoContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#literalCaso}.
	 * @param ctx the parse tree
	 */
	void exitLiteralCaso(ZetarianoParser.LiteralCasoContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaFor}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaFor(ZetarianoParser.SentenciaForContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaFor}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaFor(ZetarianoParser.SentenciaForContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#forInit}.
	 * @param ctx the parse tree
	 */
	void enterForInit(ZetarianoParser.ForInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#forInit}.
	 * @param ctx the parse tree
	 */
	void exitForInit(ZetarianoParser.ForInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#declaracionVariableSinPuntoYComa}.
	 * @param ctx the parse tree
	 */
	void enterDeclaracionVariableSinPuntoYComa(ZetarianoParser.DeclaracionVariableSinPuntoYComaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#declaracionVariableSinPuntoYComa}.
	 * @param ctx the parse tree
	 */
	void exitDeclaracionVariableSinPuntoYComa(ZetarianoParser.DeclaracionVariableSinPuntoYComaContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void enterForUpdate(ZetarianoParser.ForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void exitForUpdate(ZetarianoParser.ForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#expresionLista}.
	 * @param ctx the parse tree
	 */
	void enterExpresionLista(ZetarianoParser.ExpresionListaContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#expresionLista}.
	 * @param ctx the parse tree
	 */
	void exitExpresionLista(ZetarianoParser.ExpresionListaContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaWhile}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaWhile(ZetarianoParser.SentenciaWhileContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaWhile}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaWhile(ZetarianoParser.SentenciaWhileContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaDoWhile}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaDoWhile(ZetarianoParser.SentenciaDoWhileContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaDoWhile}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaDoWhile(ZetarianoParser.SentenciaDoWhileContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaReturn}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaReturn(ZetarianoParser.SentenciaReturnContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaReturn}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaReturn(ZetarianoParser.SentenciaReturnContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaBreak}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaBreak(ZetarianoParser.SentenciaBreakContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaBreak}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaBreak(ZetarianoParser.SentenciaBreakContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#sentenciaContinue}.
	 * @param ctx the parse tree
	 */
	void enterSentenciaContinue(ZetarianoParser.SentenciaContinueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#sentenciaContinue}.
	 * @param ctx the parse tree
	 */
	void exitSentenciaContinue(ZetarianoParser.SentenciaContinueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expAditiva}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpAditiva(ZetarianoParser.ExpAditivaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expAditiva}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpAditiva(ZetarianoParser.ExpAditivaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expDecimal}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpDecimal(ZetarianoParser.ExpDecimalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expDecimal}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpDecimal(ZetarianoParser.ExpDecimalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expMultiplicativa}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpMultiplicativa(ZetarianoParser.ExpMultiplicativaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expMultiplicativa}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpMultiplicativa(ZetarianoParser.ExpMultiplicativaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expIncDecPrefijo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpIncDecPrefijo(ZetarianoParser.ExpIncDecPrefijoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expIncDecPrefijo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpIncDecPrefijo(ZetarianoParser.ExpIncDecPrefijoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expArregloLiteral}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpArregloLiteral(ZetarianoParser.ExpArregloLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expArregloLiteral}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpArregloLiteral(ZetarianoParser.ExpArregloLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expAcceso}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpAcceso(ZetarianoParser.ExpAccesoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expAcceso}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpAcceso(ZetarianoParser.ExpAccesoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expAnd}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpAnd(ZetarianoParser.ExpAndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expAnd}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpAnd(ZetarianoParser.ExpAndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expLlamadaPrintln}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpLlamadaPrintln(ZetarianoParser.ExpLlamadaPrintlnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expLlamadaPrintln}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpLlamadaPrintln(ZetarianoParser.ExpLlamadaPrintlnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expTernario}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpTernario(ZetarianoParser.ExpTernarioContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expTernario}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpTernario(ZetarianoParser.ExpTernarioContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expNuevoArreglo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpNuevoArreglo(ZetarianoParser.ExpNuevoArregloContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expNuevoArreglo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpNuevoArreglo(ZetarianoParser.ExpNuevoArregloContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expIndice}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpIndice(ZetarianoParser.ExpIndiceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expIndice}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpIndice(ZetarianoParser.ExpIndiceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expEntero}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpEntero(ZetarianoParser.ExpEnteroContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expEntero}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpEntero(ZetarianoParser.ExpEnteroContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expId}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpId(ZetarianoParser.ExpIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expId}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpId(ZetarianoParser.ExpIdContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expIgualdad}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpIgualdad(ZetarianoParser.ExpIgualdadContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expIgualdad}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpIgualdad(ZetarianoParser.ExpIgualdadContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expUnario}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpUnario(ZetarianoParser.ExpUnarioContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expUnario}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpUnario(ZetarianoParser.ExpUnarioContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expNuevoObjeto}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpNuevoObjeto(ZetarianoParser.ExpNuevoObjetoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expNuevoObjeto}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpNuevoObjeto(ZetarianoParser.ExpNuevoObjetoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expRelacional}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpRelacional(ZetarianoParser.ExpRelacionalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expRelacional}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpRelacional(ZetarianoParser.ExpRelacionalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expOr}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpOr(ZetarianoParser.ExpOrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expOr}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpOr(ZetarianoParser.ExpOrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expNulo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpNulo(ZetarianoParser.ExpNuloContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expNulo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpNulo(ZetarianoParser.ExpNuloContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expLlamadaMetodo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpLlamadaMetodo(ZetarianoParser.ExpLlamadaMetodoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expLlamadaMetodo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpLlamadaMetodo(ZetarianoParser.ExpLlamadaMetodoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expCadena}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpCadena(ZetarianoParser.ExpCadenaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expCadena}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpCadena(ZetarianoParser.ExpCadenaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expLlamadaReadln}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpLlamadaReadln(ZetarianoParser.ExpLlamadaReadlnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expLlamadaReadln}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpLlamadaReadln(ZetarianoParser.ExpLlamadaReadlnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expVerdadero}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpVerdadero(ZetarianoParser.ExpVerdaderoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expVerdadero}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpVerdadero(ZetarianoParser.ExpVerdaderoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expLlamadaPrint}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpLlamadaPrint(ZetarianoParser.ExpLlamadaPrintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expLlamadaPrint}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpLlamadaPrint(ZetarianoParser.ExpLlamadaPrintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expIncDecSufijo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpIncDecSufijo(ZetarianoParser.ExpIncDecSufijoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expIncDecSufijo}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpIncDecSufijo(ZetarianoParser.ExpIncDecSufijoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expParentesis}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpParentesis(ZetarianoParser.ExpParentesisContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expParentesis}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpParentesis(ZetarianoParser.ExpParentesisContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expThis}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpThis(ZetarianoParser.ExpThisContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expThis}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpThis(ZetarianoParser.ExpThisContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expCaracter}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpCaracter(ZetarianoParser.ExpCaracterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expCaracter}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpCaracter(ZetarianoParser.ExpCaracterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expFalso}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void enterExpFalso(ZetarianoParser.ExpFalsoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expFalso}
	 * labeled alternative in {@link ZetarianoParser#expresion}.
	 * @param ctx the parse tree
	 */
	void exitExpFalso(ZetarianoParser.ExpFalsoContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetarianoParser#argumentos}.
	 * @param ctx the parse tree
	 */
	void enterArgumentos(ZetarianoParser.ArgumentosContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetarianoParser#argumentos}.
	 * @param ctx the parse tree
	 */
	void exitArgumentos(ZetarianoParser.ArgumentosContext ctx);
}