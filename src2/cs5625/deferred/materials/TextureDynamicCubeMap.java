package cs5625.deferred.materials;

import java.io.IOException;
import java.nio.Buffer;

import javax.media.opengl.GL2;
import javax.vecmath.Point3f;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.scenegraph.SceneObject;

/**
 * TextureDynamicCubeMap.java
 * 
 * The TextureDynamicCubeMap class represents a Cube Map OpenGL texture, which content is
 * generated in real-time from a given position or the center of a given object in the scene.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Ivaylo Boyadzhiev (iib2)
 * @date 2013-02-10
 */
public class TextureDynamicCubeMap extends TextureCubeMap
{
	/*
	 * Texture object state. Public things have associated 'get' methods.
	 */
	SceneObject mCenterObject = null;
	Point3f mCenterPoint = new Point3f(0.0f, 0.0f, 0.0f);
	
	/**
	 * Private constructor used by the static function `load()`.
	 */
	public TextureDynamicCubeMap(GL2 gl) throws OpenGLException
	{
		super(gl);
	}	
	
	/**	 
	 * Creates a new texture object with data loaded from an image file.
	 * The cube map files should be named "identifier{left, right, top, bottom, front, back}extension"
	 * e.g. for identifier = "base_" and extension = ".png" we will look for "base_{left, right, top, bottom, front, back}.png"
	 * 
	 * 
	 * @param gl The OpenGL context in which this texture lives.
	 * @param size The size of the cube map buffer (width = height = size).
	 * @param mipMapOn Whether or not mip map should be enabled.
	 * 
	 * @return The new texture, or null if any error has occurred.
	 */
	public static TextureDynamicCubeMap create(GL2 gl, int size, boolean mipMapOn) throws OpenGLException, IOException
	{
				
		TextureDynamicCubeMap result = new TextureDynamicCubeMap(gl);
		
		/* Allocate space for the 6 faces */
		Buffer[] buffers = new Buffer[6];
		for (int i = 0; i < 6; ++i) {
			buffers[i] = Buffers.newDirectByteBuffer(size * size * 4);
		}			

		/* Initialize with that data. */
		result.initialize(gl, Format.RGBA, Datatype.INT8, size, mipMapOn, buffers);
		return result;
	}
	
	/**
	 * Initializes a new texture object with the passed attributes and data.
	 *
	 * @param gl The OpenGL context in which this texture lives.
	 * @param format The format of the provided pixel data.
	 * @param datatype The data type of the provided pixel data.
	 * @param size The common image size (width = height = size)
	 * @param mipMapOn Enable Mip Map for the cube map faces.
	 */
	public void initialize(GL2 gl, Format format, Datatype datatype, int size, boolean mipMapOn, Buffer[] buffers) throws OpenGLException
	{
		super.initialize(gl, format, datatype, size, mipMapOn, buffers);	
	}	
	
	/** 
	 * Get the scene object, from which view point we are rendering the scene.
	 */
	public SceneObject getCenterObject()
	{
		return mCenterObject;
	}
	
	/** 
	 * Set the scene object, from which view point we will render the scene.
	 * You can use setCenterPoint() if you don't want to bind the cube map
	 * to a particular object, but just position it to a point in space.
	 */
	public void setCenterObject(SceneObject centerObject)
	{		
		mCenterObject = centerObject;
	}
	
	/** 
	 * Get the point from which we are rendering the scene.
	 */
	public Point3f getCenterPoint()
	{
		if (mCenterObject == null)
			return mCenterPoint;
		else
			return mCenterObject.transformPointToWorldSpace(new Point3f(0,0,0));
	}
	
	/** 
	 * Set the point from which we are rendering the scene.
	 * This will be set to the position of the object, if setCenterObject is used.
	 */
	public void setCenterPoint(Point3f centerPoint)
	{
		mCenterPoint = centerPoint;
	}
	
	
	
};
