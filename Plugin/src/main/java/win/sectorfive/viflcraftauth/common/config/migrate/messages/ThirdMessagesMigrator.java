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

public class ThirdMessagesMigrator implements ConfigurationMigrator {
    @Override
    public void migrate(ConfigurateHelper helper, Logger logger) {
        rename("kick-no-server", MessageKeys.KICK_NO_LOBBY, helper);
    }
}
