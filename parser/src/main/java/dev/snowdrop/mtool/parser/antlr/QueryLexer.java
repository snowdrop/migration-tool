// Generated from Query.g4 by ANTLR 4.13.2

package dev.snowdrop.mtool.parser.antlr;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class QueryLexer extends Lexer {
	static {
		RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION);
	}

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache = new PredictionContextCache();
	public static final int T__0 = 1, T__1 = 2, T__2 = 3, T__3 = 4, T__4 = 5, T__5 = 6, T__6 = 7, T__7 = 8, T__8 = 9,
			T__9 = 10, T__10 = 11, T__11 = 12, IS = 13, AND = 14, OR = 15, ID = 16, QUOTED_STRING = 17, EQUALS = 18,
			DOT = 19, COMMA = 20, LPAREN = 21, RPAREN = 22, WS = 23;
	public static String[] channelNames = {"DEFAULT_TOKEN_CHANNEL", "HIDDEN"};

	public static String[] modeNames = {"DEFAULT_MODE"};

	private static String[] makeRuleNames() {
		return new String[]{"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", "T__9", "T__10",
				"T__11", "IS", "AND", "OR", "ID", "QUOTED_STRING", "EQUALS", "DOT", "COMMA", "LPAREN", "RPAREN", "WS"};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[]{null, "'JAVA'", "'java'", "'POM'", "'pom'", "'TEXT'", "'text'", "'PROPERTY'", "'property'",
				"'YAML'", "'yaml'", "'JSON'", "'json'", "'is'", "'AND'", "'OR'", null, null, "'='", "'.'", "','", "'('",
				"')'"};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[]{null, null, null, null, null, null, null, null, null, null, null, null, null, "IS", "AND",
				"OR", "ID", "QUOTED_STRING", "EQUALS", "DOT", "COMMA", "LPAREN", "RPAREN", "WS"};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	public QueryLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
	}

	@Override
	public String getGrammarFileName() {
		return "Query.g4";
	}

	@Override
	public String[] getRuleNames() {
		return ruleNames;
	}

	@Override
	public String getSerializedATN() {
		return _serializedATN;
	}

	@Override
	public String[] getChannelNames() {
		return channelNames;
	}

	@Override
	public String[] getModeNames() {
		return modeNames;
	}

	@Override
	public ATN getATN() {
		return _ATN;
	}

	public static final String _serializedATN = "\u0004\u0000\u0017\u00a9\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"
			+ "\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"
			+ "\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"
			+ "\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"
			+ "\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"
			+ "\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"
			+ "\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014"
			+ "\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0001\u0000\u0001\u0000"
			+ "\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001"
			+ "\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"
			+ "\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"
			+ "\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005"
			+ "\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"
			+ "\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007"
			+ "\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"
			+ "\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"
			+ "\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001"
			+ "\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f"
			+ "\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e"
			+ "\u0001\u000e\u0001\u000f\u0001\u000f\u0005\u000f~\b\u000f\n\u000f\f\u000f"
			+ "\u0081\t\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0005\u0010"
			+ "\u0087\b\u0010\n\u0010\f\u0010\u008a\t\u0010\u0001\u0010\u0001\u0010\u0001"
			+ "\u0010\u0001\u0010\u0001\u0010\u0005\u0010\u0091\b\u0010\n\u0010\f\u0010"
			+ "\u0094\t\u0010\u0001\u0010\u0003\u0010\u0097\b\u0010\u0001\u0011\u0001"
			+ "\u0011\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0014\u0001"
			+ "\u0014\u0001\u0015\u0001\u0015\u0001\u0016\u0004\u0016\u00a4\b\u0016\u000b"
			+ "\u0016\f\u0016\u00a5\u0001\u0016\u0001\u0016\u0000\u0000\u0017\u0001\u0001"
			+ "\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f"
			+ "\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u001b\u000e\u001d\u000f"
			+ "\u001f\u0010!\u0011#\u0012%\u0013\'\u0014)\u0015+\u0016-\u0017\u0001\u0000"
			+ "\u0005\u0002\u0000AZaz\u0004\u0000--09AZaz\u0002\u0000\'\'\\\\\u0002\u0000"
			+ "\"\"\\\\\u0003\u0000\t\n\r\r  \u00af\u0000\u0001\u0001\u0000\u0000\u0000"
			+ "\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000"
			+ "\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000"
			+ "\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f"
			+ "\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013"
			+ "\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017"
			+ "\u0001\u0000\u0000\u0000\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b"
			+ "\u0001\u0000\u0000\u0000\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f"
			+ "\u0001\u0000\u0000\u0000\u0000!\u0001\u0000\u0000\u0000\u0000#\u0001\u0000"
			+ "\u0000\u0000\u0000%\u0001\u0000\u0000\u0000\u0000\'\u0001\u0000\u0000"
			+ "\u0000\u0000)\u0001\u0000\u0000\u0000\u0000+\u0001\u0000\u0000\u0000\u0000"
			+ "-\u0001\u0000\u0000\u0000\u0001/\u0001\u0000\u0000\u0000\u00034\u0001"
			+ "\u0000\u0000\u0000\u00059\u0001\u0000\u0000\u0000\u0007=\u0001\u0000\u0000"
			+ "\u0000\tA\u0001\u0000\u0000\u0000\u000bF\u0001\u0000\u0000\u0000\rK\u0001"
			+ "\u0000\u0000\u0000\u000fT\u0001\u0000\u0000\u0000\u0011]\u0001\u0000\u0000"
			+ "\u0000\u0013b\u0001\u0000\u0000\u0000\u0015g\u0001\u0000\u0000\u0000\u0017"
			+ "l\u0001\u0000\u0000\u0000\u0019q\u0001\u0000\u0000\u0000\u001bt\u0001"
			+ "\u0000\u0000\u0000\u001dx\u0001\u0000\u0000\u0000\u001f{\u0001\u0000\u0000"
			+ "\u0000!\u0096\u0001\u0000\u0000\u0000#\u0098\u0001\u0000\u0000\u0000%"
			+ "\u009a\u0001\u0000\u0000\u0000\'\u009c\u0001\u0000\u0000\u0000)\u009e"
			+ "\u0001\u0000\u0000\u0000+\u00a0\u0001\u0000\u0000\u0000-\u00a3\u0001\u0000"
			+ "\u0000\u0000/0\u0005J\u0000\u000001\u0005A\u0000\u000012\u0005V\u0000"
			+ "\u000023\u0005A\u0000\u00003\u0002\u0001\u0000\u0000\u000045\u0005j\u0000"
			+ "\u000056\u0005a\u0000\u000067\u0005v\u0000\u000078\u0005a\u0000\u0000"
			+ "8\u0004\u0001\u0000\u0000\u00009:\u0005P\u0000\u0000:;\u0005O\u0000\u0000"
			+ ";<\u0005M\u0000\u0000<\u0006\u0001\u0000\u0000\u0000=>\u0005p\u0000\u0000"
			+ ">?\u0005o\u0000\u0000?@\u0005m\u0000\u0000@\b\u0001\u0000\u0000\u0000"
			+ "AB\u0005T\u0000\u0000BC\u0005E\u0000\u0000CD\u0005X\u0000\u0000DE\u0005"
			+ "T\u0000\u0000E\n\u0001\u0000\u0000\u0000FG\u0005t\u0000\u0000GH\u0005"
			+ "e\u0000\u0000HI\u0005x\u0000\u0000IJ\u0005t\u0000\u0000J\f\u0001\u0000"
			+ "\u0000\u0000KL\u0005P\u0000\u0000LM\u0005R\u0000\u0000MN\u0005O\u0000"
			+ "\u0000NO\u0005P\u0000\u0000OP\u0005E\u0000\u0000PQ\u0005R\u0000\u0000"
			+ "QR\u0005T\u0000\u0000RS\u0005Y\u0000\u0000S\u000e\u0001\u0000\u0000\u0000"
			+ "TU\u0005p\u0000\u0000UV\u0005r\u0000\u0000VW\u0005o\u0000\u0000WX\u0005"
			+ "p\u0000\u0000XY\u0005e\u0000\u0000YZ\u0005r\u0000\u0000Z[\u0005t\u0000"
			+ "\u0000[\\\u0005y\u0000\u0000\\\u0010\u0001\u0000\u0000\u0000]^\u0005Y"
			+ "\u0000\u0000^_\u0005A\u0000\u0000_`\u0005M\u0000\u0000`a\u0005L\u0000"
			+ "\u0000a\u0012\u0001\u0000\u0000\u0000bc\u0005y\u0000\u0000cd\u0005a\u0000"
			+ "\u0000de\u0005m\u0000\u0000ef\u0005l\u0000\u0000f\u0014\u0001\u0000\u0000"
			+ "\u0000gh\u0005J\u0000\u0000hi\u0005S\u0000\u0000ij\u0005O\u0000\u0000"
			+ "jk\u0005N\u0000\u0000k\u0016\u0001\u0000\u0000\u0000lm\u0005j\u0000\u0000"
			+ "mn\u0005s\u0000\u0000no\u0005o\u0000\u0000op\u0005n\u0000\u0000p\u0018"
			+ "\u0001\u0000\u0000\u0000qr\u0005i\u0000\u0000rs\u0005s\u0000\u0000s\u001a"
			+ "\u0001\u0000\u0000\u0000tu\u0005A\u0000\u0000uv\u0005N\u0000\u0000vw\u0005"
			+ "D\u0000\u0000w\u001c\u0001\u0000\u0000\u0000xy\u0005O\u0000\u0000yz\u0005"
			+ "R\u0000\u0000z\u001e\u0001\u0000\u0000\u0000{\u007f\u0007\u0000\u0000"
			+ "\u0000|~\u0007\u0001\u0000\u0000}|\u0001\u0000\u0000\u0000~\u0081\u0001"
			+ "\u0000\u0000\u0000\u007f}\u0001\u0000\u0000\u0000\u007f\u0080\u0001\u0000"
			+ "\u0000\u0000\u0080 \u0001\u0000\u0000\u0000\u0081\u007f\u0001\u0000\u0000"
			+ "\u0000\u0082\u0088\u0005\'\u0000\u0000\u0083\u0087\b\u0002\u0000\u0000"
			+ "\u0084\u0085\u0005\\\u0000\u0000\u0085\u0087\t\u0000\u0000\u0000\u0086"
			+ "\u0083\u0001\u0000\u0000\u0000\u0086\u0084\u0001\u0000\u0000\u0000\u0087"
			+ "\u008a\u0001\u0000\u0000\u0000\u0088\u0086\u0001\u0000\u0000\u0000\u0088"
			+ "\u0089\u0001\u0000\u0000\u0000\u0089\u008b\u0001\u0000\u0000\u0000\u008a"
			+ "\u0088\u0001\u0000\u0000\u0000\u008b\u0097\u0005\'\u0000\u0000\u008c\u0092"
			+ "\u0005\"\u0000\u0000\u008d\u0091\b\u0003\u0000\u0000\u008e\u008f\u0005"
			+ "\\\u0000\u0000\u008f\u0091\t\u0000\u0000\u0000\u0090\u008d\u0001\u0000"
			+ "\u0000\u0000\u0090\u008e\u0001\u0000\u0000\u0000\u0091\u0094\u0001\u0000"
			+ "\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0092\u0093\u0001\u0000"
			+ "\u0000\u0000\u0093\u0095\u0001\u0000\u0000\u0000\u0094\u0092\u0001\u0000"
			+ "\u0000\u0000\u0095\u0097\u0005\"\u0000\u0000\u0096\u0082\u0001\u0000\u0000"
			+ "\u0000\u0096\u008c\u0001\u0000\u0000\u0000\u0097\"\u0001\u0000\u0000\u0000"
			+ "\u0098\u0099\u0005=\u0000\u0000\u0099$\u0001\u0000\u0000\u0000\u009a\u009b"
			+ "\u0005.\u0000\u0000\u009b&\u0001\u0000\u0000\u0000\u009c\u009d\u0005,"
			+ "\u0000\u0000\u009d(\u0001\u0000\u0000\u0000\u009e\u009f\u0005(\u0000\u0000"
			+ "\u009f*\u0001\u0000\u0000\u0000\u00a0\u00a1\u0005)\u0000\u0000\u00a1,"
			+ "\u0001\u0000\u0000\u0000\u00a2\u00a4\u0007\u0004\u0000\u0000\u00a3\u00a2"
			+ "\u0001\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5\u00a3"
			+ "\u0001\u0000\u0000\u0000\u00a5\u00a6\u0001\u0000\u0000\u0000\u00a6\u00a7"
			+ "\u0001\u0000\u0000\u0000\u00a7\u00a8\u0006\u0016\u0000\u0000\u00a8.\u0001"
			+ "\u0000\u0000\u0000\b\u0000\u007f\u0086\u0088\u0090\u0092\u0096\u00a5\u0001" + "\u0006\u0000\u0000";
	public static final ATN _ATN = new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}