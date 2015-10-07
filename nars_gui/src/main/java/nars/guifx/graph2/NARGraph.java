package nars.guifx.graph2;

import automenta.vivisect.dimensionalize.IterativeLayout;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import nars.guifx.Spacegraph;
import nars.guifx.demo.Animate;
import nars.term.Term;
import nars.util.data.random.XORShiftRandom;
import org.infinispan.commons.util.WeakValueHashMap;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 8/6/15.
 */
public class NARGraph<V> extends Spacegraph {

    final Map<Term, TermNode> terms = new WeakValueHashMap<>();


    public final SimpleObjectProperty<EdgeRenderer<TermEdge>> edgeRenderer = new SimpleObjectProperty<>();
    public final SimpleObjectProperty<IterativeLayout<TermNode, TermEdge>> layout = new SimpleObjectProperty<>();
    public final SimpleIntegerProperty maxNodes;
    public final SimpleObjectProperty<NARGrapher> source = new SimpleObjectProperty<>();




    private Animate animator;




    static final Random rng = new XORShiftRandom();


    int layoutPeriodMS = 30 /* slightly less than 2 * 17, approx sooner than 30fps */;


    public final SimpleObjectProperty<VisModel> vis = new SimpleObjectProperty<>();
    public TermNode[] displayed = new TermNode[0];


    /**
     * assumes that 's' and 't' are already ordered
     */
    public final TermEdge getConceptEdgeOrdered(TermNode s, TermNode t) {
        return getEdge(s.term, t.term);
    }

    static boolean order(final Term x, final Term y) {
        final int i = x.compareTo(y);
        if (i == 0) throw new RuntimeException("order=0 but must be non-equal");
        return i < 0;
    }

    public final TermEdge getEdge(Term a, Term b) {
        TermNode n = getTermNode(a);
        if (n != null) {
            return n.edge.get(b);
        }
        return null;
    }

    public final boolean addEdge(Term a, Term b, TermEdge e) {
        TermNode n = getTermNode(a);
        if (n != null) {
            return n.putEdge(b, e) == null;
        }
        return false;
    }

    public final TermNode getTermNode(final Term t) {
        return terms.get(t);
    }


    public final TermNode getOrCreateTermNode(final Term t/*, boolean createIfMissing*/) {
        TermNode tn = getTermNode(t);
        if (tn == null) {
            final VisModel gv = vis.get();
            tn = terms.compute(t,
                    (k, prev) -> {
                        TermNode n = gv.newNode(k);
                        layout.get().init(n);

                        if (prev != null) {
                            getVertices().remove(prev);
                        }

                        return n;
                    });
        }

        return tn;
    }


//    /**
//     * synchronizes an active graph with the scenegraph nodes
//     */
//    public void commit(final Collection<TermNode> active /* should probably be a set for fast .contains() */,
//                       ) {
//
//        final List<TermNode> toDetach = Global.newArrayList();
//
//        termList.clear();
//
//
//        runLater(() -> {
//
//
//            for (final TermNode tn : active) {
//                termList.add(tn);
//            }
//
//
//            //List<TermEdge> toDetachEdge = new ArrayList();
//            addNodes(x);
//
//            getVertices().forEach(nn -> {
//                if (!(nn instanceof TermNode)) return;
//
//                TermNode r = (TermNode) nn;
//                if (!active.contains(r.term)) {
//                    TermNode c = terms.remove(r.term);
//
//                    if (c != null) {
//                        c.setVisible(false);
//                        toDetach.add(c);
//                        //Map<Term, TermEdge> edges = c.edge;
//                        /*if (edges != null && edges.size() > 0) {
//                            //iterate the map, because the array snapshot may differ until its next update
//                            toDetachEdge.addAll(edges.values());
//                        }*/
//                    }
//                }
//
//            });
//
//            removeNodes((Collection) toDetach);
//
//            //removeEdges((Collection) toDetachEdge);
//
//            termList.clear();
//            termList.addAll(terms.values());
//
//            //print();
//            toDetach.clear();
//
//        });
//    }




    public void setVertices(Set<TermNode> active) {
        runLater(() -> {

            ObservableList<Node> v = getVertices();
            v.setAll(active);

            displayed = v.toArray(displayed);

            //System.out.println("cached: " + terms.size() + ", displayed: " + displayed.length + " , shown=" + v.size());
        });
    }


//    @FunctionalInterface
//    public interface PreallocatedResultFunction<X, Y> {
//        public void apply(X x, Y setResultHereAndReturnIt);
//    }

    @FunctionalInterface
    public interface PairConsumer<A, B> {
        public void accept(A a, B b);
    }


//    protected void updateNodes() {
//        if (termList != null)
//            termList.forEach(n -> {
//                if (n != null) n.update();
//            });
//    }


    final Color FADEOUT = Color.BLACK;
    //new Color(0,0,0,0.5);

    public interface EdgeRenderer<E> extends Consumer<E> {
        /**
         * called before any update begins
         */
        public void reset(NARGraph g);
    }


    /**
     * called in JavaFX thread
     */
    protected void rerender() {

        /** apply vis properties */
        VisModel v = vis.get();
        for (TermNode t : displayed)
            v.accept(t);


        /** apply layout */
        IterativeLayout<TermNode, TermEdge> l;
        if ((l = layout.get()) != null) {
            l.run(this, 1);
        } else {
            System.err.println(this + " has no layout");
        }


        edgeRenderer.get().reset(this);


        //Collections.addAll(removable, edges.getChildren());
        final EdgeRenderer<TermEdge> er = edgeRenderer.get();

        for (TermNode n : displayed) {

            //termList.forEach((Consumer<TermNode>) n -> {

//        for (int i = 0, termListSize = termList.size(); i < termListSize; i++) {
//            final TermNode n = termList.get(i);
//            for (final TermEdge e : n.getEdges()) {
//                removable.remove(e);
//            }
//        });
//
//
//
//        termList.forEach((Consumer<TermNode>)n -> {
//        for (int i = 0, termListSize = termList.size(); i < termListSize; i++) {
//            final TermNode n = termList.get(i);
            if (n != null) {
                for (final TermEdge e : n.getEdges())
                    if (e != null) er.accept(e);
            }
        }

//        removable.forEach(x -> {
//            edges.getChildren().remove(x);
//        });
//        edges.getChildren().removeAll(removable);

//        removable.clear();


    }


    public NARGraph(NARGrapher g, int size) {
        super();


        this.maxNodes = new SimpleIntegerProperty(size);

        source.addListener((e, c, v) -> {

            if (c != null) {
                v.stop(this);
            }

            if (v != null) {
                v.start(this);
            } else {
                System.out.println("no signal");
            }
        });


        //.onEachNthFrame(this::updateGraph, 1);

                /*.forEachCycle(() -> {
                    double[] dd = new double[4];
                    nar.memory.getControl().conceptPriorityHistogram(dd);
                    System.out.println( Arrays.toString(dd) );

                    System.out.println(
                            nar.memory.getActivePrioritySum(true, false, false) +
                            " " +
                            nar.memory.getActivePrioritySum(false, true, false) +
                            " " +
                            nar.memory.getActivePrioritySum(false, false, true)  );

                })*/
        ;

        //TODO add enable override boolean switch
        parentProperty().addListener(v -> checkVisibility());
        visibleProperty().addListener(v -> checkVisibility());
        runLater(() -> checkVisibility() );

        source.set(g);

    }

    protected synchronized void checkVisibility() {
        if (isVisible() && getParent() != null)
            start();
        else
            stop();
    }

    void start() {
            if (this.animator == null) {

                this.animator = new Animate(layoutPeriodMS, a -> {
                    if (displayed.length != 0) {
                        rerender();
                    }
                });

                /*this.updaterSlow = new Animate(updatePeriodMS, a -> {
                    if (!termList.isEmpty()) {
                        layoutNodes();
                        renderEdges();
                    }
                });*/
                animator.start();
                //updaterSlow.start();
            }

    }

    void stop() {
            if (this.animator != null) {
                animator.stop();
                animator = null;
            }

    }

    //    private class TermEdgeConsumer implements Consumer<TermEdge> {
//        private final Consumer<TermNode> updateFunc;
//        private final TermNode nodeToQuery;
//
//        public TermEdgeConsumer(Consumer<TermNode> updateFunc, TermNode nodeToQuery) {
//            this.updateFunc = updateFunc;
//            this.nodeToQuery = nodeToQuery;
//        }
//
//        @Override
//        public void accept(TermEdge te) {
//            if (te.isVisible())
//                updateFunc.accept(te.otherNode(nodeToQuery));
//        }
//    }
}