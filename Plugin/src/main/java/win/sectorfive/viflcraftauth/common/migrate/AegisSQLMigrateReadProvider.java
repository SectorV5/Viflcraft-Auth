/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.migrate;

import win.sectorfive.viflcraftauth.api.Logger;
import win.sectorfive.viflcraftauth.api.crypto.HashedPassword;
import win.sectorfive.viflcraftauth.api.database.User;
import win.sectorfive.viflcraftauth.api.database.connector.SQLDatabaseConnector;
import win.sectorfive.viflcraftauth.common.database.AuthenticUser;
import win.sectorfive.viflcraftauth.common.util.CryptoUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class AegisSQLMigrateReadProvider extends SQLMigrateReadProvider {

    public AegisSQLMigrateReadProvider(String tableName, Logger logger, SQLDatabaseConnector connector) {
        super(tableName, logger, connector);
    }

    @Override
    public Collection<User> getAllUsers() {
        return connector.runQuery(connection -> {
            var ps = connection.prepareStatement("SELECT * FROM `%s`".formatted(tableName));

            var rs = ps.executeQuery();

            var users = new HashSet<User>();

            while (rs.next()) {

                try {
                    var uuid = UUID.fromString(rs.getString("uuid").replace(".", "")); //Aegis at it again, this time with a dot.
                    var onlineID = rs.getString("onlineId");
                    var nickname = rs.getString("name");
                    var passwordRaw = rs.getString("password");

                    HashedPassword password = null;

                    if (passwordRaw != null && !passwordRaw.contentEquals("")) { //God-damn Aegis.
                        if (passwordRaw.startsWith("$2a$")) {
                            password = CryptoUtil.convertFromBCryptRaw(passwordRaw);
                        } else {
                            logger.error("User " + nickname + " has an invalid password hash");
                        }
                    }

                    users.add(new AuthenticUser(uuid, onlineID == null || !rs.getBoolean("premium") ? null : UUID.fromString(onlineID.replace(".", "")), //Aegis at it again, this time with a dot.
                            password, nickname, Timestamp.valueOf(LocalDateTime.now()), null, null, null,
                            null,
                            null
                    ));

                } catch (Exception e) {
                    logger.error("Failed to read user from Aegis db, omitting");
                }

            }

            return users;
        });
    }
}
