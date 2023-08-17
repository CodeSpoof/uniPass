package io.codespoof.univpassm;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PasswordGenerator {
    final String chars = "abcdefghijklmnopqrstuvwxyz";
    final String signs;
    final String numbers = "0123456789";
    final String urlCompatibles = ".-_+";
    final String otherSigns = ",:#*=!";
    SecureRandom secureRandomGenerator;
    final int length;

    public PasswordGenerator(boolean urlCompatible, int length) {
        try {
            secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.length = length;
        this.signs = this.urlCompatibles + (urlCompatible ? "" : this.otherSigns);
    }

    public String generatePassword() {
        StringBuilder pool = new StringBuilder();
        int upper = 0;
        for (int i = 0; i < (length - 3); i++) {
            String c = chars.substring(secureRandomGenerator.nextInt(chars.length())).substring(0, 1);
            if (upper < (length - 4) && secureRandomGenerator.nextBoolean()) {
                c = c.toUpperCase();
                upper++;
            }
            pool.append(c);
        }
        pool.append(signs.charAt(secureRandomGenerator.nextInt(signs.length())));
        pool.append(numbers.charAt(secureRandomGenerator.nextInt(numbers.length())));
        pool.append(numbers.charAt(secureRandomGenerator.nextInt(numbers.length())));

        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int ind = secureRandomGenerator.nextInt(pool.length());
            ret.append(pool.charAt(ind));
            pool.deleteCharAt(ind);
        }
        return ret.toString();
    }
}
