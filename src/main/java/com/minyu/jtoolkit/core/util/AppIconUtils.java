package com.minyu.jtoolkit.core.util;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.GraphicsEnvironment;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
public final class AppIconUtils {
    private static final String LOGO_16 = "/images/logo_16.png";
    private static final String LOGO_24 = "/images/logo_24.png";
    private static final String LOGO_32 = "/images/logo_32.png";
    private static final String LOGO_64 = "/images/logo_64.png";
    private static final String LOGO_128 = "/images/logo_128.png";
    private static final String LOGO_256 = "/images/logo_256.png";
    private static final String LOGO_512 = "/images/logo_512.png";
    private static final int DOCK_ICON_PADDING = 16;

    private AppIconUtils() {
    }

    public static void configureApplicationIcons(Stage stage) {
        stage.getIcons().setAll(loadStageIcons());
        configureDockIcon();
    }

    public static List<Image> loadStageIcons() {
        return List.of(
                loadJavaFxImage(LOGO_16),
                loadJavaFxImage(LOGO_24),
                loadJavaFxImage(LOGO_32),
                loadJavaFxImage(LOGO_64),
                loadJavaFxImage(LOGO_128),
                loadJavaFxImage(LOGO_256),
                loadJavaFxImage(LOGO_512)
        );
    }

    public static BufferedImage loadDockIconImage() {
        BufferedImage source = loadBufferedIcon(LOGO_128);
        return addDockPadding(source);
    }

    private static BufferedImage loadBufferedIcon(String resourcePath) {
        try (InputStream stream = AppIconUtils.class.getResourceAsStream(resourcePath)) {
            return ImageIO.read(Objects.requireNonNull(stream, "Missing icon resource: " + resourcePath));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load dock icon resource: " + resourcePath, e);
        }
    }

    private static BufferedImage addDockPadding(BufferedImage source) {
        int targetWidth = Math.max(1, source.getWidth() - DOCK_ICON_PADDING * 2);
        int targetHeight = Math.max(1, source.getHeight() - DOCK_ICON_PADDING * 2);
        BufferedImage padded = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < targetHeight; y++) {
            int sourceY = y * source.getHeight() / targetHeight;
            for (int x = 0; x < targetWidth; x++) {
                int sourceX = x * source.getWidth() / targetWidth;
                padded.setRGB(DOCK_ICON_PADDING + x, DOCK_ICON_PADDING + y, source.getRGB(sourceX, sourceY));
            }
        }
        return padded;
    }

    private static void configureDockIcon() {
        if (!isMacOs() || GraphicsEnvironment.isHeadless() || !Taskbar.isTaskbarSupported()) {
            return;
        }

        try {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(loadDockIconImage());
            }
        } catch (UnsupportedOperationException | SecurityException | IllegalStateException e) {
            log.debug("Unable to configure macOS Dock icon: {}", e.getMessage());
        }
    }

    private static Image loadJavaFxImage(String resourcePath) {
        InputStream stream = AppIconUtils.class.getResourceAsStream(resourcePath);
        return new Image(Objects.requireNonNull(stream, "Missing icon resource: " + resourcePath));
    }

    private static boolean isMacOs() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }
}
