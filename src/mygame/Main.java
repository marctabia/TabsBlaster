package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.awt.Rectangle;
import java.util.Random;

/**
 * test
 * @author normenhansen
 */
public class Main extends SimpleApplication {
    
    // player
    private Spatial player;
    
    // bullet
    private Node bulletNode;
    private long bulletCooldown;
    
    // enemies
    private Node enemyNode;
    private long enemySpawnCooldown;
    private float enemySpawnChance = 80;
    
    // sound
    private Sound sound;
    
    // black hole
    private long spawnBlackHoleCoolDown;
    private Node blackHoleNode;
    
    // hud
    private Hud hud;
    
    // grid
    private Grid grid;
    
    // pariticleManager
    private ParticleManager particleManager;
    
    // particle node
    private Node particleNode;
    
    private boolean gameOver = false;
    
    
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // setup camera for 2d games
        cam.setParallelProjection(true);
        cam.setLocation(new Vector3f(0, 0, 0.5f));
        getFlyByCamera().setEnabled(false);
        
        // This part is for disabling the stats view window and the fps counter.
        setDisplayStatView(false);
        setDisplayFps(false);
        
        // setting bloom effects
        FilterPostProcessor fpp=new FilterPostProcessor(assetManager);
        BloomFilter bloom=new BloomFilter();
        bloom.setBloomIntensity(2f);
        bloom.setExposurePower(2);
        bloom.setExposureCutOff(0f);
        bloom.setBlurScale(1.5f);
        fpp.addFilter(bloom);
        guiViewPort.addProcessor(fpp);
        guiViewPort.setClearColor(true);
        
        // setup grid
        Rectangle size = new Rectangle(0, 0, settings.getWidth(), settings.getHeight());
        Vector2f spacing = new Vector2f(25,25);
        grid = new Grid(size, spacing, guiNode, assetManager);
        
        // custom cursor
        inputManager.setMouseCursor(
                (JmeCursor)assetManager.loadAsset("Textures/Pointer.ico"));
 
        // init custom key config; we'll implement it manually
        initKeys();
        
        // initialize HUD
        hud = new Hud(assetManager, guiNode, 
                settings.getWidth(), settings.getHeight());
        hud.reset();
        
        // create sound instance
        sound = new Sound(assetManager);
        
        // start music
        sound.startMusic();
        
        // create node for bullets
        bulletNode = new Node("bullets");
        guiNode.attachChild(bulletNode);
        
        // create node for enemies
        enemyNode = new Node("enemies");
        guiNode.attachChild(enemyNode);
        
        // create node for black hole
        blackHoleNode = new Node("blackholes");
        guiNode.attachChild(blackHoleNode);
        
        // crete node for particles
        particleNode = new Node("particles");
        guiNode.attachChild(particleNode);
        
        // initialize particleManager
        particleManager = new ParticleManager(particleNode, guiNode, 
                getSpatial("Laser"), getSpatial("Glow"), 
                settings.getWidth(), settings.getHeight());
        
        // create player using getSpatial function
        player = getSpatial("Player");
        // setup player
        player.setUserData("alive", true);
        player.move(settings.getWidth()/2, settings.getHeight()/2, 0);
        player.addControl(new PlayerControl(particleManager, 
                settings.getWidth(), settings.getHeight()));
        guiNode.attachChild(player);
    }

    @Override
    public void simpleUpdate(float tpf) {
        
        // update hud regardless if player is alive
        hud.update();
        
        // update grid also
        grid.update(tpf);
        
        if ((Boolean)player.getUserData("alive")) {
            spawnEnemies();
            spawnBlackHoles();
            handleCollisions();
            handleGravity(tpf);
        } else if (System.currentTimeMillis() - 
                (Long)player.getUserData("dieTime") > 4000f && !gameOver) {
            grid.applyDirectedForce(new Vector3f(0,0,5000), 
                    player.getLocalTranslation(), 100);
            // spawn player
            player.setLocalTranslation(500, 500, 0);
            guiNode.attachChild(player);
            player.setUserData("alive", true);
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
    private void initKeys() {
        // add mappings
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("return", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("shoot", 
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        
        // add listeners
        inputManager.addListener(actionListener, "up");
        inputManager.addListener(actionListener, "down");
        inputManager.addListener(actionListener, "left");
        inputManager.addListener(actionListener, "right");
        inputManager.addListener(analogListener, "shoot");
    }
    
    private Spatial getSpatial(String name) {
        // create node for spatial
        Node node = new Node(name);
        
        // load a Picture for 2d and load a texture2d
        Picture pic = new Picture(name);
        Texture2D tex = (Texture2D)assetManager
                .loadTexture("Textures/"+name+".png");
        pic.setTexture(assetManager, tex, true);
        
        // adjust picture so that the center of the image is in the middle
        float width = tex.getImage().getWidth();
        float height = tex.getImage().getHeight();
        pic.setWidth(width);
        pic.setHeight(height);
        pic.move(-width/2f, -height/2f, 0);
        
        // add a material to the node for the picture
        Material picMat = new Material(assetManager, 
                "Common/MatDefs/Gui/Gui.j3md");
        picMat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
        node.setMaterial(picMat);
        
        // set the radius of the node with width as approximation
        // this is for collision detection
        node.setUserData("radius", width/2);
        node.attachChild(pic);
        
        return node;
    }
    
    // This is for one time action controls, i.e. button press, etc.
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean isPressed, float tpf) {
            if((Boolean)player.getUserData("alive")) {
                if (name.equals("up")) {
                    player.getControl(PlayerControl.class).up = isPressed;
                } else if (name.equals("down")) {
                    player.getControl(PlayerControl.class).down = isPressed;
                } else if (name.equals("left")) {
                    player.getControl(PlayerControl.class).left = isPressed;
                } else if (name.equals("right")) {
                    player.getControl(PlayerControl.class).right = isPressed;
                }
            }
        }
    };
    
    // this is for continuous action controls 
    // i.e holding down buttons, direction
    private AnalogListener analogListener = new AnalogListener() {
        public void onAnalog(String name, float value, float tpf) {
            if ((Boolean)player.getUserData("alive")) {
                if (name.equals("shoot")) {
                    // shoot bullets, with timing per bullet
                    if (System.currentTimeMillis() - bulletCooldown > 100f) {
                        bulletCooldown = System.currentTimeMillis();
                        
                        Vector3f aim = getAimDirection();
                        Vector3f offset = new Vector3f(aim.y/3, -aim.x/3, 0);
                        
                        // instantiating 2 bullets adjacent to each other
                        Spatial bullet1 = getSpatial("Bullet");
                        // add final offset of bullet
                        Vector3f finalOffset = aim.add(offset).mult(30);
                        // we compute the bullet translation via aim of player
                        Vector3f trans = player.getLocalTranslation()
                                .add(finalOffset);
                        bullet1.setLocalTranslation(trans);
                        // add bullet control
                        bullet1.addControl(new BulletControl(aim, 
                                settings.getWidth(), settings.getHeight(), 
                                particleManager, grid));
                        // attach to parent bulllet node
                        bulletNode.attachChild(bullet1);
                         
                        // similar to bullet 1, with offset negated so they are
                        // adjacent to each other
                        Spatial bullet2 = getSpatial("Bullet");
                        finalOffset = aim.add(offset.negate()).mult(30);
                        trans = player.getLocalTranslation().add(finalOffset);
                        bullet2.setLocalTranslation(trans);
                        bullet2.addControl(new BulletControl(aim, 
                                settings.getWidth(), settings.getHeight(), 
                                particleManager, grid));
                       bulletNode.attachChild(bullet2);
                       
                       // play sound
                       sound.shoot();
                    }
                }
            }
        }
    };
    
    private void spawnEnemies() {
        if (System.currentTimeMillis() - enemySpawnCooldown >= 10f) {
            enemySpawnCooldown = System.currentTimeMillis();
            
            if (enemyNode.getQuantity() < 50) {
                if (new Random().nextInt((int)enemySpawnChance) == 0) {
                    // create seeker enemy
                    createSeeker();
                }
                if (new Random().nextInt((int)enemySpawnChance) == 0) {
                    // create seeker enemy
                    createWanderer();
                }
            }
            
            // increase spawn time
            if (enemySpawnChance >= 1.1f) {
                enemySpawnChance -= 0.005f;
            }
        }
    }
    
    private void spawnBlackHoles() {
        if (System.currentTimeMillis() - spawnBlackHoleCoolDown > 5f) {
            spawnBlackHoleCoolDown = System.currentTimeMillis();
            
            if (blackHoleNode.getQuantity() < 2) { 
                if ((new Random().nextInt(1000) == 0)) {
                    createBlackHole();
                }
            }
        }
    }
    
    private void handleGravity(float tpf) {
        for (int i = 0; i < blackHoleNode.getQuantity(); i++) {
            if (!(Boolean)blackHoleNode.getChild(i).getUserData("active")) {
                continue;
            }
            int radius = 250;
            
            // check player
            if (!isNearby(player, blackHoleNode.getChild(i), radius)) {
                applyGravity(blackHoleNode.getChild(i), player, tpf);
            }
            
            // check bullets
            for (int j = 0; j < bulletNode.getQuantity(); j++) {
                if (isNearby(bulletNode.getChild(j), 
                        blackHoleNode.getChild(i), radius)) {
                    applyGravity(blackHoleNode.getChild(i), 
                            bulletNode.getChild(j), tpf);
                }
            }
            
            // check enemies
            for (int j = 0; j < enemyNode.getQuantity(); j++) {
                if (!(Boolean)enemyNode.getChild(j).getUserData("active")) {
                    continue;
                }
                if (isNearby(enemyNode.getChild(j), 
                        blackHoleNode.getChild(i), radius)) {
                    applyGravity(blackHoleNode.getChild(i), 
                            enemyNode.getChild(j), tpf);
                }
            }
            
            // check Particles
            for (int j=0; j<particleNode.getQuantity(); j++) {
                if (particleNode.getChild(j).getUserData("affectedByGravity")) {
                    applyGravity(blackHoleNode.getChild(i), 
                            particleNode.getChild(j), tpf);
                }
            }
        }
    }
    
    private void handleCollisions() {
        // should the player be killed?
        for (int i = 0; i < enemyNode.getQuantity(); i++) {
            if ((Boolean)enemyNode.getChild(i).getUserData("active")) {
                if (checkCollision(player, enemyNode.getChild(i))) {
                    killPlayer();
                }
            }
        }
        // should an enemy be killed?
        int i = 0;
        while (i < enemyNode.getQuantity()) {
            int j = 0;
            while (j < bulletNode.getQuantity()) {
                if (checkCollision(enemyNode.getChild(i), 
                        bulletNode.getChild(j))) {
                    if (enemyNode.getChild(i).getName().equals("Seeker")) {
                        hud.addPoints(2);
                    } else if (enemyNode.getChild(i)
                            .getName().equals("Wanderer")) {
                        hud.addPoints(1);
                    }
                    particleManager.enemyExplosion(enemyNode.getChild(i)
                            .getLocalTranslation());
                    enemyNode.detachChildAt(i);
                    bulletNode.detachChildAt(j);
                    sound.explosion();
                    break;
                }
                j++;
            }
            i++;
        }
        
        // should a black hole be destroyed?
        for (int j = 0; j < blackHoleNode.getQuantity(); j++) {
            Spatial blackHole = blackHoleNode.getChild(j);
            if ((Boolean)blackHole.getUserData("active")) {
                // check collision with what type
                // player
                if (checkCollision(player, blackHole)) {
                    killPlayer();
                }
                
                // enemies
                int k = 0;
                while (k < enemyNode.getQuantity()) {
                    if (checkCollision(enemyNode.getChild(k), blackHole)) {
                        particleManager.enemyExplosion(enemyNode.getChild(k)
                                .getLocalTranslation());
                        enemyNode.detachChildAt(k);
                    }
                    k++;
                }
                
                // bullets
                k = 0;
                while (k < bulletNode.getQuantity()) {
                    if (checkCollision(bulletNode.getChild(k), blackHole)) {
                        bulletNode.detachChildAt(k);
                        blackHole.getControl(BlackHoleControl.class)
                                .blackHoleWasShot(blackHoleNode.getChild(j)
                                .getLocalTranslation());
                        if (blackHole.getControl(BlackHoleControl.class)
                                .blackHoleIsDestroyed()) {
                            blackHoleNode.detachChild(blackHole);
                            sound.explosion();
                        }
                    }
                    k++;
                }      
            }
        }   
    }
    
    private boolean checkCollision(Spatial a, Spatial b) {
        float distance = a.getLocalTranslation()
                .distance(b.getLocalTranslation());
        float maxDistance = (Float)a.getUserData("radius") + 
                (Float)b.getUserData("radius");
        return distance <= maxDistance;
    }
    
    // seeker enemy
    private void createSeeker() {
        Spatial seeker = getSpatial("Seeker");
        seeker.setLocalTranslation(getSpawnPosition());
        seeker.addControl(new SeekerControl(player));
        seeker.setUserData("active", false);
        enemyNode.attachChild(seeker);
        sound.spawn();
    }
    
    // wanderer enemy
    private void createWanderer() {
        Spatial wanderer = getSpatial("Wanderer");
        wanderer.setLocalTranslation(getSpawnPosition());
        wanderer.addControl(new WandererControl(settings.getWidth(), 
                settings.getHeight()));
        wanderer.setUserData("active", false);
        enemyNode.attachChild(wanderer);
        sound.spawn();
    }
    
    // black hole
    private void createBlackHole() {
        Spatial blackHole = getSpatial("Black Hole");
        blackHole.setLocalTranslation(getSpawnPosition());
        blackHole.setUserData("active", false);
        blackHole.addControl(new BlackHoleControl(particleManager, grid));
        blackHoleNode.attachChild(blackHole);    
    }
    
    // player killed
    private void killPlayer() {
        player.removeFromParent();
        player.getControl(PlayerControl.class).reset();
        player.setUserData("alive", false);
        player.setUserData("dieTime", System.currentTimeMillis());
        enemyNode.detachAllChildren();
        blackHoleNode.detachAllChildren();
        particleManager.playerExplosion(player.getLocalTranslation());
        sound.explosion();
        if (!hud.removeLife()) {
            hud.endGame();
            gameOver = true;
        }
    }
     
    // Helper functions for getting aim direction, 
    // converting from angle to vector and vice-versa
    
    private Vector3f getAimDirection() {
        Vector2f mousePos = inputManager.getCursorPosition();
        Vector3f playerPos = player.getLocalTranslation();
        Vector3f diff = new Vector3f(mousePos.x - playerPos.x, 
                mousePos.y - playerPos.y, 0);
        return diff.normalizeLocal();
    }
    
    public static float getAngleFromVector(Vector3f vec) {
        Vector2f vec2 = new Vector2f(vec.x, vec.y);
        return vec2.getAngle();
    }
    
    public static Vector3f getVectorFromAngle(float angle) {
        return new Vector3f(FastMath.cos(angle), FastMath.sin(angle), 0);
    }
    
    private Vector3f getSpawnPosition() {
        Vector3f pos;
        do {
            pos = new Vector3f(new Random().nextInt(settings.getWidth()), 
                    new Random().nextInt(settings.getHeight()), 0);
        } while (pos.distanceSquared(player.getLocalTranslation()) < 8000);
        return pos;
    }

    private boolean isNearby(Spatial a, Spatial b, float distance) {
        Vector3f pos1 = a.getLocalTranslation();
        Vector3f pos2 = b.getLocalTranslation();
        return pos1.distanceSquared(pos2) <= (distance * distance);
    }

    private void applyGravity(Spatial blackHole, Spatial target, float tpf) {
        Vector3f difference = blackHole.getLocalTranslation()
                .subtract(target.getLocalTranslation());
        
        Vector3f gravity = difference.normalize().multLocal(tpf);
        float distance = difference.length();
        
        if (target.getName().equals("Player")) {
            gravity.multLocal(250f/distance);
            target.getControl(PlayerControl.class)
                    .applyGravity(gravity.mult(80f));
        } else if (target.getName().equals("Bullet")) {
            gravity.multLocal(250f/distance);
            target.getControl(BulletControl.class)
                    .applyGravity(gravity.mult(-0.8f));
        } else if (target.getName().equals("Seeker")) {
            gravity.multLocal(250f/distance);
            target.getControl(SeekerControl.class)
                    .applyGravity(gravity.mult(150000f));
        } else if (target.getName().equals("Wanderer")) {
            gravity.multLocal(250f/distance);
            target.getControl(WandererControl.class)
                    .applyGravity(gravity.mult(150000f));
        }  else if (target.getName().equals("Laser") || 
                target.getName().equals("Glow")) {
            target.getControl(ParticleControl.class)
                    .applyGravity(gravity.mult(15000), distance);
        }
    }
}
