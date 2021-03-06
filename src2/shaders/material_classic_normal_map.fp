/**
 * material_classic_normal_map.fp
 * 
 * Vertex shader shader which writes material information needed for Normal Map shading to
 * the gbuffer.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488), John DeCorato (jd537)
 * @date 2013-02-2012
 */
 
 #version 110
 
 /* ID of Blinn-Phong material, since the normal map only effects things pre-color computation. */
const int BLINNPHONG_MATERIAL_ID = 3;

/* Material properties passed from the application. */
uniform vec3 DiffuseColor;
uniform vec3 SpecularColor;
uniform float PhongExponent;

/* Textures and flags for whether they exist. */
uniform sampler2D DiffuseTexture;
uniform sampler2D SpecularTexture;
uniform sampler2D ExponentTexture;
uniform sampler2D NormalTexture;

uniform bool HasDiffuseTexture;
uniform bool HasSpecularTexture;
uniform bool HasExponentTexture;
uniform bool HasNormalTexture;

varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;
varying vec2 TexCoord;

/* Encodes a normalized vector as a vec2. See Renderer.java for more info. */
vec2 encode(vec3 n)
{
	return normalize(n.xy) * sqrt(0.5 * n.z + 0.5);
}

void main()
{
	// TODO PA2: Store diffuse color, position, encoded normal, material ID, and all other useful data in the g-buffer.
	//			 Use the normal map to get a new normal.
	
	
	vec3 diffuse = (HasDiffuseTexture ? DiffuseColor * texture2D(DiffuseTexture, TexCoord).xyz : DiffuseColor);
	vec3 specular = (HasSpecularTexture ? SpecularColor * texture2D(SpecularTexture, TexCoord).xyz : SpecularColor);
	float exponent = (HasExponentTexture ? PhongExponent * texture2D(ExponentTexture, TexCoord).x : PhongExponent);
	vec3 normal = (HasNormalTexture ? texture2D(NormalTexture, TexCoord).xyz : EyespaceNormal);
	
	vec2 n = encode(normal);
	
	gl_FragData[0] = vec4(diffuse, n.x);
	gl_FragData[1] = vec4(EyespacePosition, n.y);
	gl_FragData[2] = vec4(float(BLINNPHONG_MATERIAL_ID), specular);
	gl_FragData[3] = vec4(exponent, 0.0, 0.0, 0.0);
}