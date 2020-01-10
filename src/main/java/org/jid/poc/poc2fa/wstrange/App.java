package org.jid.poc.poc2fa.wstrange;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey.Builder;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.HmacHashFunction;
import com.warrenstrange.googleauth.KeyRepresentation;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args) {

        GoogleAuthenticator authenticator = new GoogleAuthenticator(getAuthConfig());
        // Disabled for testing purposes. It can be used to persist the user-secret relationship
        //authenticator.setCredentialRepository(null);
        //authenticator.setCredentialRepository(new MapCredentialRepository());

        // Create credentials
        //GoogleAuthenticatorKey credentials = authenticator.createCredentials("user@test.org");
        GoogleAuthenticatorKey credentials = authenticator.createCredentials();
        System.out.println("credentials - key = " + credentials.getKey());
        System.out.println("credentials - verificationCode = " + credentials.getVerificationCode());

        // WARNING: It doesn't work with Google Authenticator because it generates '=' chars
        String sanitizedSecret = credentials.getKey().replace("=", "");
        GoogleAuthenticatorKey sanitizedCredentials = new Builder(sanitizedSecret)
            .setConfig(getAuthConfig())
            .build();

        // Generate QR and otpAuthUrl
        String qrUrl = GoogleAuthenticatorQRGenerator
            .getOtpAuthURL("TestOrg", "user@test.org", sanitizedCredentials);
        System.out.println("qrUrl = " + qrUrl);

        // Alternative method to generate QR
        String otpUrl = GoogleAuthenticatorQRGenerator
            .getOtpAuthTotpURL("TestOrg", "user@test.org", sanitizedCredentials);
        System.out.println("otpUrl = " + otpUrl);

        try(Scanner scanner = new Scanner(System.in)) {
            System.out.println("Insert code: ");
            while(scanner.hasNextLine()) {
                int verificationCode = scanner.nextInt();
                boolean isAuthorized = authenticator.authorize(sanitizedCredentials.getKey(), verificationCode);
                System.out.println("isAuthorized = " + isAuthorized);
            }
        }


    }


    private static GoogleAuthenticatorConfig getAuthConfig() {
        return new GoogleAuthenticatorConfigBuilder()
            .setCodeDigits(6)
            .setWindowSize(3)
            .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30L))
            .setKeyRepresentation(KeyRepresentation.BASE32)
            .setHmacHashFunction(HmacHashFunction.HmacSHA256)
            .setSecretBits(256) // >= 128 bits
            .setNumberOfScratchCodes(5)
            .build();
    }

}
