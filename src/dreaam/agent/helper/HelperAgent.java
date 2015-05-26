package dreaam.agent.helper;

import dreaam.developer.TaskModelEditor;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Map;
import sami.engine.Mediator;
import sami.mission.Place;
import sami.mission.Transition;
import sami.mission.Vertex;

/**
 *
 * @author pscerri
 */
public abstract class HelperAgent {

    private boolean enabled = true;
    public Mediator mediator = Mediator.getInstance();

    abstract public void run();

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static Point getVertexPoint(Map<Vertex, Point2D> locations, boolean forTransition) {
        if (locations == null) {
            return null;
        }
        if(locations.isEmpty()) {
            return new Point(0, 0);
        }
        double maxXTransition = Double.MIN_VALUE;
        double maxXPlace = Double.MIN_VALUE;
        double minYTransition = Double.MAX_VALUE;
        double minYPlace = Double.MAX_VALUE;
        for (Vertex vertex : locations.keySet()) {
            Point2D point = locations.get(vertex);
            if (vertex instanceof Place) {
                if (point.getX() > maxXPlace) {
                    maxXPlace = point.getX();
                }
                if (point.getY() < minYPlace) {
                    minYPlace = point.getY();
                }
            } else if (vertex instanceof Transition) {
                if (point.getX() > maxXTransition) {
                    maxXTransition = point.getX();
                }
                if (point.getY() < minYTransition) {
                    minYTransition = point.getY();
                }
            }
        }
        Point vertexPoint = null;
        if (forTransition) {
            vertexPoint = new Point((int) (maxXTransition), (int) (Math.min(minYPlace, minYTransition) - TaskModelEditor.GRID_LENGTH));
        } else {
            vertexPoint = new Point((int) (maxXPlace), (int) (Math.min(minYPlace, minYTransition) - TaskModelEditor.GRID_LENGTH));
        }
        vertexPoint = TaskModelEditor.snapToGrid(vertexPoint);
        return vertexPoint;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
