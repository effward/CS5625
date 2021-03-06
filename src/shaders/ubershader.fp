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
 * @author Asher Dunn (ad488), Sean Ryan (ser99), Ivo Boyadzhiev (iib2)
 * @date 2012-03-24
 */

/* Copy the IDs of any new materials here. */
const int UNSHADED_MATERIAL_ID = 1;
const int LAMBERTIAN_MATERIAL_ID = 2;
const int BLINNPHONG_MATERIAL_ID = 3;
const int COOKTORRANCE_MATERIAL_ID = 4;
const int ISOTROPIC_WARD_MATERIAL_ID = 5;
const int ANISOTROPIC_WARD_MATERIAL_ID = 6;

/* Some constant maximum number of lights which GLSL and Java have to agree on. */
#define MAX_LIGHTS 40

/* Samplers for each texture of the GBuffer. */
uniform sampler2DRect DiffuseBuffer;
uniform sampler2DRect PositionBuffer;
uniform sampler2DRect MaterialParams1Buffer;
uniform sampler2DRect MaterialParams2Buffer;
uniform sampler2DRect SilhouetteBuffer; // Unused in PA1.

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
	n.z = 2.0 * length(v.xy) - 1.0;
	n.xy = normalize(v.xy) * sqrt(1.0 - n.z*n.z);
	return n;
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
	
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * diffuse * ndotl;
	
}

/**
 * Performs Blinn-Phong shading on the passed fragment data (color, normal, etc.) for a single light.
 *  
 * @param diffuse The diffuse color of the material at this fragment
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
	
	finalColor = lightColor * attenuation * (diffuse * max(0.0, nDotL) + specular * finalScalar);
	finalColor = vec3(finalScalar);
	return finalColor;

	
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
	int materialID = int(abs(materialParams1.x));

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
		vec3 col = vec3(0.0, 0.0, 0.0);
		for (int l = 0; l < NumLights; l++) 
		{
			col += shadeLambertian(
				diffuse, 
				position, 
				normal, 
				LightPositions[l], 
				LightColors[l], 
				LightAttenuations[l]
			);
		}
		gl_FragColor.rgb = vec3(min(col.r, 1.0), min(col.g, 1.0), min(col.b, 1.0));
		
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
	else
	{
		/* Unknown material, so just use the diffuse color. */
		gl_FragColor.rgb = diffuse;
	}
	
}
