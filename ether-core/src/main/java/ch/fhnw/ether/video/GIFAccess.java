package ch.fhnw.ether.video;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;

import ch.fhnw.ether.image.IGPUImage;
import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;

public class GIFAccess extends FrameAccess {
	private List<IHostImage> frames = new ArrayList<>();
	private int              currFrame;
	private double           duration = 0;
	private final byte[]     rgb      = new byte[4];
	private int              width;
	private int              height;
	
	public GIFAccess(URLVideoSource src, int numPlays) throws IOException, URISyntaxException {
		super(src, numPlays);
		ImageLoader loader = new ImageLoader();
		ImageData[] images = loader.load(src.getURL().openStream());
		for(int i = 0; i < images.length; i++) {
			width  = Math.max(width,  images[i].x + images[i].width); 
			height = Math.max(height, images[i].y + images[i].height); 
		}
		for(int i = 0; i < images.length; i++)
			frames.add(convert(i == 0 ? null : frames.get(i-1), images[i]));
	}

	private IHostImage convert(IHostImage previous, ImageData imageData) {
		duration += imageData.delayTime / 1000.0;
		IHostImage result = previous == null ? IHostImage.create(width, height, ComponentType.BYTE, ComponentFormat.RGB) : previous.copy();
		for(int y = imageData.height; --y >= 0;)
			for(int x = imageData.width; --x >= 0;) {
				int pixel = imageData.getPixel(x, y);
				if(pixel == imageData.transparentPixel) continue;
				RGB srcrgb = imageData.palette.getRGB(pixel);
				rgb[0] = (byte) srcrgb.red;
				rgb[1] = (byte) srcrgb.green;
				rgb[2] = (byte) srcrgb.blue;
				result.setPixel(x+imageData.x, height-(y+imageData.y+1), rgb);
			}
		return result;
	}

	@Override
	public void dispose() {
		frames.clear();
	}

	@Override
	public double getDuration() {
		return duration;
	}

	@Override
	public float getFrameRate() {
		return (float) (frames.size() / getDuration());
	}

	@Override
	public long getFrameCount() {
		return frames.size();
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void rewind() {
		numPlays--;
		currFrame = 0;
	}

	@Override
	public boolean decodeFrame() {
		currFrame++;
		if(currFrame >= frames.size())
			rewind();
		return numPlays > 0;
	}

	@Override
	public double getPlayOutTimeInSec() {
		return currFrame / getFrameRate();
	}

	@Override
	public boolean isKeyframe() {
		return true;
	}

	@Override
	public IHostImage getHostImage(BlockingQueue<float[]> audioData) {
		return frames.get(currFrame % frames.size());
	}

	@Override
	public IGPUImage getGPUImage(BlockingQueue<float[]> audioData) {
		return getHostImage(audioData).createGPUImage();
	}
}
