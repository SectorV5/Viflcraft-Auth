/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.totp;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import win.sectorfive.viflcraftauth.api.database.User;
import win.sectorfive.viflcraftauth.api.totp.TOTPData;
import win.sectorfive.viflcraftauth.api.totp.TOTPProvider;
import win.sectorfive.viflcraftauth.common.AuthenticLibreLogin;
import win.sectorfive.viflcraftauth.common.config.ConfigurationKeys;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class AuthenticTOTPProvider implements TOTPProvider {

    private final AuthenticLibreLogin<?, ?> plugin;
    private final SecretGenerator secretGenerator;
    private final ZxingPngQrGenerator qrGenerator;
    private final CodeVerifier verifier;

    public AuthenticTOTPProvider(AuthenticLibreLogin<?, ?> plugin) {
        this.plugin = plugin;
        secretGenerator = new DefaultSecretGenerator();
        qrGenerator = new ZxingPngQrGenerator();
        qrGenerator.setImageSize(256);

        verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
    }

    @Override
    public TOTPData generate(User user) {
        return generate(user, secretGenerator.generate());
    }

    @Override
    public TOTPData generate(User user, String secret) {
        var data = new QrData.Builder()
                .label(user.getLastNickname())
                .issuer(plugin.getConfiguration().get(ConfigurationKeys.TOTP_LABEL))
                .secret(secret)
                .build();

        byte[] code;

        try {
            code = qrGenerator.generate(data);
        } catch (QrGenerationException e) {
            throw new RuntimeException(e);
        }

        try {
            return new TOTPData(ImageIO.read(new ByteArrayInputStream(code)), secret);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean verify(Integer code, String secret) {
        return verifier.isValidCode(secret, code.toString());
    }
}
