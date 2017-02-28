#version 330

uniform mat4 transformation;

in vec4 vertexPosition;

void main() {
	gl_Position = transformation * vertexPosition;
}
