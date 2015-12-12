package nars.guifx.demo;

import nars.guifx.NARide;
import nars.guifx.graph2.ConceptsSource;
import nars.guifx.graph2.TermEdge;
import nars.guifx.graph2.impl.BlurCanvasEdgeRenderer;
import nars.guifx.graph2.scene.DefaultNodeVis;
import nars.guifx.graph2.source.DefaultGrapher;
import nars.guifx.graph2.source.SpaceGrapher;
import nars.guifx.util.TabX;
import nars.nar.Default;
import nars.process.BagForgettingEnhancer;

/**
 * Created by me on 8/15/15.
 */
public class NARGraph1Test {


    public static SpaceGrapher newGraph(Default n) {


        new BagForgettingEnhancer(n.memory, n.core.concepts(), 0.8f, 0.8f, 0.8f);

//        n.memory.conceptForgetDurations.setValue(8);
//        n.memory.termLinkForgetDurations.setValue(12);
//        n.memory.taskLinkForgetDurations.setValue(12);

        //n.input(new File("/tmp/h.nal"));
        n.input("<hydochloric --> acid>.");
        n.input("<#x-->base>. %0.65%");
        n.input("<neutralization --> (acid,base)>. %0.75;0.90%");
        n.input("<(&&, <#x --> hydochloric>, eat:#x) --> nice>. %0.75;0.90%");
        n.input("<(&&,a,b,ca)-->#x>?");

        //n.frame(5);


        DefaultGrapher g = new DefaultGrapher(

                new ConceptsSource(n),

                128,

                new DefaultNodeVis(),
                //new DefaultNodeVis.HexagonNodeVis(),

                (A, B) -> {
                    return new TermEdge(A, B) {
                        @Override
                        public double getWeight() {
                            //return ((Concept)A.term).getPriority();
                            return pri;
                        }
                    };
                    //return $.pro(A.getTerm(), B.getTerm());
                },

                new BlurCanvasEdgeRenderer()
        );

        //g.setLayout(HyperassociativeMap2D.class);
        //g.pan(2000,2000);



        return g;
    }

    public static void main(String[] args)  {


        Default n = new Default();

        NARide.show(n.loop(), ide -> {

            //ide.addView(new IOPane(n));
            ide.content.getTabs().setAll(
                    new TabX("Graph", newGraph(n)));


            ide.setSpeed(150);
            //n.frame(5);

        });

//        NARfx.run((a,b)-> {
//            b.setScene(
//                new Scene(newGraph(n), 600, 600)
//            );
//            b.show();
//
//            n.spawnThread(250, x -> {
//
//            });
//        });





//        TextOutput.out(n);
//        new Thread(() -> n.loop(185)).start();


    }

}
