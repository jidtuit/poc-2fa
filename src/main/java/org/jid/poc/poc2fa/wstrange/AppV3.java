package org.jid.poc.poc2fa.wstrange;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey.Builder;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.HmacHashFunction;
import com.warrenstrange.googleauth.KeyRepresentation;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.im.InputContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class AppV3 {

    private static final String LOGO_FILE = "ClarityLogoIsotype24.jpg";

    public static void main(String[] args) throws IOException, WriterException {
        new AppV3().run();
    }

    public void run() throws IOException, WriterException {
        GoogleAuthenticator authenticator = new GoogleAuthenticator(getAuthConfig());

        // Create credentials
        GoogleAuthenticatorKey credentials = authenticator.createCredentials();
        System.out.println("credentials - key = " + credentials.getKey());

        // WARNING: It doesn't work with Google Authenticator because it generates '=' chars
        GoogleAuthenticatorKey sanitizedCredentials = sanitizeCredentials(credentials);
        System.out.println("sanitizedCredentials.getKey() = " + sanitizedCredentials.getKey());

        // Generate otpAuthUrl
        String otpUrl = GoogleAuthenticatorQRGenerator
            .getOtpAuthTotpURL("Test Org", "user@test.org", sanitizedCredentials);
        System.out.println("otpUrl = " + otpUrl);

        // Generate QR
        String qr = getQRCodeImage(otpUrl, 200, 200);
        System.out.println("qr = " + qr);
        System.out.println("qr.length() = " + qr.length());

        try(Scanner scanner = new Scanner(System.in)) {
            System.out.println("Insert code: ");
            while(scanner.hasNextLine()) {
                int verificationCode = scanner.nextInt();
                boolean isAuthorized = authenticator.authorize(sanitizedCredentials.getKey(), verificationCode);
                System.out.println("isAuthorized = " + isAuthorized);
            }
        }


    }

    private GoogleAuthenticatorConfig getAuthConfig() {
        return new GoogleAuthenticatorConfigBuilder()
            .setCodeDigits(6)
            .setWindowSize(3)
            .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30L))
            .setKeyRepresentation(KeyRepresentation.BASE32)
            .setHmacHashFunction(HmacHashFunction.HmacSHA256)
            .setSecretBits(160) // >= 128 bits --- If 160 bits (20 Bytes) then no need to sanitize
            .setNumberOfScratchCodes(5)
            .build();
    }

    private GoogleAuthenticatorKey sanitizeCredentials(GoogleAuthenticatorKey credentials) {
        // WARNING: It doesn't work with Google Authenticator because it generates '=' chars
        String sanitizedSecret = credentials.getKey().replace("=", "");

        return new Builder(sanitizedSecret)
            .setConfig(getAuthConfig())
            .build();
    }

    private String getQRCodeImage(String otpauth, int width, int height)
        throws WriterException, IOException {

        // Create new configuration that specifies the error correction. Mandatory if logo.
        Map<EncodeHintType, ErrorCorrectionLevel> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

        // Create QR without logo
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(otpauth, BarcodeFormat.QR_CODE, width, height, hints);

        // Load QR image
        MatrixToImageConfig matrixToImageConfig = new MatrixToImageConfig(0xFF000000, 0xFFA8E6CA);
        BufferedImage qrBImage = MatrixToImageWriter.toBufferedImage(bitMatrix, matrixToImageConfig);

        // Load logo
        InputStream logoFile = getClass().getClassLoader().getResourceAsStream(LOGO_FILE);
        BufferedImage logoBImage = ImageIO.read(logoFile);

        // Calculate the delta height and width between QR code and logo
        int deltaHeight = qrBImage.getHeight() - logoBImage.getHeight();
        int deltaWidth = qrBImage.getWidth() - logoBImage.getWidth();

        // Initialize combined image
        BufferedImage combined = new BufferedImage(qrBImage.getHeight(), qrBImage.getWidth(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) combined.getGraphics();

        // Write QR code to new image at position 0/0
        g.drawImage(qrBImage, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Write logo into combine image at position (deltaWidth / 2) and
        // (deltaHeight / 2). Background: Left/Right and Top/Bottom must be
        // the same space for the logo to be centered
        g.drawImage(logoBImage, (int) Math.round(deltaWidth / 2), (int) Math.round(deltaHeight / 2), null);

        // Write combined image as PNG to OutputStream
        ByteArrayOutputStream imgOutputStream = new ByteArrayOutputStream();
        ImageIO.write(combined, "PNG", imgOutputStream);

        byte[] imgBytes = imgOutputStream.toByteArray();

        String imgStr = Base64.getEncoder()
            .encodeToString(imgBytes);

        return "data:image/png;base64," + imgStr;
    }

}

