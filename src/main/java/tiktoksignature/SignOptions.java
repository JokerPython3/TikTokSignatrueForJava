// SignOptions carry optional signing parameters plus deterministic randomness
// overrides (used by tests). All fields except timestamp have defaults.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

public final class SignOptions {

    public final Long timestamp;
    public final int aid;
    public final int licenseID;
    public final int platform;
    public final String sdkVersion;
    public final int sdkVersionInt;
    public final int version;
    public final String secDeviceID;
    public final Byte gorgonByte3;
    public final Byte gorgonByte7;
    public final Integer argusRand;
    public final byte[] ladonRandom;

    private SignOptions(Long timestamp, int aid, int licenseID, int platform,
                        String sdkVersion, int sdkVersionInt, int version,
                        String secDeviceID, Byte gorgonByte3, Byte gorgonByte7,
                        Integer argusRand, byte[] ladonRandom) {
        this.timestamp = timestamp;
        this.aid = aid;
        this.licenseID = licenseID;
        this.platform = platform;
        this.sdkVersion = sdkVersion;
        this.sdkVersionInt = sdkVersionInt;
        this.version = version;
        this.secDeviceID = secDeviceID;
        this.gorgonByte3 = gorgonByte3;
        this.gorgonByte7 = gorgonByte7;
        this.argusRand = argusRand;
        this.ladonRandom = ladonRandom;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long timestamp;
        private int aid;
        private int licenseID;
        private int platform;
        private String sdkVersion;
        private int sdkVersionInt;
        private int version;
        private String secDeviceID;
        private Byte gorgonByte3;
        private Byte gorgonByte7;
        private Integer argusRand;
        private byte[] ladonRandom;

        public Builder timestamp(Long t) { this.timestamp = t; return this; }
        public Builder aid(int v) { this.aid = v; return this; }
        public Builder licenseID(int v) { this.licenseID = v; return this; }
        public Builder platform(int v) { this.platform = v; return this; }
        public Builder sdkVersion(String v) { this.sdkVersion = v; return this; }
        public Builder sdkVersionInt(int v) { this.sdkVersionInt = v; return this; }
        public Builder version(int v) { this.version = v; return this; }
        public Builder secDeviceID(String v) { this.secDeviceID = v; return this; }
        public Builder gorgonByte3(Byte v) { this.gorgonByte3 = v; return this; }
        public Builder gorgonByte7(Byte v) { this.gorgonByte7 = v; return this; }
        public Builder argusRand(Integer v) { this.argusRand = v; return this; }
        public Builder ladonRandom(byte[] v) { this.ladonRandom = v; return this; }

        public SignOptions build() {
            return new SignOptions(timestamp, aid, licenseID, platform,
                    sdkVersion, sdkVersionInt, version, secDeviceID,
                    gorgonByte3, gorgonByte7, argusRand, ladonRandom);
        }
    }
}
