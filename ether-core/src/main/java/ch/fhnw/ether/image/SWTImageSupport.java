package ch.fhnw.ether.image;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;

public class SWTImageSupport extends STBImageSupport {
	private static final PaletteData RGB8  = new PaletteData(0xFF0000, 0xFF00, 0xFF);
	private static final PaletteData RGBA8 = new PaletteData(0xFF000000, 0xFF0000, 0xFF00);

	private ThreadLocal<ImageLoader> writer = new ThreadLocal<ImageLoader>() {
		@Override
		protected ImageLoader initialValue() {
			return new ImageLoader();
		}
	};

	@Override
	public void write(IImage frame, OutputStream out, FileFormat format) throws IOException {
		writer.get().data = new ImageData[] {toImageData(frame)};
		writer.get().save(out, swtFormat(format));
		out.close();
	}

	private ImageData toImageData(IImage frame) throws IOException {
		if(frame instanceof ByteImage) {
			ByteImage  image  = (ByteImage)frame;
			ByteBuffer pixels = image.getPixels();

			pixels.clear();
			byte[] data = new byte[pixels.capacity()];
			pixels.clear();
			int pixelSize = image.getNumBytesPerPixel();
			int linelen = image.getWidth() * pixelSize;
			for(int j = image.getHeight(); --j >= 0;)
				pixels.get(data, j * linelen, linelen);
			ImageData result = new ImageData(image.getWidth(), image.getHeight(), pixelSize * 8, getPaletteData(image), 1, data);
			if(image.getComponentFormat().hasAlpha()) {
				byte[]    alpha  = new byte[image.getWidth() * image.getHeight()];
				int idx = 3;
				for(int j = image.getHeight(); --j >= 0;) {
					for(int i = 0; i < image.getWidth(); i++, idx += pixelSize)
						alpha[j * image.getWidth() + i] = pixels.get(idx);
				}
				result.alphaData = alpha;
			}
			return result;

		} else throw new IOException("Unsupported image format: " + frame.getClass().getName());
	}

	private PaletteData getPaletteData(ByteImage image) throws IOException {
		switch(image.getComponentFormat()) {
		case RGB:  return RGB8;
		case RGBA: return RGBA8;
		default:   throw new IOException("Unsupported component format: " + image.getComponentFormat());
		}
	}

	private int swtFormat(FileFormat format) {
		switch (format) {
		case JPEG:	return SWT.IMAGE_JPEG;
		case PNG:	return SWT.IMAGE_PNG;
		default:    return SWT.IMAGE_BMP;
		}
	}
}
