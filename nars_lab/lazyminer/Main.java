/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package lazyminer;

/**
 *
 * @author patha
 */
public class Main {
    
    public static void main(String args[]) throws InterruptedException {
        LazyMiner lm = new LazyMiner();
        PatientSimulator pat = new PatientSimulator(lm);
        for(int i=0;i<1000;i++) {
            lm.cycles(1);
            pat.step();
            lm.HowSensorEventReachedValue("heartrate",110);
            //Thread.sleep(100);
        }
        lm.cycles(1000);
        lm.HowSensorEventReachedValue("heartrate",110);
        lm.cycles(1000);
    }
}
