import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;

class Player extends PhysicsEntity {
    private int lives;
    private double baseAcceleration = 0.8;
    private double acceleration = baseAcceleration;
    private Vector2D spawnPoint;
    private String name;

    // Ability States
    private int abilityTimer = 0;
    private Ability.Type activeAbility = null;

    // Dash States
    private int dashCooldown = 0;
    private double dashPower = 18.0; 
    private double lastDirX = 0;
    private double lastDirY = 0;

    public Player(String name, double x, double y, Color color) {
        super(x, y, 30, color);
        this.spawnPoint = new Vector2D(x, y);
        this.lives = 3;
        this.name = name;
    }

    public void applyForce(double dirX, double dirY) {
        if (dirX == 0 && dirY == 0) return;
        
        
        double mag = Math.hypot(dirX, dirY);
        double normX = dirX / mag;
        double normY = dirY / mag;
        
        this.velocity.x += normX * acceleration;
        this.velocity.y += normY * acceleration;
        
        
        this.lastDirX = normX;
        this.lastDirY = normY;
    }

    public void dash() {
        if (dashCooldown <= 0) {
            double dirX = lastDirX;
            double dirY = lastDirY;
            
            // If they haven't moved yet default face towards the center
            if (dirX == 0 && dirY == 0) {
                dirX = (spawnPoint.x < 400) ? 1 : -1;
            }
            
            double mag = Math.hypot(dirX, dirY);
            if (mag > 0) {
                this.velocity.x += (dirX / mag) * dashPower;
                this.velocity.y += (dirY / mag) * dashPower;
                this.dashCooldown = 180; // 3 seconds cooldown
            }
        }
    }

    public void applyAbility(Ability.Type type) {
        this.activeAbility = type;
        this.abilityTimer = 300; 
        
        if (type == Ability.Type.SPEED) {
            this.acceleration = 1.6; 
            this.mass = 1.0;         
        } else if (type == Ability.Type.HEAVY) {
            this.acceleration = baseAcceleration; 
            this.mass = 3.5;                      
        }
    }

    @Override
    public void update() {
        super.update(); // Handles physics
        
        // Handle dash cooldown decay
        if (dashCooldown > 0) {
            dashCooldown--;
        }
        
        // Handle ability decay
        if (abilityTimer > 0) {
            abilityTimer--;
            if (abilityTimer <= 0) {
                this.activeAbility = null;
                this.acceleration = baseAcceleration;
                this.mass = 1.0;
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {
        super.draw(g); // Draw standard player
        
        // Draw an aura around the player if they have an active ability
        if (activeAbility != null) {
            g.setColor(activeAbility == Ability.Type.SPEED ? Color.YELLOW : Color.DARK_GRAY);
            g.setStroke(new BasicStroke(4));
            g.drawOval((int) (position.x - radius - 6), (int) (position.y - radius - 6), 
                       (int) (radius * 2 + 12), (int) (radius * 2 + 12));
        }

        // Draw Dash Cooldown Bar
        int barWidth = (int)(radius * 2);
        int barHeight = 5;
        int barX = (int)(position.x - radius);
        int barY = (int)(position.y + radius + 10);
        
        // Background
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Foreground
        if (dashCooldown == 0) {
            g.setColor(Color.GREEN); // Ready to dash
        } else {
            g.setColor(new Color(255, 80, 80)); // Cooldown recovering
            barWidth = (int)((1.0 - (dashCooldown / 180.0)) * barWidth);
        }
        g.fillRect(barX, barY, barWidth, barHeight);
    }

    public void resetPosition() {
        this.position.x = spawnPoint.x;
        this.position.y = spawnPoint.y;
        this.velocity.x = 0;
        this.velocity.y = 0;
        
        this.dashCooldown = 0;
        this.lastDirX = 0;
        this.lastDirY = 0;
        
        this.abilityTimer = 0;
        this.activeAbility = null;
        this.acceleration = baseAcceleration;
        this.mass = 1.0;
    }

    public void loseLife() {
        if (lives > 0) lives--;
    }

    public int getLives() { return lives; }
    public String getName() { return name; }
}