import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

class SumoGame extends JPanel implements Runnable, KeyListener {
    private enum GameState { MENU, PLAYING_2P, PAUSED }

    private GameState currentState = GameState.MENU;
    private Thread gameThread;
    private boolean running = false;

    private List<Player> players;
    private List<Ability> abilities;
    private int abilitySpawnTimer = 180; 
    
    private Vector2D arenaCenter;
    private double arenaRadius = 250;

    // Key states
    private boolean w, a, s, d, up, down, left, right;

    public SumoGame() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(240, 240, 240));
        
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                keyPressed(e);
            } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                keyReleased(e);
            }
            return false;
        });
        
        arenaCenter = new Vector2D(400, 300);
        initPlayers();
    }

    private void initPlayers() {
        players = new ArrayList<>();
        abilities = new ArrayList<>();
        
        players.add(new Player("Player 1", 250, 300, new Color(70, 130, 180)));
        players.add(new Player("Player 2", 550, 300, new Color(220, 20, 60)));
        
        resetRound();
    }

    private void resetRound() {
        for (Player p : players) {
            p.resetPosition();
        }
        abilities.clear();
        
        // Spawn an ability right in the middle at the start of every roun
        Ability.Type type = Math.random() > 0.5 ? Ability.Type.SPEED : Ability.Type.HEAVY;
        abilities.add(new Ability(arenaCenter.x, arenaCenter.y, type));
        
        abilitySpawnTimer = 300 + (int)(Math.random() * 300); // 5 to 10 secs before next one
    }

    private void spawnAbility() {
        double x, y;
        
        // 25% chance to spawn an ability in the exact center throughout the match
        if (Math.random() < 0.25) {
            x = arenaCenter.x;
            y = arenaCenter.y;
        } else {
            double angle = Math.random() * Math.PI * 2;
            double r = Math.random() * (arenaRadius - 30);
            x = arenaCenter.x + r * Math.cos(angle);
            y = arenaCenter.y + r * Math.sin(angle);
        }
        
        Ability.Type type = Math.random() > 0.5 ? Ability.Type.SPEED : Ability.Type.HEAVY;
        abilities.add(new Ability(x, y, type));
    }

    public void start() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000D / 60D;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta--;
            }
            repaint();
            
            try { Thread.sleep(2); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private void update() {
        if (currentState != GameState.PLAYING_2P) return;

        Player p1 = players.get(0);
        Player p2 = players.get(1);

        // Apply inputs for Player 1
        double p1DirX = 0, p1DirY = 0;
        if (w) p1DirY -= 1;
        if (s) p1DirY += 1;
        if (a) p1DirX -= 1;
        if (d) p1DirX += 1;
        p1.applyForce(p1DirX, p1DirY);

        // Apply inputs for Player 2
        double p2DirX = 0, p2DirY = 0;
        if (up) p2DirY -= 1;
        if (down) p2DirY += 1;
        if (left) p2DirX -= 1;
        if (right) p2DirX += 1;
        p2.applyForce(p2DirX, p2DirY);

        // Update physics
        for (Player p : players) {
            p.update();
        }

        // Handle Player-to-Player Collisions
        if (p1.isCollidingWith(p2)) {
            resolveCollision(p1, p2);
        }

        // Handle Ability Spawning
        abilitySpawnTimer--;
        if (abilitySpawnTimer <= 0) {
            if (abilities.size() < 3) { 
                spawnAbility();
            }
            abilitySpawnTimer = 300 + (int)(Math.random() * 300); 
        }

        // Handle Ability Collecting
        for (int i = 0; i < abilities.size(); i++) {
            Ability ab = abilities.get(i);
            if (ab.isCollidingWith(p1)) {
                p1.applyAbility(ab.type);
                abilities.remove(i);
                break; 
            } else if (ab.isCollidingWith(p2)) {
                p2.applyAbility(ab.type);
                abilities.remove(i);
                break;
            }
        }

        // Handle Ring Outs
        for (Player p : players) {
            if (p.position.distance(arenaCenter) > arenaRadius) {
                p.loseLife();
                
                // Check win condition
                if (players.get(0).getLives() == 0 || players.get(1).getLives() == 0) {
                    currentState = GameState.MENU;
                    initPlayers(); // Reset full game
                } else {
                    resetRound(); // Reset the round elements and placements
                }
                break;
            }
        }
    }

    private void resolveCollision(Player p1, Player p2) {
        double dx = p2.position.x - p1.position.x;
        double dy = p2.position.y - p1.position.y;
        double distance = p1.position.distance(p2.position);

        if (distance == 0) return; 

        double nx = dx / distance;
        double ny = dy / distance;

        double bouncePower = 3.5;

        p1.velocity.x -= nx * (bouncePower / p1.mass);
        p1.velocity.y -= ny * (bouncePower / p1.mass);

        p2.velocity.x += nx * (bouncePower / p2.mass);
        p2.velocity.y += ny * (bouncePower / p2.mass);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentState == GameState.MENU) {
            drawMenu(g2d);
        } else if (currentState == GameState.PLAYING_2P) {
            drawGame(g2d);
        } else if (currentState == GameState.PAUSED) {
            drawGame(g2d);
            drawPause(g2d);
        }
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString("SUMO GAME", 250, 150);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("[Press SPACE to Play 2P]", 270, 250);
        
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Player 1 Controls: W, A, S, D  |  Dash: SPACE", 180, 320);
        g.drawString("Player 2 Controls: ARROWS      |  Dash: ENTER", 180, 360);
    }

    private void drawGame(Graphics2D g) {
        // Draw Arena
        g.setColor(Color.WHITE);
        g.fillOval((int) (arenaCenter.x - arenaRadius), (int) (arenaCenter.y - arenaRadius), 
                   (int) (arenaRadius * 2), (int) (arenaRadius * 2));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3));
        g.drawOval((int) (arenaCenter.x - arenaRadius), (int) (arenaCenter.y - arenaRadius), 
                   (int) (arenaRadius * 2), (int) (arenaRadius * 2));

        // Draw Abilities
        for (Ability a : abilities) {
            a.draw(g);
        }

        // Draw Players
        for (Player p : players) {
            p.draw(g);
        }

        // Draw UI (Lives)
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(players.get(0).color);
        g.drawString(players.get(0).getName() + " Lives: " + players.get(0).getLives(), 20, 30);
        
        g.setColor(players.get(1).color);
        g.drawString(players.get(1).getName() + " Lives: " + players.get(1).getLives(), 620, 30);
        
        g.setColor(Color.DARK_GRAY);
        g.drawString("[ESC] Pause", 340, 30);
    }

    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString("PAUSED", 300, 250);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        g.drawString("[Press ESC to Resume]", 280, 300);
        g.drawString("[Press M for Menu]", 295, 340);
    }

    //INPUT HANDLING
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (currentState == GameState.MENU) {
            if (key == KeyEvent.VK_SPACE) currentState = GameState.PLAYING_2P;
        } else if (currentState == GameState.PLAYING_2P) {
            
            // Movement P1
            if (key == KeyEvent.VK_W) w = true;
            if (key == KeyEvent.VK_S) s = true;
            if (key == KeyEvent.VK_A) a = true;
            if (key == KeyEvent.VK_D) d = true;
            if (key == KeyEvent.VK_SPACE) players.get(0).dash(); // P1 Dash

            // Movement P2
            if (key == KeyEvent.VK_UP) up = true;
            if (key == KeyEvent.VK_DOWN) down = true;
            if (key == KeyEvent.VK_LEFT) left = true;
            if (key == KeyEvent.VK_RIGHT) right = true;
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_NUMPAD0) players.get(1).dash(); // P2 Dash

            if (key == KeyEvent.VK_ESCAPE) currentState = GameState.PAUSED;
            
        } else if (currentState == GameState.PAUSED) {
            if (key == KeyEvent.VK_ESCAPE) currentState = GameState.PLAYING_2P;
            if (key == KeyEvent.VK_M) currentState = GameState.MENU;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) w = false;
        if (key == KeyEvent.VK_S) s = false;
        if (key == KeyEvent.VK_A) a = false;
        if (key == KeyEvent.VK_D) d = false;
        
        if (key == KeyEvent.VK_UP) up = false;
        if (key == KeyEvent.VK_DOWN) down = false;
        if (key == KeyEvent.VK_LEFT) left = false;
        if (key == KeyEvent.VK_RIGHT) right = false;
    }

    @Override public void keyTyped(KeyEvent e) {}
}