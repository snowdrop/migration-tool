// Generated from Query.g4 by ANTLR 4.13.2

package dev.snowdrop.mtool.parser.antlr;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({ "all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape" })
public class QueryParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache = new PredictionContextCache();
    public static final int T__0 = 1, T__1 = 2, T__2 = 3, T__3 = 4, T__4 = 5, T__5 = 6, T__6 = 7, T__7 = 8, T__8 = 9,
            T__9 = 10, T__10 = 11, T__11 = 12, T__12 = 13, T__13 = 14, T__14 = 15, FIND = 16, IS = 17,
            AND = 18, OR = 19, ID = 20, QUOTED_STRING = 21, EQUALS = 22, DOT = 23, COMMA = 24, LPAREN = 25,
            RPAREN = 26, WS = 27;
    public static final int RULE_searchQuery = 0, RULE_operation = 1, RULE_clause = 2, RULE_assignmentOp = 3,
            RULE_valueOrPairs = 4, RULE_fileType = 5, RULE_symbol = 6, RULE_keyValuePair = 7,
            RULE_key = 8, RULE_value = 9;

    private static String[] makeRuleNames() {
        return new String[] {
                "searchQuery", "operation", "clause", "assignmentOp", "valueOrPairs",
                "fileType", "symbol", "keyValuePair", "key", "value"
        };
    }

    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[] {
                null, "'all'", "'JAVA'", "'java'", "'POM'", "'pom'", "'TEXT'", "'text'",
                "'PROPERTY'", "'property'", "'PROPERTIES'", "'properties'", "'YAML'",
                "'yaml'", "'JSON'", "'json'", null, "'is'", "'AND'", "'OR'", null, null,
                "'='", "'.'", "','", "'('", "')'"
        };
    }

    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[] {
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, "FIND", "IS", "AND", "OR", "ID", "QUOTED_STRING",
                "EQUALS", "DOT", "COMMA", "LPAREN", "RPAREN", "WS"
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
    public ATN getATN() {
        return _ATN;
    }

    public QueryParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SearchQueryContext extends ParserRuleContext {
        public OperationContext operation() {
            return getRuleContext(OperationContext.class, 0);
        }

        public SearchQueryContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_searchQuery;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterSearchQuery(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitSearchQuery(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitSearchQuery(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final SearchQueryContext searchQuery() throws RecognitionException {
        SearchQueryContext _localctx = new SearchQueryContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_searchQuery);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(20);
                operation(0);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class OperationContext extends ParserRuleContext {
        public OperationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_operation;
        }

        public OperationContext() {
        }

        public void copyFrom(OperationContext ctx) {
            super.copyFrom(ctx);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class OrOperationContext extends OperationContext {
        public List<OperationContext> operation() {
            return getRuleContexts(OperationContext.class);
        }

        public OperationContext operation(int i) {
            return getRuleContext(OperationContext.class, i);
        }

        public TerminalNode OR() {
            return getToken(QueryParser.OR, 0);
        }

        public OrOperationContext(OperationContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterOrOperation(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitOrOperation(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitOrOperation(this);
            else
                return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SimpleClauseContext extends OperationContext {
        public ClauseContext clause() {
            return getRuleContext(ClauseContext.class, 0);
        }

        public SimpleClauseContext(OperationContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterSimpleClause(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitSimpleClause(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitSimpleClause(this);
            else
                return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AndOperationContext extends OperationContext {
        public List<OperationContext> operation() {
            return getRuleContexts(OperationContext.class);
        }

        public OperationContext operation(int i) {
            return getRuleContext(OperationContext.class, i);
        }

        public TerminalNode AND() {
            return getToken(QueryParser.AND, 0);
        }

        public AndOperationContext(OperationContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterAndOperation(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitAndOperation(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitAndOperation(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final OperationContext operation() throws RecognitionException {
        return operation(0);
    }

    private OperationContext operation(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        OperationContext _localctx = new OperationContext(_ctx, _parentState);
        OperationContext _prevctx = _localctx;
        int _startState = 2;
        enterRecursionRule(_localctx, 2, RULE_operation, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                {
                    _localctx = new SimpleClauseContext(_localctx);
                    _ctx = _localctx;
                    _prevctx = _localctx;

                    setState(23);
                    clause();
                }
                _ctx.stop = _input.LT(-1);
                setState(33);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 1, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null)
                            triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(31);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 0, _ctx)) {
                                case 1: {
                                    _localctx = new AndOperationContext(new OperationContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_operation);
                                    setState(25);
                                    if (!(precpred(_ctx, 3)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                                    setState(26);
                                    match(AND);
                                    setState(27);
                                    operation(4);
                                }
                                    break;
                                case 2: {
                                    _localctx = new OrOperationContext(new OperationContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_operation);
                                    setState(28);
                                    if (!(precpred(_ctx, 2)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                                    setState(29);
                                    match(OR);
                                    setState(30);
                                    operation(3);
                                }
                                    break;
                            }
                        }
                    }
                    setState(35);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 1, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ClauseContext extends ParserRuleContext {
        public FileTypeContext fileType() {
            return getRuleContext(FileTypeContext.class, 0);
        }

        public TerminalNode FIND() {
            return getToken(QueryParser.FIND, 0);
        }

        public TerminalNode DOT() {
            return getToken(QueryParser.DOT, 0);
        }

        public SymbolContext symbol() {
            return getRuleContext(SymbolContext.class, 0);
        }

        public AssignmentOpContext assignmentOp() {
            return getRuleContext(AssignmentOpContext.class, 0);
        }

        public ValueOrPairsContext valueOrPairs() {
            return getRuleContext(ValueOrPairsContext.class, 0);
        }

        public ClauseContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_clause;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterClause(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitClause(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitClause(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ClauseContext clause() throws RecognitionException {
        ClauseContext _localctx = new ClauseContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_clause);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(37);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == FIND) {
                    {
                        setState(36);
                        match(FIND);
                    }
                }

                setState(40);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == T__0) {
                    {
                        setState(39);
                        match(T__0);
                    }
                }

                setState(42);
                fileType();
                setState(45);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 4, _ctx)) {
                    case 1: {
                        setState(43);
                        match(DOT);
                        setState(44);
                        symbol();
                    }
                        break;
                }
                setState(50);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 5, _ctx)) {
                    case 1: {
                        setState(47);
                        assignmentOp();
                        setState(48);
                        valueOrPairs();
                    }
                        break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AssignmentOpContext extends ParserRuleContext {
        public TerminalNode IS() {
            return getToken(QueryParser.IS, 0);
        }

        public TerminalNode EQUALS() {
            return getToken(QueryParser.EQUALS, 0);
        }

        public AssignmentOpContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_assignmentOp;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterAssignmentOp(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitAssignmentOp(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitAssignmentOp(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final AssignmentOpContext assignmentOp() throws RecognitionException {
        AssignmentOpContext _localctx = new AssignmentOpContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_assignmentOp);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(52);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 4325378L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ValueOrPairsContext extends ParserRuleContext {
        public ValueContext value() {
            return getRuleContext(ValueContext.class, 0);
        }

        public TerminalNode LPAREN() {
            return getToken(QueryParser.LPAREN, 0);
        }

        public List<KeyValuePairContext> keyValuePair() {
            return getRuleContexts(KeyValuePairContext.class);
        }

        public KeyValuePairContext keyValuePair(int i) {
            return getRuleContext(KeyValuePairContext.class, i);
        }

        public TerminalNode RPAREN() {
            return getToken(QueryParser.RPAREN, 0);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(QueryParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(QueryParser.COMMA, i);
        }

        public ValueOrPairsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_valueOrPairs;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterValueOrPairs(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitValueOrPairs(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitValueOrPairs(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ValueOrPairsContext valueOrPairs() throws RecognitionException {
        ValueOrPairsContext _localctx = new ValueOrPairsContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_valueOrPairs);
        int _la;
        try {
            setState(66);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case ID:
                case QUOTED_STRING:
                    enterOuterAlt(_localctx, 1); {
                    setState(54);
                    value();
                }
                    break;
                case LPAREN:
                    enterOuterAlt(_localctx, 2); {
                    setState(55);
                    match(LPAREN);
                    setState(56);
                    keyValuePair();
                    setState(61);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    while (_la == COMMA) {
                        {
                            {
                                setState(57);
                                match(COMMA);
                                setState(58);
                                keyValuePair();
                            }
                        }
                        setState(63);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                    }
                    setState(64);
                    match(RPAREN);
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FileTypeContext extends ParserRuleContext {
        public FileTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_fileType;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterFileType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitFileType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitFileType(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final FileTypeContext fileType() throws RecognitionException {
        FileTypeContext _localctx = new FileTypeContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_fileType);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(68);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 65532L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SymbolContext extends ParserRuleContext {
        public TerminalNode ID() {
            return getToken(QueryParser.ID, 0);
        }

        public SymbolContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_symbol;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterSymbol(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitSymbol(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitSymbol(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final SymbolContext symbol() throws RecognitionException {
        SymbolContext _localctx = new SymbolContext(_ctx, getState());
        enterRule(_localctx, 12, RULE_symbol);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(70);
                match(ID);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class KeyValuePairContext extends ParserRuleContext {
        public KeyContext key() {
            return getRuleContext(KeyContext.class, 0);
        }

        public TerminalNode EQUALS() {
            return getToken(QueryParser.EQUALS, 0);
        }

        public ValueContext value() {
            return getRuleContext(ValueContext.class, 0);
        }

        public KeyValuePairContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_keyValuePair;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterKeyValuePair(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitKeyValuePair(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitKeyValuePair(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final KeyValuePairContext keyValuePair() throws RecognitionException {
        KeyValuePairContext _localctx = new KeyValuePairContext(_ctx, getState());
        enterRule(_localctx, 14, RULE_keyValuePair);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(72);
                key();
                setState(73);
                match(EQUALS);
                setState(74);
                value();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class KeyContext extends ParserRuleContext {
        public TerminalNode QUOTED_STRING() {
            return getToken(QueryParser.QUOTED_STRING, 0);
        }

        public TerminalNode ID() {
            return getToken(QueryParser.ID, 0);
        }

        public KeyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_key;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterKey(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitKey(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitKey(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final KeyContext key() throws RecognitionException {
        KeyContext _localctx = new KeyContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_key);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(76);
                _la = _input.LA(1);
                if (!(_la == ID || _la == QUOTED_STRING)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ValueContext extends ParserRuleContext {
        public TerminalNode QUOTED_STRING() {
            return getToken(QueryParser.QUOTED_STRING, 0);
        }

        public TerminalNode ID() {
            return getToken(QueryParser.ID, 0);
        }

        public ValueContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_value;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).enterValue(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof QueryListener)
                ((QueryListener) listener).exitValue(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof QueryVisitor)
                return ((QueryVisitor<? extends T>) visitor).visitValue(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final ValueContext value() throws RecognitionException {
        ValueContext _localctx = new ValueContext(_ctx, getState());
        enterRule(_localctx, 18, RULE_value);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(78);
                _la = _input.LA(1);
                if (!(_la == ID || _la == QUOTED_STRING)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF)
                        matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
            case 1:
                return operation_sempred((OperationContext) _localctx, predIndex);
        }
        return true;
    }

    private boolean operation_sempred(OperationContext _localctx, int predIndex) {
        switch (predIndex) {
            case 0:
                return precpred(_ctx, 3);
            case 1:
                return precpred(_ctx, 2);
        }
        return true;
    }

    public static final String _serializedATN = "\u0004\u0001\u001bQ\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002" +
            "\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002" +
            "\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002" +
            "\b\u0007\b\u0002\t\u0007\t\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001" +
            "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001" +
            "\u0001\u0001\u0005\u0001 \b\u0001\n\u0001\f\u0001#\t\u0001\u0001\u0002" +
            "\u0003\u0002&\b\u0002\u0001\u0002\u0003\u0002)\b\u0002\u0001\u0002\u0001" +
            "\u0002\u0001\u0002\u0003\u0002.\b\u0002\u0001\u0002\u0001\u0002\u0001" +
            "\u0002\u0003\u00023\b\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001" +
            "\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0005\u0004<\b\u0004\n\u0004" +
            "\f\u0004?\t\u0004\u0001\u0004\u0001\u0004\u0003\u0004C\b\u0004\u0001\u0005" +
            "\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007" +
            "\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0000\u0001\u0002" +
            "\n\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0000\u0003\u0003\u0000" +
            "\u0001\u0001\u0011\u0011\u0016\u0016\u0001\u0000\u0002\u000f\u0001\u0000" +
            "\u0014\u0015N\u0000\u0014\u0001\u0000\u0000\u0000\u0002\u0016\u0001\u0000" +
            "\u0000\u0000\u0004%\u0001\u0000\u0000\u0000\u00064\u0001\u0000\u0000\u0000" +
            "\bB\u0001\u0000\u0000\u0000\nD\u0001\u0000\u0000\u0000\fF\u0001\u0000" +
            "\u0000\u0000\u000eH\u0001\u0000\u0000\u0000\u0010L\u0001\u0000\u0000\u0000" +
            "\u0012N\u0001\u0000\u0000\u0000\u0014\u0015\u0003\u0002\u0001\u0000\u0015" +
            "\u0001\u0001\u0000\u0000\u0000\u0016\u0017\u0006\u0001\uffff\uffff\u0000" +
            "\u0017\u0018\u0003\u0004\u0002\u0000\u0018!\u0001\u0000\u0000\u0000\u0019" +
            "\u001a\n\u0003\u0000\u0000\u001a\u001b\u0005\u0012\u0000\u0000\u001b " +
            "\u0003\u0002\u0001\u0004\u001c\u001d\n\u0002\u0000\u0000\u001d\u001e\u0005" +
            "\u0013\u0000\u0000\u001e \u0003\u0002\u0001\u0003\u001f\u0019\u0001\u0000" +
            "\u0000\u0000\u001f\u001c\u0001\u0000\u0000\u0000 #\u0001\u0000\u0000\u0000" +
            "!\u001f\u0001\u0000\u0000\u0000!\"\u0001\u0000\u0000\u0000\"\u0003\u0001" +
            "\u0000\u0000\u0000#!\u0001\u0000\u0000\u0000$&\u0005\u0010\u0000\u0000" +
            "%$\u0001\u0000\u0000\u0000%&\u0001\u0000\u0000\u0000&(\u0001\u0000\u0000" +
            "\u0000\')\u0005\u0001\u0000\u0000(\'\u0001\u0000\u0000\u0000()\u0001\u0000" +
            "\u0000\u0000)*\u0001\u0000\u0000\u0000*-\u0003\n\u0005\u0000+,\u0005\u0017" +
            "\u0000\u0000,.\u0003\f\u0006\u0000-+\u0001\u0000\u0000\u0000-.\u0001\u0000" +
            "\u0000\u0000.2\u0001\u0000\u0000\u0000/0\u0003\u0006\u0003\u000001\u0003" +
            "\b\u0004\u000013\u0001\u0000\u0000\u00002/\u0001\u0000\u0000\u000023\u0001" +
            "\u0000\u0000\u00003\u0005\u0001\u0000\u0000\u000045\u0007\u0000\u0000" +
            "\u00005\u0007\u0001\u0000\u0000\u00006C\u0003\u0012\t\u000078\u0005\u0019" +
            "\u0000\u00008=\u0003\u000e\u0007\u00009:\u0005\u0018\u0000\u0000:<\u0003" +
            "\u000e\u0007\u0000;9\u0001\u0000\u0000\u0000<?\u0001\u0000\u0000\u0000" +
            "=;\u0001\u0000\u0000\u0000=>\u0001\u0000\u0000\u0000>@\u0001\u0000\u0000" +
            "\u0000?=\u0001\u0000\u0000\u0000@A\u0005\u001a\u0000\u0000AC\u0001\u0000" +
            "\u0000\u0000B6\u0001\u0000\u0000\u0000B7\u0001\u0000\u0000\u0000C\t\u0001" +
            "\u0000\u0000\u0000DE\u0007\u0001\u0000\u0000E\u000b\u0001\u0000\u0000" +
            "\u0000FG\u0005\u0014\u0000\u0000G\r\u0001\u0000\u0000\u0000HI\u0003\u0010" +
            "\b\u0000IJ\u0005\u0016\u0000\u0000JK\u0003\u0012\t\u0000K\u000f\u0001" +
            "\u0000\u0000\u0000LM\u0007\u0002\u0000\u0000M\u0011\u0001\u0000\u0000" +
            "\u0000NO\u0007\u0002\u0000\u0000O\u0013\u0001\u0000\u0000\u0000\b\u001f" +
            "!%(-2=B";
    public static final ATN _ATN = new ATNDeserializer().deserialize(_serializedATN.toCharArray());
    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}