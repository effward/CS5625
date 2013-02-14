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
	// TODO PA1: Store diffuse color, position, encoded normal, material ID, and all other useful data in the g-buffer.
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
		gl_FragData[2] = vec4(bitangent_sign * float(ANISOTROPIC_WARD_MATERIAL_ID), SpecularColor * texture2D(SpecularTexture, TexCoord).xyz);
	}
	else {
		gl_FragData[2] = vec4(bitangent_sign * float(ANISOTROPIC_WARD_MATERIAL_ID), SpecularColor);
	}
	
	vec4 temp = vec4(AlphaX, AlphaY, enc2);
	
	if (HasAlphaXTexture) {
		temp.x = texture2D(AlphaXTexture, TexCoord).x;
	}
	if (HasAlphaYTexture) {
		temp.y = texture2D(AlphaYTexture, TexCoord).x;
	}
	
	//gl_FragData[3] = temp;
	
	if (HasDiffuseTexture) {
		gl_FragData[0] = vec4(DiffuseColor * texture2D(DiffuseTexture, TexCoord).xyz, enc.x);
	}
	
	else {
		gl_FragData[0] = vec4(DiffuseColor, enc.x);
	}
	
	//gl_FragData[1] = vec4(EyespacePosition, enc.y);
	//gl_FragData[2] = vec4(bitangent_sign * float(ANISOTROPIC_WARD_MATERIAL_ID), SpecularColor);
	//gl_FragData[3] = vec4(AlphaX, AlphaY, enc2);
	
	
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
	

	gl_FragData[3] = temp;

	//gl_FragData[0] = gl_FragData[1] = gl_FragData[2] = gl_fragData[3] = vec4(1.0);

	
}
