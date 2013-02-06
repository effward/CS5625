package cs5625.deferred.apps;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;

import cs5625.deferred.materials.AnisotropicWardMaterial;
import cs5625.deferred.materials.BlinnPhongMaterial;
import cs5625.deferred.materials.CookTorranceMaterial;
import cs5625.deferred.materials.IsotropicWardMaterial;
import cs5625.deferred.materials.Material;
import cs5625.deferred.materials.Texture2D;
import cs5625.deferred.misc.ScenegraphException;
import cs5625.deferred.misc.Util;
import cs5625.deferred.scenegraph.Geometry;
import cs5625.deferred.scenegraph.Mesh;
import cs5625.deferred.scenegraph.PointLight;

/**
 * MaterialTestSceneController.java
 * 
 * This scene uses many of the materials from PA1. Derived from the DefaultSceneController.
 * 
 * Drag the mouse to orbit the camera, and scroll to zoom. Numbers 1-9 preview individual gbuffer 
 * textures, and 0 views the shaded result.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488), Sean Ryan (ser99)
 * @date 2013-01-30
 */
public class MaterialTestSceneController extends SceneController
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
			
			/* Load some meshes and textures. */
			Mesh cube   = Geometry.load("models/cube.obj",   false, true).get(0).getMeshes().get(0);
			Mesh monkey = Geometry.load("models/monkey.obj", false, true).get(0).getMeshes().get(0);		
			Mesh sphere = Geometry.load("models/sphere.obj", false, true).get(0).getMeshes().get(0);
			
			Texture2D brickDiffuse  = Texture2D.load(gl, "textures/Diffuse_example.jpg");
			Texture2D brickSpecular = Texture2D.load(gl, "textures/Specular_example.jpg");
			Texture2D maskTexture   = Texture2D.load(gl, "textures/gloss_map.jpg");
			Texture2D faceTexture   = Texture2D.load(gl, "textures/texture_face.jpg");
			Texture2D nTexture      = Texture2D.load(gl, "textures/N.jpg");
			Texture2D alphaTexture  = Texture2D.load(gl, "textures/alpha.jpg");
			Texture2D alphaXTexture  = Texture2D.load(gl, "textures/alpha_x.jpg");
			Texture2D alphaYTexture  = Texture2D.load(gl, "textures/alpha_y.jpg");
			
			
			/* phong1: Texture-less Blinn-Phong. */
			BlinnPhongMaterial phong1 = new BlinnPhongMaterial();
			phong1.setDiffuseColor(new Color3f(0.8f, 0.1f, 0.1f));
			phong1.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			phong1.setPhongExponent(100.0f);
			
			/* phong2: Fully textured Blinn-Phong with white diffuse color. */
			BlinnPhongMaterial phong2 = new BlinnPhongMaterial();
			phong2.setDiffuseColor(new Color3f(1.0f, 1.0f, 1.0f));
			phong2.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			phong2.setDiffuseTexture(brickDiffuse);
			phong2.setSpecularTexture(brickSpecular);
			phong2.setExponentTexture(maskTexture);
			
			/* phong3: Fully textured Blinn-Phong with non-white diffuse color. */
			BlinnPhongMaterial phong3 = new BlinnPhongMaterial();
			phong3.setDiffuseColor(new Color3f(0.8f, 0.1f, 0.1f));
			phong3.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			phong3.setDiffuseTexture(brickDiffuse);
			phong3.setSpecularTexture(brickSpecular);
			phong3.setExponentTexture(maskTexture);
			
			/* cook1: Texture-less Cook-Torrance. */
			CookTorranceMaterial cook1 = new CookTorranceMaterial();
			cook1.setDiffuseColor(new Color3f(0.1f, 0.7f, 0.0f));
			cook1.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			cook1.setM(0.2f);
			cook1.setN(4.0f);
			
			/* cook2: Fully textured Cook-Torrance with white diffuse color. */
			CookTorranceMaterial cook2 = new CookTorranceMaterial();
			cook2.setDiffuseColor(new Color3f(1.0f, 1.0f, 1.0f));
			cook2.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			cook2.setM(0.2f);
			cook2.setN(4.0f);
			cook2.setDiffuseTexture(brickDiffuse);
			cook2.setSpecularTexture(brickSpecular);
			cook2.setMTexture(faceTexture);
			cook2.setNTexture(nTexture);
			
			/* cook3: Fully textured Cook-Torrance with non-white diffuse color. */
			CookTorranceMaterial cook3 = new CookTorranceMaterial();
			cook3.setDiffuseColor(new Color3f(0.1f, 0.7f, 0.0f));
			cook3.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			cook3.setM(0.2f);
			cook3.setN(4.0f);
			cook3.setDiffuseTexture(brickDiffuse);
			cook3.setSpecularTexture(brickSpecular);
			cook3.setMTexture(faceTexture);
			cook3.setNTexture(nTexture);				
			
			/* anisotropicWard1: Texture-less Anisotropic Ward */
			AnisotropicWardMaterial anisotropicWard1 = new AnisotropicWardMaterial();
			anisotropicWard1.setDiffuseColor(new Color3f(0.1f, 0.7f, 0.0f));
			anisotropicWard1.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			anisotropicWard1.setAlphaX(0.15f);
			anisotropicWard1.setAlphaY(0.5f);
			
			/* anisotropicWard2: Fully textured Anisotropic Ward with white diffuse color. */
			AnisotropicWardMaterial anisotropicWard2 = new AnisotropicWardMaterial();
			anisotropicWard2.setDiffuseColor(new Color3f(1.0f, 1.0f, 1.0f));
			anisotropicWard2.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			anisotropicWard2.setAlphaX(0.0f);
			anisotropicWard2.setAlphaY(0.0f);
			anisotropicWard2.setDiffuseTexture(brickDiffuse);
			anisotropicWard2.setSpecularTexture(brickSpecular);	
			anisotropicWard2.setAlphaXTexture(alphaXTexture);
			anisotropicWard2.setAlphaYTexture(alphaYTexture);
			
			/* anisotropicWard3: Fully textured Anisotropic Ward with non-white diffuse color. */
			AnisotropicWardMaterial anisotropicWard3 = new AnisotropicWardMaterial();
			anisotropicWard3.setDiffuseColor(new Color3f(0.1f, 0.7f, 0.0f));
			anisotropicWard3.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			anisotropicWard3.setAlphaX(0.0f);
			anisotropicWard3.setAlphaY(0.0f);
			anisotropicWard3.setDiffuseTexture(brickDiffuse);
			anisotropicWard3.setSpecularTexture(brickSpecular);		
			anisotropicWard3.setAlphaXTexture(alphaXTexture);
			anisotropicWard3.setAlphaYTexture(alphaYTexture);			
			
			/* isotropicWard1: Texture-less Isotropic Ward */
			IsotropicWardMaterial isotropicWard1 = new IsotropicWardMaterial();
			isotropicWard1.setDiffuseColor(new Color3f(0.1f, 0.7f, 0.0f));
			isotropicWard1.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			isotropicWard1.setAlpha(0.4f);
			
			/* anisotropicWard2: Fully textured Anisotropic Ward with white diffuse color. */
			IsotropicWardMaterial isotropicWard2 = new IsotropicWardMaterial();
			isotropicWard2.setDiffuseColor(new Color3f(1.0f, 1.0f, 1.0f));
			isotropicWard2.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			isotropicWard2.setAlpha(0.0f);
			isotropicWard2.setDiffuseTexture(brickDiffuse);
			isotropicWard2.setSpecularTexture(brickSpecular);	
			isotropicWard2.setAlphaTexture(alphaTexture);
			
			/* anisotropicWard3: Fully textured Anisotropic Ward with non-white diffuse color. */
			IsotropicWardMaterial isotropicWard3 = new IsotropicWardMaterial();
			isotropicWard3.setDiffuseColor(new Color3f(0.1f, 0.7f, 0.0f));
			isotropicWard3.setSpecularColor(new Color3f(1.0f, 1.0f, 1.0f));
			isotropicWard3.setAlpha(0.0f);
			isotropicWard3.setDiffuseTexture(brickDiffuse);
			isotropicWard3.setSpecularTexture(brickSpecular);
			isotropicWard3.setAlphaTexture(alphaTexture);
			
			/* Put together all of the Blinn-Phong meshes. */
			Mesh[] phongMeshes = {monkey.clone(), cube.clone(), cube.clone(), cube.clone(), sphere.clone()};
			Material[] phongMats = {phong1, phong1, phong2, phong3, phong2};
			buildMeshRow(phongMeshes, phongMats, new Point3f(-6.0f, 0.0f, -4.5f));
			
			/* Put together all of the CookTorrance meshes. */
			Mesh[] cookMeshes = {monkey.clone(), cube.clone(), cube.clone(), cube.clone(), sphere.clone()};
			Material[] cookMats = {cook1, cook1, cook2, cook3, cook2};
			buildMeshRow(cookMeshes, cookMats, new Point3f(-6.0f, 0.0f, -1.5f));
			
			/* Put together all of the Anisotropic Ward meshes. */
			Mesh[] anisotropicWardMeshes = {monkey.clone(), sphere.clone(), sphere.clone(), sphere.clone(), cube.clone()};
			Material[] anisotropicWardMats = {anisotropicWard1, anisotropicWard1, anisotropicWard2, anisotropicWard3, anisotropicWard2};
			buildMeshRow(anisotropicWardMeshes, anisotropicWardMats, new Point3f(-6.0f, 0.0f, 1.5f));
			
			/* Put together all of the Isotropic Ward meshes. */
			Mesh[] isotropicWardMeshes = {monkey.clone(), sphere.clone(), sphere.clone(), sphere.clone(), cube.clone()};
			Material[] isotropicWardMats = {isotropicWard1, isotropicWard1, isotropicWard2, isotropicWard3, isotropicWard2};
			buildMeshRow(isotropicWardMeshes, isotropicWardMats, new Point3f(-6.0f, 0.0f, 4.5f));
			
			/* Calculate tangent vectors, used in the Anisotropic Ward model */
			mSceneRoot.calculateTangentVectorsForAllGeometry();
			
			/* Add two unattenuated point lights to provide overall illumination. */
			PointLight light = new PointLight();
			light.setConstantAttenuation(1.0f);
			light.setLinearAttenuation(0.0f);
			light.setQuadraticAttenuation(0.0f);
			light.setPosition(new Point3f(5.0f, 180.0f, 5.0f));
			mSceneRoot.addChild(light);
			
			PointLight light2 = new PointLight();
			light2.setConstantAttenuation(1.0f);
			light2.setLinearAttenuation(0.0f);
			light2.setQuadraticAttenuation(0.0f);
			light2.setPosition(new Point3f(50.0f, -20.0f, 100.0f));
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
	 * Hides away a bit of boiler-plate code for a grid of meshes.
	 * @param meshes An array of cloned meshes that still need their materials set.
	 * @param materials The materials to use with the above meshes.
	 * @param start The beginning of the row in world space; each mesh will be 3.0f further along x.
	 * @throws ScenegraphException
	 */
	private void buildMeshRow(Mesh[] meshes, Material[] materials, Point3f start) throws ScenegraphException
	{
		if(meshes.length != materials.length)
		{
			throw new RuntimeException("buildMeshGrid: meshes and materials must have the same size!");
		}
		
		ArrayList<Geometry> geoList = new ArrayList<Geometry>();
		for(int i = 0; i < meshes.length; i++)
		{
			Mesh mesh = meshes[i];
			mesh.setMaterial(materials[i]);
			
			Geometry geo = new Geometry();
			geo.addMesh(mesh);
			geo.setPosition(new Point3f(start.x + 3.0f * i, start.y, start.z));
			geoList.add(geo);
		}
		
		mSceneRoot.addGeometry(geoList);
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
