package nxt.user;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

final class GenerateAuthorizationToken extends UserRequestHandler {

    static final GenerateAuthorizationToken instance = new GenerateAuthorizationToken();

    private GenerateAuthorizationToken() {}

    @Override
    public JSONObject processRequest(HttpServletRequest req, User user) throws IOException {
        String secretPhrase = req.getParameter("secretPhrase");
        if (! user.secretPhrase.equals(secretPhrase)) {
            JSONObject response = new JSONObject();
            response.put("response", "showMessage");
            response.put("message", "Invalid secret phrase!");
            return response;
        }
        byte[] website = req.getParameter("website").trim().getBytes("UTF-8");
        byte[] data = new byte[website.length + 32 + 4];
        System.arraycopy(website, 0, data, 0, website.length);
        System.arraycopy(user.publicKey, 0, data, website.length, 32);
        int timestamp = Convert.getEpochTime();
        data[website.length + 32] = (byte)timestamp;
        data[website.length + 32 + 1] = (byte)(timestamp >> 8);
        data[website.length + 32 + 2] = (byte)(timestamp >> 16);
        data[website.length + 32 + 3] = (byte)(timestamp >> 24);

        byte[] token = new byte[100];
        System.arraycopy(data, website.length, token, 0, 32 + 4);
        System.arraycopy(Crypto.sign(data, user.secretPhrase), 0, token, 32 + 4, 64);
        String tokenString = "";
        for (int ptr = 0; ptr < 100; ptr += 5) {

            long number = ((long)(token[ptr] & 0xFF)) | (((long)(token[ptr + 1] & 0xFF)) << 8) | (((long)(token[ptr + 2] & 0xFF)) << 16)
                    | (((long)(token[ptr + 3] & 0xFF)) << 24) | (((long)(token[ptr + 4] & 0xFF)) << 32);
            if (number < 32) {

                tokenString += "0000000";

            } else if (number < 1024) {

                tokenString += "000000";

            } else if (number < 32768) {

                tokenString += "00000";

            } else if (number < 1048576) {

                tokenString += "0000";

            } else if (number < 33554432) {

                tokenString += "000";

            } else if (number < 1073741824) {

                tokenString += "00";

            } else if (number < 34359738368L) {

                tokenString += "0";

            }
            tokenString += Long.toString(number, 32);

        }

        JSONObject response = new JSONObject();
        response.put("response", "showAuthorizationToken");
        response.put("token", tokenString);

        return response;
    }
}
