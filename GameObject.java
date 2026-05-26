import java.awt.Color;

public abstract class GameObject implements Renderable, Updatable {
    public Vector2D position;
    protected Color color;

    public GameObject(double x, double y, Color color) {
        this.position = new Vector2D(x, y);
        this.color = color;
    }
}
