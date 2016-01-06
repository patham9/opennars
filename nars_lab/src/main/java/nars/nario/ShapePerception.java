//package nars.nario;
//
//import automenta.vivisect.swing.NWindow;
//import boofcv.alg.color.ColorHsv;
//import boofcv.alg.feature.detect.edge.CannyEdge;
//import boofcv.alg.feature.shapes.ShapeFittingOps;
//import boofcv.alg.filter.binary.BinaryImageOps;
//import boofcv.alg.filter.binary.Contour;
//import boofcv.core.image.ConvertBufferedImage;
//import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
//import boofcv.gui.feature.VisualizeShapes;
//import boofcv.struct.ConnectRule;
//import boofcv.struct.PointIndex_I32;
//import boofcv.struct.image.ImageFloat32;
//import boofcv.struct.image.ImageUInt8;
//import boofcv.struct.image.MultiSpectral;
//import nars.gui.output.ImagePanel;
//import nars.util.data.random.XORShiftRandom;
//
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.util.Random;
//import java.util.function.Supplier;
//
///**
// * Created by me on 5/20/15.
// */
//public class ShapePerception extends ImagePerception {
//    ImagePanel panel;
//    NWindow w;
//
//    public MultiSpectral<ImageFloat32> hsv;
//    public MultiSpectral<ImageFloat32> rgb;
//    public BufferedImage valueBuffer, hueBuffer;
//    public BufferedImage output;
//
//    final static Random rng = new XORShiftRandom();
//
//    // Polynomial fitting tolerances
//    double toleranceDist;
//    double toleranceAngle;
//
//    public ShapePerception(Supplier<BufferedImage> img) {
//        super("i1", img);
//        panel = new ImagePanel(400, 400);
//        w = new NWindow("i1", panel).show(500, 400);
//        toleranceDist = 2;
//        toleranceAngle = Math.PI / 10;
//    }
//
//    /**
//     * Detects contours inside the binary image generated by canny.  Only the external contour is relevant. Often
//     * easier to deal with than working with Canny edges directly.
//     */
//    public void fitCannyBinary(ImageFloat32 input, Graphics2D overlay) {
//
//        BufferedImage displayImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
//        ImageUInt8 binary = new ImageUInt8(input.width, input.height);
//
//        final int blurRadius = 1;
//
//        // Finds edges inside the image
//        CannyEdge<ImageFloat32, ImageFloat32> canny =
//                FactoryEdgeDetectors.canny(blurRadius, false, true, ImageFloat32.class, ImageFloat32.class);
//
//        canny.process(input, 0.1f, 0.3f, binary);
//
//        java.util.List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
//
//
//        overlay.setStroke(new BasicStroke(1));
//
//
//        final int iterations = 80;
//        for (Contour c : contours) {
//            // Only the external contours are relevant.
//            java.util.List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true,
//                    toleranceDist, toleranceAngle, iterations);
//
//            overlay.setColor(new Color(rng.nextInt()));
//            VisualizeShapes.drawPolygon(vertexes, true, overlay);
//        }
//
//        //ShowImages.showWindow(displayImage, "Canny Contour");
//    }
//
//
//    @Override
//    protected synchronized float[] process(BufferedImage i) {
//
//        rgb = ConvertBufferedImage.convertFromMulti(i, rgb, false, ImageFloat32.class);
//
//        if (hsv == null)
//            hsv = new MultiSpectral<ImageFloat32>(ImageFloat32.class, rgb.width, rgb.height, 3);
//
//        ColorHsv.rgbToHsv_F32(rgb, hsv);
//
//
//        //valueBuffer = ConvertBufferedImage.convertTo(hsv, valueBuffer, false);
//
//        ImageFloat32 value = hsv.getBand(2);
//        valueBuffer = ConvertBufferedImage.convertTo(value, valueBuffer);
//
//
//        ImageFloat32 hue = hsv.getBand(0);
//        hueBuffer = ConvertBufferedImage.convertTo(hue, hueBuffer);
//
//        /*final int targetWidth = valueBuffer.getWidth();
//        final int targetHeight = valueBuffer.getHeight();
//        if ((output == null || (output.getWidth() != targetWidth) || (output.getHeight() != targetHeight))) {
//            output = new BufferedImage(targetWidth, targetHeight, valueBuffer.getType());
//        }
//        Graphics2D g = (Graphics2D) output.getGraphics();
//        g.drawImage(valueBuffer, 0, 0, null);*/
//        //g.drawImage(hueBuffer, valueBuffer.getWidth(), 0, hueBuffer.getWidth()/2, hueBuffer.getHeight()/2, null);
//
//        fitCannyBinary(value, (Graphics2D) valueBuffer.getGraphics());
//        panel.setImage(valueBuffer);
//
//
//        return value.getData();
//    }
// }
