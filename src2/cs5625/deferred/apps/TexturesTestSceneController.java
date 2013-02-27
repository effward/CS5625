package cs5625.deferred.apps;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;

import cs5625.deferred.materials.ClassicNormalMapMaterial;
import cs5625.deferred.materials.CookTorranceMaterial;
import cs5625.deferred.materials.NormalMapMaterial;
import cs5625.deferred.materials.ReflectionMaterial;
import cs5625.deferred.materials.Texture2D;
import cs5625.deferred.materials.TextureDynamicCubeMap;
import cs5625.deferred.misc.Util;
import cs5625.deferred.scenegraph.Geometry;
import cs5625.deferred.scenegraph.Mesh;
import cs5625.deferred.scenegraph.PointLight;

/**
 * DefaultSceneController.java
 * 
 * The default scene controller creates a simple scene and allows the user to orbit the camera 
 * and preview the renderer's gbuffer.
 * 
 * Drag the mouse to orbit the camera, and scroll to zoom. Numbers 1-9 preview individual gbuffer 
 * textures, and 0 views the shaded result.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488), John DeCorato (jd537), Ivaylo Boyadzhiev (iib2)
 * @date 2012-03-23
 */
public class TexturesTestSceneController extends SceneController
{
	/* Keeps track of camera's orbit position. Latitude and longitude are in degrees. */
	private float mCameraLongitude = 50.0f, mCameraLatitude = -40.0f;
	private float mCameraRadius = 15.0f;
	
	/* Used to calculate mouse deltas to orbit the camera in mouseDragged(). */ 
	private Point mLastMouseDrag;
	
	@Override
	public void initializeScene()
	{
		try
		{
			GL2 gl = GLU.getCurrentGL().getGL2();
			
			/* Load default scene with materials. */
			mSceneRoot.addGeometry(Geometry.load("models/default-scene.obj", true, true));
			
			/* Example of manipulating an object by name. */
			mSceneRoot.findDescendantByName("fighter1").getOrientation().set(new AxisAngle4f(0.0f, 1.0f, 0.0f, -(float)Math.PI / 4.0f));
			//((Geometry)mSceneRoot.findDescendantByName("Cylinder")).getMeshes().get(0).setMaterial(new LambertianMaterial(new Color3f(0.64f, 0.47f, 0.26f)));
			
			ClassicNormalMapMaterial normalMaterial1 = new ClassicNormalMapMaterial();
			Texture2D faceTexture = Texture2D.load(gl, "textures/texture_face.jpg");
			Texture2D faceNormalTexture = Texture2D.load(gl, "textures/normal_face.jpg");
			normalMaterial1.setDiffuseTexture(faceTexture);
			normalMaterial1.setNormalTexture(faceNormalTexture);
			
			((Geometry)mSceneRoot.findDescendantByName("Plane")).getMeshes().get(0).setMaterial(normalMaterial1);
			
			NormalMapMaterial normalMaterial2 = new NormalMapMaterial();
			Texture2D brickTexture = Texture2D.load(gl, "textures/stoneBrickDiffuse.jpg");
			Texture2D brickSpecularTexture = Texture2D.load(gl, "textures/Specular_example.jpg");
			Texture2D brickNormalTexture = Texture2D.load(gl, "textures/stoneBrickNormal.jpg");
			normalMaterial2.setDiffuseTexture(brickTexture);
			normalMaterial2.setSpecularTexture(brickSpecularTexture);
			normalMaterial2.setNormalTexture(brickNormalTexture);
			
			((Geometry)mSceneRoot.findDescendantByName("Cube")).calculateTangentVectorsForAllGeometry();
			((Geometry)mSceneRoot.findDescendantByName("Cube")).getMeshes().get(0).setMaterial(normalMaterial2);
			
			/* Change the material of the Icosphere to Reflection */			
			ReflectionMaterial reflectionMaterial1 = new ReflectionMaterial(mRenderer.getStaticCubeMap());
			((Geometry)mSceneRoot.findDescendantByName("Icosphere")).getMeshes().get(0).setMaterial(reflectionMaterial1);
			mSceneRoot.findDescendantByName("Icosphere").getPosition().y += 2.0f;			
			
			/* Add a new plane and put it on the top */
			Mesh plane = Geometry.load("models/plane.obj", false, true).get(0).getMeshes().get(0);

			Geometry newPlaneGeo = new Geometry();
			newPlaneGeo.addMesh(plane);			
			newPlaneGeo.setName("PlaneTop");
			newPlaneGeo.setScale(2.0f);
			mSceneRoot.addChild(newPlaneGeo);
			
			Point3f posPlane = mSceneRoot.findDescendantByName("PlaneTop").getPosition();
			posPlane.y += 15.0f;
			
			/* Add a new reflective sphere where the torus used to be. */
			Mesh sphere = Geometry.load("models/sphere.obj", false, true).get(0).getMeshes().get(0);
			Point3f pos = mSceneRoot.findDescendantByName("Torus").getPosition();
			pos.y += 2.0f;
			
			Geometry newGeo = new Geometry();
			newGeo.addMesh(sphere);
			newGeo.setPosition(pos);
			newGeo.setName("DynamicSphere");
			
			mSceneRoot.addChild(newGeo);
			mSceneRoot.removeChild(mSceneRoot.findDescendantByName("Torus"));
			
			TextureDynamicCubeMap dynamicCubeMap2 = mRenderer.getNewDynamicCubeMap();
			dynamicCubeMap2.setCenterObject(mSceneRoot.findDescendantByName("DynamicSphere"));
			
			ReflectionMaterial reflectionMaterial3 = new ReflectionMaterial(dynamicCubeMap2);
			((Geometry)mSceneRoot.findDescendantByName("DynamicSphere")).getMeshes().get(0).setMaterial(reflectionMaterial3);						
			
			/* Change the material of the Monkey to Cook-Torrance with mirror component. */
			/* Texture-less Cook-Torrance. */
			CookTorranceMaterial cook1 = new CookTorranceMaterial();
			cook1.setDiffuseColor(new Color3f(0.0f, 0.0f, 1.0f));
			cook1.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			cook1.setM(0.2f);
			cook1.setN(4.0f);
			cook1.setCubeMapTexture(mRenderer.getStaticCubeMap());
			
//			LambertianMaterial lambertian1 = new LambertianMaterial(new Color3f(1.0f, 0.0f, 0.0f));
			
			((Geometry)mSceneRoot.findDescendantByName("Monkey")).getMeshes().get(0).setMaterial(cook1);
			
			/* Add an unattenuated point light to provide overall illumination. */
			PointLight light1 = new PointLight();
			
			light1.setConstantAttenuation(1.0f);
			light1.setLinearAttenuation(0.0f);
			light1.setQuadraticAttenuation(0.0f);
			
			light1.setPosition(new Point3f(50.0f, 180.0f, 100.0f));
			light1.setColor(new Color3f(0.5f, 0.5f, 0.5f));
			mSceneRoot.addChild(light1);		
			
			PointLight light2 = new PointLight();
			
			light2.setConstantAttenuation(1.0f);
			light2.setLinearAttenuation(0.0f);
			light2.setQuadraticAttenuation(0.0f);
			
			light2.setPosition(new Point3f(-50.0f, 180.0f, -100.0f));
			light2.setColor(new Color3f(0.5f, 0.5f, 0.5f));
			mSceneRoot.addChild(light2);	
		}
		catch (Exception err)
		{
			/* If anything goes wrong, just die. */
			err.printStackTrace();
			System.exit(-1);
		}
		
		/* Initialize camera position. */
		updateCamera();
	}
		
	/**
	 * Updates the camera position and orientation based on orbit parameters.
	 */
	protected void updateCamera()
	{
		/* Compose the "horizontal" and "vertical" rotations. */
		Quat4f longitudeQuat = new Quat4f();
		longitudeQuat.set(new AxisAngle4f(0.0f, 1.0f, 0.0f, mCameraLongitude * (float)Math.PI / 180.0f));
		
		Quat4f latitudeQuat = new Quat4f();
		latitudeQuat.set(new AxisAngle4f(1.0f, 0.0f, 0.0f, mCameraLatitude * (float)Math.PI / 180.0f));

		mCamera.getOrientation().mul(longitudeQuat, latitudeQuat);
		
		/* Set the camera's position so that it looks towards the origin. */
		mCamera.setPosition(new Point3f(0.0f, 0.0f, mCameraRadius));
		Util.rotateTuple(mCamera.getOrientation(), mCamera.getPosition());
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent mouseWheel) {
		/* Zoom in and out by the scroll wheel. */
		mCameraRadius += mouseWheel.getUnitsToScroll();
		updateCamera();
		requiresRender();
	}

	@Override
	public void mousePressed(MouseEvent mouse)
	{
		/* Remember the starting point of a drag. */
		mLastMouseDrag = mouse.getPoint();
	}
	
	@Override
	public void mouseDragged(MouseEvent mouse)
	{
		/* Calculate dragged delta. */
		float deltaX = -(mouse.getPoint().x - mLastMouseDrag.x);
		float deltaY = -(mouse.getPoint().y - mLastMouseDrag.y);
		mLastMouseDrag = mouse.getPoint();
		
		/* Update longitude, wrapping as necessary. */
		mCameraLongitude += deltaX;
		
		if (mCameraLongitude > 360.0f)
		{
			mCameraLongitude -= 360.0f;
		}
		else if (mCameraLongitude < 0.0f)
		{
			mCameraLongitude += 360.0f;
		}
		
		/* Update latitude, clamping as necessary. */
		if (Math.abs(mCameraLatitude + deltaY) <= 89.0f)
		{
			mCameraLatitude += deltaY;
		}
		else
		{
			mCameraLatitude = 89.0f * Math.signum(mCameraLatitude);
		}
	
		updateCamera();
		requiresRender();
	}
}
