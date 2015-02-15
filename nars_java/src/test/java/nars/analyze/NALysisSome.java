package nars.analyze;

import nars.build.Default;
import nars.core.NewNAR;
import org.junit.Ignore;

import java.io.FileNotFoundException;

/**
 * report filtered by failures
 */
@Ignore
public class NALysisSome extends NALysis {


    public NALysisSome(NewNAR b) {
        super(b);
    }

    public static void main(String[] args) throws FileNotFoundException {



        showOutput = true;
        showTrace = false;

        nal("test7", "7.18", new Default().setInternalExperience(null), 256);
        //nal("test5", "5.18", new Default().setInternalExperience(null), 256);



        //results.printARFF(new PrintStream(dataOut));
        //results.printCSV(new PrintStream(System.out));

    }


}
