// Signatures holds the six signed header values.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

public final class Signatures {

    public final String xArgus;
    public final String xLadon;
    public final String xGorgon;
    public final String xKhronos;
    public final String xTTRequestTicket;
    public final String xSSStub;

    public Signatures(String xArgus, String xLadon, String xGorgon,
                      String xKhronos, String xTTRequestTicket, String xSSStub) {
        this.xArgus = xArgus;
        this.xLadon = xLadon;
        this.xGorgon = xGorgon;
        this.xKhronos = xKhronos;
        this.xTTRequestTicket = xTTRequestTicket;
        this.xSSStub = xSSStub;
    }
}
