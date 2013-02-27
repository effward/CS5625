/**
 * material_reflection.fp
 * 
 * Fragment shader for calculating Cube Map reflection.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Ivaylo Boyadzhiev (iib2)
 * @date 2013-02-11
 */
 
 /* ID of Reflection material, so the lighting shader knows what material
 * this pixel is. */
const int REFLECTION_MATERIAL_ID = 7;

/* The index of the cube map, attached to this material object. */
uniform int CubeMapIndex;

/* Fragment position and normal passed from the vertex shader. */
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;

/* Encodes a normalized vector as a vec2. See Renderer.java for more info. */
vec2 encode(vec3 n)
{
	return normalize(n.xy) * sqrt(0.5 * n.z + 0.5);
}

void main()
{
    // TODO PA2: Store position, encoded normal, material ID, and the cube map index in the g-buffer.
	
	gl_FragData[0] = gl_FragData[1] = gl_FragData[2] = gl_FragData[3] = vec4(1.0);		
}
