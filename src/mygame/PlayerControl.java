/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 *
 * @author Marc Tabia
 */
public class PlayerControl extends AbstractControl {
    
    private int screenWidth, screenHeight;
    private ParticleManager particleManager;
    
    // is the player currently moving?
    public boolean up, down, left, right;
    
    // player speed
    private float speed = 600f;
    
    // last rotation of the player
    private float lastRotation;
    
    // constructor for the Player Control
    public PlayerControl(ParticleManager particleManager, 
            int width, int height) {
        this.particleManager = particleManager;
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        // here put the controls that happen every update.
        // for the player control we designate the movement and direction
        if (up) {
            if (spatial.getLocalTranslation().y < 
                    screenHeight - (Float)spatial.getUserData("radius")) {
                spatial.move(0, tpf * speed, 0);
            }
            spatial.rotate(0, 0, -lastRotation + FastMath.PI/2);
            lastRotation = FastMath.PI/2;
        } else if (down) {
            if (spatial.getLocalTranslation().y > 
                    (Float)spatial.getUserData("radius")) {
                spatial.move(0, tpf * -speed, 0);
            }
            spatial.rotate(0, 0, -lastRotation + FastMath.PI * 1.5f);
            lastRotation = FastMath.PI * 1.5f;
        } else if (left) {
            if (spatial.getLocalTranslation().x > 
                    (Float)spatial.getUserData("radius")) {
                spatial.move(tpf * -speed, 0, 0);
            }
            spatial.rotate(0, 0, -lastRotation + FastMath.PI);
            lastRotation = FastMath.PI;
        } else if (right) {
             if (spatial.getLocalTranslation().x < 
                    screenWidth - (Float)spatial.getUserData("radius")) {
                spatial.move(tpf * speed, 0, 0);
            }
            spatial.rotate(0, 0, -lastRotation + 0);
            lastRotation = 0;
        }
        if (up || down || left || right) {
            particleManager.makeExhaustFire(
                    spatial.getLocalTranslation(), lastRotation);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // not used yet
    }
    
    public void reset() {
        up = false;
        down = false;
        left = false;
        right = false;
    }
    
    public void applyGravity(Vector3f gravity) {
        spatial.move(gravity);
    }
}
