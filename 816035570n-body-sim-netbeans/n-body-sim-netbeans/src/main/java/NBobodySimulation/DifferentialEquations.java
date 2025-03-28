package NBobodySimulation;

//import org.apache.commons.math4.legacy.ode.FirstOrderDifferentialEquations;
 import org.apache.commons.math3.ode.FirstOrderDifferentialEquations; // If you're using Maven, use this import statement instead

/**
 * A class that represents the differential equations, which govern the movement of particles in an arbitrary number of dimensions.
 */
class DifferentialEquations implements FirstOrderDifferentialEquations {

    /**
     * Universal gravitational constant, in in km^3 * earthmasses^-1 * seconds^-2
     */
    private static final double G = 398575.0725;

    /**
     * Stores the x and y accelerations of each particle so other classes can access them.
     */
    private static double[][] accelerationStorage;

    /**
     * The mass of each particle.
     */
    private final double[] masses;

    /**
     * Initializes a differential equation set based on given masses.
     *
     * @param masses The masses of each particle, in an array.
     */
    DifferentialEquations(double[] masses) {
        this.masses = masses;
        accelerationStorage = new double[masses.length][2];
    }

    /**
     * Gets the acceleration for a particle and a dimension.
     *
     * @param particleID The particle with acceleration.
     * @param dimension  The dimension of acceleration.
     * @return The acceleration.
     */
    static double getAcceleration(int particleID, int dimension) {
        return accelerationStorage[particleID][dimension]; // Particle IDs start with 1, but indices start with 0.
    }

    /**
     * Gets the dimensions of the differential equations. From the interface.
     *
     * @return The dimensions of the equations. Always 12.
     */
    @Override
    public int getDimension() {
        return masses.length * 2 * 2; // n particles * 2 dimensions * 2 derivatives (both displacement -> velocity and velocity -> acceleration)
    }

    /**
     * Takes the derivative of an array and stores it an another array. From the interface.
     *
     * @param t    The current time. Parameter inherited from interface, not used.
     * @param y    The initial state of the function. <br>[X-pos of particle 1, Y-pos of particle 1, X-vel of particle 1, Y-vel of particle 1, X-pos of particle 2, Y-pos of particle 2, X-vel of particle 2, Y-vel of particle 2, X-pos of particle 3, Y-pos of particle 3, X-vel of particle 3, Y-vel of particle 3]
     * @param yDot The array where the derivatives are stored. <br>[X-vel of particle 1, Y-vel of particle 1, X-acc of particle 1, Y-acc of particle 1, X-vel of particle 2, Y-vel of particle 2, X-acc of particle 2, Y-acc of particle 2, X-vel of particle 3, Y-vel of particle 3, X-acc of particle 3, Y-acc of particle 3]
     */
    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        int numParticles = masses.length;

        // Copy velocity from y to yDot in the correct position.
        for (int particle = 0; particle < numParticles; particle++) {
            yDot[4 * particle] = y[4 * particle + 2];
            yDot[4 * particle + 1] = y[4 * particle + 3];
        }

        // Reset acceleration
        accelerationStorage = new double[numParticles][2];

        // Calculate acceleration on each object from every other object.
        for (int i = 0; i < numParticles; i++) {
            for (int j = i + 1; j < numParticles; j++) {
                double dx = y[4 * j] - y[4 * i];
                double dy = y[4 * j + 1] - y[4 * i + 1];
                addToAcceleration(new double[]{dx, dy}, masses[i], masses[j], i, j);
            }
        }

        // Set yDot acceleration indices to the correct value
        for (int particle = 0; particle < numParticles; particle++) {
            yDot[4 * particle + 2] = accelerationStorage[particle][0];
            yDot[4 * particle + 3] = accelerationStorage[particle][1];
        }
    }

    /**
     * Calculates acceleration due to gravity between two different objects and stores it in the accelerationStorage array.
     *
     * @param vector The distance between the objects.
     * @param mass1  The mass of object 1.
     * @param mass2  The mass of object 2.
     * @param id1    The id of object 1 in the storage array.
     * @param id2    The id of object 2 in the storage array.
     */
    private void addToAcceleration(double[] vector, double mass1, double mass2, int id1, int id2) {
        // Absolute value of vector, according to pythagorean theorem
        double absoluteDistance = Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2));

        // Common factors of acceleration: vector / absVector^3
        double[] baseAcceleration;
        if (absoluteDistance != 0) {
            baseAcceleration = new double[]{vector[0] / Math.pow(absoluteDistance, 3), vector[1] / Math.pow(absoluteDistance, 3)};
        } else {
            // obviously zero if there is no distance between two objects.
            baseAcceleration = new double[]{0, 0};
        }

        // individual coefficients for each acceleration
        double massFactor1 = mass2 * G;
        // Reversed for mass2, because vectors have direction
        double massFactor2 = -1 * mass1 * G;

        // Multiply together.
        for (int i = 0; i < 2; i++) {
            accelerationStorage[id1][i] += massFactor1 * baseAcceleration[i];
            accelerationStorage[id2][i] += massFactor2 * baseAcceleration[i];
        }
    }
}
