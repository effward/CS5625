package cs5625.deferred.materials;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;

import com.jogamp.common.nio.Buffers;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.misc.Util;
import cs5625.deferred.rendering.FramebufferObject;
import cs5625.deferred.rendering.ShaderProgram;

/**
 * TextureCubeMap.java
 * 
 * The TextureCubeMap class represents a Cube Map OpenGL texture.
 * You can load textures from image files or render into them using the FramebufferObject class.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Ivaylo Boyadzhiev (iib2)
 * @date 2013-02-10
 */
public class TextureCubeMap extends Texture
{
	/*
	 * Texture object state. Public things have associated 'get' methods.
	 */
	protected int mSize = -1;
	protected int mTarget = -1;
	protected int mCubeMapIndex = -1;
	
	/* The shader program that will be used to blur the texture. */
	protected ShaderProgram mBlurShader = null;
	
	/* The blur shader FBO. */
	protected FramebufferObject mBlurTextureFBO;
	
	/* Gaussian blur width and variance on the X and Y axes. */
	int mBlurWidthX = -1;
	float mBlurVarianceX = 1.0f;
	
	int mBlurWidthY = -1;		
	float mBlurVarianceY = 1.0f;
	
	static protected String mCubeMapPostfix[] = {
		"right", "left", "bottom", "top", "front", "back"
	};
	
	
	/**
	 * Private constructor used by the static function `load()`.
	 */
	protected TextureCubeMap(GL2 gl) throws OpenGLException
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
	 * @param identifier The common base name of the image files to load.
	 * @param extension The common extension of the image files.
	 * 
	 * @return The new texture, or null if any of the files don't exist.
	 */
	public static TextureCubeMap load(GL2 gl, String identifier, String extension, boolean mipMapOn) throws OpenGLException, IOException
	{
				
		TextureCubeMap result = new TextureCubeMap(gl);
		Buffer[] buffers = new Buffer[6];
		int common_size = -1;
		
		/* Load all cube map data */
		for (int i = 0; i < 6; ++i) {
			String curr_identifier = identifier + mCubeMapPostfix[i] + extension;
			
			URL url = TextureCubeMap.class.getClassLoader().getResource(curr_identifier);
			if (url == null)
			{
				result.releaseGPUResources(gl);
				throw new IOException("Could not find texture file '" + curr_identifier + "'.");
			}
		
			BufferedImage image;
		
			/* Try to load image. */
			try
			{
				image = ImageIO.read(url);
			}
			catch (IOException err)
			{
				result.releaseGPUResources(gl);
				throw err;
			}
		
			/* Create buffer of image data. */
			buffers[i] = createBufferFromImage(image);
			int width = image.getWidth();
			int height = image.getHeight();
		
			/* Check the dimensions. */
			if (width != height) 
			{
				result.releaseGPUResources(gl);			
				throw new OpenGLException("Cube map file " + curr_identifier + " width != height: " + width + " " + height);
			}
			
			/* Check that the size agree with the previous file. */
			if (common_size > 0 && width != common_size) 
			{
				result.releaseGPUResources(gl);
				throw new OpenGLException("Cube map file " + curr_identifier + " width != prev_width: " + width + " " + common_size);
			}
			
			common_size = width;
		}					

		/* Initialize with that data. */
		result.initialize(gl, Format.RGBA, Datatype.INT8, common_size, mipMapOn, buffers);
		return result;
	}

	
	/**
	 * Initializes a new texture object with the passed attributes and data.
	 *
	 * @param gl The OpenGL context in which this texture lives.
	 * @param format The format of the provided pixel data.
	 * @param datatype The data type of the provided pixel data.
	 * @param size The common image size (width = height = size)
	 * @param mipMapOn Enable mip map for the cube map faces.
	 * @param data The pixel data to copy into the textures. May be null, in which
	 *        case an empty texture of the indicated format and size is created.
	 */
	protected void initialize(GL2 gl, Format format, Datatype datatype, int size, boolean mipMapOn, Buffer[] data) throws OpenGLException
	{
		try
		{
			/* Get GL formats first, in case something is invalid. */
			int gltype = datatype.toGLtype();
			int glformat = format.toGLformat();
			int glinternalformat = format.toGLinternalformat(datatype);
			
			/* Set the cube map texture target. */
			mTarget = GL2.GL_TEXTURE_CUBE_MAP;

			/* If everything seems good, proceed. */
			mSize = size;
			mFormat = format;
			mDatatype = datatype;
			
			/* Create the blur texture FBO. */
			mBlurTextureFBO = new FramebufferObject(gl, getFormat(), getDatatype(), getSize(), getSize(), 1, true, false);

			/* Bind and send texture data to OpenGL. */
			bind(gl, 0);

			int previousActive[] = new int[1];
			gl.glGetIntegerv(GL2.GL_ACTIVE_TEXTURE, previousActive, 0);
			gl.glActiveTexture(GL2.GL_TEXTURE0 + getBoundTextureUnit());						
		    
			if (mipMapOn) {
				/* Add support for Mip Map on every cube map surface. */
				enableMipMap(gl, true);
			} else {
				enableMipMap(gl, false);
				
				gl.glTexParameterf(mTarget, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
				gl.glTexParameterf(mTarget, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
			}
			   
		    /* Initialize the mip map levels to 0's. */
			gl.glTexParameteri(mTarget, GL2. GL_TEXTURE_BASE_LEVEL, 0);
		    gl.glTexParameteri(mTarget, GL2.GL_TEXTURE_MAX_LEVEL, 0);
		    
			/* GL_CLAMP_TO_EDGE gives seamless transition between the cube faces. */
			gl.glTexParameteri(mTarget, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(mTarget, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(mTarget, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_EDGE);
			
			/* Allocate space for all cube map faces. */
			for (int i=0; i<6; i++) {
				gl.glTexImage2D(GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, glinternalformat, 
					mSize, mSize, 0, glformat, gltype, data == null ? null : data[i]);
			}
			
			gl.glActiveTexture(previousActive[0]);

			/* Unbind and make sure it all went smoothly. */
			unbind(gl);
			OpenGLException.checkOpenGLError(gl);
		}
		catch (OpenGLException err)
		{
			/* Clean up on error. */
			releaseGPUResources(gl);
			throw err;
		}
	}
	
	/**
	 * Blur all faces of the cube map, using the current width and variances on X and Y.
	 * Note that this is a destructive function, that will overwrite the content of the texture map.
	 * If you apply this function twice in a row, without updating the texture in between, you will get twice the blur.
	 */
	public void Blur(GL2 gl) throws OpenGLException
	{
		
		if (mBlurShader == null) {
			throw new OpenGLException("You haven't specified a blur shader");
		}
		
		/* First, blur horizontally. */
		if (mBlurWidthX >= 0) {
			BlurAxis(gl, 0);
		}
		
		/* Second, blur vertically. */
		if (mBlurWidthY >= 0) {
			BlurAxis(gl, 1);
		}
	}
	
	/**
	 * Blur all faces of the cube map in the given axis, using the current width and variances: 
	 * axis = 0 means to blur horizontally, and axis = 1 means to blur vertically. 
	 */
	protected void BlurAxis(GL2 gl, int axis) throws OpenGLException
	{
		int width = ((axis == 0) ? mBlurWidthX : mBlurWidthY);
		float variance = ((axis == 0) ? mBlurVarianceX : mBlurVarianceY);
		
		/* Go through all of the cube map faces. */
		for (int i = 0; i < 6; ++i) {
			
			/* Copy the current face data. */
			Buffer buf = copyTextureImage(gl, i);
			
			/* Create a temporary texture to represent the current face. */
			Texture2D cubeFaceTexture = new Texture2D(gl, getFormat(), getDatatype(), getSize(), getSize(), buf);
			cubeFaceTexture.setWrapMode(gl, Texture.WrapMode.CLAMP_TO_EDGE);
			
			/* Bind the copied cube map face texture as texture unit 0. */
			cubeFaceTexture.bind(gl, 0); 								
			
			/* Bind the blur FBO and shader. */
			mBlurTextureFBO.bindGiven(gl, GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, getHandle(), 0);					
			mBlurShader.bind(gl);
			
			/* Set the program shader uniforms */
			gl.glUniform1i(mBlurShader.getUniformLocation(gl, "Axis"), axis);
			gl.glUniform1i(mBlurShader.getUniformLocation(gl, "KernelWidth"), width);
			gl.glUniform1f(mBlurShader.getUniformLocation(gl, "KernelVariance"), variance);
			gl.glUniform1f(mBlurShader.getUniformLocation(gl, "TextureSize"), getSize());
						
			/* Render the texture on a quad. This will call the shader program, which on
			 * the other hand will produce a blur version of the current cube face texture. */
			Util.renderTextureFullscreen(gl, cubeFaceTexture);			
			
			/* Unbind the blur shader and the textures. */
			mBlurShader.unbind(gl);		
			mBlurTextureFBO.unbind(gl);
			cubeFaceTexture.unbind(gl);			
		}		
	}
	
	/**
	 * Create a buffer of raw pixel data from the passed image.
	 * The returned data is RGBA8 formatted (Format.RGBA and Datatype.INT8).
	 */
	protected static Buffer createBufferFromImage(BufferedImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		
		/* Allocate space to hold data. */
		ByteBuffer result = Buffers.newDirectByteBuffer(width * height * 4);

		/* Loop over pixels and fill buffer. */
		for (int y = 0; y < height; ++y)
		{
			for (int x = 0; x < width; ++x)
			{
				int pixel = image.getRGB(x, height - 1 - y);
				
				/* getRGB() returns an ARGB-packed int */
				int alpha = (pixel >> 24) & 0xff;
				int red   = (pixel >> 16) & 0xff;
				int green = (pixel >> 8)  & 0xff;
				int blue  = (pixel >> 0)  & 0xff;
					
				/* Repack into RGBA order for OpenGL. */
				result.put((byte)red);
				result.put((byte)green);
				result.put((byte)blue);
				result.put((byte)alpha);
			}
		}
		
		/* All done. */
		result.rewind();
		return result;
	}
	
	/** 
	 * The common size of the texture, in texels.
	 */
	public int getSize()
	{
		return mSize;
	}
	
	/**
	 * The width of the texture, in texels.
	 */
	public int getWidth()
	{
		return mSize;
	}
	
	/** 
	 * The height of the texture, in texels.
	 */
	public int getHeight()
	{
		return mSize;
	}
	
	public int getCubeMapIndex()
	{
		return mCubeMapIndex;
	}
	
	public void setCubeMapIndex(int cubeMapIndex)
	{
		mCubeMapIndex = cubeMapIndex;
	}
	
	public void setBlurShaderProgram(ShaderProgram blurShaderProgram)
	{
		mBlurShader = blurShaderProgram;
	}
	
	public void setBlurWidthX(int blurWidthX) 
	{
		mBlurWidthX = blurWidthX;
	}
	
	public int getBlurWidthX() 
	{
		return mBlurWidthX;
	}
	
	public void setBlurVarianceX(float blurVarianceX) 
	{
		mBlurVarianceX = blurVarianceX;
	}
	
	public float getBlurVarianceX() 
	{
		return mBlurVarianceX;
	}
	
	
	public void setBlurWidthY(int blurWidthY) 
	{
		mBlurWidthY = blurWidthY;
	}
	
	public int getBlurWidthY()
	{
		return mBlurWidthY;
	}
	
	public void setBlurVarianceY(float blurVarianceY) 
	{
		mBlurVarianceY = blurVarianceY;
	}
	
	public float getBlurVarianceY() 
	{
		return mBlurVarianceY;
	}
	
	
	/**
	 * Copies the texture image associated with the given face from the GPU and returns it in a buffer.
	 *
	 * The format of the returned data is described by `getFormat()` and `getDatatype()`, with the exception
	 * that 16-bit floating-point textures are read onto the CPU as regular 32-bit floats.
	 * 
	 * @param faceIndex The index of the cube face, which we want to copy. 
	 */
	public Buffer copyTextureImage(GL2 gl, int faceIndex) throws OpenGLException
	{
		return copyTextureImage(gl, getFormat(), getDatatype(), faceIndex);
	}
	
	/**
	 * Copies the texture image associated with the given face from the GPU and returns it in a buffer.
	 *
	 * @param format The desired format of the returned data. Does not have to match the format of the texture.
	 * @param datatype The desired datatype of the returned data. Does not have to match the datatype of the texture.
	 * @param faceIndex The index of the cube face, which we want to copy. 
	 */
	public Buffer copyTextureImage(GL2 gl, Format format, Datatype datatype, int faceIndex) throws OpenGLException
	{
		int numChannels;
	
		/* Determine number of channels per pixel. */
		switch(format)
		{
		case RGB:       numChannels = 3; break;
		case RGBA:      numChannels = 4; break;
		case LUMINANCE: numChannels = 1; break; 
		case DEPTH:     numChannels = 1; break;
		default:		throw new OpenGLException("Invalid Format enum " + format + ".");
		}
		
		/* Allocate buffer based on datatype. */
		Buffer result;
		int bufferSize = getWidth() * getHeight() * numChannels;
		
		switch(datatype)
		{
		case INT8:    result = Buffers.newDirectByteBuffer(bufferSize);  break;
		case INT16:   result = Buffers.newDirectShortBuffer(bufferSize); break;
		case INT32:   result = Buffers.newDirectIntBuffer(bufferSize);   break;
		case FLOAT16: result = Buffers.newDirectFloatBuffer(bufferSize); break;
		case FLOAT32: result = Buffers.newDirectFloatBuffer(bufferSize); break;
		default:	  throw new OpenGLException("Invalid Datatype enum " + datatype + ".");
		}
		
		/* Bound if we weren't already. */
		boolean wasBound = isBound();
		if (!wasBound)
		{
			bind(gl, 0);
		}
		
		/* Switch to this texture, get pixel data, and switch back. */
		int previousActive[] = new int[1];
		gl.glGetIntegerv(GL2.GL_ACTIVE_TEXTURE, previousActive, 0);
		gl.glActiveTexture(GL2.GL_TEXTURE0 + getBoundTextureUnit());

		gl.glGetTexImage(GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_X + faceIndex, 0, mFormat.toGLformat(), mDatatype.toGLtype(), result);
		
		gl.glActiveTexture(GL2.GL_TEXTURE0 + getBoundTextureUnit());
		
		/* Unbind if we were unbound before. */
		if (!wasBound)
		{
			unbind(gl);
		}
		
		/* Make sure it all went smoothly, and done. */
		OpenGLException.checkOpenGLError(gl);
		return result;
	}
	
	/**
	 * Renders a fullscreen quad with this texture.
	 */
	public void blit(GL2 gl) throws OpenGLException
	{
		/* Save state, set color to white, and bind. */
		gl.glPushAttrib(GL2.GL_CURRENT_BIT);		
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		bind(gl, 0);
		
		Util.drawFlatCube(gl);
		
		/* Unbind, restore state, check for errors, and done. */
		unbind(gl);
		gl.glPopAttrib();
		
		OpenGLException.checkOpenGLError(gl);
	}
	
	@Override
	public int getTextureTarget() {
		return mTarget;
	}

	/**
	 * Returns true if the passed number is a power of two. Used for determining the texture target.
	 */
	protected static boolean isPOT(int n)
	{
		return (n != 0 && ((n & (n - 1)) == 0));
	}
};
