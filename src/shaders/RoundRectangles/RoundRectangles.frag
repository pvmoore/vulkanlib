#version 430 core
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shading_language_420pack : enable
#extension GL_AMD_shader_trinary_minmax : enable
#extension GL_GOOGLE_include_directive : require

// inputs
layout(location = 0) in vec2 inPixelPos;
layout(location = 1) in flat vec2 inRectPos;
layout(location = 2) in flat vec2 inRectSize;
layout(location = 3) in vec4 inColor;
layout(location = 4) in float inRadius;

// outputs
layout(location = 0) out vec4 outColor;

// bindings

void main() {
    // assuming an axis-aligned rectangle
    vec2 pos  = inPixelPos-inRectPos;
    vec2 size = inRectSize;
    vec2 mid  = size/2;

    float alpha = inColor.a;

    if(pos.x>mid.x) {
        pos.x = size.x-pos.x;
    }
    if(pos.y>mid.y) {
        pos.y = size.y-pos.y;
    }

    float dfc = distance(pos, inRadius.xx);

    if(pos.x<inRadius && pos.y<inRadius) {
        // we are in a corner
        if(dfc > inRadius) discard;

        float v = inRadius/dfc;
        alpha = clamp(v, 0, alpha);
    }

    outColor = vec4(inColor.rgb, alpha);
}
