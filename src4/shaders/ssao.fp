/**
 * ssao.fp
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2013, Computer Science Department, Cornell University.
 * 
 * @author Sean Ryan (ser99)
 * @date 2013-03-23
 */

uniform sampler2DRect DiffuseBuffer;
uniform sampler2DRect PositionBuffer;

#define MAX_RAYS 100
uniform int NumRays;
uniform vec3 SampleRays[MAX_RAYS];
uniform float SampleRadius;

uniform mat4 ProjectionMatrix;
uniform vec2 ScreenSize;

/* Decodes a vec2 into a normalized vector See Renderer.java for more info. */
vec3 decode(vec2 v)
{
	vec3 n;
	n.z = 2.0 * dot(v.xy, v.xy) - 1.0;
	n.xy = normalize(v.xy) * sqrt(1.0 - n.z*n.z);
	return n;
}

void main()
{
	// Implement SSAO. Your output color should be grayscale where white is unobscured and black is fully obscured.
	vec3 normal = decode(vec2(texture2DRect(DiffuseBuffer, gl_FragCoord.xy).a,
	                     texture2DRect(PositionBuffer, gl_FragCoord.xy).a));
	
	//compute change-of-basis matrix to get from frag surface space to screen space.
	//note the important property is that frag z values map to the normal,
	//and the axes are all orthogonal.
	vec3 nCrossZ = cross(normal, vec3(0.0, 0.0, 1.0));
	vec3 nCrossZCrossN = cross(nCrossZ, normal);
	mat3 changeBasis = mat3(nCrossZCrossN, nCrossZ, normal);
	
	float tot = 0.0;
	float totPos = 0.0; 
	vec4 ray;
	
	//Notes:
	//PositionBuffer: z vals are all negative.
	//gl_FragCoord.xy = the screen pixel x and y of the fragment. Use to look up buffer vals.
	
	
	for (int i = 0; i < NumRays; i++) {
		
		
		//Obtain eye-space sample ray by applying change-of-basis matrix to the
		//fragment-surface-space ray, and scaling by the Sample Radius:
		vec4 eyeSpaceRay = vec4((changeBasis * SampleRays[i]), 1.0);
		float vis = dot(eyeSpaceRay.xyz, normal);
		eyeSpaceRay = eyeSpaceRay * SampleRadius;
		
		//Add fragment location (offset by center of ray-sphere):
		eyeSpaceRay.xyz += texture2DRect(PositionBuffer, gl_FragCoord.xy).xyz;
		
		//Convert eye-space ray to clip-space and perform perspective divide:
		vec4 clipSpaceRay = ProjectionMatrix * eyeSpaceRay;
		clipSpaceRay.xyz /= clipSpaceRay.w;
		
		//Convert clipspace (-1, 1) coordinates to screen coordinates.
		ray.x = (clipSpaceRay.x * 0.5 + 0.5) * ScreenSize.x;
		ray.y = (clipSpaceRay.y * 0.5 + 0.5) * ScreenSize.y;
		//ray.z = clipSpaceRay.z;
		
		//Look up z buffer value at the ray's calculated pixel coords:
		float zBuff = texture2DRect(PositionBuffer, vec2(ray.x, ray.y)).z;

		totPos += vis;
		if (eyeSpaceRay.z > zBuff || zBuff == 0.0) {
			tot += vis;
		}
	}
	
	if (texture2DRect(PositionBuffer, gl_FragCoord.xy).z == 0.0) {
		gl_FragColor = vec4(1.0, 1.0, 1.0, 0.0);
	}
	else {
	
		tot /= totPos;
		gl_FragColor = vec4(tot, tot, tot, 0.0);
	}

}
