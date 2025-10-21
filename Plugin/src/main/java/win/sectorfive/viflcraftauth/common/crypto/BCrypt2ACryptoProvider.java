/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.crypto;

import at.favre.lib.crypto.bcrypt.BCrypt;
import win.sectorfive.viflcraftauth.api.crypto.CryptoProvider;
import win.sectorfive.viflcraftauth.api.crypto.HashedPassword;
import win.sectorfive.viflcraftauth.common.util.CryptoUtil;

import javax.annotation.Nullable;

public class BCrypt2ACryptoProvider implements CryptoProvider {

    public static final BCrypt.Hasher HASHER = BCrypt
            .with(BCrypt.Version.VERSION_2A);
    public static final BCrypt.Verifyer VERIFIER = BCrypt
            .verifyer(BCrypt.Version.VERSION_2A);

    @Override
    @Nullable
    public HashedPassword createHash(String password) {
        String hash;
        try {
            hash = HASHER.hashToString(10, password.toCharArray());
        } catch (IllegalArgumentException e) {
            return null;
        }
        return CryptoUtil.convertFromBCryptRaw(
                hash
        );
    }

    @Override
    public boolean matches(String input, HashedPassword password) {
        var raw = CryptoUtil.rawBcryptFromHashed(password).toCharArray();
        BCrypt.Result result;
        try {
            result = VERIFIER.verify(input.toCharArray(),
                    raw
            );
        } catch (IllegalArgumentException e) {
            return false;
        }

        return result.verified;
    }

    @Override
    public String getIdentifier() {
        return "BCrypt-2A";
    }

}
