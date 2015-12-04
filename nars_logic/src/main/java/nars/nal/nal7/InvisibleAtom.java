package nars.nal.nal7;

import nars.Op;
import nars.term.AbstractUtf8Atom;
import nars.term.Term;
import nars.term.transform.Substitution;

import java.io.IOException;

/**
 * Atom which is invisible to most if not all reasoner
 * processes, useful for placeholders and intermediate
 * representations.
 *
 */
public class InvisibleAtom extends AbstractUtf8Atom {

    public InvisibleAtom(String id) {
        super(id);
    }

    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }



    @Override
    public final int structure() { return 0;     }

    @Override
    public final Op op() {
        return Op.INTERVAL;
    }



    /** preferably use toCharSequence if needing a CharSequence; it avoids a duplication */
    @Override
    public StringBuilder toStringBuilder(final boolean pretty) {
        StringBuilder sb = new StringBuilder();
        try {
            append(sb, pretty);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb;
    }


    @Override
    public final Term substituted(Substitution s) {
        return this;
    }

    @Override
    public String toString() {
        return toStringBuilder(false).toString();
    }

    @Override
    public int vars() {
        return 0;
    }


    @Override
    public int complexity() {
        return 0;
    }
}
