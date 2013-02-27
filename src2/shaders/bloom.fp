/**
 * bloom.fp
 * 
 * Fragment shader for the bloom post-processing algorithm.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2013, Computer Science Department, Cornell University.
 * 
 * @author Sean Ryan (ser99)
 * @date 2013-01-31
 */

/* Sampler for the final scene in the GBuffer. */
uniform sampler2DRect FinalSceneBuffer;

uniform float KernelVariance;
uniform int KernelWidth;
uniform float Threshold;

// TODO PA2: Implement the bloom shader.

void main()
{
    gl_FragColor = vec4(0.0);
}
