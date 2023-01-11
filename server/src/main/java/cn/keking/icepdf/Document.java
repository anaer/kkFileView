package cn.keking.icepdf;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import org.icepdf.core.pobjects.ImageUtility;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.pobjects.Page;

public class Document extends org.icepdf.core.pobjects.Document{

    /**
     * Gets an Image of the specified page.  The image size is automatically
     * calculated given the page boundary, user rotation and zoom.  The rendering
     * quality is defined by GraphicsRenderingHints.SCREEN.
     *
     * @param pageNumber     Page number of the page to capture the image rendering.
     *                       The page number is zero-based.
     * @param renderHintType Constant specified by the GraphicsRenderingHints class.
     *                       There are two possible entries, SCREEN and PRINT each with configurable
     *                       rendering hints settings.
     * @param pageBoundary   Constant specifying the page boundary to use when
     *                       painting the page content. Typically use Page.BOUNDARY_CROPBOX.
     * @param userRotation   Rotation factor, in degrees, to be applied to the rendered page.
     *                       Arbitrary rotations are not currently supported for this method,
     *                       so only the following values are valid: 0.0f, 90.0f, 180.0f, 270.0f.
     * @param userZoom       Zoom factor to be applied to the rendered page.
     * @return an Image object of the current page.
     */
    public Image getPageImage(int pageNumber,
                              final int renderHintType, final int pageBoundary,
                              float userRotation, float userZoom) throws InterruptedException {
        Page page = getCatalog().getPageTree().getPage(pageNumber);
        page.init();
        PDimension sz = page.getSize(pageBoundary, userRotation, userZoom);

        int pageWidth = Math.max((int) sz.getWidth(), 1);
        int pageHeight = (int) sz.getHeight();

        BufferedImage image = ImageUtility.createCompatibleImage(pageWidth, pageHeight);
        Graphics g = image.createGraphics();

        page.paint(g, renderHintType,
                pageBoundary, userRotation, userZoom);
        g.dispose();

        return image;
    }


}
