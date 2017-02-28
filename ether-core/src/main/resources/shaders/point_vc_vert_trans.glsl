#version 330

#include <view_block.glsl>

uniform bool useVertexColors;

uniform vec4 materialColor;
uniform float pointSize;

uniform mat4 transformation = mat4(1.0, 0.0, 0.0, 0.0,  0.0, 1.0, 0.0, 0.0,  0.0, 0.0, 1.0, 0.0,  0.0, 0.0, 0.0, 0.1);

in vec4 vertexPosition;
in vec4 vertexColor;

out vec4 vsColor;

void main() {
	vsColor = materialColor;
	if (useVertexColors)
		vsColor *= vertexColor;

	gl_PointSize = pointSize;
	gl_Position = view.viewProjMatrix * transformation * vertexPosition;
}
