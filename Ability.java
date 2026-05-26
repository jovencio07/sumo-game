import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class Ability extends GameObject {
    public enum Type { SPEED, HEAVY }
    
    public Type type;
    public double radius = 15;

    public Ability(double x, double y, Type type) {
        // Speed is Yellow, Heavy is Dark Gray
        super(x, y, type == Type.SPEED ? Color.YELLOW : Color.DARK_GRAY);
        this.type = type;
    }

    @Override
    public void update() {
        // Static object, no physics needed
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(color);
        g.fillOval((int) (position.x - radius), (int) (position.y - radius), (int) (radius * 2), (int) (radius * 2));
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        // Draw an 'S' or 'H' to tell them apart
        g.drawString(type == Type.SPEED ? "S" : "H", (int) position.x - 5, (int) position.y + 5);
    }
    
    public boolean isCollidingWith(Player p) {
        return this.position.distance(p.position) < (this.radius + p.radius);
    }
}
