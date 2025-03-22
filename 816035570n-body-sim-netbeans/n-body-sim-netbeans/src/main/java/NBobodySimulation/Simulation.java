package NBobodySimulation;

import javafx.application.Platform;
//import org.apache.commons.math4.legacy.exception.NumberIsTooLargeException;
//import org.apache.commons.math4.legacy.exception.NumberIsTooSmallException;
//import org.apache.commons.math4.legacy.ode.nonstiff.DormandPrince853Integrator;

// If you're using Maven, use these imports instead
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;

public class Simulation implements Configurable {

    private DormandPrince853Integrator integrator;
    private double[] flattenedBodies;
    private double currentTime = 0;
    private SimulationSettings settings;
    private DifferentialEquations differentialEquations;
    

    private final SimulationPanel panel;

    private SimulationState state = SimulationState.INACTIVE;

    public Simulation() {
        panel = new SimulationPanel();
    }

    public SimulationSettings getSettings() {
        return settings;
    }

    public SimulationState getState() {
        return state;
    }
    
    

    public SimulationPanel getPanel() {
        return this.panel;
    }

    @Override
    public void configure(SimulationSettings config) {
        this.settings = config;
        this.panel.configure(this.settings);

    }

    private void reset(){
        panel.clear();
        differentialEquations = new DifferentialEquations(settings.getMasses());
        flattenedBodies = new double[settings.getBodies().size() * settings.getBodies().get(0).getDimensions() * 2]; // n bodies, d dimensions, position and acceleration
        currentTime = settings.getSkipAhead();
        
    }
   

    private void integrate(){
        try {
            // Get the position and velocity of particles at currentTime
            integrator.integrate(differentialEquations, currentTime, flattenedBodies, currentTime + (settings.getSimulationSpeed() / settings.getFrameRate()), flattenedBodies);
        } catch (NumberIsTooSmallException e) {
            // Asymptote error (the integrator can't converge and gives up)
            System.out.println(e.getMessage());
            breakSimulationAfterUpdate();
        } catch (NumberIsTooLargeException e) {
            // Double overflow error (inputs too large for double datatype to handle)
            System.out.println(e.getMessage());
            breakSimulationAfterUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            breakSimulationAfterUpdate();
        }
    }

    public void start() {
        if(settings == null) throw new RuntimeException("Simulation not configured!");
        if(settings.getBodies().isEmpty()) return;
        if(state == SimulationState.INACTIVE) {
            reset();
            integrator = new DormandPrince853Integrator(Math.pow(10, -10), 10000, 0.01, 0.0001);
            flattenedBodies = Utils.flattenBodies(settings.getBodies()); // Flatten particles into the flattenedParticles array.
            if (currentTime != 0) integrate();
            updateBodies();
            SimulationThread simulationThread = new SimulationThread();
            simulationThread.start();
        } else if (state == SimulationState.PAUSED) {
            SimulationThread simulationThread = new SimulationThread();
            simulationThread.start();
        } else throw new RuntimeException("Invalid attempt to double run simulation!");
        state = SimulationState.ACTIVE;
    }

    private void updateBodies() {
        int flattenedBodyLength = settings.getBodies().get(0).getDimensions() * 2;
        for (int i = 0; i < flattenedBodies.length; i += flattenedBodyLength) {
            double[] position = new double[]{flattenedBodies[i], flattenedBodies[i + 1]};
            double[] velocity = new double[]{flattenedBodies[i + 2], flattenedBodies[i + 3]};
            settings.getBodies().get(i / flattenedBodyLength).update(position, velocity);
        }
    }

    public void step() {
        currentTime += (settings.getSimulationSpeed() / settings.getFrameRate());
        integrate();
        Platform.runLater(this::updateBodies);
        panel.draw();
        
    }

    private void breakSimulationAfterUpdate() {
        updateBodies();
        breakSimulation();
    }

    private void breakSimulation() {
        state = SimulationState.INACTIVE;
    }

    public void pause(){
        state = SimulationState.PAUSED;
    }

    public void stop(){
        breakSimulationAfterUpdate();
    }

    private class SimulationThread extends Thread {
        @Override
        public void run() {
            while (state == SimulationState.ACTIVE) {
                long taskTime = System.currentTimeMillis(); // Record current time (to sync framerate)
                step();
                long leftoverTime = 1000/settings.getFrameRate() - (System.currentTimeMillis() - taskTime);
                if (leftoverTime > 0) {
                    try {
                        Thread.sleep(leftoverTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(state == SimulationState.INACTIVE) {
                reset();
                OrbitalBody.resetCounter();
            }
        }
    }
}
