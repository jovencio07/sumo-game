import java.awt.Color;
import java.awt.Graphics2D;

public class PhysicsEntity extends GameObject {
    public Vector2D velocity;
    public double radius;
    public double mass = 2.0; // Used for collision knockback
    protected double friction = 0.90; // Determines how slippery

    public PhysicsEntity(double x, double y, double radius, Color color) {
        super(x, y, color);
        this.velocity = new Vector2D(0, 0);
        this.radius = radius;
    }

    @Override
    public void update() {
        position.add(velocity);
        velocity.multiply(friction);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(color);
        g.fillOval((int) (position.x - radius), (int) (position.y - radius), (int) (radius * 2), (int) (radius * 2));
    }

    public boolean isCollidingWith(PhysicsEntity other) {
        return this.position.distance(other.position) < (this.radius + other.radius);
    }
}
