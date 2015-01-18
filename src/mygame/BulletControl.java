/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author Marc Tabia
 */
public class BulletControl extends AbstractControl {
    
    private int screenWidth, screenHeight;
    
    private float speed = 1100f;
    private Vector3f direction;
    private float rotation;
    
    private ParticleManager particleManager;
    private Grid grid;
    
    public BulletControl(Vector3f direction, int screenWidth, 
            int screenHeight, ParticleManager particleManager, Grid grid) {
        this.direction = direction;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.particleManager = particleManager;
        this.grid = grid;
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        // movement of the bullet
        spatial.move(direction.mult(speed * tpf));
        
        // direction of the bullet
        float actualRotation = Main.getAngleFromVector(direction);
        if (actualRotation != rotation) {
            spatial.rotate(0, 0, actualRotation - rotation);
            rotation = actualRotation;
        }
        
        // grid is repelled by bullets
        grid.applyExplosiveForce(direction.length()*(18f), 
                spatial.getLocalTranslation(), 80);
        
        // check boundaries of stage, when bullet goes beyond, remove it
        // from the parent node
        Vector3f loc = spatial.getLocalTranslation();
        if (loc.x > screenWidth || loc.x < 0 || 
            loc.y > screenHeight || loc.y < 0) {
            particleManager.bulletExplosion(loc);
            spatial.removeFromParent();
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // not implemented
    }
    
    public void applyGravity(Vector3f gravity) {
        direction.addLocal(gravity);
    }
    
}
