package NBobodySimulation;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrbitalBody {

    private static int ID_COUNTER = 0;

    private int id;
    private double[] position;
    private double[] velocity;
    private double[] acceleration;
    private int dimensions;
    private double mass;
    private Color color;
    private List<double[]> trail;
    private static final int MAX_TRAIL_SIZE = 10000;

    // Velocity components
    private void setup(double[] initialPosition, double[] initialVelocity, double mass) {
        id = ID_COUNTER++;
        this.position = initialPosition;
        this.velocity = initialVelocity;
        this.dimensions = this.position.length;
        if(dimensions < 2 || dimensions > 3) throw new IllegalArgumentException("Invalid number of dimensions '" + this.dimensions + "'. Only supports either 2 or 3 dimensions");
        this.acceleration =  new double[dimensions];
        for(int i = 0; i < dimensions; i++) this.acceleration[i] = 0;
        this.mass = mass;
        this.trail = Collections.synchronizedList(new ArrayList<>());
    }

    public OrbitalBody(double[] initialPosition, double[] initialVelocity, double mass, java.awt.Color color) {
        this.color = Utils.convertAWTColorToFXColor(color);
        setup(initialPosition, initialVelocity, mass);
    }

    public OrbitalBody(double[] initialPosition, double[] initialVelocity, double mass, Color color) {
       this.color = color;
        setup(initialPosition, initialVelocity, mass);
    }

    public static void resetCounter() {
        ID_COUNTER = 0;
    }

    public double getMass() {
        return mass;
    }

    public int getDimensions() {
        return dimensions;
    }

    // Update position based on velocity
    public void update(double[] position, double[] velocity) {
        for(int i = 0; i < dimensions; i++){
            this.position[i] = position[i];
            this.velocity[i] = velocity[i];
            this.acceleration[i] = DifferentialEquations.getAcceleration(id, i);
        }
        // Update trail
        double[] newTrailPoint = new double[dimensions];
        System.arraycopy(this.position, 0, newTrailPoint, 0, dimensions);
        trail.add(newTrailPoint);
        if (trail.size() > MAX_TRAIL_SIZE) {
            trail.remove(0);
        }
    }

    public double[] flatten(){
        double[] flattened = new double[dimensions * 2];
        for(int i = 0; i < dimensions; i++){
            flattened[i] = position[i];
        }
        for(int i = 0; i < dimensions; i++){
            flattened[dimensions + i] += velocity[i];
        }
        return flattened;
    }

    // Method to set new velocity
    public void setVelocity(double[] newVelocity) {
        if(newVelocity.length != this.dimensions) {
            throw new IllegalArgumentException("New position array length does not match required array length => " + newVelocity.length + " != " + this.dimensions);
        }
        this.velocity = newVelocity;
    }

    // Method to set new position
    public void setPosition(double[] newPosition) {
        if(newPosition.length != this.dimensions) {
            throw new IllegalArgumentException("New position array length does not match required array length => " + newPosition.length + " != " + this.dimensions);
        }
        this.position = newPosition;
    }

    public double[] getPosition() {
        return position;
    }

    public double[] getVelocity() {
        return velocity;
    }


    public Color getColor() {
        return color;
    }

    public double[][] getTrail() {
        return trail.toArray(new double[0][0]);
    }

    @Override
    public String toString() {
        return String.format("%d. {x=%.2f, y=%.2f, vx=%.2f, vy=%.2f}", id, position[0], position[1], velocity[0], velocity[1]);
    }
}
