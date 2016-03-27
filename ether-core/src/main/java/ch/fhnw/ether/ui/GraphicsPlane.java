/*
 * Copyright (c) 2013 - 2016 Stefan Muller Arisona, Simon Schubiger
 * Copyright (c) 2013 - 2016 FHNW & ETH Zurich
 * All rights reserved.
 *
 * Contributions by: Filip Schramka, Samuel von Stachelski
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of FHNW / ETH Zurich nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ch.fhnw.ether.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import ch.fhnw.ether.image.AWTFrameSupport;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry;
import ch.fhnw.ether.scene.mesh.material.ColorMapMaterial;
import ch.fhnw.ether.scene.mesh.material.Texture;
import ch.fhnw.util.UpdateRequest;

public class GraphicsPlane {
	public static final Font FONT = new Font("SansSerif", Font.BOLD, 12);

	private static final Color CLEAR_COLOR = new Color(0, 0, 0, 0);

	private final UpdateRequest updater = new UpdateRequest();

	private final DefaultMesh mesh;

	private final BufferedImage     image;
	private final Graphics2D        graphics;
	private final ColorMapMaterial  material;

	private int x;
	private int y;
	private int w;
	private int h;
	
	public GraphicsPlane(int x, int y, int w, int h) {
		image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		graphics = image.createGraphics();
		graphics.setFont(FONT);
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;

		float[] vertices = { x, y, 0, x + w, y, 0, x + w, y + h, 0, x, y, 0, x + w, y + h, 0, x, y + h, 0 };
		IGeometry geometry = DefaultGeometry.createVM(vertices, MeshUtilities.DEFAULT_QUAD_TEX_COORDS);
		material = new ColorMapMaterial(Texture.TRANSPARENT_1x1);

		mesh = new DefaultMesh(Primitive.TRIANGLES, material, geometry, Queue.SCREEN_SPACE_OVERLAY);
	}

	public final Texture getTexture() {
		return material.getTexture();
	}

	public final int getX() {
		return x;
	}

	public final int getY() {
		return y;
	}

	public final int getWidth() {
		return w;
	}

	public final int getHeight() {
		return h;
	}

	public final void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
		updateRequest();
	}

	public void clear() {
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
		fillRect(CLEAR_COLOR, x, y, w, h);
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		updateRequest();
	}

	public void fillRect(Color color, int x, int y, int w, int h) {
		graphics.setColor(color);
		graphics.fillRect(x, y, w, h);
		updateRequest();
	}

	public void drawString(String string, int x, int y) {
		drawString(Color.WHITE, string, x, y);
		updateRequest();
	}

	public void drawString(Color color, String string, int x, int y) {
		graphics.setColor(color);
		graphics.drawString(string, x, y);
		updateRequest();
	}

	public void drawStrings(String[] strings, int x, int y) {
		drawStrings(Color.WHITE, strings, x, y);
	}

	public void drawStrings(Color color, String[] strings, int x, int y) {
		graphics.setColor(color);
		int dy = 0;
		for (String s : strings) {
			graphics.drawString(s, x, y + dy);
			dy += FONT.getSize();
		}
		updateRequest();
	}

	public IMesh getMesh() {
		return mesh;
	}
	
	public void update() {
		if (updater.testAndClear())
			material.setColorMap(AWTFrameSupport.createFrame(image).getTexture());
	}

	private void updateRequest() {
		updater.request();
	}
}
