package org.jid.poc.poc2fa.wstrange;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey.Builder;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.HmacHashFunction;
import com.warrenstrange.googleauth.KeyRepresentation;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class AppV2 {

    public static void main(String[] args) throws IOException, WriterException {
        new AppV2().run();
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
        String qr = getQRCodeImage(otpUrl, 500, 500);
        System.out.println("qr = " + qr);

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

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(otpauth, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream imgOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", imgOutputStream);
        byte[] imgBytes = imgOutputStream.toByteArray();

        String imgStr = Base64.getEncoder()
            .encodeToString(imgBytes);

        return "data:image/png;base64," + imgStr;
    }

}

