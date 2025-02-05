/******************************************************************************
 * Spine Runtimes License Agreement
 * Last updated January 1, 2020. Replaces all prior versions.
 *
 * Copyright (c) 2013-2020, Esoteric Software LLC
 *
 * Integration of the Spine Runtimes into software or otherwise creating
 * derivative works of the Spine Runtimes is permitted under the terms and
 * conditions of Section 2 of the Spine Editor License Agreement:
 * http://esotericsoftware.com/spine-editor-license
 *
 * Otherwise, it is permitted to integrate the Spine Runtimes into software
 * or otherwise create derivative works of the Spine Runtimes (collectively,
 * "Products"), provided that each user of the Products must obtain their own
 * Spine Editor license and redistribution of the Products in any form must
 * include this license and copyright notice.
 *
 * THE SPINE RUNTIMES ARE PROVIDED BY ESOTERIC SOFTWARE LLC "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ESOTERIC SOFTWARE LLC BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES,
 * BUSINESS INTERRUPTION, OR LOSS OF USE, DATA, OR PROFITS) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THE SPINE RUNTIMES, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package com.esotericsoftware.spine;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.ShortArray;

import com.esotericsoftware.spine.BlendMode;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.ClippingAttachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;
import com.esotericsoftware.spine.attachments.SkeletonAttachment;
import com.esotericsoftware.spine.utils.SkeletonClipping;
import com.esotericsoftware.spine.utils.TwoColorPolygonBatch;
import com.talosvfx.talos.editor.addons.bvb.BoundEffect;
import com.talosvfx.talos.editor.addons.bvb.SkeletonContainer;
import com.talosvfx.talos.runtime.ParticleEffectInstance;
import com.talosvfx.talos.runtime.render.SpriteBatchParticleRenderer;

public class BVBSkeletonRenderer {
	static private final short[] quadTriangles = {0, 1, 2, 2, 3, 0};

	private boolean pmaColors, pmaBlendModes;
	private final FloatArray vertices = new FloatArray(32);
	private final SkeletonClipping clipper = new SkeletonClipping();
	private @Null
	com.esotericsoftware.spine.SkeletonRenderer.VertexEffect vertexEffect;
	private final Vector2 temp = new Vector2();
	private final Vector2 temp2 = new Vector2();
	private final Color temp3 = new Color();
	private final Color temp4 = new Color();
	private final Color temp5 = new Color();
	private final Color temp6 = new Color();

	/** Renders the specified skeleton. If the batch is a PolygonSpriteBatch, {@link #draw(SpriteBatchParticleRenderer, PolygonSpriteBatch, SkeletonContainer, Skeleton)} is
	 * called. If the batch is a TwoColorPolygonBatch, {@link #draw(SpriteBatchParticleRenderer, TwoColorPolygonBatch, SkeletonContainer, Skeleton)} is called. Otherwise the
	 * skeleton is rendered without two color tinting and any mesh attachments will throw an exception.
	 * <p>
	 * This method may change the batch's {@link Batch#setBlendFunctionSeparate(int, int, int, int) blending function}. The
	 * previous blend function is not restored, since that could result in unnecessary flushes, depending on what is rendered
	 * next. */
	public void draw (SpriteBatchParticleRenderer particleRenderer, Batch batch, SkeletonContainer skeletonContainer, Skeleton skeleton) {
		if (batch instanceof TwoColorPolygonBatch) {
			draw(particleRenderer, (TwoColorPolygonBatch)batch, skeletonContainer, skeleton);
			return;
		}
		if (batch instanceof PolygonSpriteBatch) {
			draw(particleRenderer, (PolygonSpriteBatch)batch, skeletonContainer, skeleton);
			return;
		}
		if (batch == null) throw new IllegalArgumentException("batch cannot be null.");
		if (skeletonContainer == null) throw new IllegalArgumentException("skeleton cannot be null.");

		com.esotericsoftware.spine.SkeletonRenderer.VertexEffect vertexEffect = this.vertexEffect;
		if (vertexEffect != null) vertexEffect.begin(skeleton);

		boolean pmaColors = this.pmaColors, pmaBlendModes = this.pmaBlendModes;
		BlendMode blendMode = null;
		float[] vertices = this.vertices.items;
		Color skeletonColor = skeleton.color;
		float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
		Object[] drawOrder = skeleton.drawOrder.items;
		for (int i = 0, n = skeleton.drawOrder.size; i < n; i++) {
			Slot slot = (Slot)drawOrder[i];
			if (!slot.bone.active) {
				clipper.clipEnd(slot);
				continue;
			}
			Attachment attachment = slot.attachment;
			if (attachment instanceof RegionAttachment) {
				RegionAttachment region = (RegionAttachment)attachment;
				region.computeWorldVertices(slot.getBone(), vertices, 0, 5);
				Color color = region.getColor(), slotColor = slot.getColor();
				float alpha = a * slotColor.a * color.a * 255;
				float multiplier = pmaColors ? alpha : 255;

				BlendMode slotBlendMode = slot.data.getBlendMode();
				if (slotBlendMode != blendMode) {
					if (slotBlendMode == BlendMode.additive && pmaColors) {
						slotBlendMode = BlendMode.normal;
						alpha = 0;
					}
					blendMode = slotBlendMode;
					blendMode.apply(batch, pmaBlendModes);
				}

				float c = NumberUtils.intToFloatColor((int)alpha << 24 //
					| (int)(b * slotColor.b * color.b * multiplier) << 16 //
					| (int)(g * slotColor.g * color.g * multiplier) << 8 //
					| (int)(r * slotColor.r * color.r * multiplier));
				float[] uvs = region.getUVs();
				for (int u = 0, v = 2; u < 8; u += 2, v += 5) {
					vertices[v] = c;
					vertices[v + 1] = uvs[u];
					vertices[v + 2] = uvs[u + 1];
				}

				if (vertexEffect != null) applyVertexEffect(vertices, 20, 5, c, 0);

				batch.draw(region.getRegion().getTexture(), vertices, 0, 20);

			} else if (attachment instanceof ClippingAttachment) {
				clipper.clipStart(slot, (ClippingAttachment)attachment);
				continue;

			} else if (attachment instanceof MeshAttachment) {
				throw new RuntimeException(batch.getClass().getSimpleName()
					+ " cannot render meshes, PolygonSpriteBatch or TwoColorPolygonBatch is required.");

			} else if (attachment instanceof SkeletonAttachment) {
				Skeleton attachmentSkeleton = ((SkeletonAttachment)attachment).getSkeleton();
				if (attachmentSkeleton != null) {
					draw(particleRenderer, batch, skeletonContainer, attachmentSkeleton); //This is messed up
				}
			}

			clipper.clipEnd(slot);
		}
		clipper.clipEnd();
		if (vertexEffect != null) vertexEffect.end();
	}

	/** Renders the specified skeleton, including meshes, but without two color tinting.
	 * <p>
	 * This method may change the batch's {@link Batch#setBlendFunctionSeparate(int, int, int, int) blending function}. The
	 * previous blend function is not restored, since that could result in unnecessary flushes, depending on what is rendered
	 * next. */
	public void draw (SpriteBatchParticleRenderer particleRenderer, PolygonSpriteBatch batch, SkeletonContainer skeletonContainer, Skeleton skeleton) {
		if (batch == null) throw new IllegalArgumentException("batch cannot be null.");
		if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");

		Vector2 tempPosition = this.temp, tempUV = this.temp2;
		Color tempLight1 = this.temp3, tempDark1 = this.temp4;
		Color tempLight2 = this.temp5, tempDark2 = this.temp6;
		com.esotericsoftware.spine.SkeletonRenderer.VertexEffect vertexEffect = this.vertexEffect;
		if (vertexEffect != null) vertexEffect.begin(skeleton);

		boolean pmaColors = this.pmaColors, pmaBlendModes = this.pmaBlendModes;
		BlendMode blendMode = null;
		int verticesLength = 0;
		float[] vertices = null, uvs = null;
		short[] triangles = null;
		Color color = null, skeletonColor = skeleton.color;
		float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
		Object[] drawOrder = skeleton.drawOrder.items;
		for (int i = 0, n = skeleton.drawOrder.size; i < n; i++) {
			Slot slot = (Slot)drawOrder[i];
			if (!slot.bone.active) {
				clipper.clipEnd(slot);
				continue;
			}

			BoundEffect boundEffect = skeletonContainer.findEffect(skeleton, slot);

			if (boundEffect != null && boundEffect.isNested() && boundEffect.isBehind()) {
				for (ParticleEffectInstance particleEffectInstance : boundEffect.getParticleEffects()) {
					particleRenderer.render(particleEffectInstance);
				}
			}

			Texture texture = null;
			int vertexSize = clipper.isClipping() ? 2 : 5;
			Attachment attachment = slot.attachment;
			if (attachment instanceof RegionAttachment) {
				RegionAttachment region = (RegionAttachment)attachment;
				verticesLength = vertexSize << 2;
				vertices = this.vertices.items;
				region.computeWorldVertices(slot.getBone(), vertices, 0, vertexSize);
				triangles = quadTriangles;
				texture = region.getRegion().getTexture();
				uvs = region.getUVs();
				color = region.getColor();

			} else if (attachment instanceof MeshAttachment) {
				MeshAttachment mesh = (MeshAttachment)attachment;
				int count = mesh.getWorldVerticesLength();
				verticesLength = (count >> 1) * vertexSize;
				vertices = this.vertices.setSize(verticesLength);
				mesh.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize);
				triangles = mesh.getTriangles();
				texture = mesh.getRegion().getTexture();
				uvs = mesh.getUVs();
				color = mesh.getColor();

			} else if (attachment instanceof ClippingAttachment) {
				ClippingAttachment clip = (ClippingAttachment)attachment;
				clipper.clipStart(slot, clip);
				continue;

			} else if (attachment instanceof SkeletonAttachment) {
				Skeleton attachmentSkeleton = ((SkeletonAttachment)attachment).getSkeleton();
				if (attachmentSkeleton != null) draw(particleRenderer, batch, skeletonContainer, attachmentSkeleton);
			}

			if (texture != null) {
				Color slotColor = slot.getColor();
				float alpha = a * slotColor.a * color.a * 255;
				float multiplier = pmaColors ? alpha : 255;

				BlendMode slotBlendMode = slot.data.getBlendMode();
				if (slotBlendMode != blendMode) {
					if (slotBlendMode == BlendMode.additive && pmaColors) {
						slotBlendMode = BlendMode.normal;
						alpha = 0;
					}
					blendMode = slotBlendMode;
					blendMode.apply(batch, pmaBlendModes);
				}

				float c = NumberUtils.intToFloatColor((int)alpha << 24 //
					| (int)(b * slotColor.b * color.b * multiplier) << 16 //
					| (int)(g * slotColor.g * color.g * multiplier) << 8 //
					| (int)(r * slotColor.r * color.r * multiplier));

				if (clipper.isClipping()) {
					clipper.clipTriangles(vertices, verticesLength, triangles, triangles.length, uvs, c, 0, false);
					FloatArray clippedVertices = clipper.getClippedVertices();
					ShortArray clippedTriangles = clipper.getClippedTriangles();
					if (vertexEffect != null) applyVertexEffect(clippedVertices.items, clippedVertices.size, 5, c, 0);
					batch.draw(texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0,
						clippedTriangles.size);
				} else {
					if (vertexEffect != null) {
						tempLight1.set(NumberUtils.floatToIntColor(c));
						tempDark1.set(0);
						for (int v = 0, u = 0; v < verticesLength; v += 5, u += 2) {
							tempPosition.x = vertices[v];
							tempPosition.y = vertices[v + 1];
							tempLight2.set(tempLight1);
							tempDark2.set(tempDark1);
							tempUV.x = uvs[u];
							tempUV.y = uvs[u + 1];
							vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2);
							vertices[v] = tempPosition.x;
							vertices[v + 1] = tempPosition.y;
							vertices[v + 2] = tempLight2.toFloatBits();
							vertices[v + 3] = tempUV.x;
							vertices[v + 4] = tempUV.y;
						}
					} else {
						for (int v = 2, u = 0; v < verticesLength; v += 5, u += 2) {
							vertices[v] = c;
							vertices[v + 1] = uvs[u];
							vertices[v + 2] = uvs[u + 1];
						}
					}
					batch.draw(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
				}
			}

			clipper.clipEnd(slot);

			if (boundEffect != null && boundEffect.isNested() && !boundEffect.isBehind()) {
				for (ParticleEffectInstance particleEffectInstance : boundEffect.getParticleEffects()) {
					particleRenderer.render(particleEffectInstance);
				}
			}
		}
		clipper.clipEnd();
		if (vertexEffect != null) vertexEffect.end();
	}

	/** Renders the specified skeleton, including meshes and two color tinting.
	 * <p>
	 * This method may change the batch's {@link Batch#setBlendFunctionSeparate(int, int, int, int) blending function}. The
	 * previous blend function is not restored, since that could result in unnecessary flushes, depending on what is rendered
	 * next. */
	public void draw (SpriteBatchParticleRenderer particleRenderer, TwoColorPolygonBatch batch, SkeletonContainer skeletonContainer, Skeleton skeleton) {
		if (batch == null) throw new IllegalArgumentException("batch cannot be null.");
		if (skeleton == null) throw new IllegalArgumentException("skeleton cannot be null.");

		Vector2 tempPosition = this.temp, tempUV = this.temp2;
		Color tempLight1 = this.temp3, tempDark1 = this.temp4;
		Color tempLight2 = this.temp5, tempDark2 = this.temp6;
		com.esotericsoftware.spine.SkeletonRenderer.VertexEffect vertexEffect = this.vertexEffect;
		if (vertexEffect != null) vertexEffect.begin(skeleton);

		boolean pmaColors = this.pmaColors, pmaBlendModes = this.pmaBlendModes;
		batch.setPremultipliedAlpha(pmaColors);
		BlendMode blendMode = null;
		int verticesLength = 0;
		float[] vertices = null, uvs = null;
		short[] triangles = null;
		Color color = null, skeletonColor = skeleton.color;
		float r = skeletonColor.r, g = skeletonColor.g, b = skeletonColor.b, a = skeletonColor.a;
		Object[] drawOrder = skeleton.drawOrder.items;
		for (int i = 0, n = skeleton.drawOrder.size; i < n; i++) {
			Slot slot = (Slot)drawOrder[i];
			if (!slot.bone.active) {
				clipper.clipEnd(slot);
				continue;
			}
			Texture texture = null;
			int vertexSize = clipper.isClipping() ? 2 : 6;
			Attachment attachment = slot.attachment;
			if (attachment instanceof RegionAttachment) {
				RegionAttachment region = (RegionAttachment)attachment;
				verticesLength = vertexSize << 2;
				vertices = this.vertices.items;
				region.computeWorldVertices(slot.getBone(), vertices, 0, vertexSize);
				triangles = quadTriangles;
				texture = region.getRegion().getTexture();
				uvs = region.getUVs();
				color = region.getColor();

			} else if (attachment instanceof MeshAttachment) {
				MeshAttachment mesh = (MeshAttachment)attachment;
				int count = mesh.getWorldVerticesLength();
				verticesLength = (count >> 1) * vertexSize;
				vertices = this.vertices.setSize(verticesLength);
				mesh.computeWorldVertices(slot, 0, count, vertices, 0, vertexSize);
				triangles = mesh.getTriangles();
				texture = mesh.getRegion().getTexture();
				uvs = mesh.getUVs();
				color = mesh.getColor();

			} else if (attachment instanceof ClippingAttachment) {
				ClippingAttachment clip = (ClippingAttachment)attachment;
				clipper.clipStart(slot, clip);
				continue;

			} else if (attachment instanceof SkeletonAttachment) {
				Skeleton attachmentSkeleton = ((SkeletonAttachment)attachment).getSkeleton();
				if (attachmentSkeleton != null) draw(particleRenderer, batch, skeletonContainer, attachmentSkeleton);
			}

			if (texture != null) {
				Color lightColor = slot.getColor();
				float alpha = a * lightColor.a * color.a * 255;
				float multiplier = pmaColors ? alpha : 255;

				BlendMode slotBlendMode = slot.data.getBlendMode();
				if (slotBlendMode != blendMode) {
					if (slotBlendMode == BlendMode.additive && pmaColors) {
						slotBlendMode = BlendMode.normal;
						alpha = 0;
					}
					blendMode = slotBlendMode;
					blendMode.apply(batch, pmaBlendModes);
				}

				float red = r * color.r * multiplier;
				float green = g * color.g * multiplier;
				float blue = b * color.b * multiplier;
				float light = NumberUtils.intToFloatColor((int)alpha << 24 //
					| (int)(blue * lightColor.b) << 16 //
					| (int)(green * lightColor.g) << 8 //
					| (int)(red * lightColor.r));
				Color darkColor = slot.getDarkColor();
				float dark = darkColor == null ? 0
					: NumberUtils.intToFloatColor((int)(blue * darkColor.b) << 16 //
					| (int)(green * darkColor.g) << 8 //
					| (int)(red * darkColor.r));

				if (clipper.isClipping()) {
					clipper.clipTriangles(vertices, verticesLength, triangles, triangles.length, uvs, light, dark, true);
					FloatArray clippedVertices = clipper.getClippedVertices();
					ShortArray clippedTriangles = clipper.getClippedTriangles();
					if (vertexEffect != null) applyVertexEffect(clippedVertices.items, clippedVertices.size, 6, light, dark);
					batch.drawTwoColor(texture, clippedVertices.items, 0, clippedVertices.size, clippedTriangles.items, 0,
						clippedTriangles.size);
				} else {
					if (vertexEffect != null) {
						tempLight1.set(NumberUtils.floatToIntColor(light));
						tempDark1.set(NumberUtils.floatToIntColor(dark));
						for (int v = 0, u = 0; v < verticesLength; v += 6, u += 2) {
							tempPosition.x = vertices[v];
							tempPosition.y = vertices[v + 1];
							tempLight2.set(tempLight1);
							tempDark2.set(tempDark1);
							tempUV.x = uvs[u];
							tempUV.y = uvs[u + 1];
							vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2);
							vertices[v] = tempPosition.x;
							vertices[v + 1] = tempPosition.y;
							vertices[v + 2] = tempLight2.toFloatBits();
							vertices[v + 3] = tempDark2.toFloatBits();
							vertices[v + 4] = tempUV.x;
							vertices[v + 5] = tempUV.y;
						}
					} else {
						for (int v = 2, u = 0; v < verticesLength; v += 6, u += 2) {
							vertices[v] = light;
							vertices[v + 1] = dark;
							vertices[v + 2] = uvs[u];
							vertices[v + 3] = uvs[u + 1];
						}
					}
					batch.drawTwoColor(texture, vertices, 0, verticesLength, triangles, 0, triangles.length);
				}
			}

			clipper.clipEnd(slot);
		}
		clipper.clipEnd();
		if (vertexEffect != null) vertexEffect.end();
	}

	private void applyVertexEffect (float[] vertices, int verticesLength, int stride, float light, float dark) {
		Vector2 tempPosition = this.temp, tempUV = this.temp2;
		Color tempLight1 = this.temp3, tempDark1 = this.temp4;
		Color tempLight2 = this.temp5, tempDark2 = this.temp6;
		com.esotericsoftware.spine.SkeletonRenderer.VertexEffect vertexEffect = this.vertexEffect;
		tempLight1.set(NumberUtils.floatToIntColor(light));
		tempDark1.set(NumberUtils.floatToIntColor(dark));
		if (stride == 5) {
			for (int v = 0; v < verticesLength; v += stride) {
				tempPosition.x = vertices[v];
				tempPosition.y = vertices[v + 1];
				tempUV.x = vertices[v + 3];
				tempUV.y = vertices[v + 4];
				tempLight2.set(tempLight1);
				tempDark2.set(tempDark1);
				vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2);
				vertices[v] = tempPosition.x;
				vertices[v + 1] = tempPosition.y;
				vertices[v + 2] = tempLight2.toFloatBits();
				vertices[v + 3] = tempUV.x;
				vertices[v + 4] = tempUV.y;
			}
		} else {
			for (int v = 0; v < verticesLength; v += stride) {
				tempPosition.x = vertices[v];
				tempPosition.y = vertices[v + 1];
				tempUV.x = vertices[v + 4];
				tempUV.y = vertices[v + 5];
				tempLight2.set(tempLight1);
				tempDark2.set(tempDark1);
				vertexEffect.transform(tempPosition, tempUV, tempLight2, tempDark2);
				vertices[v] = tempPosition.x;
				vertices[v + 1] = tempPosition.y;
				vertices[v + 2] = tempLight2.toFloatBits();
				vertices[v + 3] = tempDark2.toFloatBits();
				vertices[v + 4] = tempUV.x;
				vertices[v + 5] = tempUV.y;
			}
		}
	}

	public boolean getPremultipliedAlphaColors () {
		return pmaColors;
	}

	/** If true, colors will be multiplied by their alpha before being sent to the GPU. Set to false if premultiplied alpha is not
	 * being used or if the shader does the multiplication (libgdx's default batch shaders do not). Default is false. */
	public void setPremultipliedAlphaColors (boolean pmaColors) {
		this.pmaColors = pmaColors;
	}

	public boolean getPremultipliedAlphaBlendModes () {
		return pmaBlendModes;
	}

	/** If true, blend modes for premultiplied alpha will be used. Set to false if premultiplied alpha is not being used. Default
	 * is false. */
	public void setPremultipliedAlphaBlendModes (boolean pmaBlendModes) {
		this.pmaBlendModes = pmaBlendModes;
	}

	/** Sets {@link #setPremultipliedAlphaColors(boolean)} and {@link #setPremultipliedAlphaBlendModes(boolean)}. */
	public void setPremultipliedAlpha (boolean pmaColorsAndBlendModes) {
		pmaColors = pmaColorsAndBlendModes;
		pmaBlendModes = pmaColorsAndBlendModes;
	}

	public @Null
	com.esotericsoftware.spine.SkeletonRenderer.VertexEffect getVertexEffect () {
		return vertexEffect;
	}

	public void setVertexEffect (@Null com.esotericsoftware.spine.SkeletonRenderer.VertexEffect vertexEffect) {
		this.vertexEffect = vertexEffect;
	}

	/** Modifies the skeleton or vertex positions, UVs, or colors during rendering. */
	static public interface VertexEffect {
		public void begin (Skeleton skeleton);

		public void transform (Vector2 position, Vector2 uv, Color color, Color darkColor);

		public void end ();
	}
}
