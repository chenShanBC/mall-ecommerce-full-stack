package com.mallfei.user.domain.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LoginCaptchaRenderer {

    private static final int IMAGE_WIDTH = 320;
    private static final int IMAGE_HEIGHT = 180;
    private static final int PUZZLE_SIZE = 52;
    private static final int KNOB_SIZE = 16;
    private static final int MIN_X = 130;
    private static final int MAX_X = 230;
    private static final int MIN_Y = 42;
    private static final int MAX_Y = 92;
    private static final float JPEG_QUALITY = 0.58f;
    private static final String CAPTCHA_IMAGE_PATH = "static/images/captcha-scene.jpg";

    public LoginCaptchaRenderResult render() {
        try {
            BufferedImage scaled = scaleSourceImage();

            int x = ThreadLocalRandom.current().nextInt(MIN_X, MAX_X + 1);
            int y = ThreadLocalRandom.current().nextInt(MIN_Y, MAX_Y + 1);

            Area backgroundShape = createPuzzleShape(x, y, PUZZLE_SIZE);
            Area sliderShape = createPuzzleShape(0, 0, PUZZLE_SIZE);
            BufferedImage background = deepCopy(scaled);
            BufferedImage slider = new BufferedImage(PUZZLE_SIZE, PUZZLE_SIZE, BufferedImage.TYPE_INT_ARGB);

            paintBackgroundHole(background, backgroundShape);
            paintSliderPiece(scaled, slider, x, y, sliderShape);

            return new LoginCaptchaRenderResult(
                    toJpegBase64(background),
                    toPngBase64(slider),
                    x,
                    y,
                    PUZZLE_SIZE
            );
        } catch (IOException exception) {
            throw new IllegalStateException("生成拼图验证码失败", exception);
        }
    }

    private void paintBackgroundHole(BufferedImage background, Shape shape) {
        Graphics2D graphics = background.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setColor(new Color(15, 23, 42, 86));
        graphics.fill(shape);
        graphics.setColor(new Color(255, 255, 255, 74));
        graphics.setStroke(new BasicStroke(1.6f));
        graphics.draw(shape);
        graphics.setColor(new Color(15, 23, 42, 26));
        graphics.translate(1.0, 1.0);
        graphics.draw(shape);
        graphics.dispose();
    }

    private void paintSliderPiece(BufferedImage scaled, BufferedImage slider, int x, int y, Shape shape) {
        Graphics2D graphics = slider.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, PUZZLE_SIZE, PUZZLE_SIZE);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setClip(shape);
        graphics.drawImage(scaled.getSubimage(x, y, PUZZLE_SIZE, PUZZLE_SIZE), 0, 0, null);
        graphics.setClip(null);
        graphics.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 78), 0, PUZZLE_SIZE, new Color(255, 255, 255, 12)));
        graphics.fill(shape);
        graphics.setColor(new Color(255, 255, 255, 88));
        graphics.setStroke(new BasicStroke(1.1f));
        graphics.draw(shape);
        graphics.setColor(new Color(15, 23, 42, 22));
        graphics.translate(0.9, 0.9);
        graphics.draw(shape);
        graphics.dispose();
    }

    private static BufferedImage scaleSourceImage() {
        try {
            ClassPathResource resource = new ClassPathResource(CAPTCHA_IMAGE_PATH);
            if (!resource.exists()) {
                throw new IllegalStateException("验证码底图资源不存在: " + CAPTCHA_IMAGE_PATH);
            }
            BufferedImage source = ImageIO.read(resource.getInputStream());
            if (source == null) {
                throw new IllegalStateException("验证码底图读取失败: " + CAPTCHA_IMAGE_PATH);
            }
            BufferedImage scaled = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            graphics.drawImage(source, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
            graphics.dispose();
            return scaled;
        } catch (IOException exception) {
            throw new IllegalStateException("加载拼图验证码底图失败", exception);
        }
    }

    private Area createPuzzleShape(int x, int y, int size) {
        double knobRadius = KNOB_SIZE / 2.0;
        Area area = new Area(new Rectangle2D.Double(x, y, size, size));
        area.add(new Area(new Ellipse2D.Double(x + size - knobRadius, y + 18, KNOB_SIZE, KNOB_SIZE)));
        area.add(new Area(new Ellipse2D.Double(x + 18, y - knobRadius, KNOB_SIZE, KNOB_SIZE)));
        area.subtract(new Area(new Ellipse2D.Double(x - knobRadius, y + 18, KNOB_SIZE, KNOB_SIZE)));
        area.subtract(new Area(new Ellipse2D.Double(x + 18, y + size - knobRadius, KNOB_SIZE, KNOB_SIZE)));
        return area;
    }

    private BufferedImage deepCopy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private String toJpegBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("JPEG 编码器不可用");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private String toPngBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}
