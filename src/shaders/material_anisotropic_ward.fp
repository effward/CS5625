/**
 * material_anisotropic_ward.fp
 * 
 * Fragment shader which writes material information needed for Anisotropic Ward shading to
 * the gbuffer.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488), Sean Ryan (ser99), Ivo Boyadzhiev (iib2)
 * @date 2013-01-30
 */

/* ID of Anisotropic Ward material, so the lighting shader knows what material
 * this pixel is. */
const int ANISOTROPIC_WARD_MATERIAL_ID = 6;

/* Material properties passed from the application. */
uniform vec3 DiffuseColor;
uniform vec3 SpecularColor;
uniform float AlphaX;
uniform float AlphaY;

/* Textures and flags for whether they exist. */
uniform sampler2D DiffuseTexture;
uniform sampler2D SpecularTexture;
uniform sampler2D AlphaXTexture;
uniform sampler2D AlphaYTexture;

uniform bool HasDiffuseTexture;
uniform bool HasSpecularTexture;
uniform bool HasAlphaXTexture;
uniform bool HasAlphaYTexture;

/* Fragment position and normal, and texcoord, from vertex shader. */
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;
varying vec2 TexCoord;

/* Tangent and BiTangent vectors (in eyespace) from vertex shader */
varying vec3 EyespaceTangent;
varying vec3 EyespaceBiTangent;

/* Encodes a normalized vector as a vec2. See Renderer.java for more info. */
vec2 encode(vec3 n)
{
	return normalize(n.xy) * sqrt(0.5 * n.z + 0.5);
}

void main()
{
	vec2 enc = encode(normalize(EyespaceNormal));
	vec2 enc2 = encode(normalize(EyespaceTangent));
	float bitangent_sign = dot(cross(EyespaceNormal, EyespaceTangent), EyespaceBiTangent);
	bitangent_sign = bitangent_sign / abs(bitangent_sign);
	
	
	if (HasDiffuseTexture) {
		gl_FragData[0] = vec4(DiffuseColor * texture2D(DiffuseTexture, TexCoord).xyz, enc.x);
	}
	
	else {
		gl_FragData[0] = vec4(DiffuseColor, enc.x);
	}
	
	gl_FragData[1] = vec4(EyespacePosition, enc.y);
	
	if (HasSpecularTexture) {
		gl_FragData[2] = vec4(float(ANISOTROPIC_WARD_MATERIAL_ID), SpecularColor * texture2D(SpecularTexture, TexCoord).xyz);
	}
	else {
		gl_FragData[2] = vec4(float(ANISOTROPIC_WARD_MATERIAL_ID), SpecularColor);
		//took out bitangent sign multiplication because it was messing with the material...
	}
	
	if (HasAlphaXTexture && HasAlphaYTexture)
	{
		gl_FragData[3] = vec4(texture2D(AlphaXTexture, TexCoord).r, texture2D(AlphaYTexture, TexCoord).r, enc2);
	}
	else if (HasAlphaXTexture)
	{
		gl_FragData[3] = vec4(texture2D(AlphaXTexture, TexCoord).r, AlphaY, enc2);
	
	}
	else if (HasAlphaYTexture)
	{
		gl_FragData[3] = vec4(AlphaX, texture2D(AlphaYTexture, TexCoord).r, enc2);
	
	}
	else 
	{
		gl_FragData[3] = vec4(AlphaX, AlphaY, enc2);
	}
	

	
}
