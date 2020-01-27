package sys;

import java.security.SecureRandom;

import static java.security.SecureRandom.getInstance;

public class JavaPrng implements Prng {
    @Override
    public byte generate() {
        byte[] num = new byte[1];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(num);
        return num[0];
    }
}
