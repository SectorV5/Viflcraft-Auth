/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.config.migrate.config;

import win.sectorfive.viflcraftauth.api.Logger;
import win.sectorfive.viflcraftauth.common.config.ConfigurateHelper;
import win.sectorfive.viflcraftauth.common.config.migrate.ConfigurationMigrator;

import java.util.List;

public class FirstConfigurationMigrator implements ConfigurationMigrator {
    @Override
    public void migrate(ConfigurateHelper helper, Logger logger) {
        var limbo = helper.getString("limbo");

        helper.set("limbo", List.of(limbo));
    }
}
