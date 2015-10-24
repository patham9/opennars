package nars.nal.nal7;

import nars.NAR;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.narsese.NarseseParser;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;


public class SequenceParallelTest {
    final Terminal t = new Terminal();

    @Test public void testEmbeddedSequence() {

        String es = "(&/, b, /10, c)";
        Sequence e = t.term(es);
        //System.out.println(es + "\n" + e);
        assertEquals( 10, e.duration() );
        assertEquals( es, e.toString() );

        String ts = "(&/, a, " + es + ", /1, d)";
        Sequence s = t.term(ts);
        assertEquals( 11, s.duration() );
        assertEquals( ts, s.toString() );

    }

    @Test public void testEmbeddedParallel() {

        String fs = "(&|, b, c, /10)";
        Parallel f = t.term(fs);

        String es = "(&|, b, /10, c)";
        Parallel e = t.term(es);

        assertEquals(e, f); //commutative
        assertEquals(fs, e.toString()); //interval at end

        assertEquals(10, e.duration());


        String ts = "(&|, a, " + fs + ", /5)";
        Parallel s = t.term(ts);

        assertEquals(ts, s.toString());

        assertEquals(10, s.duration()); //maximum contained duration = 10

    }

    @Test public void testSequenceBytes() {
        //TODO
    }
    @Test public void testParallelBytes() {
        //TODO
    }
    @Test public void testSemiDuplicateParallels() {
        String ts = "(&&, (&|, x, /3), (&|, x, /1))";
        Compound c = t.term(ts);

        //TODO decide if this is correct handling
        assertEquals(1, c.length());
        assertEquals(Parallel.class, c.getClass());
        assertEquals(2, ((Parallel)c).duration()); //interpolated duration
    }
    @Test public void testSemiDuplicateSequences() {
        String ts = "(&&, (&/, x, /3), (&/, x, /1))";
        Compound c = t.term(ts);

        //TODO decide if this is correct handling
        assertEquals(1, c.length());
        assertEquals(Sequence.class, c.getClass());
        assertEquals(2, ((Sequence)c).duration()); //interpolated duration
    }

    @Test public void testEmbeddedParallelInSequence() {
    }
    @Test public void testEmbeddedSequenceInParallel() {
    }

    @Test public void testEquivalentSequencesAndParallels() {

        assertEqualTerms("(&/, x, /0)", "(&/, x)");
        assertEqualTerms("(&|, x, /0)", "(&|, x)");
        assertEqualTerms("(&|, x, /3, /5)", "(&|, x, /5)");
    }

    private  void assertEqualTerms(String abnormal, String normalized) {
        Term ta = t.term(abnormal);
        Term tb = t.term(normalized);
        assertEquals(ta, tb);
        assertEquals(ta.toString(), tb.toString());
        assertEquals(normalized, tb.toString());
        assertArrayEquals(ta.bytes(), tb.bytes());
    }


    @Test public void testChangingDuration() {
        //TODO test: tasks formed by a NAR with a duration that is being changed reflect these changes

    }
    @Test public void testParallel() {
        String seq = "(&|, <a-->b>, <a-->b>, <b-->c> )";

        Parallel x = NarseseParser.the().term(seq);

        assertEquals(2, x.length());

    }

    @Test public void testConstuction() {
        NAR nar = new Default();

        String seq = "(&/, /1, a, /2, b)";
        Sequence s = nar.term(seq);
        assertNotNull(s);
        assertNotNull(s.intervals());
        assertEquals("only non-interval terms are allowed as subterms", 2, s.length());

        String ss = s.toString();


        assertEquals(s.length() + 1, s.intervals().length);
        assertEquals("[1, 2, 0]", Arrays.toString(s.intervals()));
        assertEquals(3, s.duration());

        assertEquals("output matches input", seq, ss);

    }

    @Test public void testSingleTermSequence() {
        NAR nar = new Default();
        Term x = nar.term("(&/, a)");
        assertNotNull(x);
        assertEquals(Sequence.class, x.getClass());
    }

    @Test public void testSequenceToString() {
        NAR nar = new Default();

        testSeqTermString(nar, "(&/, a, /1, b)");
        //testSeqTermString(nar, "(&/, a, /2, b, /4)");
        testSeqTermString(nar, "(&/, a, /3, b, /5, c, /10, d)");
    }

    private void testSeqTermString(NAR nar, String s) {
        assertEquals(s, nar.term(s).toString());
    }

    @Test public void testSequenceSentenceNormalization() {
        //sequences at the top level as terms must not have any trailing intervals
        NAR nar = new Default();

        String tt = "(&/, a, /1, b, /2)";
        Sequence term = nar.term(tt);
        assertNotNull(term);

        //trailng suffix that should be removed when it becomes the sentence's content term
        Task task = nar.task(tt + ".");
        assertNotNull(task);
        assertNotNull(task.getTerm());
        assertEquals(Sequence.class, task.getTerm().getClass());
        Sequence ts = (Sequence)task.getTerm();
        assertEquals(2, ts.length());
        assertEquals("(&/, a, /1, b)", task.getTerm().toString());

        //no trailing suffix, unchanged
        Task u = nar.task("(&/, a, /1, b).");
        assertEquals(Sequence.class, u.getTerm().getClass());
        Sequence tu = (Sequence)u.getTerm();
        assertEquals(2, tu.length());
        assertEquals("(&/, a, /1, b)", u.getTerm().toString());

        //TODO test for the sentence's term to be a different instance if it was modified
    }

    @Test public void testConceptToString() {

    }

    @Test public void testDistance1() {
        NAR nar = new Default();

        Sequence a = nar.term("(&/, x, /1, y)");
        Sequence b = nar.term("(&/, x, /2, y)");
        assertEquals(1, a.distance1(b));
        assertEquals(1, b.distance1(a));
        assertEquals(0, a.distance1(a));

        Sequence c = nar.term("(&/, x, /1, y)");
        Sequence d = nar.term("(&/, x, /9, y)");
        assertEquals(Long.MAX_VALUE, c.distance1(d, 2));
        assertEquals(1, c.distance1(b, 2));


    }

}