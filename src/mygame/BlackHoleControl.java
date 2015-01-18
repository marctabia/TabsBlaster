/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.ui.Picture;
import java.util.Random;

/**
 *
 * @author Marc Tabia
 */
public class BlackHoleControl extends AbstractControl {
    
    private long spawnTime;
    private int hitPoints;
    private long lastSprayTime;
    private float sprayAngle;
    private Random rand;
    private ParticleManager particleManager;
    private Grid grid;
    
    public BlackHoleControl(ParticleManager particleManager, Grid grid) {
        
        this.particleManager = particleManager;
        this.grid = grid;
        sprayAngle = 0;
        spawnTime = System.currentTimeMillis();
        rand = new Random();
        hitPoints = 10;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if ((Boolean)spatial.getUserData("active")) {
            long sprayDif = System.currentTimeMillis() - lastSprayTime;
            if ((System.currentTimeMillis() / 250) % 2 == 0 && sprayDif > 20) {
                lastSprayTime = System.currentTimeMillis();

                Vector3f sprayVel = Main.getVectorFromAngle(sprayAngle)
                        .mult(rand.nextFloat()*3 +6);
                Vector3f randVec = Main.getVectorFromAngle(rand.nextFloat()*FastMath.PI*2);
                randVec.multLocal(4+rand.nextFloat()*4);
                Vector3f position = spatial.getLocalTranslation().add(sprayVel.mult(2f)).addLocal(randVec);

                particleManager.sprayParticle(position, sprayVel.mult(30f));
            }
            sprayAngle -= FastMath.PI*tpf/10f;
           
        } else {
            // handle "active" status
            long diff = System.currentTimeMillis() - spawnTime;
            if (diff >= 1000f) {
                spatial.setUserData("active", true);
            }
            
            ColorRGBA color = new ColorRGBA(1, 1, 1, diff/1000f);
            Node spatialNode = (Node)spatial;
            Picture pic = (Picture)spatialNode.getChild("Black Hole");
            pic.getMaterial().setColor("Color", color);
        }
        grid.applyImplosiveForce(FastMath.sin(sprayAngle/2) * 10 +20, 
                    spatial.getLocalTranslation(), 250);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        
    }
    
    public void blackHoleWasShot(Vector3f position) {
        particleManager.blackHoleExplosion(position);
        --hitPoints;
    }
    
    public boolean blackHoleIsDestroyed() {
        return hitPoints <= 0;
    }
    
}
