/**
 * material_isotropic_ward.fp
 * 
 * Fragment shader which writes material information needed for Isotropic Ward shading to
 * the gbuffer.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488), Sean Ryan (ser99), Ivo Boyadzhiev (iib2)
 * @date 2013-01-30
 */

/* ID of Isotropic Ward material, so the lighting shader knows what material
 * this pixel is. */
const int ISOTROPIC_WARD_MATERIAL_ID = 5;

/* Material properties passed from the application. */
uniform vec3 DiffuseColor;
uniform vec3 SpecularColor;
uniform float Alpha;

/* Textures and flags for whether they exist. */
uniform sampler2D DiffuseTexture;
uniform sampler2D SpecularTexture;
uniform sampler2D AlphaTexture;

uniform bool HasDiffuseTexture;
uniform bool HasSpecularTexture;
uniform bool HasAlphaTexture;

/* Fragment position and normal, and texcoord, from vertex shader. */
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
	vec2 enc = encode(EyespaceNormal);
	
	if (HasDiffuseTexture) {
		gl_FragData[0] = vec4(DiffuseColor * texture2D(DiffuseTexture, TexCoord).xyz, enc.x);
	}
	else {
		gl_FragData[0] = vec4(DiffuseColor, enc.x);
	}
	
	gl_FragData[1] = vec4(EyespacePosition, enc.y);
	
	if (HasSpecularTexture) {
		gl_FragData[2] = vec4(float(ISOTROPIC_WARD_MATERIAL_ID), SpecularColor * texture2D(SpecularTexture, TexCoord).xyz);
	}
	else {
		gl_FragData[2] = vec4(float(ISOTROPIC_WARD_MATERIAL_ID), SpecularColor);
	}
	
	if (HasAlphaTexture) {
		gl_FragData[3] = vec4(texture2D(AlphaTexture, TexCoord).x, 0.0, 0.0, 0.0);
	}
	else {
		gl_FragData[3] = vec4(Alpha, vec3(0.0));
	}
	
}
