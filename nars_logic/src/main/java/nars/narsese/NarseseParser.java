package nars.narsese;

import com.github.fge.grappa.Grappa;
import com.github.fge.grappa.annotations.Cached;
import com.github.fge.grappa.parsers.BaseParser;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import com.github.fge.grappa.stack.ValueStack;
import com.github.fge.grappa.support.Var;
import nars.*;
import nars.budget.Budget;
import nars.io.Texts;
import nars.nal.NALOperator;
import nars.nal.Sentence;
import nars.nal.Task;
import nars.nal.Truth;
import nars.nal.nal1.Inheritance;
import nars.nal.nal7.Interval;
import nars.nal.nal7.Tense;
import nars.nal.nal8.ImmediateOperation;
import nars.nal.nal8.Operation;
import nars.nal.stamp.Stamp;
import nars.nal.term.Atom;
import nars.nal.term.Compound;
import nars.nal.term.Term;
import nars.nal.term.Variable;
import nars.op.io.Echo;
import nars.op.io.PauseInput;

import java.util.*;
import java.util.function.Consumer;

import static nars.Symbols.IMAGE_PLACE_HOLDER;
import static nars.nal.NALOperator.*;

/**
 * NARese, syntax and language for interacting with a NAR in NARS.
 * https://code.google.com/p/open-nars/wiki/InputOutputFormat
 */
public class NarseseParser extends BaseParser<Object> {

    private final int level;

    //These should be set to something like RecoveringParseRunner for performance
    public final ParseRunner inputParser = new ListeningParseRunner2(Input());
    public final ParseRunner singleTaskParser = new ListeningParseRunner2(Task(true));

    //use a parameter or something to avoid this extra instance
    @Deprecated final ParseRunner singleTaskParserNonNewStamp = new ListeningParseRunner2(Task(false));

    public final ParseRunner singleTermParser = new ListeningParseRunner2(Term()); //new ErrorReportingParseRunner(Term(), 0);

    public Memory memory;

    protected NarseseParser() {
        this(8);
    }

    protected NarseseParser(int minNALLevel) {
        this.level = minNALLevel;
    }

    public boolean nal(final int n) {
        return n >= level;
    }

    public Rule Input() {
        return
                zeroOrMore(
                        sequence(
                                s(),
                                firstOf(
                                        Immediate(),
                                        Task(true),
                                        sequence("IN:",s(),Task(true),"\n") //temporary
                                )
                        )
                );
    }

    public Rule LineComment() {
        return sequence(
                firstOf(
                        string("//"),
                        "'",
                        sequence(string("***"), zeroOrMore('*')), //temporary
                        "OUT:"
                ),
                LineCommentEchoed()  );
    }

    @Cached
    public Rule LineCommentEchoed() {
        return sequence( zeroOrMore(noneOf("\n")),
                push(new Echo(match()) ), "\n");
    }

    @Cached
    public Rule PauseInput() {
        return sequence(Integer(),
                push( new PauseInput( (Integer) pop() ) ), "\n" );
    }
    @Cached
    public Rule Immediate() {
        return firstOf(
                LineComment(),
                PauseInput()
                /*Reset(),
                Volume(),*/
        );
    }

    public Rule Task(final boolean newStamp) {
        //TODO separate goal into an alternate form "!" because it does not use a tense
        Var<float[]> budget = new Var();
        Var<Character> punc = new Var();
        Var<Term> term = new Var();
        Var<Truth> truth = new Var();
        Var<Tense> tense = new Var(Tense.Eternal);

        return sequence(
                s(),

                optional(
                        sequence(Budget(), budget.set((float[]) pop()))
                ),


                Term(),
                term.set((Term) pop()),


                SentenceTypeChar(),
                punc.set(matchedChar()),


                optional(
                        s(), sequence(Tense(),
                        tense.set((Tense)pop()))
                ),

                optional(sequence(
                        s(), Truth(),
                        truth.set((Truth) pop())
                        )
                ),

                push(getTask(budget, term, punc, truth, tense, newStamp))

        );
    }

    Task getTask(Var<float[]> budget, Var<Term> term, Var<Character> punc, Var<Truth> truth, Var<Tense> tense, boolean newStamp) {

        char p = punc.get();

        Truth t = truth.get();
        if ((t == null) && ((p == Symbols.JUDGMENT) || (p == Symbols.GOAL)))
            t = new Truth.DefaultTruth(p);

        float[] b = budget.get();
        if (b != null && ((b.length == 0) || (Float.isNaN(b[0]))))
            b = null;
        Budget B = (b == null) ? new Budget(p, t) :
                b.length == 1 ? new Budget(b[0], p, t) :
                        b.length == 2 ? new Budget(b[0], b[1], t) :
                                new Budget(b[0], b[1], b[2]);

        Term content = term.get();
        if (!(content instanceof Compound)) {
            return null;
        }

        content = Sentence.termOrNull(content);
        if (content==null) return null;

        Tense te = tense.get();

        return new Task(new Sentence((Compound)content, p, t,
                getNewStamp(memory, newStamp, Stamp.UNPERCEIVED, te),
                false), B );

    }


    Rule Budget() {
        return sequence(
                Symbols.BUDGET_VALUE_MARK, ShortFloat(),
                firstOf(
                        BudgetPriorityDurabilityQuality(),
                        BudgetPriorityDurability(),
                        BudgetPriority()
                ),
                optional(Symbols.BUDGET_VALUE_MARK)
        );
    }

    boolean BudgetPriority() {
        return

                push(new float[]{(float) pop()}) //intermediate representation
        ;
    }

    Rule BudgetPriorityDurability() {
        return sequence(
                Symbols.VALUE_SEPARATOR, ShortFloat(),
                swap() && push(new float[]{(float) pop(), (float) pop()}) //intermediate representation
        );
    }

    Rule BudgetPriorityDurabilityQuality() {
        return sequence(
                Symbols.VALUE_SEPARATOR, ShortFloat(), Symbols.VALUE_SEPARATOR, ShortFloat(),
                swap() && push(new float[]{(float) pop(), (float) pop(), (float) pop()}) //intermediate representation
        );
    }

    Rule Tense() {
        return firstOf(
            sequence(Symbols.TENSE_PRESENT, push(Tense.Present)),
            sequence(Symbols.TENSE_PAST, push(Tense.Past)),
            sequence(Symbols.TENSE_FUTURE, push(Tense.Future))
        );
    }

    Rule Truth() {
        return sequence(
                Symbols.TRUTH_VALUE_MARK, ShortFloat(),

                firstOf(
                        sequence(
                            Symbols.VALUE_SEPARATOR, ShortFloat(),
                            swap() && push(new Truth.DefaultTruth((float) pop(), (float) pop()))
                        ),

                        push(new Truth.DefaultTruth((float) pop(), Global.DEFAULT_JUDGMENT_CONFIDENCE))

                ),

                optional(Symbols.TRUTH_VALUE_MARK) //tailing '%' is optional
        );
    }

    Rule ShortFloat() {
        return sequence(
                sequence(
                        optional(digit()),
                        optional('.', oneOrMore(digit()))
                ),
                push(Texts.f(matchOrDefault("NaN"), 0, 1f))
        );
    }

    Rule Integer() {
        return sequence(
                oneOrMore(digit()),
                push(Integer.parseInt(matchOrDefault("NaN")))
        );
    }

    Rule Number() {

        return sequence(
                sequence(
                        optional('-'),
                        oneOrMore(digit()),
                        optional('.', oneOrMore(digit()))
                ),
                push(Float.parseFloat(matchOrDefault("NaN")))
        );
    }

    Rule SentenceTypeChar() {
        return anyOf(".?!@");
    }

//    /**
//     * copula, statement, relation
//     */
//    Rule Copula() {
//            /*<copula> ::= "-->"                              // inheritance
//                        | "<->"                              // similarity
//                        | "{--"                              // instance
//                        | "--]"                              // property
//                        | "{-]"                              // instance-property
//                        | "==>"                              // implication
//                        | "=/>"                              // (predictive implication)
//                        | "=|>"                              // (concurrent implication)
//                        | "=\>"                              // (retrospective implication)
//                        | "<=>"                              // equivalence
//                        | "</>"                              // (predictive equivalence)
//                        | "<|>"                              // (concurrent equivalence)*/
//
//        /**
//         * ??
//         *   :- (apply, prolog implication)
//         *   -: (reverse apply)
//         */
//        //TODO use separate rules for each so a parse can identify them
//        return sequence(String.valueOf(NALOperator.STATEMENT_OPENER), StatementContent(), String.valueOf(NALOperator.STATEMENT_CLOSER));
//    }


//    Rule StatementContent() {
//        return sequence(sequence(s(), Term(), s(), CopulaOperator(), s(), Term(), s()),
//                push(getTerm((Term) pop(), (NALOperator) pop(), (Term) pop()))
//                //push(nextTermVector()) //((Term) pop(), (NALOperator) pop(), (Term) pop()))
//        );
//    }

//    Rule CopulaOperator() {
//        NALOperator[] ops = getCopulas();
//        Rule[] copulas = new Rule[ops.length];
//        for (int i = 0; i < ops.length; i++) {
//            copulas[i] = string(ops[i].symbol);
//        }
//        return sequence(
//                firstOf(copulas),
//                push(Symbols.getOperator(match()))
//        );
//    }

//    public NALOperator[] getCopulas() {
//        switch (level) {
//            case 1:
//                return new NALOperator[]{
//                        INHERITANCE
//                };
//            case 2:
//                return new NALOperator[]{
//                        INHERITANCE,
//                        SIMILARITY, PROPERTY, INSTANCE, INSTANCE_PROPERTY
//                };
//
//            //TODO case 5..6.. without temporal equiv &  impl..
//
//            default:
//                return new NALOperator[]{
//                        INHERITANCE,
//                        SIMILARITY, PROPERTY, INSTANCE, INSTANCE_PROPERTY,
//                        IMPLICATION,
//                        EQUIVALENCE,
//                        IMPLICATION_AFTER, IMPLICATION_BEFORE, IMPLICATION_WHEN,
//                        EQUIVALENCE_AFTER, EQUIVALENCE_WHEN
//                };
//        }
//    }

    static Term getTerm(Term predicate, NALOperator op, Term subject) {
        return Memory.term(op, subject, predicate);
    }

    Rule NonOperationTerm() {
        return Term(false);
    }

    Rule Term() {
        return Term(true);
    }

    @Cached
    Rule Term(boolean includeOperation) {
        /*
                 <term> ::= <word>                             // an atomic constant term
                        | <variable>                         // an atomic variable term
                        | <compound-term>                    // a term with internal structure
                        | <statement>                        // a statement can serve as a term
        */

        return sequence(
                s(),
                firstOf(

                        QuotedMultilineLiteral(),
                        QuotedLiteral(),

                        sequence(
                                includeOperation,
                                NonOperationTerm(),
                                EmptyOperationParens()
                        ),

                        //Functional form of an Operation, ex: operate(p1,p2), TODO move to FunctionalOperationTerm() rule
                        sequence(
                                includeOperation,
                                NonOperationTerm(),
                                NALOperator.COMPOUND_TERM_OPENER.symbol,
                                MultiArgTerm(NALOperator.OPERATION, NALOperator.COMPOUND_TERM_CLOSER, false, false, false, true)
                        ),


                        sequence( NALOperator.STATEMENT_OPENER.symbol,
                                MultiArgTerm(null, NALOperator.STATEMENT_CLOSER, false, true, true, false)
                        ),

                        Variable(),
                        Interval(),
                        ImageIndex(),


//                        //negation shorthand
//                        sequence(NALOperator.NEGATION.symbol, s(), Term(), push(Negation.make(term(pop())))),


                        sequence(
                                NALOperator.SET_EXT_OPENER.symbol,
                                MultiArgTerm(NALOperator.SET_EXT_OPENER, NALOperator.SET_EXT_CLOSER, false, false, false)
                        ),

                        sequence(
                                NALOperator.SET_INT_OPENER.symbol,
                                MultiArgTerm(NALOperator.SET_INT_OPENER, NALOperator.SET_INT_CLOSER, false, false, false)
                        ),



                        sequence( NALOperator.COMPOUND_TERM_OPENER.symbol,
                                firstOf(

                                        MultiArgTerm(null, NALOperator.COMPOUND_TERM_CLOSER, true, false, false, false),

                                        //default to product if no operator specified in ( )
                                        MultiArgTerm(NALOperator.PRODUCT, NALOperator.COMPOUND_TERM_CLOSER, false, false, false, false),

                                        MultiArgTerm(null, NALOperator.COMPOUND_TERM_CLOSER, false, true, true, false)
                                )
                        ),



//                        sequence( NALOperator.COMPOUND_TERM_OPENER.symbol,
//
//                        ),
//
//
//
//                        sequence( NALOperator.COMPOUND_TERM_OPENER.symbol,
//                        ),

                        NamespacedAtom(),
                        Atom()

                ),

                push(term(pop())),

                s()
        );
    }


    final static String invalidAtomCharacters = " ,.!?" + Symbols.INTERVAL_PREFIX + "<>-=*|&()<>[]{}%#$@\'\"\t\n";

    /**
     * an atomic term, returns a String because the result may be used as a Variable name
     */
    Rule Atom() {
        return sequence(
                oneOrMore(noneOf(invalidAtomCharacters)),
                push(match())
        );
    }

    /**
     * MACRO: namespace.x    becomes    <x --> namespace>
     */
    Rule NamespacedAtom() {
        return sequence(Atom(), '.', Atom(), push(Inheritance.make(Atom.the(pop()), Atom.the(pop()))));
    }

    public static Stamp getNewStamp(Memory memory, boolean newStamp, long creationTime, Tense tense) {
        return new Stamp(
                newStamp ? new long[] { memory.newStampSerial() } : new long[] { /* blank */ },
                memory, creationTime, tense);
    }

    /** creates a parser that is not associated with a memory; it will not parse any operator terms (which are registered with a Memory instance) */
    public static NarseseParser newParser() {
        return newParser((Memory)null);
    }


    final static Atom imageIndexTerm = Atom.theCached(String.valueOf(IMAGE_PLACE_HOLDER));

    Rule ImageIndex() {
        return sequence('_', push(imageIndexTerm));
    }

    Rule QuotedLiteral() {
        return sequence(dquote(), AnyString(), push('\"' + match() + '\"'), dquote());
    }

    Rule QuotedMultilineLiteral() {
        return sequence(
                dquote(), dquote(), dquote(),
                AnyString(), push('\"' + match() + '\"'),
                dquote(), dquote(), dquote()
        );
    }


    Rule AnyString() {
        //TODO handle \" escape
        return oneOrMore(noneOf("\""));
    }



    Rule Interval() {
        return sequence(Symbols.INTERVAL_PREFIX, sequence(oneOrMore(digit()), push(match()),
                push(Interval.interval(-1 + Texts.i((String) pop())))
        ));
    }

    final static char[] variables = new char[] { Symbols.VAR_INDEPENDENT, Symbols.VAR_DEPENDENT, Symbols.VAR_QUERY };

    Rule Variable() {
        /*
           <variable> ::= "$"<word>                          // independent variable
                        | "#"[<word>]                        // dependent variable
                        | "?"[<word>]                        // query variable in question
        */
        return sequence(
                anyOf(variables),
                push(match()), Atom(), swap(),
                    push(new Variable(pop() + (String) pop(), true)
                )

        );
    }

    //Rule CompoundTerm() {
        /*
         <compound-term> ::= "{" <term> {","<term>} "}"         // extensional set
                        | "[" <term> {","<term>} "]"         // intensional set
                        | "(&," <term> {","<term>} ")"       // extensional intersection
                        | "(|," <term> {","<term>} ")"       // intensional intersection
                        | "(*," <term> {","<term>} ")"       // product
                        | "(/," <term> {","<term>} ")"       // extensional image
                        | "(\," <term> {","<term>} ")"       // intensional image
                        | "(||," <term> {","<term>} ")"      // disjunction
                        | "(&&," <term> {","<term>} ")"      // conjunction
                        | "(&/," <term> {","<term>} ")"      // (sequential events)
                        | "(&|," <term> {","<term>} ")"      // (parallel events)
                        | "(--," <term> ")"                  // negation
                        | "(-," <term> "," <term> ")"        // extensional difference
                        | "(~," <term> "," <term> ")"        // intensional difference
        
        */

    //}

    Rule AnyOperator() {
        return sequence(firstOf(


                        INHERITANCE.symbol,


                        SIMILARITY.symbol,

                        PROPERTY.symbol,
                        INSTANCE.symbol,
                        INSTANCE_PROPERTY.symbol,

                        NEGATION.symbol,

                        IMPLICATION.symbol,
                        EQUIVALENCE.symbol,
                        IMPLICATION_AFTER.symbol, IMPLICATION_BEFORE.symbol, IMPLICATION_WHEN.symbol,
                        EQUIVALENCE_AFTER.symbol, EQUIVALENCE_WHEN.symbol,
                        DISJUNCTION.symbol,
                        CONJUNCTION.symbol,
                        SEQUENCE.symbol,
                        PARALLEL.symbol,

                        anyOf(
                                INTERSECTION_EXT.symbol +
                                        INTERSECTION_INT.symbol +
                                        DIFFERENCE_EXT.symbol +
                                        DIFFERENCE_INT.symbol + PRODUCT.symbol + IMAGE_EXT.symbol + IMAGE_INT.symbol
                        )


                        //OPERATION.ch
                ),
                push(Symbols.getOperator(match()))
        );
    }

    Rule CompoundOperator() {
        return sequence(
                trie(
                        NALOperator.NEGATION.symbol,
                        NALOperator.DISJUNCTION.symbol,
                        NALOperator.CONJUNCTION.symbol,
                        NALOperator.SEQUENCE.symbol,
                        NALOperator.PARALLEL.symbol,
                        NALOperator.DIFFERENCE_EXT.symbol,
                        NALOperator.DIFFERENCE_INT.symbol,
                        NALOperator.INTERSECTION_EXT.symbol,
                        NALOperator.INTERSECTION_INT.symbol,
                        NALOperator.PRODUCT.symbol,
                        NALOperator.IMAGE_EXT.symbol,
                        NALOperator.IMAGE_INT.symbol
                        //NALOperator.OPERATION.ch
                ),
                push(Symbols.getOperator(match()))
        );
    }

    /**
     * those compound operators which can take 2 arguments (should be everything except negation)
     */
    Rule CompoundOperator2() {
        return sequence(
                trie(
                        NALOperator.DISJUNCTION.symbol,
                        NALOperator.CONJUNCTION.symbol,
                        NALOperator.SEQUENCE.symbol,
                        NALOperator.PARALLEL.symbol,
                        NALOperator.DIFFERENCE_EXT.symbol,
                        NALOperator.DIFFERENCE_INT.symbol,
                        NALOperator.INTERSECTION_EXT.symbol,
                        NALOperator.INTERSECTION_INT.symbol,
                        NALOperator.PRODUCT.symbol,
                        NALOperator.IMAGE_EXT.symbol,
                        NALOperator.IMAGE_INT.symbol
                ),
                push(Symbols.getOperator(match()))
        );
    }


    Rule ArgSep() {
        return sequence(s(), String.valueOf(Symbols.ARGUMENT_SEPARATOR));

        /*
        return firstOf(
                //check the ' , ' comma separated first, it is more complex
                sequence(s(), String.valueOf(Symbols.ARGUMENT_SEPARATOR), s()),


                //then allow plain whitespace to function as a term separator?
                s()
        );*/
    }

    @Cached
    Rule MultiArgTerm(NALOperator open, NALOperator close, boolean allowInitialOp, boolean allowInternalOp, boolean allowSpaceToSeparate) {
        return MultiArgTerm(open, /*open, */close, allowInitialOp, allowInternalOp, allowSpaceToSeparate, false);
    }

    boolean OperationPrefixTerm() {
        return push( new Object[] { termable(pop()), (Operation.class) } );
    }

    /**
     * list of terms prefixed by a particular compound term operate
     */
    @Cached
    Rule MultiArgTerm(NALOperator defaultOp, /*NALOperator open, */NALOperator close, boolean initialOp, boolean allowInternalOp, boolean spaceSeparates, boolean operatorPrecedes) {


        return sequence(

                operatorPrecedes ?  OperationPrefixTerm() : push(Compound.class),

                //open != null ? sequence(open.ch, s()) : s(),

                initialOp ? AnyOperator() : Term(),

                spaceSeparates ?

                        sequence( s(), AnyOperator(), s(), Term() )

                        :

                        zeroOrMore(sequence(
                            spaceSeparates ? s() : ArgSep(),
                            allowInternalOp ? AnyOperatorOrTerm() : Term()
                        )),

                close != null ? sequence(s(), close.ch) : s(),

                push(nextTermVector(defaultOp, allowInternalOp))
        );
    }

    /**
     * operation()
     */
    Rule EmptyOperationParens() {
        return sequence(

                OperationPrefixTerm(),

                s(), NALOperator.COMPOUND_TERM_OPENER.ch, s(), NALOperator.COMPOUND_TERM_CLOSER.ch,

                push(nextTermVector(NALOperator.OPERATION, false))
        );
    }

    Rule AnyOperatorOrTerm() {
        return firstOf(AnyOperator(), Term());
    }


    /** pass-through; the object is potentially a term but don't create it yet */
    Object termable(Object o) {
        return o;
    }

    Object term(Object o) {
        if (o instanceof Term) return ((Term)o);
        if (o instanceof String) {
            String s= (String)o;
            return Atom.the(s);
        }
        throw new RuntimeException(o + " is not a term");
    }

    /**
     * produce a term from the terms (& <=1 NALOperator's) on the value stack
     */
    Term nextTermVector(NALOperator op /*default */, boolean allowInternalOp) {



        //System.err.println(getContext().getValueStack());

        ValueStack<Object> stack = getContext().getValueStack();

        List<Term> vectorterms = Global.newArrayList(stack.size() + 1);

        while (!stack.isEmpty()) {
            Object p = pop();

            if (p instanceof Object[]) {
                //it's an array so unpack by pushing everything back onto the stack except the last item which will be used as normal below
                Object[] pp = (Object[])p;
                if (pp.length > 1) {
                    for (int i = pp.length-1; i >= 1; i--) {
                        stack.push(pp[i]);
                    }
                }

                p = pp[0];
            }

            if (p == Operation.class) {
                op = OPERATION;
                break;
            }

            if (p == Compound.class) break; //beginning of stack frame for this term




            if (p instanceof String) {
                Term t = Atom.the((String) p);
                vectorterms.add(t);
            } else if (p instanceof Term) {
                Term t = (Term) p;
                vectorterms.add(t);
            } else if (p instanceof NALOperator) {

                if (op != null) {
                    if ((!allowInternalOp) && (!p.equals(op)))
                        throw new RuntimeException("Internal operator " + p + " not allowed here; default op=" + op);

                    throw new InvalidInputException("Too many operators involved: " + op + "," + p + " in " + stack + ":" + vectorterms);
                }

                op = (NALOperator)p;
            }
        }


        if (vectorterms.isEmpty()) return null;

        int v = vectorterms.size();

        Collections.reverse(vectorterms);

//        if ((op == null || op == PRODUCT) && (vectorterms.get(0) instanceof Operator)) {
//            op = NALOperator.OPERATION;
//        }

        if (op == null) op = NALOperator.PRODUCT;



        if (op == OPERATION) {
            final Term self = memory.self();
            if (!vectorterms.isEmpty() && !vectorterms.get(vectorterms.size()-1).equals(self))
                vectorterms.add(self); //SELF in final argument
            Term[] va = vectorterms.toArray(new Term[vectorterms.size()]);
            return Operation.make(va);
        }
        else {
            Term[] va = vectorterms.toArray(new Term[vectorterms.size()]);
            return Memory.term(op, va);
        }
    }



    /**
     * whitespace, optional
     */
    Rule s() {
        return zeroOrMore(anyOf(" \t\f\n"));
    }

    public static NarseseParser newParser(NAR n) {
        return newParser(n.memory);
    }

    public static NarseseParser newParser(Memory m) {
        NarseseParser np = Grappa.createParser(NarseseParser.class);
        np.memory = m;
        return np;
    }


    public void parse(String input, Collection<? super Task> c) {
        parse(input, t -> {
            c.add(t);
        });
    }

    public void parse(String input, Consumer<? super Task> c) {
        parse(input, true, c);
    }

    /**
     * parse a series of tasks
     */
    public void parse(String input, boolean newStamp, Consumer<? super Task> c) {
        ParsingResult r = inputParser.run(input);
        int size = r.getValueStack().size();

        if (size == 0) {
            c.accept(new Echo(Events.ERR.class, "Unrecognized input: " + input).newTask());
            return;
        }

        for (int i = size-1; i >= 0; i--) {
            Object o = r.getValueStack().peek(i);

            if (o instanceof Task)
                c.accept((Task) o);
            else if (o instanceof ImmediateOperation) {
                c.accept( ((ImmediateOperation)o).newTask() );
            }
            else {
                c.accept(new Echo(Echo.class, o.toString()).newTask());
                //throw new RuntimeException("unrecognized input result: " + o);
            }
        }

        r.getValueStack().clear();

//        r.getValueStack().iterator().forEachRemaining(x -> {
//            if (x instanceof Task)
//                c.accept((Task) x);
//            else {
//                throw new RuntimeException("Unknown parse result: " + x + " (" + x.getClass() + ')');
//            }
//        });
    }

    /**
     * parse one task
     */
    @Deprecated public Task parseTask(String input, boolean newStamp) throws InvalidInputException {
        ParsingResult r = null;
        try {
            input = input.trim();
            if (newStamp)
                r = singleTaskParser.run(input);
            else
                r = singleTaskParserNonNewStamp.run(input);
        }
        catch (Throwable ge) {
            throw new InvalidInputException(ge.toString() + " " + ge.getCause() + ": parsing: " + input);
        }

        if (r == null)
            throw new InvalidInputException("null parse: " + input);

        Iterator ir = r.getValueStack().iterator();
        if (ir.hasNext()) {
            Object x = ir.next();
            if (x instanceof Task)
                return (Task) x;
        }


        throw newParseException(input, r);
    }

    /**
     * parse one term
     */
    public <T extends Term> T parseTerm(String input) throws InvalidInputException {
        ParsingResult r = singleTermParser.run(input);


        if (!r.getValueStack().isEmpty()) {

            Object x = r.getValueStack().iterator().next();
            if (x instanceof String)
                x = Atom.the((String) x);

            if (x != null) {
                try {
                    return (T) x;
                } catch (ClassCastException cce) {
                    throw new InvalidInputException("Term type mismatch: " + x.getClass(), cce);
                }
            }
        }

        throw newParseException(input, r);
    }

    public static InvalidInputException newParseException(String input, ParsingResult r) {

        //if (!r.isSuccess()) {
            return new InvalidInputException("input: " + input + " (" + r.toString() + ')');
        //}
//        if (r.parseErrors.isEmpty())
//            return new InvalidInputException("No parse result for: " + input);
//
//        String all = "\n";
//        for (Object o : r.getParseErrors()) {
//            ParseError pe = (ParseError)o;
//            all += pe.getClass().getSimpleName() + ": " + pe.getErrorMessage() + " @ " + pe.getStartIndex() + "\n";
//        }
//        return new InvalidInputException(all + " for input: " + input);
    }


//    /**
//     * interactive parse test
//     */
//    public static void main(String[] args) {
//        NAR n = new NAR(new Default());
//        NarseseParser p = NarseseParser.newParser(n);
//
//        Scanner sc = new Scanner(System.in);
//
//        String input = null; //"<a ==> b>. %0.00;0.9%";
//
//        while (true) {
//            if (input == null)
//                input = sc.nextLine();
//
//            ParseRunner rpr = new ListeningParseRunner<>(p.Input());
//            //TracingParseRunner rpr = new TracingParseRunner(p.Input());
//
//            ParsingResult r = rpr.run(input);
//
//            //p.printDebugResultInfo(r);
//            input = null;
//        }
//
//    }

//    public void printDebugResultInfo(ParsingResult r) {
//
//        System.out.println("valid? " + (r.isSuccess() && (r.getParseErrors().isEmpty())));
//        r.getValueStack().iterator().forEachRemaining(x -> System.out.println("  " + x.getClass() + ' ' + x));
//
//        for (Object e : r.getParseErrors()) {
//            if (e instanceof InvalidInputError) {
//                InvalidInputError iie = (InvalidInputError) e;
//                System.err.println(e);
//                if (iie.getErrorMessage() != null)
//                    System.err.println(iie.getErrorMessage());
//                for (MatcherPath m : iie.getFailedMatchers()) {
//                    System.err.println("  ?-> " + m);
//                }
//                System.err.println(" at: " + iie.getStartIndex() + " to " + iie.getEndIndex());
//            } else {
//                System.err.println(e);
//            }
//
//        }
//
//        System.out.println(printNodeTree(r));
//
//    }


}
