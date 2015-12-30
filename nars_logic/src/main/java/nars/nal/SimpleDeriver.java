package nars.nal;

import nars.Global;
import nars.nal.meta.BooleanCondition;
import nars.nal.meta.PostCondition;

import java.util.List;


public class SimpleDeriver extends Deriver  {

    private final List<List<BooleanCondition>> unrolled;

    public SimpleDeriver(PremiseRuleSet rules) {
        super(rules);

        List<List<BooleanCondition>> u = Global.newArrayList();
        for (PremiseRule r : rules) {
            for (PostCondition p : r.postconditions)
                u.add( r.getConditions(p) );
        }
        this.unrolled = u;

        u.forEach(s -> System.out.println(s));
    }

    @Override
    protected void run(PremiseMatch m) {

        int now = m.now();

        for (List<BooleanCondition> r : unrolled) {
            for (BooleanCondition p : r) {
                try {
                    if (!p.eval(m))
                        break;
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            m.revert(now);
        }

    }
}
