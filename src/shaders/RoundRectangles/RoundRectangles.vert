#version 430 core
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
#extension GL_AMD_shader_trinary_minmax : enable
#extension GL_GOOGLE_include_directive : require

// input
layout(location = 0) in vec2  inPos;
layout(location = 1) in vec2  inRectPos;
layout(location = 2) in vec2  inRectSize;
layout(location = 3) in vec4  inColor;
layout(location = 4) in float inRadius;

// output
layout(location = 0) out vec2 outPixelPos;
layout(location = 1) out flat vec2 outPos;
layout(location = 2) out flat vec2 outSize;
layout(location = 3) out vec4 outColor;
layout(location = 4) out float outRadius;

// bindings
layout(binding = 0, std140) uniform UBO {
    mat4 viewProj;
} ubo;

void main() {
    gl_Position = ubo.viewProj * vec4(inPos, 0, 1);

    outPixelPos = inPos;
    outPos      = inRectPos;
    outSize     = inRectSize;
    outColor    = inColor;
    outRadius   = inRadius;
}