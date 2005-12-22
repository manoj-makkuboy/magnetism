package com.dumbhippo.web;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HumanVisibleException;

public abstract class AbstractPhotoServlet extends AbstractSmallImageServlet {
	private static final int[] PHOTO_DIMENSIONS = { Configuration.SHOT_LARGE_SIZE, Configuration.SHOT_SMALL_SIZE };
	protected static final int LARGEST_PHOTO_DIMENSION = Configuration.SHOT_LARGE_SIZE;
	
	private static final Log logger = GlobalSetup.getLog(AbstractPhotoServlet.class);

	@Override
	protected String getDefaultFilename() {
		return "default";
	}	
	
	private BufferedImage scaleTo(BufferedImage image, int size) {
		/* FIXME it would be nicer to avoid round-tripping a jpeg through a
		 * BufferedImage
		 */

		/* To avoid questions of aspect ratio on the client side, we always 
		 * generate a square image, adding transparent padding if required.
		 */
		
		double scaleX, scaleY;
		double translateX, translateY;
		double origWidth = image.getWidth();
		double origHeight = image.getHeight();
		if (origHeight > origWidth) {
			scaleY = size / origHeight;
			scaleX = scaleY;
			translateY = 0;
			translateX = (size - origWidth * scaleX) * 0.5;
		} else {
			scaleX = size / origWidth;
			scaleY = scaleX;
			translateX = 0;
			translateY = (size - origHeight * scaleY) * 0.5;
		}

		logger.debug("Scaling photo to " + size + " scaleX = " + scaleX + " scaleY = " + scaleY + " new width = "
				+ image.getWidth() * scaleX + " new height = " + image.getHeight() * scaleY
				+ " translation = +" + translateX + "+" + translateY);

		/* Our transformation */
		AffineTransform tx = new AffineTransform();
		tx.translate(translateX, translateY);
		tx.scale(scaleX, scaleY);
		AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
		
		/* All-transparent dest image */
		BufferedImage dest = new BufferedImage(image.getColorModel(),
				image.getRaster().createCompatibleWritableRaster(size, size),
				false, null);
		
		//logger.debug("src cm = " + image.getColorModel() + " dest cm = " + dest.getColorModel());
		
		// filled with transparent seems to be the default anyway
		//Graphics2D graphics = dest.createGraphics();
		//graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
		//graphics.fill(new Rectangle(0, 0, size, size));
		
		/* Draw the transformed image */
		return op.filter(image, dest);	
	}
	
	protected Collection<BufferedImage> readScaledPhotos(FileItem photo) throws IOException, HumanVisibleException {
		BufferedImage image = readPhoto(photo);
		
		Collection<BufferedImage> images = new ArrayList<BufferedImage>(PHOTO_DIMENSIONS.length);
		
		for (int size : PHOTO_DIMENSIONS) {
			images.add(scaleTo(image, size));
		}
		
		return images;
	}

	protected void doFinalRedirect(HttpServletRequest request, HttpServletResponse response, String filename, int version, String title, String url) throws ServletException, IOException, HumanVisibleException {
		request.setAttribute("photoFilename", filename);		
		request.setAttribute("photoVersion", version);
		XmlBuilder link = new XmlBuilder();
		link.appendTextNode("a", title, "href", url);
		request.setAttribute("homePageLink", link.toString());
		request.getRequestDispatcher("/newphoto").forward(request, response);			
	}
}
