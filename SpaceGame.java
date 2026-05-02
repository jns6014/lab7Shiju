/** Project: Solo Lab 7
 * Purpose Details: Space Game with new features added
 * Course: IST 242
 * Author: Joseph Shiju
 * Date Developed: 4/28/2026
 * Last Date Changed: 5/2/2026
 * Rev: 1
 */

import javax.swing.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class SpaceGame extends JFrame implements KeyListener {
    private static final int WIDTH = 600;

    private static final int HEIGHT = 600;

    private static final int PLAYER_WIDTH = 80;

    private static final int PLAYER_HEIGHT = 80;

    private static final int OBSTACLE_WIDTH = 60;

    private static final int OBSTACLE_HEIGHT = 60;

    private static final int PROJECTILE_WIDTH = 5;

    private static final int PROJECTILE_HEIGHT = 10;

    private static final int PLAYER_SPEED = 10;

    private static final int OBSTACLE_SPEED = 3;

    private static final int OBSTACLE_SPEED2 = 6;

    private static final int PROJECTILE_SPEED = 10;

    private static final int MAX_HEALTH = 100;

    private static final int POWERUP_HEAL = 20;

    private static final int POWERUP_SIZE = 50;

    private static final int SHIELD_DURATION = 250;

    private int score = 0;

    private int health = MAX_HEALTH;

    private boolean isGameOver = false;

    private boolean gameWon = false;

    private boolean isProjectileVisible = false;

    private boolean isFiring = false;

    private int level = 1;

    private int countdownSeconds = 90;

    private int shieldTimer = 0;

    private int spriteFrame = 0;

    private int spriteCount = 0;

    private int playerX;

    private int playerY;

    private int projectileX;

    private int projectileY;

    private ArrayList<Point> obstacles = new ArrayList<Point>();

    private ArrayList<Point> powerUps = new ArrayList<Point>();

    private int[][] stars;

    private BufferedImage spriteSheet;

    private Image shipImage;

    private Image healthboxImage;

    private JPanel gamePanel;

    private Timer timer;

    private Timer cTimer;

    private Random rand = new Random();

    /**
     * Constructor sets up the game window and starts everything
     */
    public SpaceGame() {
        setTitle("Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 30;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;

        makeStars(80);
        makeSpriteSheet();
        loadImages();

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };
        gamePanel.setBackground(Color.BLACK);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);
        add(gamePanel);

        timer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver && !gameWon) {
                    update();
                    gamePanel.repaint();
                }
            }
        });
        timer.start();
        cTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver && !gameWon) {
                    countdownSeconds--;
                    if (countdownSeconds <= 0) {
                        countdownSeconds = 0;
                        isGameOver = true;
                        gamePanel.repaint();
                    }
                }
            }
        });
        cTimer.start();
    }

    /**
     * Makes random stars for the background
     * @param count how many stars to make
     */
    private void makeStars(int count) {
        stars = new int[count][6];
        for (int i = 0; i < count; i++) {
            stars[i][0] = rand.nextInt(WIDTH);
            stars[i][1] = rand.nextInt(HEIGHT);
            stars[i][2] = rand.nextInt(3) + 1;
            stars[i][3] = 150 + rand.nextInt(106);
            stars[i][4] = 150 + rand.nextInt(106);
            stars[i][5] = 150 + rand.nextInt(106);
        }
    }

    /**
     * Makes the sprite sheet for obstacles (4x1)
     * Loads asteroid.png and copies it into all 4 frames of the sheet
     */
    private void makeSpriteSheet() {
        BufferedImage asteroidImg = null;
        try {
            asteroidImg = javax.imageio.ImageIO.read(new java.io.File("asteroid.png"));
        } catch (Exception e) {
            asteroidImg = null;
        }

        spriteSheet = new BufferedImage(OBSTACLE_WIDTH * 4, OBSTACLE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = spriteSheet.createGraphics();

        if (asteroidImg != null) {
            // copy the same image into all 4 frames
            for (int i = 0; i < 4; i++) {
                g2.drawImage(asteroidImg, i * OBSTACLE_WIDTH, 0, OBSTACLE_WIDTH, OBSTACLE_HEIGHT, null);
            }
        } else {
            // fallback shapes if image missing
            Color[] colors = {Color.RED, Color.ORANGE, Color.YELLOW, new Color(255, 80, 80)};
            for (int i = 0; i < 4; i++) {
                int x = i * OBSTACLE_WIDTH;
                g2.setColor(colors[i]);
                g2.fillOval(x + 2, 2, OBSTACLE_WIDTH - 4, OBSTACLE_HEIGHT - 4);
                g2.setColor(colors[i].darker());
                g2.fillOval(x + 12, 10, 12, 12);
                g2.setColor(Color.WHITE);
                g2.fillOval(x + 6, 5, 6, 4);
            }
        }
        g2.dispose();
    }

    /**
     * Loads ship.png and healthbox.png from the project folder
     */
    private void loadImages() {
        try {
            shipImage = javax.imageio.ImageIO.read(new java.io.File("ship.png"));
        } catch (Exception e) {
            shipImage = null;
        }
        try {
            healthboxImage = javax.imageio.ImageIO.read(new java.io.File("healthbox.png"));
        } catch (Exception e) {
            healthboxImage = null;
        }
    }

    /**
     * Plays a sound for fire or collision using wav files
     * @param filename the wav file to play
     */
    private void playSound(String filename) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioInputStream audio = AudioSystem.getAudioInputStream(new java.io.File(filename));
                    Clip clip = AudioSystem.getClip();
                    clip.open(audio);
                    clip.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Draws everything on the screen
     * @param g graphics object
     */
    private void draw(Graphics g) {
        // black background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // draw stars (feature 1 - random colored stars)
        for (int i = 0; i < stars.length; i++) {
            g.setColor(new Color(stars[i][3], stars[i][4], stars[i][5]));
            g.fillOval(stars[i][0], stars[i][1], stars[i][2], stars[i][2]);
        }

        // draw obstacles using sprite sheet (feature 4)
        for (int i = 0; i < obstacles.size(); i++) {
            Point obs = obstacles.get(i);
            int sx = spriteFrame * OBSTACLE_WIDTH;
            g.drawImage(spriteSheet, obs.x, obs.y, obs.x + OBSTACLE_WIDTH, obs.y + OBSTACLE_HEIGHT,
                    sx, 0, sx + OBSTACLE_WIDTH, OBSTACLE_HEIGHT, null);
        }

        // draw powerups using healthbox.png (feature 8)
        for (int i = 0; i < powerUps.size(); i++) {
            Point p = powerUps.get(i);
            if (healthboxImage != null) {
                g.drawImage(healthboxImage, p.x, p.y, POWERUP_SIZE, POWERUP_SIZE, null);
            } else {
                // fallback if image not loaded
                g.setColor(new Color(0, 220, 80));
                g.fillRect(p.x, p.y, POWERUP_SIZE, POWERUP_SIZE);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("+", p.x + 5, p.y + 14);
            }
        }

        // draw shield if active (feature 6)
        if (shieldTimer > 0) {
            g.setColor(new Color(0, 150, 255, 100));
            ((Graphics2D)g).fillOval(playerX - 8, playerY - 8, PLAYER_WIDTH + 16, PLAYER_HEIGHT + 16);
            g.setColor(new Color(0, 200, 255));
            g.drawOval(playerX - 8, playerY - 8, PLAYER_WIDTH + 16, PLAYER_HEIGHT + 16);
        }

        // draw player ship (feature 2 - ship.png)
        if (shipImage != null) {
            g.drawImage(shipImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);
        } else {
            // fallback if image not loaded
            int[] xPoints = {playerX + PLAYER_WIDTH / 2, playerX, playerX + PLAYER_WIDTH};
            int[] yPoints = {playerY, playerY + PLAYER_HEIGHT, playerY + PLAYER_HEIGHT};
            g.setColor(new Color(30, 144, 255));
            g.fillPolygon(xPoints, yPoints, 3);
            g.setColor(Color.CYAN);
            g.drawPolygon(xPoints, yPoints, 3);
            g.setColor(Color.ORANGE);
            g.fillOval(playerX + PLAYER_WIDTH / 2 - 8, playerY + PLAYER_HEIGHT - 4, 16, 10);
        }

        // draw bullet
        if (isProjectileVisible) {
            g.setColor(Color.YELLOW);
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        // draw score in blue (feature 3)
        g.setColor(Color.BLUE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 20);

        // draw health bar (feature 7)
        g.setColor(Color.DARK_GRAY);
        g.fillRect(10, 28, 150, 14);
        if (health > 50) {
            g.setColor(Color.GREEN);
        } else if (health > 25) {
            g.setColor(Color.YELLOW);
        } else {
            g.setColor(Color.RED);
        }
        g.fillRect(10, 28, (int)(150 * health / 100.0), 14);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("HP: " + health, 14, 40);

        // draw countdown timer (feature 9)
        if (countdownSeconds <= 10) {
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.WHITE);
        }
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Time: " + countdownSeconds, WIDTH - 110, 20);

        // draw shield status
        if (shieldTimer > 0) {
            g.setColor(Color.CYAN);
        } else {
            g.setColor(Color.GRAY);
        }
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Shield[S]: " + (shieldTimer > 0 ? "ON" : "OFF"), WIDTH - 120, 38);

        // draw shield timer bar so player can see how long shield has left
        g.setColor(Color.DARK_GRAY);
        g.fillRect(WIDTH - 120, 42, 100, 8);
        if (shieldTimer > 0) {
            g.setColor(Color.CYAN);
            int barWidth = (int)(100 * shieldTimer / (double) SHIELD_DURATION);
            g.fillRect(WIDTH - 120, 42, barWidth, 8);
        }

        // draw level
        g.setColor(Color.ORANGE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Level: " + level, WIDTH / 2 - 30, 20);

        // game over screen
        if (isGameOver) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("GAME OVER", WIDTH / 2 - 110, HEIGHT / 2 - 20);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Score: " + score, WIDTH / 2 - 45, HEIGHT / 2 + 20);
            g.drawString("Press R to play again", WIDTH / 2 - 100, HEIGHT / 2 + 50);
        }

        // win screen
        if (gameWon) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.GREEN);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("YOU WIN!", WIDTH / 2 - 90, HEIGHT / 2 - 20);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Score: " + score, WIDTH / 2 - 45, HEIGHT / 2 + 20);
            g.drawString("Press R to play again", WIDTH / 2 - 100, HEIGHT / 2 + 50);
        }
    }

    /**
     * Updates the game every tick - moves things and checks collisions
     */
    private void update() {
        int speed = OBSTACLE_SPEED;
        if (level == 2) {
            speed = OBSTACLE_SPEED2;
        }

        // animate sprite sheet
        spriteCount++;
        if (spriteCount >= 8) {
            spriteFrame++;
            if (spriteFrame >= 4) {
                spriteFrame = 0;
            }
            spriteCount = 0;
        }

        // count down shield
        if (shieldTimer > 0) {
            shieldTimer--;
        }

        // move obstacles down
        for (int i = 0; i < obstacles.size(); i++) {
            obstacles.get(i).y += speed;
        }

        // remove obstacles that went off screen
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            if (obstacles.get(i).y > HEIGHT) {
                obstacles.remove(i);
            }
        }

        // randomly add new obstacle
        if (Math.random() < 0.02) {
            int ox = (int)(Math.random() * (WIDTH - OBSTACLE_WIDTH));
            obstacles.add(new Point(ox, 0));
        }

        // move bullet up
        if (isProjectileVisible) {
            projectileY -= PROJECTILE_SPEED;
            if (projectileY < 0) {
                isProjectileVisible = false;
            }
        }

        // move powerups down
        for (int i = 0; i < powerUps.size(); i++) {
            powerUps.get(i).y += 2;
        }

        // remove powerups off screen
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            if (powerUps.get(i).y > HEIGHT) {
                powerUps.remove(i);
            }
        }

        // randomly spawn powerup
        if (Math.random() < 0.004) {
            int px = (int)(Math.random() * (WIDTH - POWERUP_SIZE));
            powerUps.add(new Point(px, 0));
        }

        // shrink the hit boxes by 15px on each side so collision feels fair
        int shrink = 15;
        Rectangle playerRect = new Rectangle(playerX + shrink, playerY + shrink, PLAYER_WIDTH - shrink * 2, PLAYER_HEIGHT - shrink * 2);

        // check if player hit an obstacle
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Point obs = obstacles.get(i);
            Rectangle obsRect = new Rectangle(obs.x + shrink, obs.y + shrink, OBSTACLE_WIDTH - shrink * 2, OBSTACLE_HEIGHT - shrink * 2);
            if (playerRect.intersects(obsRect)) {
                obstacles.remove(i);
                if (shieldTimer <= 0) {
                    health -= 20;
                    playSound("collision.wav");
                    if (health <= 0) {
                        health = 0;
                        isGameOver = true;
                    }
                }
            }
        }

        // check if bullet hit an obstacle
        if (isProjectileVisible) {
            Rectangle bulletRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
            for (int i = obstacles.size() - 1; i >= 0; i--) {
                Rectangle obsRect = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                if (bulletRect.intersects(obsRect)) {
                    obstacles.remove(i);
                    score += 10;
                    isProjectileVisible = false;
                    break;
                }
            }
        }

        // check if player picked up a powerup
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            Rectangle pRect = new Rectangle(powerUps.get(i).x, powerUps.get(i).y, POWERUP_SIZE, POWERUP_SIZE);
            if (playerRect.intersects(pRect)) {
                powerUps.remove(i);
                health = health + POWERUP_HEAL;
                if (health > MAX_HEALTH) {
                    health = MAX_HEALTH;
                }
            }
        }

        // level 2 starts at 100 points (feature 10)
        if (score >= 100 && level == 1) {
            level = 2;
        }

        // win if you reach 250 points
        if (score >= 250) {
            gameWon = true;
        }
    }

    /**
     * Handles key presses for moving shooting and shield
     * @param e the key event
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= PLAYER_SPEED;
        }
        if (key == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH) {
            playerX += PLAYER_SPEED;
        }
        if (key == KeyEvent.VK_UP && playerY > 0) {
            playerY -= PLAYER_SPEED;
        }
        if (key == KeyEvent.VK_DOWN && playerY < HEIGHT - PLAYER_HEIGHT) {
            playerY += PLAYER_SPEED;
        }
        if (key == KeyEvent.VK_SPACE && !isFiring) {
            isFiring = true;
            isProjectileVisible = true;
            projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY = playerY;
            playSound("fire.wav");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(400);
                        isFiring = false;
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();
        }
        // press S for shield (feature 6)
        if (key == KeyEvent.VK_S) {
            shieldTimer = SHIELD_DURATION;
        }
        // press R to restart
        if (key == KeyEvent.VK_R && (isGameOver || gameWon)) {
            restartGame();
        }
    }

    /**
     * Not used but required by KeyListener
     * @param e key event
     */
    @Override
    public void keyTyped(KeyEvent e) {}

    /**
     * Not used but required by KeyListener
     * @param e key event
     */
    @Override
    public void keyReleased(KeyEvent e) {}

    /**
     * Resets everything to start a new game
     */
    private void restartGame() {
        score = 0;
        health = MAX_HEALTH;
        isGameOver = false;
        gameWon = false;
        level = 1;
        countdownSeconds = 90;
        shieldTimer = 0;
        isProjectileVisible = false;
        isFiring = false;
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 30;
        obstacles.clear();
        powerUps.clear();
        makeStars(80);
        timer.restart();
        cTimer.restart();
        gamePanel.repaint();
    }

    /**
     * Main method - starts the game
     * @param args not used
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SpaceGame().setVisible(true);
            }
        });
    }
}