/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.config.migrate.messages;

import win.sectorfive.viflcraftauth.api.Logger;
import win.sectorfive.viflcraftauth.common.config.ConfigurateHelper;
import win.sectorfive.viflcraftauth.common.config.MessageKeys;
import win.sectorfive.viflcraftauth.common.config.migrate.ConfigurationMigrator;

public class SecondMessagesMigrator implements ConfigurationMigrator {
    @Override
    public void migrate(ConfigurateHelper helper, Logger logger) {
        // TOTP functionality has been removed in Viflcraft Auth
        // No migration needed for totp-show-info
        // Original migration code:
        // logger.warn("Sorry, but I've needed to reset the totp-show-info message because the process has significantly changed. Here is the original: " + helper.getString("totp-show-info"));
        // helper.set("totp-show-info", MessageKeys.TOTP_SHOW_INFO.defaultValue());
    }
}
