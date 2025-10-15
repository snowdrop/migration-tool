// Generated from Query.g4 by ANTLR 4.13.2

package dev.snowdrop.parser.antlr;

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
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, FIND=13, WHERE=14, AND=15, OR=16, ID=17, 
		QUOTED_STRING=18, EQUALS=19, DOT=20, COMMA=21, LPAREN=22, RPAREN=23, WS=24;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
			"T__9", "T__10", "T__11", "FIND", "WHERE", "AND", "OR", "ID", "QUOTED_STRING", 
			"EQUALS", "DOT", "COMMA", "LPAREN", "RPAREN", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'JAVA'", "'java'", "'POM'", "'pom'", "'TEXT'", "'text'", "'PROPERTY'", 
			"'property'", "'YAML'", "'yaml'", "'JSON'", "'json'", "'FIND'", "'WHERE'", 
			"'AND'", "'OR'", null, null, "'='", "'.'", "','", "'('", "')'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, "FIND", "WHERE", "AND", "OR", "ID", "QUOTED_STRING", "EQUALS", 
			"DOT", "COMMA", "LPAREN", "RPAREN", "WS"
		};
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
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Query.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\u0018\u00a8\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014"+
		"\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0005\u0010"+
		"\u0088\b\u0010\n\u0010\f\u0010\u008b\t\u0010\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0005\u0011\u0091\b\u0011\n\u0011\f\u0011\u0094\t\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013"+
		"\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016"+
		"\u0001\u0017\u0004\u0017\u00a3\b\u0017\u000b\u0017\f\u0017\u00a4\u0001"+
		"\u0017\u0001\u0017\u0000\u0000\u0018\u0001\u0001\u0003\u0002\u0005\u0003"+
		"\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015"+
		"\u000b\u0017\f\u0019\r\u001b\u000e\u001d\u000f\u001f\u0010!\u0011#\u0012"+
		"%\u0013\'\u0014)\u0015+\u0016-\u0017/\u0018\u0001\u0000\u0004\u0002\u0000"+
		"AZaz\u0004\u0000--09AZaz\u0002\u0000\'\'\\\\\u0003\u0000\t\n\r\r  \u00ab"+
		"\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000"+
		"\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000"+
		"\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000"+
		"\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011"+
		"\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015"+
		"\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0000\u0019"+
		"\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000\u0000\u0000\u001d"+
		"\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000\u0000\u0000\u0000!\u0001"+
		"\u0000\u0000\u0000\u0000#\u0001\u0000\u0000\u0000\u0000%\u0001\u0000\u0000"+
		"\u0000\u0000\'\u0001\u0000\u0000\u0000\u0000)\u0001\u0000\u0000\u0000"+
		"\u0000+\u0001\u0000\u0000\u0000\u0000-\u0001\u0000\u0000\u0000\u0000/"+
		"\u0001\u0000\u0000\u0000\u00011\u0001\u0000\u0000\u0000\u00036\u0001\u0000"+
		"\u0000\u0000\u0005;\u0001\u0000\u0000\u0000\u0007?\u0001\u0000\u0000\u0000"+
		"\tC\u0001\u0000\u0000\u0000\u000bH\u0001\u0000\u0000\u0000\rM\u0001\u0000"+
		"\u0000\u0000\u000fV\u0001\u0000\u0000\u0000\u0011_\u0001\u0000\u0000\u0000"+
		"\u0013d\u0001\u0000\u0000\u0000\u0015i\u0001\u0000\u0000\u0000\u0017n"+
		"\u0001\u0000\u0000\u0000\u0019s\u0001\u0000\u0000\u0000\u001bx\u0001\u0000"+
		"\u0000\u0000\u001d~\u0001\u0000\u0000\u0000\u001f\u0082\u0001\u0000\u0000"+
		"\u0000!\u0085\u0001\u0000\u0000\u0000#\u008c\u0001\u0000\u0000\u0000%"+
		"\u0097\u0001\u0000\u0000\u0000\'\u0099\u0001\u0000\u0000\u0000)\u009b"+
		"\u0001\u0000\u0000\u0000+\u009d\u0001\u0000\u0000\u0000-\u009f\u0001\u0000"+
		"\u0000\u0000/\u00a2\u0001\u0000\u0000\u000012\u0005J\u0000\u000023\u0005"+
		"A\u0000\u000034\u0005V\u0000\u000045\u0005A\u0000\u00005\u0002\u0001\u0000"+
		"\u0000\u000067\u0005j\u0000\u000078\u0005a\u0000\u000089\u0005v\u0000"+
		"\u00009:\u0005a\u0000\u0000:\u0004\u0001\u0000\u0000\u0000;<\u0005P\u0000"+
		"\u0000<=\u0005O\u0000\u0000=>\u0005M\u0000\u0000>\u0006\u0001\u0000\u0000"+
		"\u0000?@\u0005p\u0000\u0000@A\u0005o\u0000\u0000AB\u0005m\u0000\u0000"+
		"B\b\u0001\u0000\u0000\u0000CD\u0005T\u0000\u0000DE\u0005E\u0000\u0000"+
		"EF\u0005X\u0000\u0000FG\u0005T\u0000\u0000G\n\u0001\u0000\u0000\u0000"+
		"HI\u0005t\u0000\u0000IJ\u0005e\u0000\u0000JK\u0005x\u0000\u0000KL\u0005"+
		"t\u0000\u0000L\f\u0001\u0000\u0000\u0000MN\u0005P\u0000\u0000NO\u0005"+
		"R\u0000\u0000OP\u0005O\u0000\u0000PQ\u0005P\u0000\u0000QR\u0005E\u0000"+
		"\u0000RS\u0005R\u0000\u0000ST\u0005T\u0000\u0000TU\u0005Y\u0000\u0000"+
		"U\u000e\u0001\u0000\u0000\u0000VW\u0005p\u0000\u0000WX\u0005r\u0000\u0000"+
		"XY\u0005o\u0000\u0000YZ\u0005p\u0000\u0000Z[\u0005e\u0000\u0000[\\\u0005"+
		"r\u0000\u0000\\]\u0005t\u0000\u0000]^\u0005y\u0000\u0000^\u0010\u0001"+
		"\u0000\u0000\u0000_`\u0005Y\u0000\u0000`a\u0005A\u0000\u0000ab\u0005M"+
		"\u0000\u0000bc\u0005L\u0000\u0000c\u0012\u0001\u0000\u0000\u0000de\u0005"+
		"y\u0000\u0000ef\u0005a\u0000\u0000fg\u0005m\u0000\u0000gh\u0005l\u0000"+
		"\u0000h\u0014\u0001\u0000\u0000\u0000ij\u0005J\u0000\u0000jk\u0005S\u0000"+
		"\u0000kl\u0005O\u0000\u0000lm\u0005N\u0000\u0000m\u0016\u0001\u0000\u0000"+
		"\u0000no\u0005j\u0000\u0000op\u0005s\u0000\u0000pq\u0005o\u0000\u0000"+
		"qr\u0005n\u0000\u0000r\u0018\u0001\u0000\u0000\u0000st\u0005F\u0000\u0000"+
		"tu\u0005I\u0000\u0000uv\u0005N\u0000\u0000vw\u0005D\u0000\u0000w\u001a"+
		"\u0001\u0000\u0000\u0000xy\u0005W\u0000\u0000yz\u0005H\u0000\u0000z{\u0005"+
		"E\u0000\u0000{|\u0005R\u0000\u0000|}\u0005E\u0000\u0000}\u001c\u0001\u0000"+
		"\u0000\u0000~\u007f\u0005A\u0000\u0000\u007f\u0080\u0005N\u0000\u0000"+
		"\u0080\u0081\u0005D\u0000\u0000\u0081\u001e\u0001\u0000\u0000\u0000\u0082"+
		"\u0083\u0005O\u0000\u0000\u0083\u0084\u0005R\u0000\u0000\u0084 \u0001"+
		"\u0000\u0000\u0000\u0085\u0089\u0007\u0000\u0000\u0000\u0086\u0088\u0007"+
		"\u0001\u0000\u0000\u0087\u0086\u0001\u0000\u0000\u0000\u0088\u008b\u0001"+
		"\u0000\u0000\u0000\u0089\u0087\u0001\u0000\u0000\u0000\u0089\u008a\u0001"+
		"\u0000\u0000\u0000\u008a\"\u0001\u0000\u0000\u0000\u008b\u0089\u0001\u0000"+
		"\u0000\u0000\u008c\u0092\u0005\'\u0000\u0000\u008d\u0091\b\u0002\u0000"+
		"\u0000\u008e\u008f\u0005\\\u0000\u0000\u008f\u0091\t\u0000\u0000\u0000"+
		"\u0090\u008d\u0001\u0000\u0000\u0000\u0090\u008e\u0001\u0000\u0000\u0000"+
		"\u0091\u0094\u0001\u0000\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000"+
		"\u0092\u0093\u0001\u0000\u0000\u0000\u0093\u0095\u0001\u0000\u0000\u0000"+
		"\u0094\u0092\u0001\u0000\u0000\u0000\u0095\u0096\u0005\'\u0000\u0000\u0096"+
		"$\u0001\u0000\u0000\u0000\u0097\u0098\u0005=\u0000\u0000\u0098&\u0001"+
		"\u0000\u0000\u0000\u0099\u009a\u0005.\u0000\u0000\u009a(\u0001\u0000\u0000"+
		"\u0000\u009b\u009c\u0005,\u0000\u0000\u009c*\u0001\u0000\u0000\u0000\u009d"+
		"\u009e\u0005(\u0000\u0000\u009e,\u0001\u0000\u0000\u0000\u009f\u00a0\u0005"+
		")\u0000\u0000\u00a0.\u0001\u0000\u0000\u0000\u00a1\u00a3\u0007\u0003\u0000"+
		"\u0000\u00a2\u00a1\u0001\u0000\u0000\u0000\u00a3\u00a4\u0001\u0000\u0000"+
		"\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000\u0000"+
		"\u0000\u00a5\u00a6\u0001\u0000\u0000\u0000\u00a6\u00a7\u0006\u0017\u0000"+
		"\u0000\u00a70\u0001\u0000\u0000\u0000\u0005\u0000\u0089\u0090\u0092\u00a4"+
		"\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}