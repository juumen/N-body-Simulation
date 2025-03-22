package NBobodySimulation;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.awt.Color;

public class SimulationSettings {
    private Color backgroundColor;
    private boolean isInfinite;
    private boolean showTrail;
    private boolean showGrid;
    private boolean showCenterOfGravity;
    private int frameRate;
    private int skipAhead;
    private double simulationSpeed; 
    private ArrayList<OrbitalBody> bodies;
    
    
    // constructor
    public SimulationSettings(){
        this.bodies = new ArrayList<>();
    }
    
    // accessors & mutators
    public Color getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(Color backgroundColor){
        this.backgroundColor = backgroundColor;
    }
    
    public boolean isFinite(){
        return isInfinite;
    }
    
    public void setInfinite(boolean infinite){
        this.isInfinite = infinite;
    }
    
    public boolean showTrail(){
        return showTrail;
    }
    
    public void setShowTrail(boolean showTrail){
        this.showTrail = showTrail;
    }
    
    public boolean showGrid() {
        return showGrid;
    }
    
    public void setShowGrid(boolean showGrid){
        this.showGrid = showGrid;
    }
    
    public boolean showCenterOfGravity(){
        return showCenterOfGravity;
    }
    
    public void setShowCenterOfGravity(boolean showCenterOfGravity){
        this.showCenterOfGravity = showCenterOfGravity;
    }
    
    public void setBodies(ArrayList<OrbitalBody> bodies){
        this.bodies = bodies;
    }
    
    public void addOrbitalBody(OrbitalBody b){
        this.bodies.add(b);
    }
    
    public void removeOrbitalBody(OrbitalBody b){
        this.bodies.remove(b);
    }
    
    public void setSkipAhead(int skipAhead){
        this.skipAhead = skipAhead;
    }
    
    public int getSkipAhead() {
        return skipAhead;
    }
    
    public ArrayList<OrbitalBody> getBodies(){
        return bodies;
    }
    
    public double[] getMasses(){
        double[] masses = new double [bodies.size()];
        for(int i = 0; i < bodies.size();i++){
            masses[i] = bodies.get(i).getMass();    
        }
        return masses;
    }
    
    public int getDimensions() {
        if (bodies.isEmpty()) {
            return 0; 
        }
        return bodies.get(0).getDimensions();
    }
    
    public double getSimulationSpeed() {
        return simulationSpeed;
    }
    
      public void setSimulationSpeed(double simulationSpeed) {
        this.simulationSpeed = simulationSpeed;
    }
    
      public int getFrameRate() {
        return frameRate;
    }
    
       public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }
    
     public double totalMass() {
        double totalMass = 0.0;
        for (OrbitalBody b : bodies) {
            totalMass += b.getMass();
        }
        return totalMass;
    }
    
    public double avgMass() {
        if (bodies.isEmpty()) {
            return 0.0;
        }
        return totalMass() / bodies.size();
    }
    
        public double[] flattenedBodies() {
        int dimensions = getDimensions();
        int totalElements = bodies.size() * (dimensions * 2); // Positions and velocities
        
        double[] flattened = new double[totalElements];
        int index = 0;
        
        for (OrbitalBody body : bodies) {
            double[] pos = body.getPosition();
            double[] vel = body.getVelocity();
            
            
            System.arraycopy(pos, 0, flattened, index, dimensions);
            System.arraycopy(vel, 0, flattened, index + dimensions, dimensions);
            
            index += dimensions * 2; 
        }
        
        return flattened;
    }    
}