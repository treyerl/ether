#version 330

#include <view_block.glsl>

struct VertexData {
	vec4 position;				// vertex position in eye space
};

in vec4 vertexPosition;

uniform mat4 transformation = mat4(1.0, 0.0, 0.0, 0.0,  0.0, 1.0, 0.0, 0.0,  0.0, 0.0, 1.0, 0.0,  0.0, 0.0, 0.0, 0.1);

out VertexData vd;

void main() {
	vd.position = view.viewMatrix * transformation * vertexPosition;
	gl_Position = view.viewProjMatrix * transformation * vertexPosition;
}
