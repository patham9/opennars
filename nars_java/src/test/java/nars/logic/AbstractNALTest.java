package nars.logic;

import junit.framework.TestCase;
import nars.analyze.meter.CountDerivationCondition;
import nars.analyze.meter.CountOutputEvents;
import nars.core.Memory;
import nars.core.NAR;
import nars.core.NewNAR;
import nars.core.Parameters;
import nars.io.ExampleFileInput;
import nars.io.condition.OutputCondition;
import nars.io.meter.Metrics;
import nars.io.meter.event.DoubleMeter;
import nars.io.meter.event.HitMeter;
import nars.io.meter.event.ObjectMeter;
import nars.logic.meta.Derivations;
import org.junit.After;
import org.junit.Ignore;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * Created by me on 2/10/15.
 */
@Ignore
abstract public class AbstractNALTest extends TestCase {

    public static final long randomSeed = 1;

    private static final int similarsToSave = 3;

    static final DoubleMeter testScore, testTime, testSeed;
    static final HitMeter testConcepts;
    static final ObjectMeter<String> testBuild;
    public static OutputStream dataOut = null;
    PrintStream log = System.out;

//    static {
//        try {
//            csvOut = new FileOutputStream("/tmp/out.csv");
//        } catch (FileNotFoundException e) {
//            csvOut = System.out;
//        }
//    }

    public static final Metrics<String,Object> results = new Metrics().addMeters(
            testScore = new DoubleMeter("Score"),
            testBuild = new ObjectMeter<String>("Build"),
            testSeed = new DoubleMeter("Seed"),
            testTime = new DoubleMeter("uSecPerCycle"),
            testConcepts = new HitMeter("Concepts")
    );

    //Derivations derivations = new Derivations(false, false);
    Derivations derivations = null;
    public static final CountOutputEvents eventCounter = new CountOutputEvents(results);
    public static final CountDerivationCondition deriveMethodCounter = new CountDerivationCondition(results);


    public final TestNAR nar;
    public final NewNAR build;
    public final List<OutputCondition> conditions;


    public AbstractNALTest(NewNAR b) {
        super();

        Memory.resetStatic(randomSeed);
        Parameters.DEBUG = true;

        this.build = b;
        this.nar = new TestNAR(b);
        conditions = nar.musts;

    }


    /** called before test runs */
    public static void startAnalysis(NAR nar) {

        /*
        if (this.derivations != null) {
            derivations.record(nar);
        }
        */
        nar.addPlugin(eventCounter);
        nar.addPlugin(deriveMethodCounter);


    }

    public static void endAnalysis(String label, TestNAR nar, NewNAR build, long nanos, long seed, boolean success) {

        testBuild.set(build.toString());
        testScore.set( OutputCondition.score(nar.musts) );
        testSeed.set(seed);
        testTime.set( (((double)nanos)/1000.0) / (nar.time()) ); //in microseconds
        testConcepts.hit(nar.memory.concepts.size());

        results.update(label);


        /*if (derivations!=null)
            derivations.print(log);*/


        eventCounter.reset();
        eventCounter.cancel();
        deriveMethodCounter.cancel();


        nar.reset(); //to help GC

    }

    public static long runScript(TestNAR nar, String path, int maxCycles, long rngSeed) {

        Memory.resetStatic(rngSeed);
        Parameters.DEBUG = true;

        String script = ExampleFileInput.getExample(path);
        nar.musts.addAll(OutputCondition.getConditions(nar, script, similarsToSave));

        nar.addInput(script);

        long start = System.nanoTime();

        nar.run(maxCycles);

        return System.nanoTime() - start;
    }


    //Default test procedure
    @After public void test() {

        //assertTrue("No conditions to test", !conditions.isEmpty());
        if (conditions.isEmpty()) {
            System.err.println("WARNING: No Conditions Added");
            new Exception().printStackTrace();
            assertTrue(false);
        }





        assertTrue("No cycles elapsed", nar.time() > 0);


        String report = "";
        boolean suc = nar.getError()==null;
        for (OutputCondition e : conditions) {
            if (!e.succeeded) {
                report += e.getFalseReason().toString() + '\n';
                suc = false;
            }
            else {
                report += e.getTrueReasons().toString() + '\n';
            }
        }

        assertTrue(report, suc);
    }







}
