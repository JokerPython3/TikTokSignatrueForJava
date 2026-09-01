// ProtoBuf writer — exact port of SignerPy.protobuf / tiktok-signature/protobuf.go.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class ProtoBuf {

    private ProtoBuf() {}

    public static final int KIND_VARINT = 0;
    public static final int KIND_STRING = 2;

    public static final class Field {
        public final long idx;
        public final int kind;
        public final long varint;
        public final byte[] data;

        public Field(long idx, int kind, long varint, byte[] data) {
            this.idx = idx;
            this.kind = kind;
            this.varint = varint;
            this.data = data;
        }

        public static Field varint(long idx, long value) {
            return new Field(idx, KIND_VARINT, value, null);
        }

        public static Field string(long idx, byte[] data) {
            return new Field(idx, KIND_STRING, 0, data);
        }
    }

    public static byte[] serialize(List<Field> fields) {
        ByteArrayOutputStream w = new ByteArrayOutputStream();
        for (Field f : fields) {
            long key = (f.idx << 3) | ((long) f.kind & 7);
            writeVarint(w, key);
            switch (f.kind) {
                case KIND_VARINT:
                    writeVarint(w, f.varint);
                    break;
                case KIND_STRING:
                    writeVarint(w, f.data.length);
                    w.write(f.data, 0, f.data.length);
                    break;
            }
        }
        return w.toByteArray();
    }

    // Replicates ProtoWriter.writeVarint, including the 32-bit mask and the
    // "> 0x80" loop condition of the Python reference.
    static void writeVarint(ByteArrayOutputStream w, long vint) {
        vint &= 0xFFFFFFFFL;
        while (vint > 0x80) {
            w.write((byte) ((vint & 0x7F) | 0x80) & 0xFF);
            vint >>>= 7;
        }
        w.write((byte) (vint & 0x7F) & 0xFF);
    }
}
