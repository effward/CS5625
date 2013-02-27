package cs5625.deferred.materials;

import javax.media.opengl.GL2;
import javax.vecmath.Color3f;

import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.rendering.ShaderProgram;

/**
 * NormapMapMaterial.java
 * 
 * Implements the Blinn-Phong shading model with the option for a normal map.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2013, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488), John DeCorato (jd537)
 * @date 2013-02-12
 */
public class NormalMapMaterial extends Material{
	/* Blinn-Phong material properties. */
	private Color3f mDiffuseColor = new Color3f(1.0f, 1.0f, 1.0f);
	private Color3f mSpecularColor = new Color3f(1.0f, 1.0f, 1.0f);
	private float mPhongExponent = 50.0f;
	
	/* Optional textures for texture parameterized rendering. */
	private Texture2D mDiffuseTexture = null;
	private Texture2D mSpecularTexture = null;
	private Texture2D mExponentTexture = null;
	
	/* Normal map */
	private Texture2D mNormalTexture = null;
	
	/* Uniform locations for the shader. */
	private int mDiffuseUniformLocation = -1;
	private int mSpecularUniformLocation = -1;
	private int mExponentUniformLocation = -1;

	private int mHasDiffuseTextureUniformLocation = -1;
	private int mHasSpecularTextureUniformLocation = -1;
	private int mHasExponentTextureUniformLocation = -1;
	private int mHasNormalTextureUniformLocation = -1;
	
	public NormalMapMaterial() {
		
	}
	
	public NormalMapMaterial(Color3f diffuseColor, Texture2D normalTexture) {
		mDiffuseColor = diffuseColor;
		mNormalTexture = normalTexture;
	}
	
	public Color3f getDiffuseColor()
	{
		return mDiffuseColor;
	}
	
	public void setDiffuseColor(Color3f diffuse)
	{
		mDiffuseColor = diffuse;
	}

	public Color3f getSpecularColor()
	{
		return mSpecularColor;
	}
	
	public void setSpecularColor(Color3f specular)
	{
		mSpecularColor = specular;
	}

	public Texture2D getDiffuseTexture()
	{
		return mDiffuseTexture;
	}
	
	public void setDiffuseTexture(Texture2D texture)
	{
		mDiffuseTexture = texture;
	}

	public Texture2D getSpecularTexture()
	{
		return mSpecularTexture;
	}
	
	public void setSpecularTexture(Texture2D texture)
	{
		mSpecularTexture = texture;
	}
	
	public Texture2D getExponentTexture()
	{
		return mExponentTexture;
	}
	
	public void setExponentTexture(Texture2D texture)
	{
		mExponentTexture = texture;
	}

	
	public Texture2D getNormalTexture()
	{
		return mNormalTexture;
	}
	
	public void setNormalTexture(Texture2D texture)
	{
		mNormalTexture = texture;
	}
	
	@Override
	public void bind(GL2 gl) throws OpenGLException {
		getShaderProgram().bind(gl);
		
		//TODO PA2: Set shader uniforms and bind textures			
		
	}

	@Override
	protected void initializeShader(GL2 gl, ShaderProgram shader)
	{
		/* Get locations of uniforms in this shader. */
		mDiffuseUniformLocation = shader.getUniformLocation(gl, "DiffuseColor");
		mSpecularUniformLocation = shader.getUniformLocation(gl, "SpecularColor");
		mExponentUniformLocation = shader.getUniformLocation(gl, "PhongExponent");
		
		mHasDiffuseTextureUniformLocation = shader.getUniformLocation(gl, "HasDiffuseTexture");
		mHasSpecularTextureUniformLocation = shader.getUniformLocation(gl, "HasSpecularTexture");
		mHasExponentTextureUniformLocation = shader.getUniformLocation(gl, "HasExponentTexture");
		mHasNormalTextureUniformLocation = shader.getUniformLocation(gl, "HasNormalTexture");
		
		/* These are only set once, so set them here. */
		shader.bind(gl);
		gl.glUniform1i(shader.getUniformLocation(gl, "DiffuseTexture"), 0);
		gl.glUniform1i(shader.getUniformLocation(gl, "SpecularTexture"), 1);
		gl.glUniform1i(shader.getUniformLocation(gl, "ExponentTexture"), 2);
		gl.glUniform1i(shader.getUniformLocation(gl, "NormalTexture"), 3);
		shader.unbind(gl);
	}
	
	@Override
	public void unbind(GL2 gl) {
		getShaderProgram().unbind(gl);
		
		// TODO PA2: Unbind any used textures.
		
	}

	@Override
	public String[] getRequiredVertexAttributes() {
		return new String[] {"VertexTangent"};
	}
	
	@Override
	public String getShaderIdentifier() {
		return "shaders/material_normal_map";
	}

}
