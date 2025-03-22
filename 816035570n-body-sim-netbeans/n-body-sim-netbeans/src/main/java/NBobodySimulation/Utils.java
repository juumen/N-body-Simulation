package NBobodySimulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Utils {
    public static javafx.scene.paint.Color convertAWTColorToFXColor(java.awt.Color awtColor) {
        int red = awtColor.getRed();
        int green = awtColor.getGreen();
        int blue = awtColor.getBlue();
        int alpha = awtColor.getAlpha();
        return javafx.scene.paint.Color.rgb(red, green, blue, alpha / 255.0);
    }

    public static double[] flattenBodies(ArrayList<OrbitalBody> bodies) {
        if (bodies.isEmpty()) return null;
        double[] flattened = new double[bodies.size() * bodies.get(0).getDimensions() * 2];
        int index = 0;
        for (OrbitalBody body : bodies) {
            double[] fb = body.flatten();
            for (double component : fb) {
                flattened[index++] = component;
            }
        }
        return flattened;
    }

    // Uses reflection to dynamically load and cache classes at runtime
    private static final Map<String, Class<?>> classCache = new HashMap<>();
    public static Class<?> loadClass(String className, String backupClassName) {
        if(classCache.containsKey(className)) return classCache.get(className);
        if(classCache.containsKey(backupClassName)) return classCache.get(backupClassName);
        try {
            Class<?> cl = Class.forName(className);
            classCache.put(className, cl);
            return cl;
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + className);
            System.err.println("Attempting to load class: " + backupClassName + " instead");
            try {
                Class<?> cl = Class.forName(backupClassName);
                classCache.put(backupClassName, cl);
                return cl;
            } catch (ClassNotFoundException e2) {
                System.err.println("Class not found: " + backupClassName);
                System.err.println("Terminating...");
                System.exit(1);
            }
        }
        return null;
    }
}
