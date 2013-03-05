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
const int COOKTORRANCE_MATERIAL_ID = 4;
const int ISOTROPIC_WARD_MATERIAL_ID = 5;
const int ANISOTROPIC_WARD_MATERIAL_ID = 6;
const int REFLECTION_MATERIAL_ID = 7;

/* Some constant maximum number of lights which GLSL and Java have to agree on. */
#define MAX_LIGHTS 40

/* Samplers for each texture of the GBuffer. */
uniform sampler2DRect DiffuseBuffer;
uniform sampler2DRect PositionBuffer;
uniform sampler2DRect MaterialParams1Buffer;
uniform sampler2DRect MaterialParams2Buffer;
uniform sampler2DRect SilhouetteBuffer;

/* Pass the inverse eye/camera matrix, that can transform points from eye to world space. */
uniform mat3 CameraInverseRotation;

/* Cube map textures */
uniform samplerCube StaticCubeMapTexture;

/* Some constant maximum number of dynamic cube maps which GLSL and Java have to agree on. */
#define MAX_DYNAMIC_CUBE_MAPS 3

uniform samplerCube DynamicCubeMapTexture0;
uniform samplerCube DynamicCubeMapTexture1;
uniform samplerCube DynamicCubeMapTexture2;

uniform bool EnableToonShading;

/* Uniform specifying the sky (background) color. */
uniform vec3 SkyColor;

/* Uniforms describing the lights. */
uniform int NumLights;
uniform vec3 LightPositions[MAX_LIGHTS];
uniform vec3 LightAttenuations[MAX_LIGHTS];
uniform vec3 LightColors[MAX_LIGHTS];

/* Decodes a vec2 into a normalized vector See Renderer.java for more info. */
vec3 decode(vec2 v)
{
	vec3 n;
	n.z = 2.0 * dot(v.xy, v.xy) - 1.0;
	n.xy = normalize(v.xy) * sqrt(1.0 - n.z*n.z);
	return n;
}

/**
 * Sample a cube map, based on a given index. If index = 1,
 * this means to sample the static cube map, otherwise we
 * will sample the corresponding dynamic cube map.
 *
 * @param reflectedDirection The reflected vector in world space.
 * @param cubeMapIndex The index, identifying the cube map.
 * 
 * @return The color of the sample.
 */
vec3 sampleCubeMap(vec3 reflectedDirection, int cubeMapIndex)
{
	/* Transform the index, so that	valid values will be within 
	   [-1, MAX_DYNAMIC_CUBE_MAPS - 1], where -1 means to sample 
	   the static cube map, and values larger than 0 will be identified 
	   with the dynamic cube maps. */
 	   
   	cubeMapIndex = cubeMapIndex - 2; 	   
 	   	
 	vec3 sampledColor = vec3(0.0);
 	
	if (cubeMapIndex == -1) { /* Sample the static cube map */
		sampledColor = textureCube(StaticCubeMapTexture, reflectedDirection).xyz;
	} else if (cubeMapIndex == 0) {
		sampledColor = textureCube(DynamicCubeMapTexture0, reflectedDirection).xyz;
	} else if (cubeMapIndex == 1) {
		sampledColor = textureCube(DynamicCubeMapTexture1, reflectedDirection).xyz;
	} else if (cubeMapIndex == 2) {
		sampledColor = textureCube(DynamicCubeMapTexture2, reflectedDirection).xyz;
	} 	 	 	 	
	
	return sampledColor;
}

/**
 * Mix the base color of a pixel (e.g. computed by Cook-Torrance) with the reflected color from an environment map.
 * The base color and reflected color are linearly mixed based on the Fresnel term at this fragment. The Fresnel term is 
 * computed based on the cosine of the angle between the view vector and the normal, using Schlick's approximation.
 *
 * @param cubeMapIndex The ID of the cube map
 * @param baseColor The base color computed so far (aka. contribution from all lights was added)
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param n The index of refraction at this fragment.
 * 
 * @return The final color, mixture of base and reflected color.
 */
vec3 mixEnvMapWithBaseColor(int cubeMapIndex, vec3 baseColor, vec3 position, vec3 normal, float n) {
	// TODO PA2: Implement the requirements of this function. 
	// Hint: You can use the GLSL command mix to linearly blend between two colors.
	float theta = dot(normalize(position), normalize(normal));
	float cosTheta = cos(theta);
	
	//Schlick approx
	float rf = pow((n - 1.0)/(n + 1.0), 2.0);
	float F = rf + (1.0 - rf) * pow((1.0 - cosTheta), 5.0); 
	
	vec3 reflection = vec3(0.0);
	
	float nDotV = dot(normal, position);
	reflection = vec3(normalize(CameraInverseRotation * (2.0 * nDotV * normal - position)));
	
	vec3 reflectedColor = sampleCubeMap(reflection, cubeMapIndex);
	
	return baseColor * (1.0 - F) + reflectedColor * F;	
}

/**
 * Performs the "3x3 nonlinear filter" mentioned in Decaudin 1996 to detect silhouettes
 * based on the silhouette buffer.
 */
float silhouetteStrength()
{
	// TODO PA2: Compute the silhouette strength (see page 7 of Decaudin 1996).
	//           Hint: You have to use texture2DRect to sample the silhouette buffer,
	//                 it expects pixel indices instead of texture coordinates. Use
	//                 gl_FragCoord.xy to get the current pixel.
	
	
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

	// TODO PA2: Update this function to threshold its n.l and n.h values if toon shading is enabled.	
	
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
	
	// TODO PA2: Update this function to threshold its n.l and n.h values if toon shading is enabled.	
	
	float pow_ndoth = (ndotl > 0.0 && ndoth > 0.0 ? pow(ndoth, exponent) : 0.0);


	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * (diffuse * ndotl + specular * pow_ndoth);
}

/**
 * Performs Cook-Torrance shading on the passed fragment data (color, normal, etc.) for a single light.
 * 
 * @param diffuse The diffuse color of the material at this fragment.
 * @param specular The specular color of the material at this fragment.
 * @param m The microfacet rms slope at this fragment.
 * @param n The index of refraction at this fragment.
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param lightPosition The eyespace position of the light to compute lighting from.
 * @param lightColor The color of the light to apply.
 * @param lightAttenuation A vector of (constant, linear, quadratic) attenuation coefficients for this light.
 * @param cubeMapIndex The cube map to sample from (0 means not to use environment map lighting).
 * 
 * @return The shaded fragment color.
 */
vec3 shadeCookTorrance(vec3 diffuse, vec3 specular, float m, float n, vec3 position, vec3 normal,
	vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 viewDirection = -normalize(position);
	vec3 lightDirection = normalize(lightPosition - position);
	vec3 halfDirection = normalize(lightDirection + viewDirection);
	
	float nDotH = dot(normal, halfDirection);
	float nDotV = dot(normal, viewDirection);
	float nDotL = dot(normal, lightDirection);
	float vDotH = dot(viewDirection, halfDirection);
	
	//Schlick approx
	float rf = pow((n - 1.0)/(n + 1.0), 2.0);
	float F = rf + (1.0 - rf) * pow((1.0 - nDotL), 5.0); 
	
	//Masking and shadowing
	float G = max(0.0, min(
		min(
			1.0, 
			2.0 * nDotH * nDotV / vDotH
		),
		2.0 * nDotH * nDotL / vDotH
	));
	
	//Beckmann distrib
	//Using (tan(alpha) / m)^2 = (1 - cos(alpha)^2) / (cos(alpha)^2 * m^2)
	//and n dot h = cos(alpha)
	float D = exp(-(1.0 - nDotH * nDotH) / (nDotH * nDotH * m * m));
	D = D / (4.0 * m * m * pow(nDotH, 4.0));
	
	//Cook-Torrance specular coefficient
	//Using the nDotH > 0 cutoff that BlinnPhong above does (looks like a hack but w/e)
	//to prevent some very obnoxious artifacts
	float ct = (nDotH > 0.0 ?  F * D * G / (3.1415926536 * nDotL * nDotV) : 0.0);
	
	//Lighting
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * (diffuse * max(0.0, nDotL) + specular * ct);
	
	// TODO PA2: Update this function to threshold its n.l and n.h values if toon shading is enabled.	
}

/**
 * Performs Anisotropic Ward shading on the passed fragment data (color, normal, etc.) for a single light.
 * 
 * @param diffuse The diffuse color of the material at this fragment.
 * @param specular The specular color of the material at this fragment.
 * @param alpha_x The surface roughness in x.
 * @param alpha_y The surface roughness in y. 
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param tangent The eyespace tangent vector at this fragment.
 * @param bitangent The eyespace bitangent vector at this fragment.
 * @param lightPosition The eyespace position of the light to compute lighting from.
 * @param lightColor The color of the light to apply.
 * @param lightAttenuation A vector of (constant, linear, quadratic) attenuation coefficients for this light. 
 * 
 * @return The shaded fragment color.
 */
vec3 shadeAnisotropicWard(vec3 diffuse, vec3 specular, float alphaX, float alphaY, vec3 position, vec3 normal,
	vec3 tangent, vec3 bitangent, vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 viewDirection = -normalize(position);
	vec3 lightDirection = normalize(lightPosition - position);
	vec3 halfDirection = normalize(lightDirection + viewDirection);
	vec3 finalColor = vec3(0.0);

	
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	float PI = 3.14159265358979323846264;
	//float ndoth = max(0.0, dot(normal, halfDirection));
	float ndoth = dot(normal, halfDirection);
	float exponentX = pow(dot(halfDirection, tangent) / alphaX, 2.0);
	float exponentY = pow(dot(halfDirection, bitangent) / alphaY, 2.0);
	float exponent = -2.0 * ((exponentX + exponentY) / (1.0 + ndoth));
	float rho = dot(normal, lightDirection);
	float scalar = 1.0 / (4.0 * PI * alphaX * alphaY);
	float dots = 1.0 / (sqrt(dot(normal, lightDirection) * dot(normal, viewDirection)));
	
	float finalScalar = (dot(normal, lightDirection) > 0.0 ? rho * scalar * dots * exp(exponent) : 0.0);
	
	float nDotL = dot(normal, lightDirection);
	
	finalColor = lightColor * attenuation * (diffuse * max(0.0, nDotL));// + specular * finalScalar);
	
	// TODO PA2: Update this function to threshold its n.l and n.h values if toon shading is enabled.	
	
	
	return attenuation * finalColor;
}

/**
 * Performs Isotropic Ward shading on the passed fragment data (color, normal, etc.) for a single light.
 * 
 * @param diffuse The diffuse color of the material at this fragment.
 * @param specular The specular color of the material at this fragment.
 * @param alpha The surface roughness. 
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param lightPosition The eyespace position of the light to compute lighting from.
 * @param lightColor The color of the light to apply.
 * @param lightAttenuation A vector of (constant, linear, quadratic) attenuation coefficients for this light.
 * 
 * @return The shaded fragment color.
 */
vec3 shadeIsotropicWard(vec3 diffuse, vec3 specular, float alpha, vec3 position, vec3 normal,
	vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 viewDirection = -normalize(position);
	vec3 lightDirection = normalize(lightPosition - position);
	vec3 halfDirection = normalize(lightDirection + viewDirection);

	
	
	float nDotL = dot(normal, lightDirection);
	float nDotH = dot(normal, halfDirection);
	
	float W = exp(-(1.0 - nDotH * nDotH) / (nDotH * nDotH * alpha * alpha));
	W = (nDotL > 0.0 ? W / (4.0 * 3.1415926535 * alpha * alpha * sqrt(nDotL * nDotH)) : 0.0);
	
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * (diffuse * max(0.0, nDotL) + specular * W);
	
	// TODO PA2: Update this function to threshold its n.l and n.h values if toon shading is enabled.	
	
}

/**
 * Performs reflective shading on the passed fragment data (normal, position).
 * 
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param cubeMapIndex The id of the cube map to use (1 = static, >2 means dynamic,
 * where the DynamicCubeMapTexture{cubeMapIndex - 2} sampeld object will be used).
 * 
 * @return The reflected color.
 */
vec3 shadeReflective(vec3 position, vec3 normal, int cubeMapIndex)
{	
	// TODO PA2: Implement a perfect mirror material using environmnet map lighting.
	vec3 reflection = vec3(0.0);
	
	float nDotV = dot(normal, position);
	reflection = vec3(normalize(CameraInverseRotation * (2.0 * nDotV * normal - position)));
		
	return sampleCubeMap(reflection, 1);
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
	int materialID = abs(int(materialParams1.x));

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
		vec3 col = vec3(0.0, 0.0, 0.0);
		for (int l = 0; l < NumLights; l++) 
		{
			col += shadeBlinnPhong(
				diffuse,
				materialParams1.yza,
				materialParams2.x,
				position, 
				normal, 
				LightPositions[l], 
				LightColors[l], 
				LightAttenuations[l]
			);
		}
		gl_FragColor.rgb = vec3(min(col.r, 1.0), min(col.g, 1.0), min(col.b, 1.0));
	}
	else if (materialID == COOKTORRANCE_MATERIAL_ID) 
	{
		vec3 col = vec3(0.0, 0.0, 0.0);
		for(int l = 0; l < NumLights; l++)
		{
			col += shadeCookTorrance(
				diffuse, 
				materialParams1.yza, 
				materialParams2.x, 
				materialParams2.y, 
				position, 
				normal,
				LightPositions[l], 
				LightColors[l], 
				LightAttenuations[l]
			);		
		}
		
		gl_FragColor.rgb = vec3(min(col.r, 1.0), min(col.g, 1.0), min(col.b, 1.0));
	}
	else if (materialID == ISOTROPIC_WARD_MATERIAL_ID)
	{
		
		vec3 col = vec3(0.0, 0.0, 0.0);
		for(int l = 0; l < NumLights; l++)
		{	
			col += shadeIsotropicWard(
				diffuse, 
				materialParams1.yza, 
				materialParams2.x, 
				position, 
				normal, 
				LightPositions[l], 
				LightColors[l], 
				LightAttenuations[l]
			);	
		}
		
		gl_FragColor.rgb = vec3(min(col.r, 1.0), min(col.g, 1.0), min(col.b, 1.0));
	}
	else if (materialID == ANISOTROPIC_WARD_MATERIAL_ID)
	{
		
		vec3 col = vec3(0.0, 0.0, 0.0);
		vec3 specular = materialParams1.yzw;
		float alphaX = float(materialParams2.x);
		float alphaY = float(materialParams2.y);
		vec3 tangent = decode(materialParams2.zw);
		float bitangent_sign = materialParams1.x / abs(materialParams1.x);
		vec3 bitangent = normalize(cross(normal, tangent) * bitangent_sign);
		for(int i = 0; i < NumLights; i++)
		{
			col += shadeAnisotropicWard(
					diffuse, 
					specular, 
					alphaX, 
					alphaY, 
					position, 
					normal, 
					tangent, 
					bitangent, 
					LightPositions[i], 
					LightColors[i], 
					LightAttenuations[i]);
			
		}
		gl_FragColor.rgb = vec3(min(col.r, 1.0), min(col.g, 1.0), min(col.b, 1.0));
	}
	// TODO PA2: (1) Add logic to handle the new reflection material; (2) Extend your Cook-Torrance
	// model to support perfect mirror reflection from an environment map, given by its index. 
	else if (materialID == REFLECTION_MATERIAL_ID)
	{
		vec3 col = vec3(0.0);
		for(int i = 0; i < NumLights; i++)
		{
			col += shadeReflective(position, normal, int(materialParams2.x));
		}
		gl_FragColor.rgb = vec3(min(col.r, 1.0), min(col.g, 1.0), min(col.b, 1.0));
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
}
