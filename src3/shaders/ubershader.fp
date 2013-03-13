/**
 * ubershader.fp
 * 
 * Fragment shader for the "ubershader" which lights the contents of the gbuffer. This shader
 * samples from the gbuffer and then computes lighting depending on the material type of this 
 * fragment.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488), Ivaylo Boyadzhiev (iib2)
 * @date 2012-03-24
 */

/* Copy the IDs of any new materials here. */
const int UNSHADED_MATERIAL_ID = 1;
const int LAMBERTIAN_MATERIAL_ID = 2;
const int BLINNPHONG_MATERIAL_ID = 3;

/* Some constant maximum number of lights which GLSL and Java have to agree on. */
#define MAX_LIGHTS 40

/* Samplers for each texture of the GBuffer. */
uniform sampler2DRect DiffuseBuffer;
uniform sampler2DRect PositionBuffer;
uniform sampler2DRect MaterialParams1Buffer;
uniform sampler2DRect MaterialParams2Buffer;
uniform sampler2DRect SilhouetteBuffer;

uniform bool EnableToonShading;

/* Uniform specifying the sky (background) color. */
uniform vec3 SkyColor;

/* Uniforms describing the lights. */
uniform int NumLights;
uniform vec3 LightPositions[MAX_LIGHTS];
uniform vec3 LightAttenuations[MAX_LIGHTS];
uniform vec3 LightColors[MAX_LIGHTS];

/* Shadow depth textures and information */
uniform int HasShadowMaps;
uniform sampler2D ShadowMap;
uniform int ShadowMode;
uniform vec3 ShadowCamPosition;
uniform float bias;
uniform float ShadowSampleWidth;
uniform float ShadowMapWidth;
uniform float ShadowMapHeight;
uniform float LightWidth;

#define DEFAULT_SHADOW_MAP 0
#define PCF_SHADOW_MAP 1
#define PCSS_SHADOW_MAP 2

/* Pass the shadow camera Projection * View matrix to help transform points, as well the Camera inverse-view Matrix */
uniform mat4 LightMatrix;
uniform mat4 InverseViewMatrix;

/* Decodes a vec2 into a normalized vector See Renderer.java for more info. */
vec3 decode(vec2 v)
{
	vec3 n;
	n.z = 2.0 * dot(v.xy, v.xy) - 1.0;
	n.xy = normalize(v.xy) * sqrt(1.0 - n.z*n.z);
	return n;
}

// Converts the depth buffer value to a linear value
float DepthToLinear(float value)
{
	float near = 0.1;
	float far = 100.0;
	return (2.0 * near) / (far + near - value * (far - near));
}

/** Returns a binary value for if this location is shadowed. 0 = shadowed, 1 = not shadowed.
 */
float getShadowVal(vec4 shadowCoord, vec2 offset) 
{
	// TODO PA3: Implement this function (see above).
	float depth = DepthToLinear(texture2D(ShadowMap, shadowCoord.xy + vec2(offset.x / shadowCoord.w, offset.y / shadowCoord.w)).w);
	//return (depth > shadowCoord.z ? 1.0 : 0.0);
	return (shadowCoord.z > depth + bias ? 0.0 : 1.0);
}

/** Calculates regular shadow map algorithm shadow strength
 *
 * @param shadowCoord The location of the position in the light projection space
 */
 float getDefaultShadowMapVal(vec4 shadowCoord)
 {
 	// TODO PA3: Implement this function (see above).
 	float depth = DepthToLinear(texture2D(ShadowMap, shadowCoord.xy).x);
	//return (depth - shadowCoord.z + 13);// + bias > shadowCoord.z ? 1.0 : 0.0);
	//return max(0.0, min(1.0, shadowCoord.y));
	//return shadowCoord.z - 10;
	return (int(shadowCoord.z) % 2 == 0 && int(shadowCoord.x) % 2 == 0 && int(shadowCoord.y) % 2 == 0 ? 1.0 : 0.0);


	//return (depth > shadowCoord.z ? 1.0 : 0.0);
	//return getShadowVal(shadowCoord, vec2(0,0));

	
 }
 
/** Calculates PCF shadow map algorithm shadow strength
 *
 * @param shadowCoord The location of the position in the light projection space
 */
 float getPCFShadowMapVal(vec4 shadowCoord)
 {
 	// TODO PA3: Implement this function (see above).
 	/*
 	float x,y,n,shadow;
 	n = pow(2.0 * ShadowSampleWidth + 1.0, 2.0);
 	for(y = -ShadowSampleWidth; y <= ShadowSampleWidth; y+=1.0)
 		for (x = -ShadowSampleWidth; x <= ShadowSampleWidth; x+=1.0)
 			shadow += getShadowVal(shadowCoord, vec2(x,y));
 	return shadow / n;
 	*/
 	return 1.0;
 }
 
 /** Calculates PCSS shadow map algorithm shadow strength
 *
 * @param shadowCoord The location of the position in the light projection space
 */
 float getPCSSShadowMapVal(vec4 shadowCoord)
 {
 	float near = 0.1;
 	float far = 100.0;
 	float blockerSampleWidth = 2.0;
 	
 	// TODO PA3: Implement this function (see above).

 	float x,y,depthBlocker,tempDepth;
 	int depthCount = 0;
 	
 	//blocker search step
 	for(y = -blockerSampleWidth; y <= blockerSampleWidth; y+=1.0) {
 		for (x = -blockerSampleWidth; x <= blockerSampleWidth; x+=1.0) {
 			tempDepth = texture2D(ShadowMap, shadowCoord.xy).w;
 			if (tempDepth < shadowCoord.z) {
 				depthBlocker += tempDepth;
 				depthCount += 1;
			}
		}
	}
	if (depthCount > 0) {
		depthBlocker /= depthCount;
		
		// Penumbra Estimation
		float widthPenumbra = (shadowCoord.z - depthBlocker) * LightWidth / depthBlocker;
		
		//Variable PCF
		float scale = 10.0;
		float sampleWidth = widthPenumbra * scale;
	 	float n = pow(2.0 * sampleWidth + 1.0, 2.0);
	 	float shadow = 0.0;
	 	
		for(y = -sampleWidth; y <= sampleWidth; y+=1.0)
			for(x = -sampleWidth; x <= sampleWidth; x+=1.0)
				shadow += getShadowVal(shadowCoord, vec2(x,y));
		return shadow / n;
	}
	else {
 		return 1.0;
	}

 }

/** Gets the shadow value based on the current shadowing mode
 *
 * @param position The eyespace position of the surface at this fragment
 *
 * @return A 0-1 value for how shadowed the object is. 0 = shadowed and 1 = lit
 */
float getShadowStrength(vec3 position) {
	// TODO PA3: Transform position to ShadowCoord
	vec4 ShadowCoord =  LightMatrix * (InverseViewMatrix * vec4(position, 1.0));
	
	if (ShadowMode == DEFAULT_SHADOW_MAP)
	{
		return getDefaultShadowMapVal(ShadowCoord);
	}
	else if (ShadowMode == PCF_SHADOW_MAP)
	{
		return getPCFShadowMapVal(ShadowCoord);
	}
	else
	{
		return getPCSSShadowMapVal(ShadowCoord);
	}
}

/**
 * Performs the "3x3 nonlinear filter" mentioned in Decaudin 1996 to detect silhouettes
 * based on the silhouette buffer.
 */
float silhouetteStrength()
{
	// TODO PA3 Prereq (Optional): Paste in your silhouetteStrength code if you like toon shading.
	return 0.0;
}

/**
 * Performs Lambertian shading on the passed fragment data (color, normal, etc.) for a single light.
 * 
 * @param diffuse The diffuse color of the material at this fragment.
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param lightPosition The eyespace position of the light to compute lighting from.
 * @param lightColor The color of the light to apply.
 * @param lightAttenuation A vector of (constant, linear, quadratic) attenuation coefficients for this light.
 * 
 * @return The shaded fragment color; for Lambertian, this is `lightColor * diffuse * n_dot_l`.
 */
vec3 shadeLambertian(vec3 diffuse, vec3 position, vec3 normal, vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 lightDirection = normalize(lightPosition - position);
	float ndotl = max(0.0, dot(normal, lightDirection));

	// TODO PA3 Prereq (Optional): Paste in your n.l and n.h thresholding code if you like toon shading.
	
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * diffuse * ndotl;
}

/**
 * Performs Blinn-Phong shading on the passed fragment data (color, normal, etc.) for a single light.
 *  
 * @param diffuse The diffuse color of the material at this fragment.
 * @param specular The specular color of the material at this fragment.
 * @param exponent The Phong exponent packed into the alpha channel. 
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param lightPosition The eyespace position of the light to compute lighting from.
 * @param lightColor The color of the light to apply.
 * @param lightAttenuation A vector of (constant, linear, quadratic) attenuation coefficients for this light.
 * 
 * @return The shaded fragment color.
 */
vec3 shadeBlinnPhong(vec3 diffuse, vec3 specular, float exponent, vec3 position, vec3 normal,
	vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 viewDirection = -normalize(position);
	vec3 lightDirection = normalize(lightPosition - position);
	vec3 halfDirection = normalize(lightDirection + viewDirection);
		
	float ndotl = max(0.0, dot(normal, lightDirection));
	float ndoth = max(0.0, dot(normal, halfDirection));
	
	// TODO PA3 Prereq (Optional): Paste in your n.l and n.h thresholding code if you like toon shading.
	
	float pow_ndoth = (ndotl > 0.0 && ndoth > 0.0 ? pow(ndoth, exponent) : 0.0);


	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * (diffuse * ndotl + specular * pow_ndoth);
}

void main()
{
	/* Sample gbuffer. */
	vec3 diffuse         = texture2DRect(DiffuseBuffer, gl_FragCoord.xy).xyz;
	vec3 position        = texture2DRect(PositionBuffer, gl_FragCoord.xy).xyz;
	vec4 materialParams1 = texture2DRect(MaterialParams1Buffer, gl_FragCoord.xy);
	vec4 materialParams2 = texture2DRect(MaterialParams2Buffer, gl_FragCoord.xy);
	vec3 normal          = decode(vec2(texture2DRect(DiffuseBuffer, gl_FragCoord.xy).a,
	                                   texture2DRect(PositionBuffer, gl_FragCoord.xy).a));
	
	/* Initialize fragment to black. */
	gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);

	/* Branch on material ID and shade as appropriate. */
	int materialID = int(materialParams1.x);

	if (materialID == 0)
	{
		/* Must be a fragment with no geometry, so set to sky (background) color. */
		gl_FragColor = vec4(SkyColor, 1.0);
	}
	else if (materialID == UNSHADED_MATERIAL_ID)
	{
		/* Unshaded material is just a constant color. */
		gl_FragColor.rgb = diffuse;
	}
	else if (materialID == LAMBERTIAN_MATERIAL_ID)
	{
		/* Accumulate Lambertian shading for each light. */
		for (int i = 0; i < NumLights; ++i)
		{
			gl_FragColor.rgb += shadeLambertian(diffuse, position, normal, LightPositions[i], LightColors[i], LightAttenuations[i]);
		}
	}
	else if (materialID == BLINNPHONG_MATERIAL_ID)
	{
		/* Accumulate Blinn-Phong shading for each light. */
		for (int i = 0; i < NumLights; ++i)
		{
			gl_FragColor.rgb += shadeBlinnPhong(
				diffuse,
				materialParams1.yza,
				materialParams2.x,
				position, 
				normal, 
				LightPositions[i], 
				LightColors[i], 
				LightAttenuations[i]
			);
		}
	}
	else
	{
		/* Unknown material, so just use the diffuse color. */
		gl_FragColor.rgb = diffuse;
	}

	if (EnableToonShading)
	{
		gl_FragColor.rgb = mix(gl_FragColor.rgb, vec3(0.0), silhouetteStrength()); 
	}
	
	if (HasShadowMaps == 1 && materialID != 0) {	
		gl_FragColor.rgb *= getShadowStrength(position);
	}
	
}
