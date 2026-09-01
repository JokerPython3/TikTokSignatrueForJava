// SignRequest mirrors the arguments of SignerPy.sign().
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import java.util.List;

public final class SignRequest {

    public final String url;
    public final List<Param> params;
    public final List<Param> data;
    public final List<Param> payload;
    public final List<Param> cookie;
    public final String rawBody;

    public SignRequest(String url, List<Param> params, List<Param> data,
                       List<Param> payload, List<Param> cookie, String rawBody) {
        this.url = url;
        this.params = params;
        this.data = data;
        this.payload = payload;
        this.cookie = cookie;
        this.rawBody = rawBody;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String url;
        private List<Param> params;
        private List<Param> data;
        private List<Param> payload;
        private List<Param> cookie;
        private String rawBody;

        public Builder url(String url) { this.url = url; return this; }
        public Builder params(List<Param> params) { this.params = params; return this; }
        public Builder data(List<Param> data) { this.data = data; return this; }
        public Builder payload(List<Param> payload) { this.payload = payload; return this; }
        public Builder cookie(List<Param> cookie) { this.cookie = cookie; return this; }
        public Builder rawBody(String rawBody) { this.rawBody = rawBody; return this; }

        public SignRequest build() {
            return new SignRequest(url, params, data, payload, cookie, rawBody);
        }
    }
}
