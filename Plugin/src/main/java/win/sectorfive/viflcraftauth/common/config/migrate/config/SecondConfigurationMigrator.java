/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.config.migrate.config;

import win.sectorfive.viflcraftauth.api.Logger;
import win.sectorfive.viflcraftauth.common.config.ConfigurateHelper;
import win.sectorfive.viflcraftauth.common.config.migrate.ConfigurationMigrator;

public class SecondConfigurationMigrator implements ConfigurationMigrator {
    @Override
    public void migrate(ConfigurateHelper helper, Logger logger) {
        var list = helper.getStringList("allowed-commands-while-unauthorized");
        list.add("2faconfirm");
        helper.set("allowed-commands-while-unauthorized", list);
    }
}
