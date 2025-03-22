package NBobodySimulation;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
//import org.apache.commons.math4.legacy.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator; // if you're using Maven, use this import instead

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

class InvalidSimulationPanelSizeException extends RuntimeException {
    public InvalidSimulationPanelSizeException(int width, int height, int minWidth, int minHeight) {
        super(String.format("Invalid simulation panel size: width=%d (minimum=%d), height=%d (minimum=%d). "
                        + "Please call simulation.getPanel().setSize(int, int) with appropriate values before calling simulation.configure(SimulationSettings).",
                width, minWidth, height, minHeight));
    }
}

public class SimulationPanel extends JPanel implements Configurable {

    private static final int MIN_WIDTH = 800;
    private static final int MIN_HEIGHT = 800;

    double particleScale = 1;
    double[] translationScale = new double[2];

    private final JFXPanel jfxPanel;
    private Canvas canvas;
    private Canvas trailCanvas;
    private Canvas gridCanvas;
    private ArrayList<OrbitalBody> bodies;
    private SimulationSettings settings;
    private Color backgroundColor = Color.BLACK;
    double[] circleDiameter;
    double[][] oldCanvasPos;

    public SimulationPanel() {
        jfxPanel = new JFXPanel();
        this.setLayout(new BorderLayout());
        this.add(jfxPanel, BorderLayout.CENTER);
        this.canvas = new Canvas(getWidth(), getHeight());
        this.trailCanvas = new Canvas(getWidth(), getHeight());
        this.gridCanvas = new Canvas(getWidth(), getHeight());
    }

    @Override
    public void setSize(int width, int height){
        validatePanelSize(width, height);
        super.setSize(width, height);
    }

    private void validatePanelSize(int width, int height){
        try {
            if(width < MIN_WIDTH || height < MIN_HEIGHT) throw new InvalidSimulationPanelSizeException(getWidth(), getHeight(), MIN_WIDTH, MIN_HEIGHT);
        } catch (InvalidSimulationPanelSizeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void configure(SimulationSettings settings) {
        Platform.runLater(() -> {
            validatePanelSize(getWidth(), getHeight());
            this.settings = settings;
            canvas = new Canvas(getWidth(), getHeight());
            trailCanvas = new Canvas(getWidth(), getHeight());
            gridCanvas = new Canvas(getWidth(), getHeight());

            if(this.settings == null || settings.getBodies().isEmpty()) { // just draw gridlines
                System.out.println("No bodies found. Outputting blank grid");
                drawGrid(gridCanvas.getGraphicsContext2D());
                return;
            }

            bodies = settings.getBodies();

            circleDiameter = new double[bodies.size()];
            for (int i = 0; i < bodies.size(); i++) circleDiameter[i] = Math.sqrt(bodies.get(i).getMass()/settings.avgMass()) * 10;
            oldCanvasPos = new double[bodies.size()][settings.getBodies().get(0).getDimensions()];
            for (int i = 0; i < bodies.size(); i++) oldCanvasPos[i] = returnRelativePosition(bodies.get(i).getPosition());
            setBackgroundColor(settings.getBackgroundColor());
            StackPane root = new StackPane();
            Scene scene = new Scene(root, getWidth(), getHeight(), backgroundColor);
            root.getChildren().addAll(gridCanvas, trailCanvas, canvas);
            jfxPanel.setScene(scene);

            DormandPrince853Integrator integrator = new DormandPrince853Integrator(Math.pow(10, -10), 10000, 0.01, 0.0001);
            double[][] scales = generateScale(integrator, Utils.flattenBodies(bodies), settings);
            double[][] canvasRectangle = calculateRectangle(scales, calculateBuffer(settings.getBodies()));
            setScaleFactors(canvasRectangle);
        });
    }

    private double[][] generateScale(DormandPrince853Integrator integrator, double[] flatBodies, SimulationSettings settings) {
        final int SIMULATION_LENGTH = 10;
        double simulationTime = settings.getSkipAhead();

        double[][] minsAndMaxs = minAndMaxPositions(flatBodies);

        DifferentialEquations differentialEquations = new DifferentialEquations(settings.getMasses());

        for (double time = simulationTime; time < SIMULATION_LENGTH * settings.getSimulationSpeed() + simulationTime; time += settings.getSimulationSpeed() / 5) {
            try {
                integrator.integrate(differentialEquations, time, flatBodies, time + settings.getSimulationSpeed() / 5, flatBodies);
                double[][] currentMinsAndMaxs = minAndMaxPositions(flatBodies);
                for (int i = 0; i < 2; i++) {
                    if (currentMinsAndMaxs[0][i] < minsAndMaxs[0][i]) {
                        minsAndMaxs[0][i] = currentMinsAndMaxs[0][i];
                    }
                    if (currentMinsAndMaxs[1][i] > minsAndMaxs[1][i]) {
                        minsAndMaxs[1][i] = currentMinsAndMaxs[1][i];
                    }
                }
            } catch (Exception e) {
                break;
            }
        }

        double[][] coordinatePositions = new double[4][2];
        int index = 0;

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                coordinatePositions[index++] = new double[]{minsAndMaxs[i][0], minsAndMaxs[j][1]};
            }
        }
        return coordinatePositions;
    }

    private double[][] minAndMaxPositions(double[] flatBodies) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (int i = 0; i < flatBodies.length; i += settings.getBodies().get(0).getDimensions() * 2) {
            minX = Math.min(minX, flatBodies[i]);
            minY = Math.min(minY, flatBodies[i + 1]);
            maxX = Math.max(maxX, flatBodies[i]);
            maxY = Math.max(maxY, flatBodies[i + 1]);
        }
        return new double[][]{{minX, minY}, {maxX, maxY}};
    }

    private static double[][] calculateRectangle(double[][] originalRectangle, double buffer) {

        // Declaring the new rectangle variable
        double[][] newRectangle = new double[4][2];

        // Setting a constant minimum buffer proportion
        final double BUFFER_PROPORTION = 0.1;

        // Calculates the buffer space by which to adjust the original rectangle
        double bufferProportionalWidth = (originalRectangle[2][0] - originalRectangle[0][0]) * BUFFER_PROPORTION + buffer;
        double bufferProportionalHeight = (originalRectangle[1][1] - originalRectangle[0][1]) * BUFFER_PROPORTION + buffer;

        // Adjusting point 1 (lower left)
        newRectangle[0][0] = originalRectangle[0][0] - bufferProportionalWidth;
        newRectangle[0][1] = originalRectangle[0][1] - bufferProportionalHeight;

        // Adjusting point 2 (upper left)
        newRectangle[1][0] = originalRectangle[1][0] - bufferProportionalWidth;
        newRectangle[1][1] = originalRectangle[1][1] + bufferProportionalHeight;

        // Adjusting point 3 (lower right)
        newRectangle[2][0] = originalRectangle[2][0] + bufferProportionalWidth;
        newRectangle[2][1] = originalRectangle[2][1] - bufferProportionalHeight;

        // Adjusting point 4 (upper right)
        newRectangle[3][0] = originalRectangle[3][0] + bufferProportionalWidth;
        newRectangle[3][1] = originalRectangle[3][1] + bufferProportionalHeight;

        return newRectangle;
    }

    private double calculateBuffer(ArrayList<OrbitalBody> bodies) {
        // Declares buffer variable
        double buffer;

        // Constructs an array of the absolute values of the particle velocities
        double[] squares = new double[bodies.size()];
        for (int i = 0; i < bodies.size(); i++) {
            squares[i] = bodies.get(i).getVelocity()[0] * bodies.get(i).getVelocity()[0] + bodies.get(i).getVelocity()[1] * bodies.get(i).getVelocity()[1];
        }

        // Calculates the average squared velocity
        double avgSquaredVelocity = 0;
        for(int i = 0; i < bodies.size(); i++) {
            avgSquaredVelocity += squares[i] * bodies.get(i).getMass();
        }
        avgSquaredVelocity = Math.sqrt(avgSquaredVelocity / settings.totalMass());

        // Determines the buffer as a product of ASV and a constant
        buffer = avgSquaredVelocity * 5;

        return buffer;
    }

    private void setScaleFactors(double[][] canvasRectangle) {
        double aspectFactor;
        double aspectAdjust;
        double adjDiff;
        double newHeight;
        double newWidth;
        double rectangleHeight;
        double rectangleWidth;
        double[] particleScaleArray = {0, 0};

        // Calculates absolute height and width of the rectangle
        rectangleHeight = Math.abs(canvasRectangle[1][1] - canvasRectangle[0][1]);
        rectangleWidth = Math.abs(canvasRectangle[3][0] - canvasRectangle[1][0]);

        if (rectangleHeight == 0 || rectangleWidth == 0) {
            particleScale = 1;
            translationScale = new double[]{-400, 360};
            return;
        }

        // Calculates the aspect ratios of the canvas and the rectangle
        double rectangleAspect = rectangleWidth / rectangleHeight;
        double canvasAspect = 10.0 / 9.0;

        // Adjusts the rectangle to fit the canvas aspect ratio
        if (rectangleAspect > canvasAspect) {
            aspectFactor = rectangleAspect / canvasAspect;
            aspectAdjust = rectangleHeight * aspectFactor;
            adjDiff = (aspectAdjust - rectangleHeight) / 2;

            canvasRectangle[1][1] = canvasRectangle[1][1] + adjDiff;
            canvasRectangle[3][1] = canvasRectangle[3][1] + adjDiff;
            canvasRectangle[0][1] = canvasRectangle[0][1] - adjDiff;
            canvasRectangle[2][1] = canvasRectangle[2][1] - adjDiff;
        } else if (rectangleAspect < canvasAspect) {
            aspectFactor = canvasAspect / rectangleAspect;
            aspectAdjust = rectangleWidth * aspectFactor;
            adjDiff = (aspectAdjust - rectangleWidth) / 2;

            canvasRectangle[2][0] = canvasRectangle[2][0] + adjDiff;
            canvasRectangle[3][0] = canvasRectangle[3][0] + adjDiff;
            canvasRectangle[0][0] = canvasRectangle[0][0] - adjDiff;
            canvasRectangle[1][0] = canvasRectangle[1][0] - adjDiff;
        }


        // Defines the new height and width of the rectangle
        newHeight = Math.abs(canvasRectangle[1][1] - canvasRectangle[0][1]);
        newWidth = Math.abs(canvasRectangle[3][0] - canvasRectangle[1][0]);

        // Calculates a scale factor by which to adjust the canvas particles
        particleScaleArray[0] = ((newHeight - 720) / 720) + 1;
        particleScaleArray[1] = ((newWidth - 800) / 800) + 1;
        particleScale = (particleScaleArray[0] + particleScaleArray[1]) / 2;

        // Calculates the coordinates of the upper left corner of the rectangle
        translationScale[0] = canvasRectangle[1][0];
        translationScale[1] = canvasRectangle[1][1];
    }

    public void setBackgroundColor(java.awt.Color color) {
        this.backgroundColor = Utils.convertAWTColorToFXColor(color);
        Platform.runLater(() -> {
            jfxPanel.getScene().setFill(backgroundColor);
        });
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        Platform.runLater(() -> {
            jfxPanel.getScene().setFill(backgroundColor);
        });
    }

    private double[] returnRelativePosition(double[] absolutePosition) {
        return new double[]{
                (absolutePosition[0] - translationScale[0]) / particleScale, -(absolutePosition[1] - translationScale[1]) / particleScale
        };
    }

    public void clear(){
        canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        trailCanvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        GraphicsContext gridGC = gridCanvas.getGraphicsContext2D();
        gridGC.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if(settings.showGrid()) drawGrid(gridGC);
    }

    public void draw() {
        Platform.runLater(() -> {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            if (settings.showGrid())
                drawGrid(gridCanvas.getGraphicsContext2D());
            if(settings.showCenterOfGravity())
                drawCenterOfMass(gc);
            synchronized (bodies) {
                    for(int i = 0; i < bodies.size(); i++) {
                        OrbitalBody body = bodies.get(i);
                        double[] relativePosition = returnRelativePosition(body.getPosition());
                        oldCanvasPos[i][0] = relativePosition[0];
                        oldCanvasPos[i][1] = relativePosition[1];
                        gc.setFill(body.getColor());
                        double radius = circleDiameter[i] / 2;
                        gc.fillOval(relativePosition[0] - radius, relativePosition[1] - radius, radius * 2, radius * 2);
                        if (settings.showTrail())
                            drawTrail(trailCanvas.getGraphicsContext2D(), body, radius/6);
                    }
            }
        });
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.GRAY);
        for (int i = 0; i < canvas.getWidth(); i += 50) {
            gc.strokeLine(i, 0, i, canvas.getHeight());
        }
        for (int i = 0; i < canvas.getHeight(); i += 50) {
            gc.strokeLine(0, i, canvas.getWidth(), i);
        }
    }

    private void drawTrail(GraphicsContext gc, OrbitalBody body, double lineWidth) {
        gc.setStroke(body.getColor());
        gc.setLineWidth(lineWidth);  // increase the trail size
        double[][] trail = body.getTrail();
        for (int i = 1; i < trail.length; i++) {
            double x1 = trail[i - 1][0];
            double y1 = trail[i - 1][1];
            double x2 = trail[i][0];
            double y2 = trail[i][1];

            double[] s = returnRelativePosition(new double[]{x1, y1});
            double[] e = returnRelativePosition(new double[]{x2, y2});

            gc.strokeLine(s[0], s[1], e[0], e[1]);
        }
        gc.setLineWidth(1);  // reset to default line width
    }

    private void drawCenterOfMass(GraphicsContext gc){
        double[] centerOfMassAbsolutePosition = new double[2];

        // Calculates the center of mass
        for (int i = 0; i < 2; i++) {
            centerOfMassAbsolutePosition[i] = ((bodies.get(0).getMass() * bodies.get(0).getPosition()[i])
                    + (bodies.get(1).getMass() * bodies.get(1).getPosition()[i])
                    + (bodies.get(2).getMass() * bodies.get(2).getPosition()[i])) / settings.totalMass();
        }

        double[] centerOfMassRelativePosition = returnRelativePosition(centerOfMassAbsolutePosition);


        // Displays the center of mass
        gc.setFill(Color.valueOf("#555555"));
        gc.fillOval((centerOfMassRelativePosition[0] - 5), (centerOfMassRelativePosition[1] - 5), 10, 10);

        gc.setStroke(Color.valueOf("#555555"));
        for (int i = 0; i < 2; i++) {
            for (int j = i + 1; j < 3; j++) {
                gc.strokeLine(oldCanvasPos[i][0], oldCanvasPos[i][1], oldCanvasPos[j][0], oldCanvasPos[j][1]);
            }
        }
        for (int i = 0; i < 3; i++) {
            gc.strokeLine(oldCanvasPos[i][0], oldCanvasPos[i][1], centerOfMassRelativePosition[0], centerOfMassRelativePosition[1]);
        }
    }
}
